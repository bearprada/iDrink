package lab.prada.android.app.idrink;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements OnClickListener {

        private static final int AR_SETTING = 0;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container,
                    false);
            rootView.findViewById(R.id.btn_list).setOnClickListener(this);
            rootView.findViewById(R.id.btn_setting).setOnClickListener(this);
            return rootView;
        }

        @Override
        public void onClick(View v) {
            Intent intent;
            switch(v.getId()) {
            case R.id.btn_list:
                intent = new Intent(getActivity(), LogActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting:
                intent = new Intent(getActivity(), SettingsActivity.class);
                startActivityForResult(intent, AR_SETTING);
                break;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch(requestCode) {
            case AR_SETTING:
                if (resultCode == Activity.RESULT_OK) {
                    // TODO refresh setting
                }
                break;
            }
        }
    }

}
