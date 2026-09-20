package com.noticall.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

public final class NoticallPrefs {
    private static final String NAME = "noticall_prefs";
    private static SharedPreferences p(Context c) { return c.getSharedPreferences(NAME, Context.MODE_PRIVATE); }

    public static boolean enabled(Context c) { return p(c).getBoolean("enabled", false); }
    public static void setEnabled(Context c, boolean v) { p(c).edit().putBoolean("enabled", v).apply(); }

    public static boolean setupCompleted(Context c) { return p(c).getBoolean("setup_completed", false); }
    public static void setSetupCompleted(Context c, boolean v) { p(c).edit().putBoolean("setup_completed", v).apply(); }

    public static boolean samsungSetupOpened(Context c) { return p(c).getBoolean("samsung_setup_opened", false); }
    public static void setSamsungSetupOpened(Context c, boolean v) { p(c).edit().putBoolean("samsung_setup_opened", v).apply(); }
    public static boolean samsungSetupConfirmed(Context c) { return p(c).getBoolean("samsung_setup_confirmed", false); }
    public static void setSamsungSetupConfirmed(Context c, boolean v) { p(c).edit().putBoolean("samsung_setup_confirmed", v).apply(); }

    public static boolean serviceRunning(Context c) { return p(c).getBoolean("service_running", false); }
    public static void setServiceRunning(Context c, boolean v) { p(c).edit().putBoolean("service_running", v).apply(); }

    public static String business(Context c) { return p(c).getString("business", "La tua attività"); }
    public static void setBusiness(Context c, String v) { p(c).edit().putString("business", v).apply(); }

    public static String link(Context c) { return p(c).getString("link", "https://example.com/prenota"); }
    public static void setLink(Context c, String v) { p(c).edit().putString("link", v).apply(); }

    public static String template(Context c) {
        return p(c).getString("template", "Ciao! Abbiamo visto la tua chiamata a {attivita}. Non siamo riusciti a rispondere. Puoi prenotare qui: {link}");
    }
    public static void setTemplate(Context c, String v) { p(c).edit().putString("template", v).apply(); }

    public static int delaySeconds(Context c) { return p(c).getInt("delay", 10); }
    public static void setDelaySeconds(Context c, int v) { p(c).edit().putInt("delay", Math.max(0, Math.min(300, v))).apply(); }

    public static int antiSpamHours(Context c) { return p(c).getInt("antispam", 12); }
    public static void setAntiSpamHours(Context c, int v) { p(c).edit().putInt("antispam", Math.max(0, Math.min(168, v))).apply(); }

    public static boolean timeWindowEnabled(Context c) { return p(c).getBoolean("window_enabled", false); }
    public static void setTimeWindowEnabled(Context c, boolean v) { p(c).edit().putBoolean("window_enabled", v).apply(); }

    public static int startHour(Context c) { return p(c).getInt("start_hour", 8); }
    public static int endHour(Context c) { return p(c).getInt("end_hour", 22); }
    public static void setStartHour(Context c, int v) { p(c).edit().putInt("start_hour", clampHour(v)).apply(); }
    public static void setEndHour(Context c, int v) { p(c).edit().putInt("end_hour", clampHour(v)).apply(); }

    public static boolean includeRejected(Context c) { return p(c).getBoolean("include_rejected", true); }
    public static void setIncludeRejected(Context c, boolean v) { p(c).edit().putBoolean("include_rejected", v).apply(); }

    private static int clampHour(int h) { return Math.max(0, Math.min(23, h)); }

    public static boolean isInsideTimeWindow(Context c) {
        if (!timeWindowEnabled(c)) return true;
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int start = startHour(c);
        int end = endHour(c);
        if (start == end) return true;
        if (start < end) return h >= start && h < end;
        return h >= start || h < end;
    }

    public static String renderMessage(Context c, String number) {
        return template(c)
                .replace("{attivita}", business(c))
                .replace("{link}", link(c))
                .replace("{numero}", number == null ? "" : number);
    }

    public static long ringStart(Context c) { return p(c).getLong("ring_start", 0L); }
    public static void setRingStart(Context c, long v) { p(c).edit().putLong("ring_start", v).apply(); }
    public static boolean ringing(Context c) { return p(c).getBoolean("ringing", false); }
    public static void setRinging(Context c, boolean v) { p(c).edit().putBoolean("ringing", v).apply(); }
    public static boolean answered(Context c) { return p(c).getBoolean("answered", false); }
    public static void setAnswered(Context c, boolean v) { p(c).edit().putBoolean("answered", v).apply(); }

    public static long lastProcessedCall(Context c) { return p(c).getLong("last_processed_call", 0L); }
    public static void setLastProcessedCall(Context c, long v) { p(c).edit().putLong("last_processed_call", v).apply(); }

    private static String lastKey(String number) { return "last_auto_" + Math.abs((number == null ? "" : number).hashCode()); }
    public static boolean isAntiSpamBlocked(Context c, String number) {
        int hours = antiSpamHours(c);
        if (hours <= 0) return false;
        long last = p(c).getLong(lastKey(number), 0L);
        return last > 0 && (System.currentTimeMillis() - last) < hours * 3600000L;
    }
    public static void markAutoScheduled(Context c, String number) {
        p(c).edit().putLong(lastKey(number), System.currentTimeMillis()).apply();
    }
}
