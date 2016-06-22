package org.maowtm.android.tagalarm;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class AlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        final long alarmId = intent.getLongExtra(Alarms.INTENT_EXTRA_ALARM_ID, -1);
        if (intent.getAction().equals(Alarms.ACTION_ALARM_ALERT) && alarmId >= 0) {
            final boolean allowDirectDismiss = intent.getBooleanExtra(Alarms.INTENT_EXTRA_ALLOW_DIRECT_DISMISS, false);
            Log.v("AlertReceiver", "received alarm id = " + alarmId);
            Intent notify = new Intent(Alarms.ACTION_ALARM_ALERT);
            notify.putExtra(Alarms.INTENT_EXTRA_ALARM_ID, alarmId);
            notify.putExtra(Alarms.INTENT_EXTRA_ALLOW_DIRECT_DISMISS, allowDirectDismiss);
            PendingIntent pendingNotify = PendingIntent.getBroadcast(context, 0, notify, PendingIntent.FLAG_CANCEL_CURRENT);
            Notification.Builder nBuilder = new Notification.Builder(context);
            nBuilder.setSmallIcon(R.drawable.icon);
            nBuilder.setContentText(context.getText(R.string.noti_content));
            nBuilder.setContentTitle(context.getText(R.string.app_name));
            nBuilder.setOngoing(true);
            nBuilder.setContentIntent(pendingNotify);
            nBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
            Notification notification = nBuilder.build();
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify((int)(alarmId % Integer.MAX_VALUE), notification);

            Alarms.showAlertUI(context, alarmId, allowDirectDismiss);
            /* TODO:
            if (alCursor.getInt(2) <= 0 || alCursor.getLong(1) < System.currentTimeMillis() - (1000*60*30)) {
                alCursor.close();
                return null;
            }
            */

            Intent playAlarm = new Intent(context, AlertService.class);
            playAlarm.putExtra(Alarms.INTENT_EXTRA_ALARM_ID, alarmId);
            playAlarm.putExtra(Alarms.INTENT_EXTRA_ALLOW_DIRECT_DISMISS, allowDirectDismiss);
            playAlarm.setAction(AlertService.ACTION_PLAY_ALARM);
            context.startService(playAlarm);

            AsyncTask<Void, Void, Void> updateAlarmStatus = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Uri uri = Uri.parse("content://" + AlarmProvider.AUTH + "/alarm/" + alarmId);
                    Cursor alrCsr = context.getContentResolver().query(uri,
                            new String[] {"daysofweek", "enabled"}, null, null, null);
                    if (alrCsr.moveToFirst()) {
                        Alarms.DaysOfWeek dow = new Alarms.DaysOfWeek((byte) alrCsr.getInt(0));
                        if (!dow.isRepeat() && alrCsr.getInt(1) > 0) {
                            ContentValues cv = new ContentValues();
                            cv.put("enabled", 0);
                            context.getContentResolver().update(uri, cv, null, null);
                        }
                    }
                    alrCsr.close();
                    return null;
                }
            };
            updateAlarmStatus.execute((Void) null);
        }
    }
    public static void acceptDismiss(Context context, long alarmId) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel((int)(alarmId % Integer.MAX_VALUE));
        Intent stopAlarm = new Intent(context, AlertService.class);
        stopAlarm.putExtra(Alarms.INTENT_EXTRA_ALARM_ID, alarmId);
        stopAlarm.setAction(AlertService.ACTION_STOP_ALARM);
        context.startService(stopAlarm);
    }
}
