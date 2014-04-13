package lab.prada.android.app.idrink;

import java.util.Calendar;
import java.util.Date;

import lab.prada.android.app.idrink.LogProvider.LogDbHelper;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.AbsListViewDelegate;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

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
            // TODO go to chart view
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements OnRefreshListener {

        private ListView mListView;
        private LogAdapter mAdapter;
        private PullToRefreshLayout mPullToRefreshLayout;
        private Handler mHandler;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_log, container,
                    false);
            mAdapter = new LogAdapter(getActivity());
            mListView = (ListView) rootView.findViewById(R.id.list_view);
            mListView.setAdapter(mAdapter);
            updateData();

            mPullToRefreshLayout = (PullToRefreshLayout) rootView.findViewById(R.id.ptr_layout);
            ActionBarPullToRefresh.from(getActivity())
                .options(Options.create()
                        .scrollDistance(.5f)
                        .build())
                .theseChildrenArePullable(R.id.list_view)
                .useViewDelegate(GridView.class, new AbsListViewDelegate())
                .listener(this)
                .setup(mPullToRefreshLayout);
            mHandler = new Handler();
            return rootView;
        }

        private void updateData() {
            mAdapter.clear();

            Calendar queryTime = Calendar.getInstance();
            queryTime.set(Calendar.HOUR, 0); //setting time to 0, as its set to current time by default. 
            queryTime.set(Calendar.MINUTE, 0);
            long t1 = queryTime.getTimeInMillis();

            queryTime.set(Calendar.DATE, queryTime.get(Calendar.DATE) + 1);
            long t2 = queryTime.getTimeInMillis();

            Cursor c = getActivity().getContentResolver().query(LogProvider.URI,
                    null, LogDbHelper.TIMESTAMP + ">?" + " and " + LogDbHelper.TIMESTAMP + "<?", 
                    new String[]{String.valueOf(t1), String.valueOf(t2)}, null);
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                ContentValues values = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(c, values);
                mAdapter.add(new LogData(values));
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onRefreshStarted(View view) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateData();
                    mPullToRefreshLayout.setRefreshComplete();
                }
            }, 1000);
        }
    }

    public static class LogAdapter extends ArrayAdapter<LogData> {

        private final LayoutInflater mInFlator;

        public LogAdapter(Context context) {
            super(context, -1);
            mInFlator = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = mInFlator.inflate(R.layout.item_log, null);
            }
            TextView tvCC = (TextView) view.findViewById(R.id.textview_cc);
            TextView tvTime = (TextView) view.findViewById(R.id.textview_timestamp);
            SeekBar sbCc = (SeekBar) view.findViewById(R.id.seekbar_current_cc);
            sbCc.setEnabled(false);
            LogData data = getItem(position);
            tvCC.setText(data.mCc + "cc");
            Date date = new Date(data.mTimestamp);
            tvTime.setText(date.getHours() + ":" + date.getMinutes());
            sbCc.setProgress(data.mCc);
            return view;
        }
    }

    public static class LogData {
        final public long mTimestamp;
        final public int mCc;

        public LogData(int cc, long timestamp) {
            mCc = cc;
            mTimestamp = timestamp;
        }

        public LogData(ContentValues values) {
            mCc = values.getAsInteger(LogDbHelper.WATER_CC);
            mTimestamp = values.getAsLong(LogDbHelper.TIMESTAMP);
        }
    }

}
