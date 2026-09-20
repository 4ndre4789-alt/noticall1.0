package com.noticall.app;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;
import java.util.List;

public final class SmsEngine {

    public static boolean send(Context context, String number, String message, boolean isTest) {
        NoticallLogDb db = new NoticallLogDb(context);

        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            db.add("ERROR", number, "Permesso SEND_SMS non concesso");
            return false;
        }
        // Non blocchiamo l'invio in base a FEATURE_TELEPHONY_MESSAGING.
        // Alcuni firmware/OEM possono riportare questo feature flag in modo non affidabile
        // anche quando SmsManager e la SIM inviano correttamente gli SMS.
        // L'invio reale tramite SmsManager è la verifica autorevole.
        if (number == null || number.trim().isEmpty()) {
            db.add("ERROR", "", "Numero SMS non valido");
            return false;
        }

        try {
            ResolvedSms resolved = resolveSmsManager(context);
            SmsManager sms = resolved.manager;
            int subId = resolved.subscriptionId;

            ArrayList<String> parts = sms.divideMessage(message == null ? "" : message);
            if (parts == null || parts.isEmpty()) {
                parts = new ArrayList<>();
                parts.add(message == null ? "" : message);
            }

            String batchId = String.valueOf(System.currentTimeMillis());
            if (parts.size() == 1) {
                PendingIntent sentPi = sentPendingIntent(context, number, isTest, subId, batchId, 1, 1);
                sms.sendTextMessage(number, null, parts.get(0), sentPi, null);
            } else {
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                for (int i = 0; i < parts.size(); i++) {
                    sentIntents.add(sentPendingIntent(context, number, isTest, subId, batchId, i + 1, parts.size()));
                }
                sms.sendMultipartTextMessage(number, null, parts, sentIntents, null);
            }

            String simText = subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                    ? "SIM automatica"
                    : "SIM subscriptionId=" + subId;
            db.add(isTest ? "TEST" : "SENDING", number,
                    (isTest ? "SMS di prova affidato al sistema" : "SMS automatico affidato al sistema")
                            + " • " + simText + " • " + parts.size() + " parte/i");
            return true;
        } catch (SecurityException e) {
            db.add("ERROR", number, "Android ha bloccato SEND_SMS: SecurityException");
            return false;
        } catch (UnsupportedOperationException e) {
            db.add("ERROR", number, "SMS non supportati: UnsupportedOperationException");
            return false;
        } catch (Exception e) {
            db.add("ERROR", number, "Errore invio SMS: " + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : " • " + e.getMessage()));
            return false;
        }
    }

    private static PendingIntent sentPendingIntent(Context context, String number, boolean isTest,
                                                   int subId, String batchId, int part, int total) {
        Intent sent = new Intent(context, SmsStatusReceiver.class);
        sent.putExtra("number", number);
        sent.putExtra("test", isTest);
        sent.putExtra("subId", subId);
        sent.putExtra("batchId", batchId);
        sent.putExtra("part", part);
        sent.putExtra("total", total);
        int requestCode = (int) ((System.nanoTime() + part) & 0x7fffffff);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                sent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static ResolvedSms resolveSmsManager(Context context) {
        int subId = SubscriptionManager.getDefaultSmsSubscriptionId();

        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
                if (sm != null) {
                    List<SubscriptionInfo> active = sm.getActiveSubscriptionInfoList();
                    if (active != null && !active.isEmpty()) {
                        subId = active.get(0).getSubscriptionId();
                    }
                }
            } catch (Exception ignored) { }
        }

        SmsManager manager;
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SmsManager base = context.getSystemService(SmsManager.class);
                if (base == null) throw new IllegalStateException("SmsManager non disponibile");
                manager = base.createForSubscriptionId(subId);
            } else {
                manager = SmsManager.getSmsManagerForSubscriptionId(subId);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                manager = context.getSystemService(SmsManager.class);
                if (manager == null) throw new IllegalStateException("SmsManager non disponibile");
            } else {
                manager = SmsManager.getDefault();
            }
        }
        return new ResolvedSms(manager, subId);
    }

    private static final class ResolvedSms {
        final SmsManager manager;
        final int subscriptionId;
        ResolvedSms(SmsManager manager, int subscriptionId) {
            this.manager = manager;
            this.subscriptionId = subscriptionId;
        }
    }

    private SmsEngine() { }
}
