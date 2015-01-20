package lab.prada.android.app.idrink;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import lab.prada.android.app.idrink.LogProvider.LogDbHelper;

public class LogActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_chart) {
            Intent intent = new Intent(this, ChartActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private ListView mListView;
        private LogAdapter mAdapter;
        private ContentObserver mContentObserver;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_log, container,
                    false);
            mAdapter = new LogAdapter(getActivity(), getQuery());
            mContentObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    mAdapter.changeCursor(getQuery()); // we reset cursor here
                }
            };
            mListView = (ListView) rootView.findViewById(R.id.list_view);
            mListView.setAdapter(mAdapter);
            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            getActivity().getContentResolver().registerContentObserver(LogProvider.URI, true, mContentObserver);
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
        }

        private Cursor getQuery() {
            Calendar queryTime = Calendar.getInstance();
            queryTime.set(Calendar.HOUR, 0); //setting time to 0, as its set to current time by default.
            queryTime.set(Calendar.MINUTE, 0);
            long t1 = queryTime.getTimeInMillis();
            queryTime.set(Calendar.DATE, queryTime.get(Calendar.DATE) + 1);
            long t2 = queryTime.getTimeInMillis();

            return getActivity().getContentResolver().query(
                    LogProvider.URI,
                    null, LogDbHelper.TIMESTAMP + ">? and " + LogDbHelper.TIMESTAMP + "<?",
                    new String[] { String.valueOf(t1), String.valueOf(t2) },
                    null);
        }
    }

    public static class LogAdapter extends CursorAdapter {

        public LogAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.item_log, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tvCC = (TextView) view.findViewById(R.id.textview_cc);
            TextView tvTime = (TextView) view.findViewById(R.id.textview_timestamp);
            SeekBar sbCc = (SeekBar) view.findViewById(R.id.seekbar_current_cc);
            sbCc.setEnabled(false);
            int idxCc = cursor.getColumnIndex(LogDbHelper.WATER_CC);
            int idxTime = cursor.getColumnIndex(LogDbHelper.TIMESTAMP);
            int cc = cursor.getInt(idxCc);
            long timestamp = cursor.getLong(idxTime);
            tvCC.setText(cc + "cc");
            Date date = new Date(timestamp);
            tvTime.setText(date.getHours() + ":" + date.getMinutes());
            sbCc.setProgress(cc);
        }
    }
}