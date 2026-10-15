/*
 * SPDX-FileCopyrightText: 2014 Jacob Klinker
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.transaction;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;
import android.telephony.TelephonyManager;

import com.android.mms.service_alt.SubscriptionIdChecker;
import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.NotificationInd;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduParser;
import com.google.android.mms.pdu_alt.PduPersister;
import org.prauga.messages.carrier.CarrierMmsConfig;
import com.klinker.android.send_message.MmsAcknowledger;
import com.klinker.android.send_message.MmsReceivedReceiver;
import com.klinker.android.send_message.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

import static android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;
import static com.google.android.mms.pdu_alt.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;

/**
 * Receives Intent.WAP_PUSH_RECEIVED_ACTION intents and starts the
 * TransactionService by passing the push-data to it.
 */
public class PushReceiver extends BroadcastReceiver {

    static final String[] PROJECTION = new String[]{Mms.CONTENT_LOCATION, Mms.LOCKED};

    static final int COLUMN_CONTENT_LOCATION = 0;

    private static final long DUPLICATE_WINDOW_SECONDS = 7 * 24 * 60 * 60;

    private static final ExecutorService PUSH_RECEIVER_EXECUTOR = Executors.newSingleThreadExecutor();

    private class ReceivePushTask extends AsyncTask<Intent, Void, Void> {
        private Context mContext;
        private PendingResult pendingResult;

        private ReceivePushTask(Context context, PendingResult pendingResult) {
            mContext = context;
            this.pendingResult = pendingResult;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            Timber.v("receiving a new mms message");
            Intent intent = intents[0];

            int subId = intent.getIntExtra("subscription", Utils.getDefaultSubscriptionId());
            CarrierMmsConfig config = CarrierMmsConfig.get(mContext, subId);

            // Get raw PDU push-data from the message and parse it
            byte[] pushData = intent.getByteArrayExtra("data");
            PduParser parser = new PduParser(pushData, config.getSupportMmsContentDisposition());
            GenericPdu pdu = parser.parse();

            if (pdu == null) {
                Timber.e("Invalid PUSH data");
                return null;
            }

            PduPersister p = PduPersister.getPduPersister(mContext);
            int type = pdu.getMessageType();

            try {
                switch (type) {
                    case MESSAGE_TYPE_NOTIFICATION_IND: {
                        NotificationInd nInd = (NotificationInd) pdu;

                        if (config.getTransactionIdEnabled()) {
                            byte[] contentLocation = nInd.getContentLocation();
                            if (contentLocation != null && contentLocation.length > 0
                                    && '=' == contentLocation[contentLocation.length - 1]) {
                                byte[] transactionId = nInd.getTransactionId();
                                byte[] contentLocationWithId = new byte[contentLocation.length
                                        + transactionId.length];
                                System.arraycopy(contentLocation, 0, contentLocationWithId,
                                        0, contentLocation.length);
                                System.arraycopy(transactionId, 0, contentLocationWithId,
                                        contentLocation.length, transactionId.length);
                                nInd.setContentLocation(contentLocationWithId);
                            }
                        }

                        if (!isDuplicateNotification(mContext, nInd)) {
                            // Save the pdu. If we can start downloading the real pdu immediately,
                            // don't allow persist() to create a thread for the notificationInd
                            // because it causes UI jank.
                            Uri uri = p.persist(pdu, Inbox.CONTENT_URI,
                                    PduPersister.DUMMY_THREAD_ID, true, true, null);
                            setSubscriptionId(mContext, uri, subId);

                            String location = getContentLocation(mContext, uri);

                            String from = senderAddress(nInd);
                            if (shouldAutoDownload(mContext, subId, config, from)) {
                                DownloadManager.getInstance().downloadMultimediaMessage(mContext, location, uri, true, subId);
                            } else {
                                Timber.v("Not downloading MMS automatically");
                                if (nInd.getTransactionId() != null) {
                                    MmsAcknowledger.sendNotifyResp(mContext, subId,
                                            nInd.getTransactionId(), PduHeaders.STATUS_DEFERRED,
                                            location);
                                }
                                onNotificationReceived(mContext, uri, subId);
                            }
                        } else {
                            Timber.v("Skip downloading duplicate message: " + new String(nInd.getContentLocation()));
                        }
                        break;
                    }
                    default:
                        Timber.v("Received unsupported WAP Push of type " + type);
                }
            } catch (MmsException e) {
                Timber.e(e, "Failed to save the data from PUSH: type=" + type);
            } catch (RuntimeException e) {
                Timber.e(e, "Unexpected RuntimeException.");
            }

            Timber.v("PUSH Intent processed.");

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            pendingResult.finish();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.v(intent.getAction() + " " + intent.getType());
        if ((intent.getAction().equals(WAP_PUSH_DELIVER_ACTION) || intent.getAction().equals(WAP_PUSH_RECEIVED_ACTION))
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            Timber.v("Received PUSH Intent: " + intent);

            new ReceivePushTask(context, goAsync()).executeOnExecutor(PUSH_RECEIVER_EXECUTOR, intent);

            Timber.v(context.getPackageName() + " received and aborted");
        }
    }

    protected boolean shouldAutoDownload(Context context, int subId, CarrierMmsConfig config,
                                         String from) {
        return config.getAutoRetrieveEnabled()
                && (!isRoaming(context, subId) || config.getRoamingAutoRetrieveEnabled());
    }

    static String senderAddress(NotificationInd notification) {
        if (notification.getFrom() == null) return null;
        String from = notification.getFrom().getString();
        int type = from.indexOf("/TYPE=");
        return type >= 0 ? from.substring(0, type) : from;
    }

    protected void onNotificationReceived(Context context, Uri notificationUri, int subId) {
    }

    public static boolean isRoaming(Context context, int subId) {
        try {
            TelephonyManager telephony = context.getSystemService(TelephonyManager.class);
            if (telephony == null) return false;
            int resolved = CarrierMmsConfig.resolveSubscriptionId(subId);
            if (resolved >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                telephony = telephony.createForSubscriptionId(resolved);
            }
            return telephony.isNetworkRoaming();
        } catch (RuntimeException e) {
            Timber.w(e, "Unable to tell if we're roaming");
            return false;
        }
    }

    private static void setSubscriptionId(Context context, Uri uri, int subId) {
        if (subId < 0 || !SubscriptionIdChecker.getInstance(context).canUseSubscriptionId()) {
            return;
        }

        try {
            ContentValues values = new ContentValues(1);
            values.put(Mms.SUBSCRIPTION_ID, subId);
            context.getContentResolver().update(uri, values, null, null);
        } catch (RuntimeException e) {
            Timber.w(e, "Unable to store the subscription of the MMS notification");
        }
    }

    public static String getContentLocation(Context context, Uri uri) throws MmsException {
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                uri, PROJECTION, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    String location = cursor.getString(COLUMN_CONTENT_LOCATION);
                    cursor.close();
                    return location;
                }
            } finally {
                cursor.close();
            }
        }

        throw new MmsException("Cannot get X-Mms-Content-Location from: " + uri);
    }

    private static boolean isDuplicateNotification(Context context, NotificationInd nInd) {
        byte[] rawLocation = nInd.getContentLocation();
        if (rawLocation == null) {
            return false;
        }

        long nowSeconds = System.currentTimeMillis() / 1000;
        String selection = "(" + Mms.MESSAGE_TYPE + " != ? OR " + Mms.EXPIRY + " > ?)"
                + " AND " + Mms.DATE + " > ? AND " + Mms.DATE + " < ?"
                + " AND " + Mms.CONTENT_LOCATION + " = ?";
        String[] selectionArgs = new String[]{
                String.valueOf(MESSAGE_TYPE_NOTIFICATION_IND),
                String.valueOf(nowSeconds),
                String.valueOf(nowSeconds - DUPLICATE_WINDOW_SECONDS),
                String.valueOf(nowSeconds + DUPLICATE_WINDOW_SECONDS),
                new String(rawLocation)
        };

        Cursor cursor = SqliteWrapper.query(
                context, context.getContentResolver(),
                Mms.CONTENT_URI, new String[]{Mms._ID},
                selection, selectionArgs, null);
        if (cursor == null) {
            return false;
        }
        try {
            return cursor.getCount() > 0;
        } finally {
            cursor.close();
        }
    }
}
