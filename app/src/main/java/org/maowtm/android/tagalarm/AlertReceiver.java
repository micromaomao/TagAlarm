package org.maowtm.android.tagalarm;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(Alarms.INTENT_EXTRA_ALARM_ID, -1);
        if (intent.getAction().equals(Alarms.ACTION_ALARM_ALERT) && alarmId >= 0) {
            Log.v("AlertReceiver", "received alarm id = " + alarmId);
            Intent notify = new Intent(Alarms.ACTION_ALARM_ALERT);
            notify.putExtra(Alarms.INTENT_EXTRA_ALARM_ID, alarmId);
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
            nm.notify(alarmId, notification);

            Alarms.showAlertUI(context, alarmId);

            Intent playAlarm = new Intent(context, AlertService.class);
            playAlarm.putExtra(Alarms.INTENT_EXTRA_ALARM_ID, alarmId);
            playAlarm.setAction(AlertService.ACTION_PLAY_ALARM);
            context.startService(playAlarm);
        }
    }
    public static void acceptDismiss(Context context, int alarmId) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(alarmId);
        Intent stopAlarm = new Intent(context, AlertService.class);
        stopAlarm.putExtra(Alarms.INTENT_EXTRA_ALARM_ID, alarmId);
        stopAlarm.setAction(AlertService.ACTION_STOP_ALARM);
        context.startService(stopAlarm);
    }
}
