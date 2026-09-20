package com.noticall.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if ((Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))
                && NoticallPrefs.enabled(context)) {
            NoticallService.ensureRunning(context);
            new NoticallLogDb(context).add("SYSTEM", "", "Noticall riattivato automaticamente");
        }
    }
}
