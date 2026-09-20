package com.noticall.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneStateReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state == null) return;

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            NoticallPrefs.setRinging(context, true);
            NoticallPrefs.setAnswered(context, false);
            NoticallPrefs.setRingStart(context, System.currentTimeMillis());
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            if (NoticallPrefs.ringing(context)) NoticallPrefs.setAnswered(context, true);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            boolean wasRinging = NoticallPrefs.ringing(context);
            boolean answered = NoticallPrefs.answered(context);
            long ringStart = NoticallPrefs.ringStart(context);
            NoticallPrefs.setRinging(context, false);
            NoticallPrefs.setAnswered(context, false);

            if (wasRinging && !answered) {
                final PendingResult pending = goAsync();
                final Context app = context.getApplicationContext();
                new Thread(() -> {
                    try {
                        // Il registro chiamate può aggiornarsi un istante dopo lo stato IDLE.
                        Thread.sleep(1500L);
                        CallLogCheckReceiver.process(app, ringStart);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        pending.finish();
                    }
                }, "Noticall-CallLogCheck").start();
            }
        }
    }
}
