package org.maowtm.android.tagalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

/*
    * TODO: Custom volume / ringtone
    * TODO: Vibrate when can't play ringtone
    * TODO: Proof-of-wake
    * TODO: Show notification before alarm ring and allow "Dismiss now".
    * TODO: Allow play music
*/

public abstract class Alarms {
    public static final String ACTION_ALARM_ALERT = "org.maowtm.android.tagalarm.actions.ALARM_ALERT";
    public static final String INTENT_EXTRA_ALARM_ID = "org.maowtm.android.tagalarm.intentextras.ALARM_ID";
    public static final class DaysOfWeek {
        // 2^7-1 (binary 1111111)
        public static final int MAX_MDAYS = 127;

        public DaysOfWeek(byte code) {
            // Notice that the max possible value for byte is 127.
            if (code < 0) {
                throw new IllegalArgumentException("code not in range.");
            }
            this.mDays = code;
        }
        public DaysOfWeek(DaysOfWeek c) {
            this(c.getCode());
        }

        // Bitmask of all repeating days, starting from Sunday
        // all day index in this class starts from monday.
        private byte mDays;
        public void set(int day, boolean set) {
            if (day < 0 || day > 6) {
                throw new IllegalArgumentException("Day must be within 0 and 6.");
            }
            if (set) {
                this.mDays |= (1 << day);
            } else {
                this.mDays &= ~(1 << day);
            }
        }
        public boolean get(int day) {
            if (day < 0 || day > 6) {
                throw new IllegalArgumentException("Day must be within 0 and 6.");
            }
            return ((this.mDays & (1 << day)) > 0);
        }
        public boolean isRepeat() {
            return this.mDays != 0;
        }
        public String toString() {
            // TODO
            StringBuilder sb = new StringBuilder();
            sb.append("Code: ");
            sb.append(this.mDays);
            return sb.toString();
        }
        public byte getCode() {
            return this.mDays;
        }
        protected static int calDay2dowDay(int calDay) {
            return calDay - Calendar.SUNDAY;
        }
        protected static int dowDay2calDay(int dowDay) {
            return dowDay + Calendar.SUNDAY;
        }

        /**
         * @param w `DaysOfWeek` from alarm.
         * @param nowDay
         * @return how many days left until the alarm should go off again. -1 if never.
         */
        public static int findNextDayOffset(DaysOfWeek w, int nowDay) {
            if (nowDay < 0 || nowDay > 6) {
                throw new IllegalArgumentException("Day must be within 0 and 6.");
            }
            if (!w.isRepeat()) {
                return -1;
            }
            int count;
            for (count = 0; count < 7; count ++) {
                if (w.get((nowDay + count) % 7))
                    break;
            }
            return count;
        }
    }

    public static long calculateNextTime(int hour, int minute, DaysOfWeek dow) {
        Calendar cl = Calendar.getInstance();
        int nH = cl.get(Calendar.HOUR_OF_DAY),
            nM = cl.get(Calendar.MINUTE),
            nD = DaysOfWeek.calDay2dowDay(cl.get(Calendar.DAY_OF_WEEK));

        cl.set(Calendar.HOUR_OF_DAY, hour);
        cl.set(Calendar.MINUTE, minute);
        cl.set(Calendar.SECOND, 0);

        boolean passedToday = nH > hour || (nH == hour && nM >= minute);
        final int oneDay = (1000*60*60*24);
        if (!dow.isRepeat()) {
            if (!passedToday) {
                return cl.getTimeInMillis();
            } else {
                return cl.getTimeInMillis() + oneDay;
            }
        } else {
            int nextDayOffset = DaysOfWeek.findNextDayOffset(dow, nD);
            if (nextDayOffset == -1)
                return -1;
            if (nextDayOffset == 0 && passedToday) {
                nextDayOffset = 7;
            }
            return cl.getTimeInMillis() + oneDay * nextDayOffset;
        }
    }
    public static void setAlert(final Context context, final long time, final long alarmId) {
        Log.d("reach", "setAlert");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alertIntent = new Intent(ACTION_ALARM_ALERT);
        alertIntent.putExtra(INTENT_EXTRA_ALARM_ID, alarmId);
        am.cancel(PendingIntent.getBroadcast(context, 0, alertIntent, 0));
        if (time > System.currentTimeMillis() && alarmId >= 0) {
            PendingIntent sender = PendingIntent.getBroadcast(
                    context, 0, alertIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            Intent showIntent = new Intent(context.getApplicationContext(), AlarmActivity.class);
            showIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent pendingShowIntent = PendingIntent.getActivity(context.getApplicationContext(),
                    0, showIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(time, pendingShowIntent), sender);
        }
    }
    public static void showAlertUI(Context context, long alarmId) {
        Intent alertUI = new Intent(context, AlertActivity.class);
        alertUI.putExtra(Alarms.INTENT_EXTRA_ALARM_ID, alarmId);
        alertUI.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(alertUI);
    }
}
