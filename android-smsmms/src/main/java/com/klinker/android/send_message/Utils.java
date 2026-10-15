package com.klinker.android.send_message;

import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

/**
 * Common methods for sending and receiving messages
 *
 * @author Jake Klinker
 */
public class Utils {

    /**
     * Gets the current users phone number
     *
     * @param context is the context of the activity or service
     * @return a string of the phone number on the device
     */
    public static String getMyPhoneNumber(Context context) {
        return getMyPhoneNumber(context, -1);
    }

    public static String getMyPhoneNumber(Context context, int subId) {
        try {
            TelephonyManager telephony = context.getSystemService(TelephonyManager.class);
            if (telephony == null) return null;
            int resolved = CarrierMmsConfig.resolveSubscriptionId(subId);
            if (resolved >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                telephony = telephony.createForSubscriptionId(resolved);
            }
            return telephony.getLine1Number();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static int getDefaultSubscriptionId() {
        return SmsManager.getDefaultSmsSubscriptionId();
    }
}
