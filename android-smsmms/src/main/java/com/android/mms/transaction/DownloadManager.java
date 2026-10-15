package com.android.mms.transaction;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import com.klinker.android.send_message.BroadcastUtils;
import org.prauga.messages.carrier.CarrierMmsConfig;
import org.prauga.messages.carrier.MmsConfigOverrides;
import com.klinker.android.send_message.MmsReceivedReceiver;
import com.klinker.android.send_message.SmsManagerFactory;

import timber.log.Timber;

import java.io.File;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DownloadManager {
    private static final DownloadManager ourInstance = new DownloadManager();
    private static final Set<String> inProgress = ConcurrentHashMap.newKeySet();

    public static DownloadManager getInstance() {
        return ourInstance;
    }

    private DownloadManager() {

    }

    public void downloadMultimediaMessage(final Context context, final String location, Uri uri, boolean byPush, int subscriptionId) {
        if (location == null || !inProgress.add(location)) {
            return;
        }

        // TransactionService can keep uri and location in memory while SmsManager download Mms.
        if (!isNotificationExist(context, location)) {
            finishDownload(location);
            return;
        }

        Timber.v("receiving with system method");
        final String fileName = "download." + Math.abs(new Random().nextLong()) + ".dat";
        File mDownloadFile = new File(context.getCacheDir(), fileName);
        Uri contentUri = (new Uri.Builder())
                .authority(context.getPackageName() + ".MmsFileProvider")
                .path(fileName)
                .scheme(ContentResolver.SCHEME_CONTENT)
                .build();

        Intent download = new Intent(MmsReceivedReceiver.MMS_RECEIVED);
        BroadcastUtils.addClassName(context, download, MmsReceivedReceiver.MMS_RECEIVED);
        download.putExtra(MmsReceivedReceiver.EXTRA_FILE_PATH, mDownloadFile.getPath());
        download.putExtra(MmsReceivedReceiver.EXTRA_LOCATION_URL, location);
        download.putExtra(MmsReceivedReceiver.EXTRA_TRIGGER_PUSH, byPush);
        download.putExtra(MmsReceivedReceiver.EXTRA_URI, uri);
        download.putExtra(MmsReceivedReceiver.EXTRA_SUBSCRIPTION_ID, subscriptionId);

        final int mutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, location.hashCode(), download, PendingIntent.FLAG_UPDATE_CURRENT | mutable);

        final SmsManager smsManager = SmsManagerFactory.INSTANCE.createSmsManager(
                CarrierMmsConfig.resolveSubscriptionId(subscriptionId));
        Bundle configOverrides = MmsConfigOverrides.create(context, subscriptionId);

        try {
            smsManager.downloadMultimediaMessage(context, location, contentUri, configOverrides, pendingIntent);
        } catch (RuntimeException e) {
            Timber.e(e, "Unable to start MMS download");
            finishDownload(location);
            try {
                pendingIntent.send(SmsManager.MMS_ERROR_UNSPECIFIED);
            } catch (PendingIntent.CanceledException canceled) {
                Timber.e(canceled, "Unable to report the failed download");
            }
        }
    }

    public static void finishDownload(String location) {
        if (location != null) {
            inProgress.remove(location);
        }
    }

    private static boolean isNotificationExist(Context context, String location) {
        String selection = Telephony.Mms.CONTENT_LOCATION + " = ?";
        String[] selectionArgs = new String[] { location };
        Cursor c = SqliteWrapper.query(
                context, context.getContentResolver(),
                Telephony.Mms.CONTENT_URI, new String[] { Telephony.Mms._ID },
                selection, selectionArgs, null);
        if (c != null) {
            try {
                if (c.getCount() > 0) {
                    return true;
                }
            } finally {
                c.close();
            }
        }

        return false;
    }
}
