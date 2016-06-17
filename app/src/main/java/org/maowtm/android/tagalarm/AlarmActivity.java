package org.maowtm.android.tagalarm;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import java.util.Locale;

public class AlarmActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static class AlarmCursorAdapter extends ResourceCursorAdapter {
        public AlarmCursorAdapter(Context context) {
            super(context, R.layout.alarmlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.alarmitem_time))
                .setText(String.format(Locale.getDefault(), "%02d:%02d", cursor.getInt(1), cursor.getInt(2)));
        }
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            return view;
        }
    }
    public static class RecalculateAsyncTask extends AsyncTask<Context,Void,Void> {
        protected Void doInBackground(Context... ctx) {
            if (ctx.length != 1)
                throw new IllegalArgumentException("ctx.length must be 1.");
            Context ct = ctx[0];
            ct.getContentResolver().update(Uri.parse("content://" + AlarmProvider.AUTH + "/alarm/recalculate"),
                    null, null, null);
            return null;
        }
    }

    protected CursorAdapter adpList;
    protected LoaderManager lm;
    protected ListView alarmlist;
    protected static final int LOADER_ID_SHOW_ALARMS = 0;
    @Override
    protected void onCreate(Bundle sis) {
        super.onCreate(sis);
        this.lm = this.getLoaderManager();
        lm.initLoader(LOADER_ID_SHOW_ALARMS, null, this);
        this.adpList = new AlarmCursorAdapter(this);
        this.setContentView(R.layout.mainlayout);
        this.alarmlist = (ListView) this.findViewById(R.id.alarmslist);
        this.alarmlist.setAdapter(this.adpList);
        if (sis == null) {
            new RecalculateAsyncTask().execute(this);
        }
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
