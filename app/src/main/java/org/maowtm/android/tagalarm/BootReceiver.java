package org.maowtm.android.tagalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        context.getContentResolver().update(Uri.parse("content://" + AlarmProvider.AUTH + "/alarm/recalculate"),
                null, null, null);
    }
}
