/*
 * Copyright (C) 2015 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.util_alt.SqliteWrapper;
import timber.log.Timber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

        String path = intent.getStringExtra(EXTRA_FILE_PATH);
        Timber.v(path);

        FileInputStream reader = null;
        Uri messageUri = null;
        String errorMessage = null;

        try {
            File mDownloadFile = new File(path);
            final int nBytes = (int) mDownloadFile.length();
            reader = new FileInputStream(mDownloadFile);
            final byte[] response = new byte[nBytes];
            reader.read(response, 0, nBytes);

            final int subId = intent.getIntExtra(EXTRA_SUBSCRIPTION_ID, Utils.getDefaultSubscriptionId());
            messageUri = DownloadRequest.persist(context, response,
                    CarrierMmsConfig.get(context, subId).getSupportMmsContentDisposition(),
                    intent.getStringExtra(EXTRA_LOCATION_URL),
                    subId, null);

            Timber.v("response saved successfully");
            Timber.v("response length: " + response.length);
            mDownloadFile.delete();
        } catch (FileNotFoundException e) {
            errorMessage = "MMS received, file not found exception";
            Timber.e(e, errorMessage);
        } catch (IOException e) {
            errorMessage = "MMS received, io exception";
            Timber.e(e, errorMessage);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    errorMessage = "MMS received, io exception";
                    Timber.e(e, "MMS received, io exception");
                }
            }
        }

        handleHttpError(context, intent);
        DownloadManager.finishDownload(intent.getStringExtra(EXTRA_LOCATION_URL));

        if (messageUri != null) {
            onMessageReceived(messageUri);
        }

        if (errorMessage != null) {
            onError(errorMessage);
        }
    }

    protected void onMessageReceived(Uri messageUri) {
        // Override this in a custom receiver, if you want access to the message, outside of the
        // internal SMS/MMS database
    }

    protected void onError(String error) {

    }

    private void handleHttpError(Context context, Intent intent) {
        final int httpError = intent.getIntExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, 0);
        if (httpError == 404) {
            ContentValues values = new ContentValues(1);
            values.put(Telephony.Mms.STATUS, STATUS_PERMANENT_FAILURE);
            SqliteWrapper.update(context,
                    context.getContentResolver(),
                    Telephony.Mms.CONTENT_URI,
                    values,
                    LOCATION_SELECTION,
                    new String[]{
                            Integer.toString(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND),
                            intent.getStringExtra(EXTRA_LOCATION_URL)
                    });
        }
    }
}
