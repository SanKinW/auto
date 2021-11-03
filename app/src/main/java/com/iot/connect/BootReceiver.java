package com.iot.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Amber on 2016/8/23.
 */
public class BootReceiver extends BroadcastReceiver {
    private final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            Intent intent2 = new Intent(context, MainActivity.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent2);
        }
    }
}