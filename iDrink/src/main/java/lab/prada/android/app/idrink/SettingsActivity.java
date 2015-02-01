package lab.prada.android.app.idrink;

import lab.prada.android.app.idrink.service.BluetoothChatService;
import lab.prada.android.app.idrink.utils.Consts;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    private static final int REQUEST_CONNECT_DEVICE = 1;

    private BluetoothChatService mChatService;
    private AlarmManager mAlarmService;
    private PendingIntent mPendingIntent;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        bindService(new Intent(this, 
                BluetoothChatService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(MainActivity.PREF_NAME);

        mAlarmService = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmBoardcastReceiver.class);
        intent.setAction(AlarmBoardcastReceiver.ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, MainActivity.AR_ALARM_TRIGGER, intent, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_bluetooth_pair:
            // if no connection.
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        default:
            return false;
        }
    }

    void cancelAlarmTrigger() {
        mAlarmService.cancel(mPendingIntent);
        Toast.makeText(this, R.string.warning_alarm_stop, Toast.LENGTH_LONG).show();
    }

    void setAlarmTrigger() {
        int duration = Integer.valueOf(getPreferenceManager().getSharedPreferences()
                .getString(getString(R.string.key_duration), String.valueOf(Consts.DEFAULT_DURATION)));
        mAlarmService.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                duration * 1000, mPendingIntent);
        Toast.makeText(this, R.string.warning_alarm_start, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);
    }

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Attempt to connect to the device
                if (mChatService != null) {
                    mChatService.connect(address);
                }
                Toast.makeText(this, "conntect to " + address, Toast.LENGTH_LONG).show();
            }
            break;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mChatService = ((BluetoothChatService.LocalBinder)service).getService();

            // Tell the user about this for our demo.
            Toast.makeText(SettingsActivity.this, R.string.local_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mChatService = null;
            Toast.makeText(SettingsActivity.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (getString(R.string.key_notification).equals(key)) {
            if (pref.getBoolean(key, false)) {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setAlarmTrigger();
                    }
                });
            } else {
                cancelAlarmTrigger();
            }
            return;
        }
        if (getString(R.string.key_duration).equals(key)) {
            cancelAlarmTrigger();
            setAlarmTrigger();
            return;
        }
        if (getString(R.string.key_weight).equals(key) || getString(R.string.key_mode).equals(key)) {
            calculateTarget(pref);
            return;
        }
    }

    private void calculateTarget(SharedPreferences pref) {
        // 1 kg x 30 cc(but athlete should be 40 cc) = daily requirement
        int scale = pref.getBoolean(getString(R.string.key_mode), false) ? 40 : 30;
        int weight = Integer.valueOf(pref.getString(getString(R.string.key_weight), String.valueOf(Consts.DEFAULT_KG)));
        pref.edit().putInt(MainActivity.KEY_DAILY_TARGET, weight * scale).apply();
    }
}
