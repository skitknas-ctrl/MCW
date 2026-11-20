package se.dick.mcw;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.SizeF;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MissedCallWidget extends AppWidgetProvider {

    /**
     * Luren ligger ner när widgeten är tydligt bredare än hög, annars står den upp.
     * Kvadratiska och nästan kvadratiska widgetar (högst 10 % bredare än höga) räknas som 1:1 och får stående lur.
     */
    private static final float WIDE_RATIO = 1.1f;

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
        for (int id : ids) manager.updateAppWidget(id, buildViews(context, manager, id));
    }

    /** Widgeten har fått ny storlek: luren kan behöva vändas. */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle newOptions) {
        manager.updateAppWidget(id, buildViews(context, manager, id));
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
        for (int id : ids) manager.updateAppWidget(id, buildViews(context, manager, id));
    }

    static boolean isWide(float width, float height) {
        return width > height * WIDE_RATIO;
    }

    /**
     * Bygger widgetens utseende för en viss widget. Hemskärmen talar om vilka storlekar widgeten kan få
     * (en för stående och en för liggande telefon); varje storlek får lur åt rätt håll, och hemskärmen
     * väljer den som passar just nu. Då blir det rätt även när telefonen vrids.
     */
    static RemoteViews buildViews(Context context, AppWidgetManager manager, int id) {
        MissedCalls.State state = MissedCalls.state(context);
        Bundle options = manager.getAppWidgetOptions(id);
        ArrayList<SizeF> sizes = options == null ? null
                : options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
        if (sizes != null && !sizes.isEmpty()) {
            Map<SizeF, RemoteViews> bySize = new HashMap<>();
            for (SizeF size : sizes) {
                if (bySize.size() >= 16) break; // systemets gräns
                bySize.put(size, buildViews(context, state, isWide(size.getWidth(), size.getHeight())));
            }
            return new RemoteViews(bySize);
        }
        // Reserv om hemskärmen inte anger storlekar: stående telefon = minsta bredd och största höjd.
        boolean wide = false;
        if (options != null) {
            boolean landscape = context.getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            int w = options.getInt(landscape ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                    : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int h = options.getInt(landscape ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                    : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            wide = w > 0 && h > 0 && isWide(w, h);
        }
        return buildViews(context, state, wide);
    }

    /** @param wide true = liggande lur (widgeten är bredare än hög), false = stående lur */
    static RemoteViews buildViews(Context context, MissedCalls.State state, boolean wide) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_mcw);
        int bg, icon, desc;
        switch (state) {
            case MISSED:
                bg = R.drawable.bg_missed;
                icon = wide ? R.drawable.ic_missed : R.drawable.ic_missed_v;
                desc = R.string.state_missed;
                break;
            case NO_PERMISSION:
                bg = R.drawable.bg_noperm;
                icon = wide ? R.drawable.ic_noperm : R.drawable.ic_noperm_v;
                desc = R.string.state_noperm;
                break;
            default:
                bg = R.drawable.bg_idle;
                icon = wide ? R.drawable.ic_idle : R.drawable.ic_idle_v;
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
