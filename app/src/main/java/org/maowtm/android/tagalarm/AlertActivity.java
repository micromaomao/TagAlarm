package org.maowtm.android.tagalarm;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Date;

public class AlertActivity extends Activity {
    protected java.text.DateFormat timeFormat;
    protected TextView timeView;
    public final class UpdateTimeRunnable implements Runnable {
        @Override
        public void run () {
            try {
                AlertActivity.this.updateTime();
            } finally {
                AlertActivity.this.updateTimeHandler.postDelayed(this, 1000);
            }
        }
    }
    protected UpdateTimeRunnable updateTimeRunnable;
    protected Handler updateTimeHandler;
    protected int alarmId;
    @Override
    protected void onCreate(Bundle sis) {
        super.onCreate(sis);
        this.alarmId = this.getIntent().getIntExtra(Alarms.INTENT_EXTRA_ALARM_ID, -1);
        this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.timeFormat = android.text.format.DateFormat.getTimeFormat(this);
        this.setContentView(R.layout.alertlayout);

        this.timeView = (TextView) this.findViewById(R.id.timeView);
        this.updateTimeHandler = new Handler();
        this.updateTimeRunnable = new UpdateTimeRunnable();
        this.findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertActivity.this.startDismiss();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle sis) {
        super.onSaveInstanceState(sis);
    }
    @Override
    protected void onPause() {
        super.onPause();
        updateTimeHandler.removeCallbacks(this.updateTimeRunnable);
    }
    @Override
    protected void onResume() {
        super.onResume();
        this.updateTimeRunnable.run();
    }
    protected void updateTime() {
        CharSequence time = this.timeFormat.format(new Date());
        this.timeView.setText(time);
    }
    protected void startDismiss() {
        // TODO: Add requirement for proof-of-wake
        AlertReceiver.acceptDismiss(this, this.alarmId);
        this.finish();
    }
}
