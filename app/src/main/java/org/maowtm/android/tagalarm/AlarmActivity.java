package org.maowtm.android.tagalarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;

import android.text.format.DateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        protected LayoutInflater inflater;
        protected String[] powTypeNames;
        public AlarmCursorAdapter(AlarmActivity am) {
            super(am, R.layout.alarmlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            this.activity = am;
            this.inflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.powTypeNames = this.activity.getResources().getStringArray(R.array.pow_types);
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
                dowInitial.set(((DaysOfWeekCheckBox) buttonView).getDowTarget(), isChecked);
                cv.put("daysofweek", dowInitial.getCode());
                cv.put("enabled", 1);
                UpdateAsyncTask uat = new UpdateAsyncTask(this.activity, this.alarmId, cv);
                uat.execute((Void) null);
            }
        }

        @Override
        public void bindView(View view, final Context context, Cursor cursor) {
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
                DaysOfWeekCheckBox cb = (DaysOfWeekCheckBox) view.findViewWithTag("cb-" + i);
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
            view.findViewById(R.id.alarmitem_test).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent bd = new Intent(Alarms.ACTION_ALARM_ALERT);
                    bd.putExtra(Alarms.INTENT_EXTRA_ALARM_ID, alarmId);
                    bd.putExtra(Alarms.INTENT_EXTRA_ALLOW_DIRECT_DISMISS, true);
                    context.sendBroadcast(bd);
                }
            });

            LinearLayout powList = (LinearLayout) view.findViewById(R.id.alarmitem_pow_list);
            ProofOfWakes pow;
            ProofOfWakes.ProofRequirement[] powR;
            powList.removeAllViews();
            boolean added = false;
            try {
                pow = new ProofOfWakes(new JSONArray(cursor.getString(5)));
                powR = pow.getAll();
                for (ProofOfWakes.ProofRequirement pr : powR) {
                    powList.addView(this.getViewForPr(pr, powList));
                    added = true;
                }
            } catch (JSONException e) {
                // TODO
            }
            if (!added) {
                view.findViewById(R.id.alarmitem_pow_list_desc)
                        .setVisibility(View.GONE);
            } else {
                ((TextView) view.findViewById(R.id.alarmitem_pow_add_text))
                        .setText(R.string.alarmitem_pow_add_more);
            }
        }

        public static class ShakeDialogFragment extends DialogFragment {
            protected LayoutInflater inflater;
            protected ProofOfWakes.ProofRequirement pr;
            public ShakeDialogFragment() {
                super();
            }
            @Override
            public Dialog onCreateDialog(Bundle sis) {
                this.inflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                String prJSON = this.getArguments().getString("pr");
                if (prJSON == null)
                    throw new IllegalStateException("no pr argument.");
                try {
                    this.pr = ProofOfWakes.ProofRequirement.fromJSONObject(new JSONObject(prJSON));
                } catch (JSONException e) {
                    throw new IllegalStateException("pr argument not valid.", e);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.powconfig_shake_title);
                builder.setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                View vg = inflater.inflate(R.layout.powconfigshake, null);
                ((EditText) vg.findViewById(R.id.powconfig_shake_amount))
                        .setText(String.format(Locale.getDefault(), "%d", pr.amount));
                builder.setView(vg);
                return builder.create();
            }
        }

        protected View getViewForPr(final ProofOfWakes.ProofRequirement pr, ViewGroup parent) {
            View v = this.inflater.inflate(R.layout.alarmlistpowlistitem, parent, false);
            TextView nameText = (TextView) v.findViewById(R.id.alarmitem_pow_item_name);
            nameText.setText(pr.toString(this.activity.getResources()));
            nameText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (pr.type) {
                        case ProofOfWakes.ProofRequirement.TYPE_SHAKE:
                            ShakeDialogFragment sdf = new ShakeDialogFragment();
                            Bundle args = new Bundle();
                            try {
                                args.putString("pr", pr.toJSON().toString());
                            } catch (JSONException e) {
                                AlarmCursorAdapter.this.activity.reQuery_AsyncCall(false);
                                break;
                            }
                            sdf.setArguments(args);
                            sdf.show(AlarmCursorAdapter.this.activity.getFragmentManager(), "pow_config_shake");
                            break;
                        case ProofOfWakes.ProofRequirement.TYPE_WAIT:
                            DialogFragment df = new DialogFragment() {
                                @Override
                                public Dialog onCreateDialog(Bundle sis) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                    builder.setTitle(R.string.powconfig_wait_title);
                                    return builder.create();
                                }
                            };
                            df.show(AlarmCursorAdapter.this.activity.getFragmentManager(), "pow_config_shake");
                            break;
                    }
                }
            });
            return v;
        }
        @Override
        public View newView(final Context context, Cursor cursor, final ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            TableRow dowTableRow = ((TableRow) view.findViewById(R.id.alarmitem_dowtable_row));
            for (int i = 0; i < 7; i ++) {
                LinearLayout cb_container = new LinearLayout(new ContextThemeWrapper(activity, R.style.AlarmItem_DaysOfWeekGird_Container));
                DaysOfWeekCheckBox cb = new DaysOfWeekCheckBox(new ContextThemeWrapper(activity, R.style.AlarmItem_DaysOfWeekGird_Item));
                cb.setTag("cb-" + i);
                cb.setDowTarget(i);
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
    protected int test = 0;
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

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ViewGroup.LayoutParams lp = this.alarmlist.getLayoutParams();
            DisplayMetrics metrics = this.getResources().getDisplayMetrics();
            int max_width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, metrics);
            lp.width = (metrics.widthPixels > max_width ? max_width : ViewGroup.LayoutParams.MATCH_PARENT);
            this.alarmlist.setLayoutParams(lp);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("reach", "onDestroy()");
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
            return new CursorLoader(this, query, new String[] {"id AS _id", "hours", "minutes", "daysofweek", "enabled", "proofwake"}
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
