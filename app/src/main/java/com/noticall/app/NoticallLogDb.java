package com.noticall.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NoticallLogDb extends SQLiteOpenHelper {
    private static final String DB = "noticall.db";
    private static final int VERSION = 1;

    public NoticallLogDb(Context c) { super(c, DB, null, VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, type TEXT NOT NULL, number TEXT, detail TEXT)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    public void add(String type, String number, String detail) {
        ContentValues v = new ContentValues();
        v.put("ts", System.currentTimeMillis());
        v.put("type", type);
        v.put("number", number == null ? "" : number);
        v.put("detail", detail == null ? "" : detail);
        getWritableDatabase().insert("events", null, v);
    }

    public Cursor recent(int limit) {
        return getReadableDatabase().query("events", null, null, null, null, null, "ts DESC", String.valueOf(limit));
    }

    public int countToday(String type) {
        long now = System.currentTimeMillis();
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        Cursor cur = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM events WHERE type=? AND ts>=?", new String[]{type, String.valueOf(start)});
        try { return cur.moveToFirst() ? cur.getInt(0) : 0; } finally { cur.close(); }
    }

    public void clearAll() { getWritableDatabase().delete("events", null, null); }
}
