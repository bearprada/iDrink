package lab.prada.android.app.idrink;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;

import com.androidquery.AQuery;

import java.util.Calendar;

import lab.prada.android.app.idrink.LogProvider.LogDbHelper;

public class MainActivity extends ActionBarActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    protected static final int REFRESH_VIEW = 0;
    private Handler mHandler;
    private AQuery aq;
    private ContentResolver mContentResolver;
    private ContentObserver mContentObserver;
    private SharedPreferences mPref;

    public static final String KEY_DAILY_TARGET = "key_daily_target";
    public static final String PREF_NAME = "idrink_pref_name";

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

        mContentResolver = getContentResolver();
        mPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        aq = new AQuery(findViewById(R.id.root_view));
        aq.find(R.id.btn_list).clicked(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LogActivity.class);
                startActivity(intent);
            }
        });
        aq.find(R.id.seekbar_current_cc_hourly).enabled(false);
        aq.find(R.id.seekbar_current_cc_daily).enabled(false);
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
            }
        };
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
        return mPref.getInt(KEY_DAILY_TARGET, 3000);
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
                if (values.containsKey(LogDbHelper.WATER_CC)) {
                    Integer cc = values.getAsInteger(LogDbHelper.WATER_CC);
                    if (cc != null) {
                        total += values.getAsInteger(LogDbHelper.WATER_CC);
                    }
                }
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
}
