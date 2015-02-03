package lab.prada.android.app.idrink.utils;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import lab.prada.android.app.idrink.MainActivity;
import lab.prada.android.app.idrink.R;

/**
 * Created by prada on 15/2/3.
 */
public class NotificationSender {
    public static final int NOTI_ALARM         = 100;
    public static final int NOTI_OVER_HR       = 101;
    public static final int NOTI_200_CC        = 102;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void send(Context ctx, int titleStrId, String message, int notifId) {
        Notification.Builder builder = new Notification.Builder(ctx);
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setAction(MainActivity.ACT_ALARM_NOTIFICATION);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
        Notification notification = builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .setContentTitle(ctx.getString(titleStrId))
                .setContentText(message)
                .getNotification();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;

        NotificationManager notificationMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationMgr.notify(notifId, notification);
    }
}
