package lab.prada.android.app.idrink;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Vector;

public class ChartActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarGraphView lineGraphView = new BarGraphView(this, "iDrink");
        GraphViewStyle style = new GraphViewStyle(Color.BLACK, Color.BLACK, Color.BLACK);
        style.setLegendWidth(30);
        lineGraphView.setGraphViewStyle(style);
        lineGraphView.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override public String formatLabel(double value, boolean isValueX) {
                if (isValueX == false)
                    return String.format("%.0f", value);
                return "";
            }
        });

        GraphViewSeries series = getDataSeries();
        if (series == null) {
            Toast.makeText(this, R.string.waring_no_data, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        lineGraphView.addSeries(series);
        setContentView(lineGraphView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chart, menu);
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


    private GraphViewSeries getDataSeries() {
        Calendar queryTime = Calendar.getInstance();
        queryTime.set(Calendar.HOUR, 0); //setting time to 0, as its set to current time by default.
        queryTime.set(Calendar.MINUTE, 0);
        long t1 = queryTime.getTimeInMillis();
        queryTime.set(Calendar.DATE, queryTime.get(Calendar.DATE) + 1);
        long t2 = queryTime.getTimeInMillis();

        Cursor c = getContentResolver().query(LogProvider.URI,
                null,
                LogProvider.LogDbHelper.TIMESTAMP + ">?" + " and " + LogProvider.LogDbHelper.TIMESTAMP + "<?",
                new String[]{String.valueOf(t1), String.valueOf(t2)},
                LogProvider.LogDbHelper.TIMESTAMP + " ASC");

        Vector<GraphView.GraphViewData> data = new Vector<GraphView.GraphViewData>(c.getCount());
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            ContentValues values = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(c, values);
            long x = values.getAsLong(LogProvider.LogDbHelper.TIMESTAMP);
            int y = values.getAsInteger(LogProvider.LogDbHelper.WATER_CC);
            android.util.Log.e("PC", " x = " + x + ", y = " + y);
            data.add(new GraphView.GraphViewData(x, y));
        }
        if (data.isEmpty()) {
            return null;
        } else {
            return new GraphViewSeries(data.toArray(new GraphView.GraphViewData[]{}));
        }
    }
}
