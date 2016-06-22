package org.maowtm.android.tagalarm;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class AlarmProvider extends ContentProvider {
    public static final String AUTH = "org.maowtm.android.tagalarm.auth";
    protected static final int URI_ALARM = 1;
    protected static final int URI_ALARM_ID = 2;
    protected static final int URI_RECALCULATE = 3;
    protected static final UriMatcher uriM = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriM.addURI(AUTH, "alarm", URI_ALARM);
        uriM.addURI(AUTH, "alarm/#", URI_ALARM_ID);
        uriM.addURI(AUTH, "alarm/recalculate", URI_RECALCULATE);
    }
    protected class DatabaseHelper extends SQLiteOpenHelper {
        protected static final String dbName = "alarms.db";
        protected static final int CURRENT_VERSION = 1;

        public DatabaseHelper(Context c) {
            super(c, dbName, null, CURRENT_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE `alarms` (" +
                    "`id`INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE," +
                    "`hours`INTEGER NOT NULL," +
                    "`minutes`INTEGER NOT NULL," +
                    "`daysofweek`INTEGER NOT NULL DEFAULT 0," +
                    "`enabled`INTEGER DEFAULT 1," +
                    "`label`TEXT DEFAULT ''," +
                    "`nexttime`INTEGER DEFAULT NULL," +
                    "`volume`REAL NOT NULL DEFAULT 1," +
                    "`ringtone`TEXT DEFAULT NULL," +
                    "`proofwake`TEXT NOT NULL DEFAULT '[]'," +
                    "`allow_early_dismiss`INTEGER NOT NULL DEFAULT 1" +
                    ");");
            db.execSQL("INSERT INTO alarms (hours, minutes, enabled, proofwake) VALUES (7, 0, 0, " +
                    "'[{\"type\": 1, \"difficulty\": 2, \"amount\": 10}, {\"type\": 5, \"second\": 1200}]');");
            AlarmProvider.this.recalculateNextTime(db, null, null);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        }
    }
    protected DatabaseHelper dbHelper;
    @Override
    public boolean onCreate() {
        Context c = this.getContext();
        if (c == null) {
            return false;
        }
        dbHelper = new DatabaseHelper(c);
        return true;
    }
    public Cursor query(@NonNull Uri uri, String[] projIn, String sel, String[] selArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int uriMatch = uriM.match(uri);
        switch (uriMatch) {
            case URI_ALARM:
                qb.setTables("alarms");
                break;
            case URI_ALARM_ID:
                qb.setTables("alarms");
                qb.appendWhere("id = ");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown uri " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor res = qb.query(db, projIn, sel, selArgs, null, null, sort);
        res.setNotificationUri(this.getContext().getContentResolver(), uri);
        return res;
    }
    @Override
    public String getType(@NonNull Uri uri) {
        int match = uriM.match(uri);
        switch (match) {
            case URI_ALARM:
                return "vnd.android.cursor.dir/alarms";
            case URI_ALARM_ID:
                return "vnd.android.cursor.item/alarms";
            default:
                throw new IllegalArgumentException("Unknown uri " + uri);
        }
    }
    protected static void checkOrThrow(ContentValues values) {
        if (values.containsKey("hours")) {
            Integer hours = values.getAsInteger("hours");
            if (hours == null || hours < 0 || hours > 23) {
                throw new IllegalArgumentException("Hours not valid.");
            }
        }
        if (values.containsKey("minutes")) {
            Integer minutes = values.getAsInteger("minutes");
            if (minutes == null || minutes < 0 || minutes > 59) {
                throw new IllegalArgumentException("Minutes not valid.");
            }
        }
        if (values.containsKey("nexttime")) {
            Long nextTime = values.getAsLong("nexttime");
            if (nextTime == null || nextTime < 0) {
                throw new IllegalArgumentException("NextTime not valid.");
            }
            if (nextTime <= System.currentTimeMillis()) {
                values.put("enabled", 0);
            }
        }
        if (values.containsKey("id")) {
            Integer id = values.getAsInteger("id");
            if (id == null || id < 0) {
                throw new IllegalArgumentException("Id not valid.");
            }
        }
        if (values.containsKey("daysofweek")) {
            Integer daysOfWeek = values.getAsInteger("daysofweek");
            if (daysOfWeek == null || daysOfWeek > 127 || daysOfWeek < 0) {
                throw new IllegalArgumentException("DaysOfWeek not in range.");
            }
        }
        if (values.containsKey("volume")) {
            Double volume = values.getAsDouble("volume");
            if (volume == null || volume < 0 || volume > 1) {
                throw new IllegalArgumentException("Volume out of range.");
            }
        }
        // TODO: Validate proof of wake
    }
    protected void recalculateNextTime(SQLiteDatabase db, long alarmId) {
        this.recalculateNextTime(db, "id = " + alarmId, null);
    }
    protected int recalculateNextTime(@NonNull SQLiteDatabase db, @Nullable String sel, @Nullable String[] selArgs) {
        Cursor cs = db.query("alarms", new String[]{"id", "hours", "minutes", "daysofweek"}, sel,
                selArgs, null, null, null, null);
        boolean empty = !cs.moveToFirst();
        int count = 0;
        while (!empty && !cs.isAfterLast()) {
            long nextTime = Alarms.calculateNextTime(cs.getInt(1), cs.getInt(2), new Alarms.DaysOfWeek((byte)cs.getInt(3)));
            ContentValues cv = new ContentValues();
            if (nextTime < 0) {
                cv.put("enabled", 0);
                cv.put("nexttime", -1);
            } else {
                cv.put("nexttime", nextTime);
            }
            checkOrThrow(cv);
            db.update("alarms", cv, "id = " + cs.getLong(0), null);
            count ++;
            cs.moveToNext();
        }
        cs.close();
        cs = db.query("alarms", new String[] {"id", "nexttime"}, "enabled = 1 AND nexttime > " + System.currentTimeMillis()
                , null, null, null, "nexttime ASC", "1");
        empty = !cs.moveToFirst();
        if (empty) {
            Alarms.setAlert(this.getContext(), 0, -1);
        } else {
            Alarms.setAlert(this.getContext(), cs.getLong(1), cs.getLong(0));
        }
        cs.close();
        return count;
    }
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String sel, String[] selArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int match = uriM.match(uri);
        int count;
        switch (match) {
            case URI_ALARM_ID:
                checkOrThrow(values);
                db.beginTransaction();
                if (TextUtils.isEmpty(sel)) {
                    count = db.update("alarms", values, "id = ?", new String[] {uri.getPathSegments().get(1)});
                    if (count > 0)
                        recalculateNextTime(db, Integer.parseInt(uri.getPathSegments().get(1)));
                } else {
                    String rowId = uri.getPathSegments().get(1);
                    String[] args;
                    if (selArgs != null) {
                        args = new String[selArgs.length + 1];
                        args[0] = rowId;
                        System.arraycopy(selArgs, 0, args, 1, selArgs.length);
                    } else {
                        args = new String[] {rowId};
                    }
                    count = db.update("alarms", values, "id = ? AND ( " + sel + ")", args);
                    if (count > 0)
                        recalculateNextTime(db, Integer.parseInt(rowId));
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            case URI_ALARM:
                checkOrThrow(values);
                db.beginTransaction();
                count = db.update("alarms", values, sel, selArgs);
                if (count > 0) {
                    recalculateNextTime(db, null, null);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            case URI_RECALCULATE:
                db.beginTransaction();
                count = recalculateNextTime(db, sel, selArgs);
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            default:
                throw new IllegalArgumentException("Unknown / unsupported uri " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initValues) {
        if (uriM.match(uri) != URI_ALARM) {
            throw new IllegalArgumentException("Unknown / unsupported uri " + uri);
        }

        ContentValues vals;
        if (initValues != null)
            vals = new ContentValues(initValues);
        else
            vals = new ContentValues();
        checkOrThrow(vals);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        long rowId = db.insert("alarms", null, vals);
        if (rowId < 0) {
            throw new SQLException("Failed to insert row into " + uri);
        }
        recalculateNextTime(db, rowId);
        db.setTransactionSuccessful();
        db.endTransaction();
        Uri newUri = ContentUris.withAppendedId(uri, rowId);
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }
    public int delete(@NonNull Uri uri, String sel, String[] selArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        int match = uriM.match(uri);
        switch (match) {
            case URI_ALARM:
                count = db.delete("alarms", sel, selArgs);
                break;
            case URI_ALARM_ID:
                if (TextUtils.isEmpty(sel)) {
                    count = db.delete("alarms", "id = ?", new String[] {uri.getPathSegments().get(1)});
                } else {
                    String rowId = uri.getPathSegments().get(1);
                    String[] args;
                    if (selArgs != null) {
                        args = new String[selArgs.length + 1];
                        args[0] = rowId;
                        System.arraycopy(selArgs, 0, args, 1, selArgs.length);
                    } else {
                        args = new String[] {rowId};
                    }
                    count = db.delete("alarms", "id = ? AND ( " + sel + ")", args);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown / unsupported uri " + uri);
        }
        this.recalculateNextTime(db, "0 = 1", null);

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
