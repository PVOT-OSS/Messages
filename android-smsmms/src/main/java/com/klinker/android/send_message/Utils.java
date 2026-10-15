package com.klinker.android.send_message;

import android.content.Context;
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
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getLine1Number();
    }

    public static int getDefaultSubscriptionId() {
        return SmsManager.getDefaultSmsSubscriptionId();
    }
}
