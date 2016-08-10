package org.maowtm.android.tagalarm;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

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
                AlertActivity.this.updateTimeHandler.postDelayed(this, 500);
            }
        }
    }
    protected UpdateTimeRunnable updateTimeRunnable;
    protected Handler updateTimeHandler;

    protected long alarmId;
    protected boolean allowDirectDismiss;
    protected long triggerTime;
    protected String proofOfWake;
    protected ProofOfWakes pows;
    protected String failure;
    protected TextView powstuffWait;
    @Override
    protected void onCreate(Bundle sis) {
        super.onCreate(sis);
        this.alarmId = this.getIntent().getLongExtra(Alarms.INTENT_EXTRA_ALARM_ID, -1);
        this.allowDirectDismiss = this.getIntent().getBooleanExtra(Alarms.INTENT_EXTRA_ALLOW_DIRECT_DISMISS, false);
        this.proofOfWake = this.getIntent().getStringExtra(Alarms.INTENT_EXTRA_PROOF_OF_WAKES);
        this.triggerTime = System.currentTimeMillis();
        if (this.proofOfWake == null) {
            this.proofOfWake = "[]";
        }
        if (sis != null) {
            this.triggerTime = sis.getLong("triggerTime", this.triggerTime);
            this.allowDirectDismiss = sis.getBoolean("allowDirectDismiss", this.allowDirectDismiss);
            this.alarmId = sis.getLong("alarmId", this.alarmId);
            this.proofOfWake = sis.getString("proofOfWake", this.proofOfWake);
        }

        try {
            this.pows = new ProofOfWakes(new JSONArray(this.proofOfWake));
        } catch (JSONException e) {
            this.failure = getString(R.string.alert_failure_msg) + "\n" + e.getMessage();
            this.pows = null;
            // TODO: Show this in UI.
        }
        this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.timeFormat = android.text.format.DateFormat.getTimeFormat(this);
        this.setContentView(R.layout.alertlayout);

        this.timeView = (TextView) this.findViewById(R.id.timeView);
        this.powstuffWait = (TextView) this.findViewById(R.id.powstuff_wait);

        this.updateTimeHandler = new Handler();
        this.updateTimeRunnable = new UpdateTimeRunnable();
        this.findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertActivity.this.startDismiss();
            }
        });

        if (this.allowDirectDismiss) {
            this.findViewById(R.id.direct_dismiss).setVisibility(View.VISIBLE);
            this.findViewById(R.id.direct_dismiss_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertReceiver.acceptDismiss(AlertActivity.this, AlertActivity.this.alarmId);
                    AlertActivity.this.finish();
                }
            });
        } else {
            this.findViewById(R.id.direct_dismiss).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle sis) {
        super.onSaveInstanceState(sis);
        sis.putLong("triggerTime", triggerTime);
        sis.putBoolean("allowDirectDismiss", allowDirectDismiss);
        sis.putLong("alarmId", this.alarmId);
        sis.putString("proofOfWake", this.proofOfWake);
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
        Date now = new Date();
        CharSequence time = this.timeFormat.format(now);

        if (this.pows != null) {
            int seconds = this.pows.getWaitTime();
            if (seconds <= 60*60*24) {
                int escaped = (int)(now.getTime() - this.triggerTime) / 1000;
                if (escaped >= seconds) {
                    this.startDismiss();
                } else {
                    this.powstuffWait.setVisibility(View.VISIBLE);
                    this.powstuffWait.setText(this.getString(R.string.powstuff_wait, seconds - escaped));
                }
            }
        }
        this.timeView.setText(time);
    }
    protected void startDismiss() {
        // TODO: Add requirement for proof-of-wake
        AlertReceiver.acceptDismiss(this, this.alarmId);
        this.finish();
    }
}
