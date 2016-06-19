package org.maowtm.android.tagalarm;

import android.animation.TimeInterpolator;
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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    protected static class UpdateAsyncTask extends AsyncTask<Void, Void, Void> {
        protected final AlarmActivity context;
        protected final Uri uri;
        protected final ContentValues cv;
        protected final int mode;
        public static final int MODE_UPDATE = 0, MODE_INSERT = 1, MODE_DELETE = 2;
        public UpdateAsyncTask(AlarmActivity activity, long alarmId, ContentValues cv) {
            this(activity, alarmId, cv, MODE_UPDATE);
        }
        public UpdateAsyncTask(AlarmActivity activity, ContentValues cv) {
            this(activity, -1, cv, MODE_INSERT);
        }
        public UpdateAsyncTask(AlarmActivity activity, long alarmId) {
            this(activity, alarmId, null, MODE_DELETE);
        }
        public UpdateAsyncTask(AlarmActivity activity, long alarmId, ContentValues cv, int mode) {
            this.context = activity;
            this.cv = cv;
            this.mode = mode;
            if (mode != MODE_INSERT) {
                this.uri = Uri.parse("content://" + AlarmProvider.AUTH + "/alarm/" + alarmId);
            } else {
                this.uri = Uri.parse("content://" + AlarmProvider.AUTH + "/alarm");
            }
        }
        protected Void doInBackground(Void... params) {
            switch (this.mode) {
                case MODE_INSERT:
                    this.context.getContentResolver()
                        .insert(this.uri, this.cv);
                    break;
                case MODE_UPDATE:
                    this.context.getContentResolver()
                        .update(this.uri, this.cv, null, null);
                    break;
                case MODE_DELETE:
                    this.context.getContentResolver()
                        .delete(this.uri, null, null);
            }
            this.context.reQuery_AsyncCall(this.mode == MODE_INSERT);
            return null;
        }
    }
    protected static class AlarmCursorAdapter extends ResourceCursorAdapter {
        protected final AlarmActivity activity;
        public AlarmCursorAdapter(AlarmActivity am) {
            super(am, R.layout.alarmlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            this.activity = am;
        }
        protected static class DaysOfWeekCheckChanged implements CompoundButton.OnCheckedChangeListener {
            protected final AlarmActivity activity;
            protected final long alarmId;
            protected final Alarms.DaysOfWeek initState;
            protected boolean used = false;
            public DaysOfWeekCheckChanged(final AlarmActivity activity, final long alarmId,
                                          final Alarms.DaysOfWeek initState) {
                this.activity = activity;
                this.alarmId = alarmId;
                this.initState = initState;
            }
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (used) {
                    return;
                }
                used = true;
                ContentValues cv = new ContentValues();
                Alarms.DaysOfWeek dowInitial = new Alarms.DaysOfWeek(this.initState);
                dowInitial.set((int) buttonView.getTag(R.id.tag_dowTargetDay), isChecked);
                cv.put("daysofweek", dowInitial.getCode());
                cv.put("enabled", 1);
                UpdateAsyncTask uat = new UpdateAsyncTask(this.activity, this.alarmId, cv);
                uat.execute((Void) null);
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.alarmitem_time))
                .setText(String.format(Locale.getDefault(), "%02d:%02d", cursor.getInt(1), cursor.getInt(2)));
            final long alarmId = cursor.getLong(0);
            Switch enableSwitch = (Switch) view.findViewById(R.id.alarmitem_switch);
            enableSwitch.setOnCheckedChangeListener(null);
            enableSwitch.setChecked(cursor.getInt(4) == 1);
            enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final ContentValues cv = new ContentValues();
                    cv.put("enabled", isChecked ? 1 : 0);
                    UpdateAsyncTask att = new UpdateAsyncTask(AlarmCursorAdapter.this.activity, alarmId, cv);
                    att.execute((Void) null);
                }
            });
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
            view.findViewById(R.id.alarmitem_sublayout).animate().setInterpolator(new LinearInterpolator())
                    .alpha(cursor.getInt(4) == 1 ? 1 : 0.7f).setDuration(50).start();
            Alarms.DaysOfWeek dow = new Alarms.DaysOfWeek((byte) cursor.getInt(3));
            DaysOfWeekCheckChanged dowC = new DaysOfWeekCheckChanged(activity, alarmId, dow);
            for(int i = 0; i < 7; i ++) {
                CheckBox cb = (CheckBox) view.findViewWithTag("cb-" + i);
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(dow.get(i));
                cb.setOnCheckedChangeListener(dowC);
            }
            view.findViewById(R.id.alarmitem_repeat_no).animate().setInterpolator(new LinearInterpolator())
                    .alpha(dow.isRepeat() ? 0 : 1).setDuration(50).start();
            view.findViewById(R.id.alarmitem_delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UpdateAsyncTask att = new UpdateAsyncTask(AlarmCursorAdapter.this.activity, alarmId);
                    att.execute((Void) null);
                }
            });
        }
        @Override
        public View newView(final Context context, Cursor cursor, final ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            TableRow dowTableRow = ((TableRow) view.findViewById(R.id.alarmitem_dowtable_row));
            for (int i = 0; i < 7; i ++) {
                LinearLayout cb_container = new LinearLayout(new ContextThemeWrapper(activity, R.style.AlarmItem_DaysOfWeekGird_Container));
                CheckBox cb = new CheckBox(new ContextThemeWrapper(activity, R.style.AlarmItem_DaysOfWeekGird_Item));
                cb.setTag("cb-" + i);
                cb.setTag(R.id.tag_dowTargetDay, (Object) i);
                cb_container.addView(cb);
                dowTableRow.addView(cb_container);
            }
            view.findViewById(R.id.alarmitem_sublayout).setAlpha(cursor.getInt(4) == 1 ? 1 : 0.7f);
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
            UpdateAsyncTask uat;
            if (this.alarmId >= 0) {
                uat = new UpdateAsyncTask((AlarmActivity) this.getActivity(), this.alarmId, cv);
            } else {
                uat = new UpdateAsyncTask((AlarmActivity) this.getActivity(), cv);
            }
            uat.execute((Void) null);
        }
    }

    protected CursorAdapter adpList;
    protected LoaderManager lm;
    protected ListView alarmlist;
    protected static final int LOADER_ID_SHOW_ALARMS = 0;
    protected Handler reQueryHandler;
    protected View fab;
    protected boolean scrollToBottomNeeded = false; // I know.
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
        this.fab = this.findViewById(R.id.fab_plus);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues cv = new ContentValues();
                TimePickerFragment tfr = new TimePickerFragment();
                Bundle args = new Bundle();
                Calendar now = Calendar.getInstance();
                now.add(Calendar.MINUTE, 1);
                args.putInt("hours", now.get(Calendar.HOUR_OF_DAY));
                args.putInt("minutes", now.get(Calendar.MINUTE));
                args.putLong("alarmId", -1);
                tfr.setArguments(args);
                // It will handle insert for us.
                tfr.show(AlarmActivity.this.getFragmentManager(), "timePicker");
            }
        });
    }

    protected void reQuery_AsyncCall(final boolean insert) {
        reQueryHandler.post(new Runnable() {
            @Override
            public void run() {
                AlarmActivity.this.findViewById(R.id.alarmitem_time);
                AlarmActivity.this.scrollToBottomNeeded = insert;
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
                    , null, null, "_id ASC");
        }
        throw new IllegalArgumentException("id not recognized.");
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_ID_SHOW_ALARMS) {
            Log.d("debug", String.format(Locale.getDefault(), "Data.getCount() = %d", data.getCount()));
            this.adpList.changeCursor(data);
            if (this.scrollToBottomNeeded) {
                this.alarmlist.smoothScrollToPosition(data.getCount());
                this.scrollToBottomNeeded = false;
            }
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
