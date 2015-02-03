package lab.prada.android.app.idrink;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.Calendar;
import java.util.Date;

import lab.prada.android.app.idrink.LogProvider.LogDbHelper;
import lab.prada.android.app.idrink.utils.Consts;
import lab.prada.android.app.idrink.utils.DBUtils;
import lab.prada.android.app.idrink.utils.NotificationSender;

public class MainActivity extends ActionBarActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    protected static final int REFRESH_VIEW = 0;
    private Handler mHandler;
    private AQuery aq;
    private ContentResolver mContentResolver;
    private ContentObserver mContentObserver;
    private SharedPreferences mPref;

    private ListView mListView;
    private LogAdapter mAdapter;

    public static final String KEY_DAILY_TARGET = "key_daily_target";
    public static final String PREF_NAME = "idrink_pref_name";

    public static final String ACT_ALARM_NOTIFICATION = "lab.prada.alarm.NOTIFY";

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

        mContentResolver = getContentResolver();
        mPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        aq = new AQuery(findViewById(R.id.root_view));
        aq.find(R.id.seekbar_current_cc_hourly).enabled(false);
        aq.find(R.id.seekbar_current_cc_daily).enabled(false);
        aq.find(R.id.btn_chart).clicked(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ChartActivity.class);
                startActivity(intent);
            }
        });
        refreshView();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case REFRESH_VIEW:
                        refreshView();
                        break;
                }
            }
        };

        mContentObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                mHandler.sendEmptyMessage(REFRESH_VIEW);
                mAdapter.changeCursor(getQuery()); // we reset cursor here
            }
        };
        mAdapter = new LogAdapter(this, getQuery());
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setAdapter(mAdapter);
    }

    private Cursor getQuery() {
        Calendar queryTime = Calendar.getInstance();
        queryTime.set(Calendar.HOUR, 0); //setting time to 0, as its set to current time by default.
        queryTime.set(Calendar.MINUTE, 0);
        long t1 = queryTime.getTimeInMillis();
        queryTime.set(Calendar.DATE, queryTime.get(Calendar.DATE) + 1);
        long t2 = queryTime.getTimeInMillis();

        return getContentResolver().query(
                LogProvider.URI,
                null, LogDbHelper.TIMESTAMP + ">? and " + LogDbHelper.TIMESTAMP + "<?",
                new String[] { String.valueOf(t1), String.valueOf(t2) },
                null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, AR_SETTING);
                return true;
            case R.id.action_debug:
                // Set an EditText view to get user input
                final EditText input = new EditText(this);
                new AlertDialog.Builder(this)
                    .setTitle(R.string.action_debug)
                    .setView(input)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                int data = Integer.valueOf(input.getText().toString());
                                // this is for debug notification
                                if (data >= Consts.LIMIT_CC_PER_DRINK) {
                                    String msg = String.format(getString(R.string.warning_limit_per_drink), Consts.LIMIT_CC_PER_DRINK);
                                    NotificationSender.send(MainActivity.this, R.string.notification_title_over_drink, msg, NotificationSender.NOTI_200_CC);
                                }

                                ContentValues values = new ContentValues();
                                values.put(LogDbHelper.WATER_CC, data);
                                values.put(LogDbHelper.TIMESTAMP, System.currentTimeMillis());
                                getContentResolver().insert(LogProvider.URI, values);

                                // this is for debug notification
                                int hourCc = DBUtils.getHourCc(MainActivity.this);
                                if (hourCc >= Consts.LIMIT_CC_PER_HOUR) {
                                    String msg = String.format(getString(R.string.warning_limit_per_hour), hourCc);
                                    NotificationSender.send(MainActivity.this, R.string.notification_title_over_drink, msg, NotificationSender.NOTI_OVER_HR);
                                }
                            } catch (Throwable t) {
                            }
                        }
                    }).setNegativeButton(R.string.cancel, null).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mContentResolver.registerContentObserver(LogProvider.URI, true, mContentObserver);
        mPref.registerOnSharedPreferenceChangeListener(this);
        refreshView();
    }

    @Override
    public void onPause() {
        super.onPause();
        mContentResolver.unregisterContentObserver(mContentObserver);
        mPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void refreshView() {
        if (aq == null) {
            return;
        }
        SeekBar dailySb = aq.find(R.id.seekbar_current_cc_daily).getSeekBar();
        SeekBar hourlySb = aq.find(R.id.seekbar_current_cc_hourly).getSeekBar();
        int dialyCc = DBUtils.getDailyCc(this);
        int hourlyCc = DBUtils.getHourCc(this);
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
        return mPref.getInt(KEY_DAILY_TARGET, Consts.DEFAULT_CC_PER_DAY);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (KEY_DAILY_TARGET.equals(s)) {
            refreshView();
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
