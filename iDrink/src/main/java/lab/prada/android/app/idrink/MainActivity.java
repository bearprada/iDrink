package lab.prada.android.app.idrink;

import java.util.Calendar;

import lab.prada.android.app.idrink.LogProvider.LogDbHelper;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.androidquery.AQuery;

public class MainActivity extends ActionBarActivity {

    static final String ACT_ALARM_NOTIFICATION = "lab.prada.alarm.NOTIFY";

    private static final int AR_SETTING             = 1;
    public static final int AR_ALARM_TRIGGER        = 2;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ACT_ALARM_NOTIFICATION.equals(getIntent().getAction())) {
            // TODO remove notification from manager
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, AR_SETTING);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements OnClickListener {
        private Handler mHandler;
        private AQuery aq;
        private ContentResolver mContentResolver;
        private ContentObserver mContentObserver;

        public PlaceholderFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContentResolver = getActivity().getContentResolver();
        }

        @Override
        public void onResume() {
            super.onResume();
            mContentResolver.registerContentObserver(LogProvider.URI, true, mContentObserver);
        }

        @Override
        public void onPause() {
            super.onPause();
            mContentResolver.unregisterContentObserver(mContentObserver);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container,
                    false);
            aq = new AQuery(rootView);
            aq.find(R.id.btn_list).clicked(this);
            aq.find(R.id.seekbar_current_cc_hourly).enabled(false);
            aq.find(R.id.seekbar_current_cc_daily).enabled(false);
            refreshView();

            mHandler = new Handler();
            mContentObserver = new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    refreshView();
                }
            };
            return rootView;
        }

        private void refreshView() {
            if (aq == null) {
                return;
            }
            SeekBar dailySb = aq.find(R.id.seekbar_current_cc_daily).getSeekBar();
            SeekBar hourlySb = aq.find(R.id.seekbar_current_cc_hourly).getSeekBar();
            int dialyCc = getDailyCc();
            int hourlyCc = getHourCc();
            dailySb.setMax(getDialyTarget());
            dailySb.setProgress(dialyCc);
            hourlySb.setMax(getHourlyTarget());
            hourlySb.setProgress(hourlyCc);

            aq.find(R.id.text_current_cc_daily)
                .text(String.format("%d / %d cc", dialyCc, dailySb.getMax()));
            aq.find(R.id.text_current_cc_hourly)
                .text(String.format("%d / %d cc", hourlyCc, hourlySb.getMax()));
        }

        private int getHourlyTarget() {
            return getDialyTarget() / 24;
        }

        private int getDialyTarget() {
            return 3000; // FIXME
        }
        private int getHourCc() {
            Calendar queryTime = Calendar.getInstance();
            queryTime.set(Calendar.MINUTE, 0);
            long t1 = queryTime.getTimeInMillis();
            queryTime.set(Calendar.HOUR, queryTime.get(Calendar.HOUR) + 1);
            long t2 = queryTime.getTimeInMillis();
            return sumCc(t1, t2);
        }

        private int sumCc(long t1, long t2) {
            Cursor c = mContentResolver.query(LogProvider.URI,
                    null, LogDbHelper.TIMESTAMP + ">?" + " and " + LogDbHelper.TIMESTAMP + "<?", 
                    new String[]{String.valueOf(t1), String.valueOf(t2)}, null);
            int total = 0;
            try {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    ContentValues values = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(c, values);
                    total += values.getAsInteger(LogDbHelper.WATER_CC);
                }
            } finally {
                c.close();
            }
            return total;
        }

        private int getDailyCc() {
            Calendar queryTime = Calendar.getInstance();
            queryTime.set(Calendar.HOUR, 0);
            queryTime.set(Calendar.MINUTE, 0);
            long t1 = queryTime.getTimeInMillis();
            queryTime.set(Calendar.DATE, queryTime.get(Calendar.DATE) + 1);
            long t2 = queryTime.getTimeInMillis();
            return sumCc(t1, t2);
        }

        @Override
        public void onClick(View v) {
            Intent intent;
            switch(v.getId()) {
            case R.id.btn_list:
                intent = new Intent(getActivity(), LogActivity.class);
                startActivity(intent);
                break;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch(requestCode) {
            case AR_SETTING:
                if (resultCode == Activity.RESULT_OK) {
                    refreshView();
                }
                break;
            }
        }
    }

}
