package lab.prada.android.app.idrink;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


import org.eazegraph.lib.charts.ValueLineChart;
import org.eazegraph.lib.models.ValueLinePoint;
import org.eazegraph.lib.models.ValueLineSeries;

import java.util.Calendar;
import java.util.Date;

public class ChartActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        ValueLineChart chartView = (ValueLineChart) findViewById(R.id.chart_view);
        setupChartView(chartView);

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


    private void setupChartView(ValueLineChart view) {
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
        ValueLineSeries series = new ValueLineSeries();
        series.setColor(0xFF56B7F1);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            ContentValues values = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(c, values);
            long x = values.getAsLong(LogProvider.LogDbHelper.TIMESTAMP);
            int y = values.getAsInteger(LogProvider.LogDbHelper.WATER_CC);
            Date date = new Date(x);
            String d = date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();
            series.addPoint(new ValueLinePoint(d, y));
        }
        view.addSeries(series);
        view.startAnimation();

    }
}
