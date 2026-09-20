package com.noticall.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 9001;
    private static final int BG = Color.rgb(7,17,31);
    private static final int SURFACE = Color.rgb(13,27,42);
    private static final int SURFACE_2 = Color.rgb(17,35,54);
    private static final int TEXT = Color.rgb(245,250,255);
    private static final int MUTED = Color.rgb(151,173,192);
    private static final int CYAN = Color.rgb(54,227,232);
    private static final int TEAL = Color.rgb(25,199,163);
    private static final int RED = Color.rgb(255,108,117);

    private FrameLayout content;
    private Button tabHome, tabActivity, tabSettings;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        setContentView(buildShell());
        if (NoticallPrefs.enabled(this)) NoticallService.ensureRunning(this);
        if (!deviceReady()) showDeviceSetup();
        else showHome();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null && content.getChildCount() > 0) {
            Object tag = content.getTag();
            if ("home".equals(tag)) showHome();
            else if ("activity".equals(tag)) showActivity();
            else if ("device_setup".equals(tag)) showDeviceSetup();
        }
    }

    private View buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(10));

        ImageView icon = new ImageView(this);
        icon.setImageResource(com.noticall.app.R.drawable.noticall_icon);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(54), dp(54));
        ip.setMargins(0,0,dp(12),0);
        header.addView(icon, ip);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Noticall", 26, TEXT, true);
        TextView subtitle = text("Chiamate perse. Opportunità recuperate.", 12, MUTED, false);
        brand.addView(title);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(12), dp(8), dp(12), dp(12));
        nav.setBackgroundColor(Color.rgb(8,20,34));
        tabHome = navButton("HOME", v -> showHome());
        tabActivity = navButton("ATTIVITÀ", v -> showActivity());
        tabSettings = navButton("IMPOSTAZIONI", v -> showSettings());
        nav.addView(tabHome, new LinearLayout.LayoutParams(0, dp(50), 1f));
        nav.addView(tabActivity, new LinearLayout.LayoutParams(0, dp(50), 1f));
        nav.addView(tabSettings, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(nav);
        return root;
    }

    private void showHome() {
        content.removeAllViews();
        content.setTag("home");
        setActiveTab(tabHome);

        LinearLayout body = page();
        body.addView(sectionTitle("Stato"));

        LinearLayout status = cardGradient();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        TextView statusTitle = text(NoticallPrefs.enabled(this) ? "Automazione attiva" : "Automazione disattivata", 19, TEXT, true);
        TextView statusSub = text(NoticallPrefs.enabled(this) ? "Noticall è pronto a rispondere alle chiamate perse." : "Attivala quando vuoi iniziare.", 13, MUTED, false);
        txt.addView(statusTitle);
        txt.addView(statusSub);
        row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(NoticallPrefs.enabled(this));
        tintSwitch(sw);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            NoticallPrefs.setEnabled(this, isChecked);
            if (isChecked) NoticallService.ensureRunning(this);
            else NoticallService.stopRunning(this);
            Toast.makeText(this, isChecked ? "Noticall attivo anche a schermo bloccato" : "Noticall disattivato", Toast.LENGTH_SHORT).show();
            showHome();
        });
        row.addView(sw);
        status.addView(row);
        body.addView(status, cardLp());

        body.addView(sectionTitle("Dispositivo"));
        LinearLayout device = card();
        device.addView(text(deviceReady() ? "✓ Dispositivo ottimizzato" : "Configurazione dispositivo richiesta", 15, deviceReady() ? TEAL : RED, true));
        TextView deviceInfo = text(deviceReady()
                ? "Noticall ha le autorizzazioni principali per lavorare anche quando non usi il telefono."
                : "Completa la configurazione guidata per evitare che Android sospenda l'automazione.", 12, MUTED, false);
        deviceInfo.setPadding(0, dp(6), 0, dp(12));
        device.addView(deviceInfo);
        Button deviceButton = primaryButton(deviceReady() ? "VERIFICA CONFIGURAZIONE" : "CONFIGURA DISPOSITIVO");
        deviceButton.setOnClickListener(v -> showDeviceSetup());
        device.addView(deviceButton);
        body.addView(device, cardLp());

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        NoticallLogDb db = new NoticallLogDb(this);
        stats.addView(statCard(String.valueOf(db.countToday("MISSED")), "Perse oggi"), new LinearLayout.LayoutParams(0, dp(100), 1f));
        LinearLayout.LayoutParams gapStat = new LinearLayout.LayoutParams(0, dp(100), 1f);
        gapStat.setMargins(dp(10),0,0,0);
        stats.addView(statCard(String.valueOf(db.countToday("SENT")), "SMS inviati"), gapStat);
        body.addView(stats, cardLp());

        body.addView(sectionTitle("Permessi"));
        LinearLayout permissions = card();
        TextView pState = text(permissionSummary(), 14, hasAllPermissions() ? TEAL : RED, true);
        permissions.addView(pState);
        TextView pInfo = text("Telefono + registro chiamate + SMS sono necessari per la prova reale.", 12, MUTED, false);
        pInfo.setPadding(0,dp(6),0,dp(12));
        permissions.addView(pInfo);
        Button grant = primaryButton(hasAllPermissions() ? "PERMESSI OK" : "CONCEDI PERMESSI");
        grant.setEnabled(!hasAllPermissions());
        grant.setOnClickListener(v -> requestNoticallPermissions());
        permissions.addView(grant);
        body.addView(permissions, cardLp());

        body.addView(sectionTitle("Anteprima SMS"));
        LinearLayout preview = card();
        TextView msg = text(NoticallPrefs.renderMessage(this, "+39 333 123 4567"), 15, TEXT, false);
        msg.setLineSpacing(0,1.15f);
        preview.addView(msg);
        body.addView(preview, cardLp());

        Button test = primaryButton("INVIA SMS DI PROVA");
        test.setOnClickListener(v -> showTestSmsDialog());
        LinearLayout.LayoutParams testLp = cardLp();
        testLp.setMargins(dp(18), dp(10), dp(18), dp(24));
        body.addView(test, testLp);

        putPage(body);
    }

    private void showDeviceSetup() {
        content.removeAllViews();
        content.setTag("device_setup");
        setActiveTab(null);

        LinearLayout body = page();
        TextView hero = text("Configura Noticall", 24, TEXT, true);
        hero.setPadding(dp(2), dp(8), 0, dp(4));
        body.addView(hero);
        TextView intro = text("Un controllo guidato per permettere a Noticall di lavorare anche con schermo spento e app non aperta.", 13, MUTED, false);
        intro.setPadding(dp(2), 0, 0, dp(14));
        body.addView(intro);

        LinearLayout checklist = cardGradient();
        checklist.addView(setupRow("Telefono, registro e SMS", hasAllPermissions(), "Necessari per rilevare la chiamata persa e inviare la risposta."));
        checklist.addView(setupRow("Notifiche servizio", notificationsReady(), "Mantiene visibile lo stato dell'automazione."));
        checklist.addView(setupRow("Batteria senza ottimizzazione", batteryReady(), "Evita che Android sospenda Noticall durante l'inattività."));
        if (isSamsung()) {
            checklist.addView(setupRow("Samsung: mai in sospensione", NoticallPrefs.samsungSetupConfirmed(this), "Aggiungi Noticall alle app che Samsung non deve sospendere."));
        }
        checklist.addView(setupRow("Motore background", NoticallPrefs.enabled(this), "Si avvia automaticamente al termine della configurazione."));
        body.addView(checklist, cardLp());

        boolean fullSetup = deviceReady() && NoticallPrefs.enabled(this);
        Button next = primaryButton(fullSetup ? "CONFIGURAZIONE COMPLETA ✓" : nextSetupLabel());
        next.setOnClickListener(v -> runNextSetupStep());
        body.addView(next, cardLp());

        if (isSamsung() && NoticallPrefs.samsungSetupOpened(this) && !NoticallPrefs.samsungSetupConfirmed(this)) {
            Button confirmed = smallButton("HO AGGIUNTO NOTICALL A “MAI IN SOSPENSIONE”");
            confirmed.setOnClickListener(v -> {
                NoticallPrefs.setSamsungSetupConfirmed(this, true);
                completeSetupIfReady();
                showDeviceSetup();
            });
            LinearLayout.LayoutParams cp = cardLp();
            cp.setMargins(dp(18), dp(0), dp(18), dp(12));
            body.addView(confirmed, cp);
        }

        LinearLayout why = card();
        why.addView(text("Perché serve?", 15, TEXT, true));
        TextView wt = text("Android e alcuni produttori possono limitare le app in background. Noticall non modifica queste protezioni di nascosto: ti porta direttamente alle schermate ufficiali e controlla cosa è già attivo.", 12, MUTED, false);
        wt.setPadding(0, dp(6), 0, 0);
        why.addView(wt);
        body.addView(why, cardLp());

        if (deviceReady() && NoticallPrefs.enabled(this)) {
            Button done = primaryButton("VAI ALLA HOME");
            done.setOnClickListener(v -> showHome());
            body.addView(done, cardLp());
        }

        putPage(body);
    }

    private LinearLayout setupRow(String title, boolean ok, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));
        TextView mark = text(ok ? "✓" : "!", 20, ok ? TEAL : RED, true);
        mark.setGravity(Gravity.CENTER);
        row.addView(mark, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(title, 14, TEXT, true));
        txt.addView(text(subtitle, 11, MUTED, false));
        row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private String nextSetupLabel() {
        if (!hasAllPermissions()) return "1. CONCEDI PERMESSI";
        if (!notificationsReady()) return "2. ATTIVA NOTIFICHE";
        if (!batteryReady()) return "3. DISATTIVA OTTIMIZZAZIONE BATTERIA";
        if (isSamsung() && !NoticallPrefs.samsungSetupConfirmed(this)) return "4. IMPOSTA “MAI IN SOSPENSIONE”";
        if (!NoticallPrefs.enabled(this)) return "ATTIVA NOTICALL";
        return "CONFIGURAZIONE COMPLETA ✓";
    }

    private void runNextSetupStep() {
        if (!hasAllPermissions()) {
            requestNoticallPermissions();
            return;
        }
        if (!notificationsReady()) {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9002);
            } else {
                openAppDetails();
            }
            return;
        }
        if (!batteryReady()) {
            requestBatteryExemption();
            return;
        }
        if (isSamsung() && !NoticallPrefs.samsungSetupConfirmed(this)) {
            openSamsungNeverSleeping();
            return;
        }
        if (!NoticallPrefs.enabled(this)) {
            NoticallPrefs.setEnabled(this, true);
            NoticallService.ensureRunning(this);
            Toast.makeText(this, "Noticall attivo", Toast.LENGTH_SHORT).show();
        }
        completeSetupIfReady();
        showDeviceSetup();
    }

    private void completeSetupIfReady() {
        if (deviceReady()) {
            NoticallPrefs.setSetupCompleted(this, true);
            if (!NoticallPrefs.enabled(this)) NoticallPrefs.setEnabled(this, true);
            NoticallService.ensureRunning(this);
        }
    }

    private boolean deviceReady() {
        return hasAllPermissions()
                && notificationsReady()
                && batteryReady()
                && (!isSamsung() || NoticallPrefs.samsungSetupConfirmed(this));
    }

    private boolean notificationsReady() {
        if (Build.VERSION.SDK_INT < 33) return true;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        return nm == null || nm.areNotificationsEnabled();
    }

    private boolean batteryReady() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private boolean isSamsung() {
        return Build.MANUFACTURER != null && Build.MANUFACTURER.toLowerCase(Locale.ROOT).contains("samsung");
    }

    private void requestBatteryExemption() {
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {
                openAppDetails();
            }
        }
    }

    private void openSamsungNeverSleeping() {
        NoticallPrefs.setSamsungSetupOpened(this, true);
        try {
            Intent intent = new Intent();
            intent.setAction("com.samsung.android.sm.ACTION_OPEN_CHECKABLE_LISTACTIVITY");
            intent.setPackage("com.samsung.android.lool");
            intent.putExtra("activity_type", 2);
            startActivity(intent);
        } catch (Exception e) {
            openAppDetails();
            Toast.makeText(this, "Aggiungi Noticall alle app mai in sospensione", Toast.LENGTH_LONG).show();
        }
    }

    private void openAppDetails() {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception ignored) { }
    }

    private void showActivity() {
        content.removeAllViews();
        content.setTag("activity");
        setActiveTab(tabActivity);
        LinearLayout body = page();
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(sectionTitle("Attività recente"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button clear = smallButton("SVUOTA");
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Svuotare lo storico?")
                .setMessage("Verranno eliminati solo gli eventi salvati localmente sul telefono.")
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Svuota", (d,w) -> { new NoticallLogDb(this).clearAll(); showActivity(); })
                .show());
        heading.addView(clear);
        body.addView(heading);

        NoticallLogDb db = new NoticallLogDb(this);
        Cursor c = db.recent(50);
        try {
            if (!c.moveToFirst()) {
                LinearLayout empty = card();
                empty.addView(text("Nessun evento ancora", 16, TEXT, true));
                TextView sub = text("Dopo una chiamata persa vedrai qui rilevamento, programmazione e stato dell'SMS.", 13, MUTED, false);
                sub.setPadding(0,dp(6),0,0);
                empty.addView(sub);
                body.addView(empty, cardLp());
            } else {
                do {
                    long ts = c.getLong(c.getColumnIndexOrThrow("ts"));
                    String type = c.getString(c.getColumnIndexOrThrow("type"));
                    String number = c.getString(c.getColumnIndexOrThrow("number"));
                    String detail = c.getString(c.getColumnIndexOrThrow("detail"));
                    body.addView(eventCard(ts, type, number, detail), cardLp());
                } while (c.moveToNext());
            }
        } finally { c.close(); }
        putPage(body);
    }

    private void showSettings() {
        content.removeAllViews();
        content.setTag("settings");
        setActiveTab(tabSettings);
        LinearLayout body = page();
        body.addView(sectionTitle("Personalizza Noticall"));

        LinearLayout form = card();
        EditText business = edit("Nome attività", NoticallPrefs.business(this), false);
        EditText link = edit("Link prenotazione", NoticallPrefs.link(this), false);
        EditText template = edit("Messaggio SMS", NoticallPrefs.template(this), true);
        EditText delay = edit("Ritardo invio (secondi)", String.valueOf(NoticallPrefs.delaySeconds(this)), false);
        delay.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText anti = edit("Anti-spam stesso numero (ore, 0 = off)", String.valueOf(NoticallPrefs.antiSpamHours(this)), false);
        anti.setInputType(InputType.TYPE_CLASS_NUMBER);

        form.addView(label("Nome attività")); form.addView(business);
        form.addView(label("Link prenotazione")); form.addView(link);
        form.addView(label("Messaggio SMS")); form.addView(template);
        TextView tokens = text("Variabili: {attivita}  {link}  {numero}", 12, CYAN, false);
        tokens.setPadding(0,dp(6),0,dp(8)); form.addView(tokens);
        form.addView(label("Ritardo invio")); form.addView(delay);
        form.addView(label("Anti-spam")); form.addView(anti);

        LinearLayout rejectedRow = switchRow("Includi chiamate rifiutate", "Tratta come perse anche le chiamate rifiutate.");
        Switch rejected = (Switch) rejectedRow.getChildAt(1);
        rejected.setChecked(NoticallPrefs.includeRejected(this));
        form.addView(rejectedRow);

        LinearLayout timeRow = switchRow("Fascia oraria", "Invia SMS solo nell'intervallo scelto.");
        Switch timeSwitch = (Switch) timeRow.getChildAt(1);
        timeSwitch.setChecked(NoticallPrefs.timeWindowEnabled(this));
        form.addView(timeRow);

        LinearLayout hours = new LinearLayout(this);
        hours.setOrientation(LinearLayout.HORIZONTAL);
        EditText start = edit("Da", String.valueOf(NoticallPrefs.startHour(this)), false);
        EditText end = edit("A", String.valueOf(NoticallPrefs.endHour(this)), false);
        start.setInputType(InputType.TYPE_CLASS_NUMBER); end.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams hp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams hp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); hp2.setMargins(dp(10),0,0,0);
        hours.addView(start, hp1); hours.addView(end, hp2);
        form.addView(hours);

        Button save = primaryButton("SALVA IMPOSTAZIONI");
        save.setOnClickListener(v -> {
            NoticallPrefs.setBusiness(this, business.getText().toString().trim());
            NoticallPrefs.setLink(this, link.getText().toString().trim());
            NoticallPrefs.setTemplate(this, template.getText().toString().trim());
            NoticallPrefs.setDelaySeconds(this, parseInt(delay.getText().toString(), 10));
            NoticallPrefs.setAntiSpamHours(this, parseInt(anti.getText().toString(), 12));
            NoticallPrefs.setIncludeRejected(this, rejected.isChecked());
            NoticallPrefs.setTimeWindowEnabled(this, timeSwitch.isChecked());
            NoticallPrefs.setStartHour(this, parseInt(start.getText().toString(), 8));
            NoticallPrefs.setEndHour(this, parseInt(end.getText().toString(), 22));
            Toast.makeText(this, "Impostazioni salvate", Toast.LENGTH_SHORT).show();
            showHome();
        });
        form.addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        body.addView(form, cardLp());

        LinearLayout info = card();
        info.addView(text("Privacy by design", 15, TEXT, true));
        TextView it = text("Nessun login e nessun cloud: impostazioni e storico restano sul dispositivo. L'SMS parte dalla SIM del telefono.", 13, MUTED, false);
        it.setPadding(0,dp(6),0,0); info.addView(it);
        TextView id = text("ID app: com.noticall.app", 11, CYAN, false); id.setPadding(0,dp(10),0,0); info.addView(id);
        body.addView(info, cardLp());
        putPage(body);
    }

    private void showTestSmsDialog() {
        if (!hasAllPermissions()) {
            requestNoticallPermissions();
            Toast.makeText(this, "Concedi prima i permessi", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText number = new EditText(this);
        number.setHint("+39 333 123 4567");
        number.setInputType(InputType.TYPE_CLASS_PHONE);
        number.setPadding(dp(14),dp(10),dp(14),dp(10));
        new AlertDialog.Builder(this)
                .setTitle("SMS di prova")
                .setMessage("Inserisci un numero su cui puoi verificare la ricezione.")
                .setView(number)
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Invia", (d,w) -> {
                    String n = number.getText().toString().trim();
                    if (n.isEmpty()) return;
                    boolean ok = SmsEngine.send(this, n, NoticallPrefs.renderMessage(this, n), true);
                    Toast.makeText(this, ok ? "SMS inviato al sistema" : "Invio non riuscito", Toast.LENGTH_LONG).show();
                }).show();
    }

    private void requestNoticallPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.SEND_SMS
        }, REQ_PERMISSIONS);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS || requestCode == 9002) {
            if ("device_setup".equals(content.getTag())) showDeviceSetup();
            else showHome();
        }
    }

    private boolean hasAllPermissions() {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private String permissionSummary() {
        if (hasAllPermissions()) return "✓ Permessi operativi";
        StringBuilder s = new StringBuilder("Mancano: ");
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) s.append("Telefono  ");
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) s.append("Registro  ");
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) s.append("SMS");
        return s.toString().trim();
    }

    private LinearLayout page() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(6), dp(18), dp(20));
        return l;
    }

    private void putPage(LinearLayout body) {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.addView(body, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(s, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 15, MUTED, true);
        t.setPadding(dp(2), dp(10), 0, dp(8));
        return t;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable g = new GradientDrawable();
        g.setColor(SURFACE);
        g.setCornerRadius(dp(20));
        g.setStroke(dp(1), Color.rgb(24,52,76));
        l.setBackground(g);
        return l;
    }

    private LinearLayout cardGradient() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16),dp(18),dp(16),dp(18));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(13,39,62), Color.rgb(8,71,77)});
        g.setCornerRadius(dp(22));
        g.setStroke(dp(1), Color.rgb(36,115,123));
        l.setBackground(g);
        return l;
    }

    private LinearLayout statCard(String value, String label) {
        LinearLayout l = card();
        l.setGravity(Gravity.CENTER_VERTICAL);
        TextView v = text(value, 28, CYAN, true);
        TextView d = text(label, 12, MUTED, false);
        l.addView(v); l.addView(d);
        return l;
    }

    private LinearLayout eventCard(long ts, String type, String number, String detail) {
        LinearLayout l = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView kind = text(eventLabel(type), 14, eventColor(type), true);
        TextView time = text(new SimpleDateFormat("dd/MM  HH:mm", Locale.ITALY).format(new Date(ts)), 12, MUTED, false);
        time.setGravity(Gravity.END);
        top.addView(kind, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(time);
        l.addView(top);
        if (number != null && !number.isEmpty()) {
            TextView n = text(number, 15, TEXT, true); n.setPadding(0,dp(7),0,0); l.addView(n);
        }
        if (detail != null && !detail.isEmpty()) {
            TextView d = text(detail, 12, MUTED, false); d.setPadding(0,dp(5),0,0); l.addView(d);
        }
        return l;
    }

    private String eventLabel(String type) {
        if ("MISSED".equals(type)) return "● CHIAMATA";
        if ("SENT".equals(type)) return "● SMS INVIATO";
        if ("SCHEDULED".equals(type)) return "● PROGRAMMATO";
        if ("TEST".equals(type)) return "● TEST";
        if ("SENDING".equals(type)) return "● INVIO";
        if ("SKIPPED".equals(type)) return "● IGNORATO";
        return "● ERRORE";
    }

    private int eventColor(String type) {
        if ("SENT".equals(type)) return TEAL;
        if ("MISSED".equals(type)) return CYAN;
        if ("ERROR".equals(type)) return RED;
        return Color.rgb(179,198,214);
    }

    private TextView label(String s) {
        TextView t = text(s, 12, MUTED, true);
        t.setPadding(0,dp(10),0,dp(6));
        return t;
    }

    private EditText edit(String hint, String value, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(TEXT);
        e.setHintTextColor(Color.rgb(98,122,145));
        e.setTextSize(15);
        e.setPadding(dp(14),dp(11),dp(14),dp(11));
        e.setSingleLine(!multiline);
        if (multiline) { e.setMinLines(3); e.setGravity(Gravity.TOP); }
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.rgb(9,22,36));
        g.setCornerRadius(dp(13));
        g.setStroke(dp(1), Color.rgb(29,58,82));
        e.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,dp(6));
        e.setLayoutParams(lp);
        return e;
    }

    private LinearLayout switchRow(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0,dp(12),0,dp(8));
        LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL);
        t.addView(text(title, 14, TEXT, true));
        t.addView(text(subtitle, 11, MUTED, false));
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this); tintSwitch(sw); row.addView(sw);
        return row;
    }

    private void tintSwitch(Switch sw) {
        sw.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{CYAN, Color.rgb(120,137,153)}));
        sw.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.rgb(21,104,110), Color.rgb(41,55,69)}));
    }

    private Button primaryButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(13);
        b.setTextColor(Color.rgb(1,28,33));
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{CYAN, TEAL});
        g.setCornerRadius(dp(14));
        b.setBackground(g);
        return b;
    }

    private Button smallButton(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(11); b.setTextColor(CYAN); b.setAllCaps(false);
        GradientDrawable g = new GradientDrawable(); g.setColor(SURFACE_2); g.setCornerRadius(dp(12));
        b.setBackground(g); return b;
    }

    private Button navButton(String s, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(11); b.setTextColor(MUTED); b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT); b.setOnClickListener(click); return b;
    }

    private void setActiveTab(Button active) {
        Button[] bs = {tabHome, tabActivity, tabSettings};
        for (Button b : bs) {
            if (b == null) continue;
            b.setTextColor(b == active ? CYAN : MUTED);
            b.setTypeface(Typeface.DEFAULT, b == active ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,dp(10));
        return lp;
    }

    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
    private int parseInt(String s, int fallback) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; } }
}
