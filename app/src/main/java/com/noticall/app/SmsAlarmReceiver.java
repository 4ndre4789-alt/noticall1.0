package com.noticall.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String number = intent.getStringExtra("number");
        String message = intent.getStringExtra("message");
        if (number == null || message == null) return;
        SmsEngine.send(context, number, message, false);
    }
}
