package lab.prada.android.app.idrink;

import java.util.Calendar;

import lab.prada.android.app.idrink.LogProvider.LogDbHelper;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Build;

public class AlarmBoardcastReceiver extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 0;
    public final static String ACTION = "com.manish.alarm.ACTION";
    
    @Override
    public void onReceive(Context ctx, Intent intent) {
        int currentCc = getHourCc(ctx);
        int targetCc = getTargetCc(ctx);
        if (currentCc < targetCc) {
            sendNotification(ctx, currentCc, targetCc);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void sendNotification(Context ctx, int currentCc, int targetCc) {
        Notification.Builder builder = new Notification.Builder(ctx);
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setAction(MainActivity.ACT_ALARM_NOTIFICATION);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

        String message = String.format("目前您這小時攝取水量%dcc，距離目標還有%dcc", 
                currentCc, (targetCc - currentCc));
        Notification notification = builder
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setContentTitle(ctx.getString(R.string.notification_title))
            .setContentText(message)
            .build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        
        NotificationManager notificationMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationMgr.notify(NOTIFICATION_ID, notification);
    }

    private int getTargetCc(Context ctx) {
        return 3000/24; // TODO replace to real implementation
    }

    private int getHourCc(Context ctx) {
        Calendar queryTime = Calendar.getInstance();
        queryTime.set(Calendar.MINUTE, 0);
        long t1 = queryTime.getTimeInMillis();
        queryTime.set(Calendar.HOUR, queryTime.get(Calendar.HOUR) + 1);
        long t2 = queryTime.getTimeInMillis();
        return sumCc(ctx, t1, t2);
    }

    private int sumCc(Context ctx, long t1, long t2) {
        Cursor c = ctx.getContentResolver().query(LogProvider.URI,
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
}
