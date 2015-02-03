package lab.prada.android.app.idrink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import lab.prada.android.app.idrink.utils.Consts;
import lab.prada.android.app.idrink.utils.DBUtils;
import lab.prada.android.app.idrink.utils.NotificationSender;

public class AlarmBoardcastReceiver extends BroadcastReceiver {

    public final static String ACTION = "com.manish.alarm.ACTION";
    
    @Override
    public void onReceive(Context ctx, Intent intent) {
        int currentCc = DBUtils.getHourCc(ctx);
        int targetCc = getTargetCc(ctx);
        if (currentCc < targetCc) {
            String message = String.format(ctx.getString(R.string.warning_drink_not_enought),
                    currentCc, (targetCc - currentCc));
            NotificationSender.send(ctx, R.string.notification_title, message, NotificationSender.NOTI_ALARM);
        }
    }

    private int getTargetCc(Context ctx) {
        return ctx.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE)
                .getInt(MainActivity.KEY_DAILY_TARGET, Consts.DEFAULT_CC_PER_DAY) / 24;
    }

}
