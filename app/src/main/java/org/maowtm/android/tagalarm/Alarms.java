package org.maowtm.android.tagalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/*
    * TODO: Save alarms
    * TODO: Handle multiple alarms (recalculate next alarm and send it to AlarmManager when alarm update / app restart / reboot)
    * TODO: Custom volume / ringtone
    * TODO: Vibrate when can't play ringtone
    * TODO: Proof-of-wake
    * TODO: Show notification before alarm ring and allow "Dismiss now".
    * TODO: Allow play music
*/

public class Alarms {
    public static final String ACTION_ALARM_ALERT = "org.maowtm.android.tagalarm.actions.ALARM_ALERT";
    public static final String INTENT_EXTRA_ALARM_ID = "org.maowtm.android.tagalarm.intentextras.ALARM_ID";
    public static final class DaysOfWeek {
        private static int[] DAY_MAP = new int[] {
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY,
        };
        public DaysOfWeek(byte code) {
            this.mDays = code;
        }

        // Bitmask of all repeating days, starting from Monday
        // all day index in this class starts from monday.
        private byte mDays;
        public void set(int day, boolean set) {
            if (set) {
                this.mDays |= (1 << day);
            } else {
                this.mDays &= ~(1 << day);
            }
        }
        public boolean get(int day) {
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
    }

    public static long calculateNextTime(int hour, int minute, DaysOfWeek dow) {
        Calendar cl = Calendar.getInstance();
        int nH = cl.get(Calendar.HOUR_OF_DAY),
            nM = cl.get(Calendar.MINUTE),
            nD = cl.get(Calendar.DAY_OF_WEEK);

        cl.set(Calendar.HOUR_OF_DAY, hour);
        cl.set(Calendar.MINUTE, minute);
        cl.set(Calendar.SECOND, 0);

        // TODO: Take repeat into account. Assuming everyday.
        if (nH < hour || (nH == hour && nM < minute)) {
            // Alarm is pending for today!
            return cl.getTimeInMillis();
        } else {
            return cl.getTimeInMillis() + (1000*60*60*24);
        }
    }
    public static void setAlert(final Context context, final long time, final int alarmId) {
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
    public static void showAlertUI(Context context, int alarmId) {
        Intent alertUI = new Intent(context, AlertActivity.class);
        alertUI.putExtra(Alarms.INTENT_EXTRA_ALARM_ID, alarmId);
        alertUI.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(alertUI);
    }
}
