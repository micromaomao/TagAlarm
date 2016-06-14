package org.maowtm.android.tagalarm;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class AlarmActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle sis) {
        super.onCreate(sis);
        this.setContentView(R.layout.mainlayout);
        this.findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Alarms.setAlert(AlarmActivity.this, Alarms.calculateNextTime(7, 0, new Alarms.DaysOfWeek((byte)0)), 0);
            }
        });
        this.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Alarms.setAlert(AlarmActivity.this, 0, -1);
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
    }
}
