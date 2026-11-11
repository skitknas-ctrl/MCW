package se.dick.mcw;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;

public class MissedCallWidget extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case TelephonyManager.ACTION_PHONE_STATE_CHANGED: {
                // När samtalet är slut skrivs det till samtalsloggen strax efteråt,
                // så kontrollera både nu och en stund senare.
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                    updateAll(context);
                    CallLogJob.scheduleDelayed(context);
                }
                break;
            }
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                CallLogJob.scheduleAll(context);
                updateAll(context);
                break;
            default:
                break;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        CallLogJob.scheduleAll(context);
        RemoteViews views = buildViews(context);
        for (int id : ids) manager.updateAppWidget(id, views);
    }

    @Override
    public void onEnabled(Context context) {
        CallLogJob.scheduleAll(context);
    }

    @Override
    public void onDisabled(Context context) {
        CallLogJob.cancelAll(context);
    }

    /** Ritar om alla widgetar. */
    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, MissedCallWidget.class));
        if (ids.length == 0) return;
        RemoteViews views = buildViews(context);
        for (int id : ids) manager.updateAppWidget(id, views);
    }

    static RemoteViews buildViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_mcw);
        int bg, icon, desc;
        switch (MissedCalls.state(context)) {
            case MISSED:
                bg = R.drawable.bg_missed;
                icon = R.drawable.ic_missed;
                desc = R.string.state_missed;
                break;
            case NO_PERMISSION:
                bg = R.drawable.bg_noperm;
                icon = R.drawable.ic_noperm;
                desc = R.string.state_noperm;
                break;
            default:
                bg = R.drawable.bg_idle;
                icon = R.drawable.ic_idle;
                desc = R.string.state_none;
                break;
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", bg);
        views.setImageViewResource(R.id.widget_icon, icon);
        views.setContentDescription(R.id.widget_icon, context.getString(desc));

        Intent open = new Intent(context, OpenCallLogActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_root, pi);
        return views;
    }
}
