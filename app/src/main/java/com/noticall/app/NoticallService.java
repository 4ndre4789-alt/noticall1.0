package com.noticall.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NoticallService extends Service {
    public static final String ACTION_SEND_AUTO = "com.noticall.app.action.SEND_AUTO_SMS";
    private static final String CHANNEL_ID = "noticall_automation";
    private static final int NOTIFICATION_ID = 2404;
    private static volatile NoticallService instance;

    private ScheduledExecutorService scheduler;

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        NoticallPrefs.setServiceRunning(this, true);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SEND_AUTO.equals(intent.getAction())) {
            final String number = intent.getStringExtra("number");
            final String message = intent.getStringExtra("message");
            final int delaySeconds = Math.max(0, intent.getIntExtra("delay", 0));
            scheduleAutomaticSms(number, message, delaySeconds);
        }
        return START_STICKY;
    }

    public static void ensureRunning(Context context) {
        Intent i = new Intent(context, NoticallService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        } catch (Exception e) {
            new NoticallLogDb(context).add("ERROR", "", "Motore background non avviato: " + e.getClass().getSimpleName());
        }
    }

    public static void stopRunning(Context context) {
        context.stopService(new Intent(context, NoticallService.class));
    }

    public static void enqueue(Context context, String number, String message, int delaySeconds) {
        NoticallService live = instance;
        if (live != null) {
            live.scheduleAutomaticSms(number, message, delaySeconds);
            return;
        }

        // Prova a riavviare il motore. Se Android impedisce l'avvio da background,
        // privilegiamo l'affidabilità: inviamo subito invece di perdere il cliente.
        try {
            Intent i = new Intent(context, NoticallService.class);
            i.setAction(ACTION_SEND_AUTO);
            i.putExtra("number", number);
            i.putExtra("message", message);
            i.putExtra("delay", delaySeconds);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        } catch (Exception e) {
            NoticallLogDb db = new NoticallLogDb(context);
            db.add("FALLBACK", number, "Motore background non disponibile: invio immediato");
            SmsEngine.send(context, number, message, false);
        }
    }

    private void scheduleAutomaticSms(final String number, final String message, int delaySeconds) {
        if (number == null || number.trim().isEmpty() || message == null) return;

        final NoticallLogDb db = new NoticallLogDb(this);
        final int safeDelay = Math.max(0, Math.min(delaySeconds, 300));
        db.add("QUEUED", number, safeDelay == 0
                ? "Invio automatico immediato"
                : "Motore background: invio tra " + safeDelay + " secondi");

        final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Noticall:AutoSms");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire((safeDelay * 1000L) + 30000L);

        scheduler.schedule(() -> {
            try {
                db.add("SENDING", number, "Avvio SMS automatico dal motore background");
                SmsEngine.send(getApplicationContext(), number, message, false);
            } finally {
                if (wakeLock.isHeld()) wakeLock.release();
            }
        }, safeDelay, TimeUnit.SECONDS);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Automazione Noticall",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Mantiene attiva la risposta automatica alle chiamate perse");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setContentTitle("Noticall attivo")
                .setContentText("Risposta automatica alle chiamate perse pronta")
                .setSmallIcon(R.drawable.noticall_icon)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    @Override public void onDestroy() {
        instance = null;
        NoticallPrefs.setServiceRunning(this, false);
        if (scheduler != null) scheduler.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
