package org.maowtm.android.tagalarm;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import android.text.format.DateFormat;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    protected static class UpdateAsyncTask extends AsyncTask<Void, Void, Void> {
        protected AlarmActivity context;
        protected Uri uri;
        protected ContentValues cv;
        public UpdateAsyncTask(AlarmActivity activity, long alarmId, ContentValues cv) {
            this.context = activity;
            this.uri = Uri.parse("content://" + AlarmProvider.AUTH + "/alarm/" + alarmId);
            this.cv = cv;
        }
        protected Void doInBackground(Void... params) {
            this.context.getContentResolver()
                    .update(this.uri, this.cv, null, null);
            this.context.reQuery_AsyncCall();
            return null;
        }
    }
    protected static class AlarmCursorAdapter extends ResourceCursorAdapter {
        protected AlarmActivity activity;
        public AlarmCursorAdapter(AlarmActivity am) {
            super(am, R.layout.alarmlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            this.activity = am;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.alarmitem_time))
                .setText(String.format(Locale.getDefault(), "%02d:%02d", cursor.getInt(1), cursor.getInt(2)));
            ((Switch) view.findViewById(R.id.alarmitem_switch)).setChecked(cursor.getInt(4) == 1);
            final long alarmId = cursor.getLong(0);
            final int initHours = cursor.getInt(1);
            final int initMinutes = cursor.getInt(2);
            view.findViewById(R.id.alarmitem_time).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimePickerFragment tfr = new TimePickerFragment();
                    Bundle args = new Bundle();
                    args.putLong("alarmId", alarmId);
                    args.putInt("hours", initHours);
                    args.putInt("minutes", initMinutes);
                    tfr.setArguments(args);
                    tfr.show(AlarmCursorAdapter.this.activity.getFragmentManager(), "timePicker");
                }
            });
        }
        @Override
        public View newView(final Context context, Cursor cursor, final ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            final long alarmId = cursor.getLong(0);
            ((Switch) view.findViewById(R.id.alarmitem_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final ContentValues cv = new ContentValues();
                    cv.put("enabled", isChecked ? 1 : 0);
                    UpdateAsyncTask att = new UpdateAsyncTask(AlarmCursorAdapter.this.activity, alarmId, cv);
                    att.execute((Void) null);
                }
            });
            return view;
        }
    }
    protected static class RecalculateAsyncTask extends AsyncTask<Context, Void, Void> {
        protected Void doInBackground(Context... ctx) {
            if (ctx.length != 1)
                throw new IllegalArgumentException("ctx.length must be 1.");
            Context ct = ctx[0];
            ct.getContentResolver().update(Uri.parse("content://" + AlarmProvider.AUTH + "/alarm/recalculate"),
                    null, null, null);
            return null;
        }
    }
    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        protected long alarmId;
        protected int hoursInitial, minutesInitial;
        public TimePickerFragment() {
            super();
        }

        @Override
        public Dialog onCreateDialog(Bundle sis) {
            Bundle args = this.getArguments();
            this.alarmId = args.getLong("alarmId");
            if (this.alarmId < 0) {
                throw new IllegalArgumentException("alarmId must not < 0.");
            }
            this.hoursInitial = args.getInt("hours");
            this.minutesInitial = args.getInt("minutes");
            if (this.hoursInitial > 23 || this.minutesInitial > 59) {
                throw new IllegalArgumentException("hours and/or minutes out of range.");
            }

            return new TimePickerDialog(this.getActivity(), this, this.hoursInitial, this.minutesInitial,
                    DateFormat.is24HourFormat(this.getActivity()));
        }
        @Override
        public void onTimeSet(TimePicker view, int hours, int minute) {
            ContentValues cv = new ContentValues();
            cv.put("hours", hours);
            cv.put("minutes", minute);
            UpdateAsyncTask uat = new UpdateAsyncTask((AlarmActivity) this.getActivity(), this.alarmId, cv);
            uat.execute((Void) null);
        }
    }

    protected CursorAdapter adpList;
    protected LoaderManager lm;
    protected ListView alarmlist;
    protected static final int LOADER_ID_SHOW_ALARMS = 0;
    protected Handler reQueryHandler;
    @Override
    protected void onCreate(Bundle sis) {
        super.onCreate(sis);
        this.lm = this.getLoaderManager();
        this.setContentView(R.layout.mainlayout);
        this.alarmlist = (ListView) this.findViewById(R.id.alarmslist);
        this.adpList = new AlarmCursorAdapter(this);
        lm.initLoader(LOADER_ID_SHOW_ALARMS, null, this);
        this.alarmlist.setAdapter(this.adpList);
        reQueryHandler = new Handler();
        if (sis == null) {
            new RecalculateAsyncTask().execute(this);
        }
    }

    protected void reQuery_AsyncCall() {
        reQueryHandler.post(new Runnable() {
            @Override
            public void run() {
                lm.initLoader(LOADER_ID_SHOW_ALARMS, null, AlarmActivity.this);
            }
        });
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        Log.d("reach", "onCreateLoader");
        if (id == LOADER_ID_SHOW_ALARMS) {
            Uri query = Uri.parse("content://" + AlarmProvider.AUTH + "/alarm");
            return new CursorLoader(this, query, new String[] {"id AS _id", "hours", "minutes", "daysofweek", "enabled"}
                    , null, null, "nexttime ASC");
        }
        throw new IllegalArgumentException("id not recognized.");
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_ID_SHOW_ALARMS) {
            Log.d("debug", String.format(Locale.getDefault(), "Data.getCount() = %d", data.getCount()));
            this.adpList.changeCursor(data);
        }
    }
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
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
