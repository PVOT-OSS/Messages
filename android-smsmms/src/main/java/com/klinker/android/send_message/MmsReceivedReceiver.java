/*
 * SPDX-FileCopyrightText: 2015 Jacob Klinker
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.klinker.android.send_message;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import com.android.mms.service_alt.DownloadRequest;
import com.android.mms.transaction.DownloadManager;
import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.NotificationInd;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduParser;
import com.google.android.mms.pdu_alt.PduPersister;
import com.google.android.mms.pdu_alt.RetrieveConf;
import com.google.android.mms.util_alt.SqliteWrapper;
import org.prauga.messages.carrier.CarrierMmsConfig;
import timber.log.Timber;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MmsReceivedReceiver extends BroadcastReceiver {
    public static final String MMS_RECEIVED = "com.klinker.android.messaging.MMS_RECEIVED";
    public static final String EXTRA_FILE_PATH = "file_path";
    public static final String EXTRA_LOCATION_URL = "location_url";
    public static final String EXTRA_TRIGGER_PUSH = "trigger_push";
    public static final String EXTRA_URI = "notification_ind_uri";
    public static final String EXTRA_SUBSCRIPTION_ID = "subscription_id";

    public static final int STATUS_PERMANENT_FAILURE = 0x87;

    private static final String LOCATION_SELECTION =
            Telephony.Mms.MESSAGE_TYPE + "=? AND " + Telephony.Mms.CONTENT_LOCATION + " =?";

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.v("MMS has finished downloading, persisting it to the database");

        final String location = intent.getStringExtra(EXTRA_LOCATION_URL);
        final Uri notificationUri = intent.getParcelableExtra(EXTRA_URI);
        final boolean byPush = intent.getBooleanExtra(EXTRA_TRIGGER_PUSH, false);
        final int subId = intent.getIntExtra(EXTRA_SUBSCRIPTION_ID, Utils.getDefaultSubscriptionId());
        final String path = intent.getStringExtra(EXTRA_FILE_PATH);

        MmsRetry retry = MmsSendResult.retryForError(getResultCode(),
                intent.getIntExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, 0),
                Utils.hasNoService(context, subId));
        Uri messageUri = null;

        if (retry == MmsRetry.NONE) {
            final byte[] response = readResponse(path);
            final boolean contentDisposition =
                    CarrierMmsConfig.get(context, subId).getSupportMmsContentDisposition();
            retry = retryForResponse(response, contentDisposition);
            storeRetrieveStatus(context, notificationUri, response, contentDisposition);

            if (retry == MmsRetry.NONE) {
                final NotificationInd notification = loadNotification(context, notificationUri);
                messageUri = DownloadRequest.persist(context, response, contentDisposition,
                        location, subId, null);

                if (messageUri == null) {
                    retry = MmsRetry.MANUAL;
                } else {
                    acknowledge(context, subId, byPush, notification, response,
                            contentDisposition, location);
                }
            }
        }
        deleteFile(path);

        Timber.v("MMS download result: " + retry);
        switch (retry) {
            case NEVER:
                markPermanentlyFailed(context, location);
                break;
            case MANUAL:
                if (byPush) {
                    NotificationInd notification = loadNotification(context, notificationUri);
                    if (notification != null && notification.getTransactionId() != null) {
                        MmsAcknowledger.sendNotifyResp(context, subId,
                                notification.getTransactionId(), PduHeaders.STATUS_DEFERRED,
                                location);
                    }
                }
                break;
            default:
                break;
        }

        DownloadManager.finishDownload(location);

        if (messageUri != null) {
            onMessageReceived(messageUri);
        } else if (retry != MmsRetry.NONE) {
            onDownloadFailed(notificationUri, subId, retry);
        }
    }

    static MmsRetry retryForResponse(byte[] response, boolean contentDisposition) {
        if (response == null || response.length == 0) {
            return MmsRetry.MANUAL;
        }

        final GenericPdu pdu;
        try {
            pdu = new PduParser(response, contentDisposition).parse();
        } catch (RuntimeException e) {
            Timber.w(e, "Unable to parse the downloaded MMS");
            return MmsRetry.NEVER;
        }

        if (!(pdu instanceof RetrieveConf)) {
            return MmsRetry.MANUAL;
        }

        final int status = ((RetrieveConf) pdu).getRetrieveStatus();
        if (status == 0 || status == PduHeaders.RETRIEVE_STATUS_OK) {
            return MmsRetry.NONE;
        }
        if (status >= PduHeaders.RETRIEVE_STATUS_ERROR_TRANSIENT_FAILURE
                && status < PduHeaders.RETRIEVE_STATUS_ERROR_PERMANENT_FAILURE) {
            return MmsRetry.AUTO;
        }
        return MmsRetry.NEVER;
    }

    private static void storeRetrieveStatus(Context context, Uri notificationUri, byte[] response,
                                            boolean contentDisposition) {
        if (notificationUri == null || response == null || response.length == 0) return;
        try {
            GenericPdu pdu = new PduParser(response, contentDisposition).parse();
            if (!(pdu instanceof RetrieveConf)) return;

            RetrieveConf conf = (RetrieveConf) pdu;
            ContentValues values = new ContentValues(2);
            values.put(Telephony.Mms.RETRIEVE_STATUS, conf.getRetrieveStatus());
            if (conf.getRetrieveText() != null) {
                values.put(Telephony.Mms.RETRIEVE_TEXT, conf.getRetrieveText().getString());
            }
            SqliteWrapper.update(context, context.getContentResolver(), notificationUri, values,
                    null, null);
        } catch (RuntimeException e) {
            Timber.w(e, "Unable to keep the retrieve status");
        }
    }

    private static void acknowledge(Context context, int subId, boolean byPush,
                                    NotificationInd notification, byte[] response,
                                    boolean contentDisposition, String location) {
        if (byPush) {
            if (notification != null && notification.getTransactionId() != null) {
                MmsAcknowledger.sendNotifyResp(context, subId, notification.getTransactionId(),
                        PduHeaders.STATUS_RETRIEVED, location);
            }
            return;
        }

        try {
            GenericPdu pdu = new PduParser(response, contentDisposition).parse();
            byte[] transactionId = pdu instanceof RetrieveConf
                    ? ((RetrieveConf) pdu).getTransactionId() : null;
            if (transactionId != null) {
                MmsAcknowledger.sendAcknowledge(context, subId, transactionId, location);
            }
        } catch (RuntimeException e) {
            Timber.w(e, "Unable to acknowledge MMS");
        }
    }

    private static NotificationInd loadNotification(Context context, Uri uri) {
        if (uri == null) return null;
        try {
            GenericPdu pdu = PduPersister.getPduPersister(context).load(uri);
            return pdu instanceof NotificationInd ? (NotificationInd) pdu : null;
        } catch (Exception e) {
            Timber.w(e, "Unable to load the MMS notification");
            return null;
        }
    }

    private static byte[] readResponse(String path) {
        if (path == null) return null;
        File file = new File(path);
        try (FileInputStream reader = new FileInputStream(file)) {
            final int length = (int) file.length();
            final byte[] response = new byte[length];
            int read = 0;
            while (read < length) {
                int count = reader.read(response, read, length - read);
                if (count < 0) break;
                read += count;
            }
            Timber.v("response length: " + read);
            return read == length ? response : null;
        } catch (IOException e) {
            Timber.e(e, "Unable to read the downloaded MMS");
            return null;
        }
    }

    private static void deleteFile(String path) {
        if (path != null) {
            //noinspection ResultOfMethodCallIgnored
            new File(path).delete();
        }
    }

    protected void onMessageReceived(Uri messageUri) {
        // Override this in a custom receiver, if you want access to the message, outside of the
        // internal SMS/MMS database
    }

    protected void onDownloadFailed(Uri notificationUri, int subId, MmsRetry retry) {

    }

    private static void markPermanentlyFailed(Context context, String location) {
        ContentValues values = new ContentValues(1);
        values.put(Telephony.Mms.STATUS, STATUS_PERMANENT_FAILURE);
        SqliteWrapper.update(context,
                context.getContentResolver(),
                Telephony.Mms.CONTENT_URI,
                values,
                LOCATION_SELECTION,
                new String[]{
                        Integer.toString(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND),
                        location
                });
    }
}
