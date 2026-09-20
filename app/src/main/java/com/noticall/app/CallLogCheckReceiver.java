package com.noticall.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

public class CallLogCheckReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        process(context, intent.getLongExtra("ringStart", 0L));
    }

    public static void process(Context context, long ringStart) {
        NoticallLogDb db = new NoticallLogDb(context);
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            db.add("ERROR", "", "Permesso registro chiamate non concesso");
            return;
        }

        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE},
                    null, null, CallLog.Calls.DATE + " DESC");
            if (c == null || !c.moveToFirst()) {
                db.add("ERROR", "", "Registro chiamate non disponibile");
                return;
            }

            String number = c.getString(0);
            int type = c.getInt(1);
            long date = c.getLong(2);

            boolean missed = type == CallLog.Calls.MISSED_TYPE;
            boolean rejected = type == CallLog.Calls.REJECTED_TYPE;
            if (!missed && !(rejected && NoticallPrefs.includeRejected(context))) return;
            if (ringStart > 0 && date < ringStart - 15000L) return;
            if (date <= NoticallPrefs.lastProcessedCall(context)) return;
            NoticallPrefs.setLastProcessedCall(context, date);

            if (number == null || number.trim().isEmpty() || number.equals("-1") || number.equals("-2")) {
                db.add("SKIPPED", "", "Numero privato o non disponibile");
                return;
            }

            db.add("MISSED", number, rejected ? "Chiamata rifiutata rilevata" : "Chiamata persa rilevata");

            if (!NoticallPrefs.enabled(context)) {
                db.add("SKIPPED", number, "Automazione disattivata");
                return;
            }
            if (!NoticallPrefs.isInsideTimeWindow(context)) {
                db.add("SKIPPED", number, "Fuori fascia oraria");
                return;
            }
            if (NoticallPrefs.isAntiSpamBlocked(context, number)) {
                db.add("SKIPPED", number, "Anti-spam: SMS già inviato/programmmato recentemente");
                return;
            }

            String message = NoticallPrefs.renderMessage(context, number);
            int delay = NoticallPrefs.delaySeconds(context);
            NoticallPrefs.markAutoScheduled(context, number);
            db.add("SCHEDULED", number, delay == 0
                    ? "SMS automatico pronto all'invio"
                    : "SMS programmato tra " + delay + " secondi");
            NoticallService.enqueue(context, number, message, delay);
        } catch (Exception e) {
            db.add("ERROR", "", "Errore lettura chiamata: " + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : " • " + e.getMessage()));
        } finally {
            if (c != null) c.close();
        }
    }
}
