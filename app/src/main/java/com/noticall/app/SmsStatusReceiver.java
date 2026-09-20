package com.noticall.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class SmsStatusReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String number = intent.getStringExtra("number");
        boolean test = intent.getBooleanExtra("test", false);
        int part = intent.getIntExtra("part", 1);
        int total = intent.getIntExtra("total", 1);
        int subId = intent.getIntExtra("subId", -1);
        NoticallLogDb db = new NoticallLogDb(context);

        int code = getResultCode();
        if (code == Activity.RESULT_OK) {
            String suffix = total > 1 ? " • parte " + part + "/" + total : "";
            db.add("SENT", number, (test ? "SMS di prova inviato" : "SMS automatico inviato")
                    + suffix + (subId >= 0 ? " • SIM " + subId : ""));
        } else {
            db.add("ERROR", number, "SMS fallito: " + describe(code)
                    + " (codice " + code + ")" + (subId >= 0 ? " • SIM " + subId : ""));
        }
    }

    private String describe(int code) {
        switch (code) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE: return "errore generico modem/rete";
            case SmsManager.RESULT_ERROR_RADIO_OFF: return "radio/telefono disattivato";
            case SmsManager.RESULT_ERROR_NULL_PDU: return "PDU non disponibile";
            case SmsManager.RESULT_ERROR_NO_SERVICE: return "nessun servizio cellulare";
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED: return "limite SMS superato";
            case SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED: return "short code non consentito";
            case SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED: return "short code bloccato";
            default: return "errore Android/operatore";
        }
    }
}
