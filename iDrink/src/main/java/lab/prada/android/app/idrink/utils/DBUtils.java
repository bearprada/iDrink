package lab.prada.android.app.idrink.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;

import java.util.Calendar;

import lab.prada.android.app.idrink.LogProvider;

/**
 * Created by prada on 15/2/3.
 */
public class DBUtils {

    public static int getDailyCc(Context ctx) {
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, 0);
        t.set(Calendar.MINUTE, 0);
        long t1 = t.getTimeInMillis();
        t.set(Calendar.DATE, t.get(Calendar.DATE) + 1);
        long t2 = t.getTimeInMillis();
        return sumCc(ctx, t1, t2);
    }

    public static int getHourCc(Context ctx) {
        Calendar queryTime = Calendar.getInstance();
        queryTime.set(Calendar.MINUTE, 0);
        long t1 = queryTime.getTimeInMillis();
        queryTime.set(Calendar.HOUR, queryTime.get(Calendar.HOUR) + 1);
        long t2 = queryTime.getTimeInMillis();
        return sumCc(ctx, t1, t2);
    }

    private static int sumCc(Context ctx, long t1, long t2) {
        Cursor c = ctx.getContentResolver().query(LogProvider.URI,
                null, LogProvider.LogDbHelper.TIMESTAMP + ">?" + " and " + LogProvider.LogDbHelper.TIMESTAMP + "<?",
                new String[]{String.valueOf(t1), String.valueOf(t2)}, null);
        int total = 0;
        try {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                ContentValues values = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(c, values);
                total += values.getAsInteger(LogProvider.LogDbHelper.WATER_CC);
            }
        } finally {
            c.close();
        }
        return total;
    }
}
