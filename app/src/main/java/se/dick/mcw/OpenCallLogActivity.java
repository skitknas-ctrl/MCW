package se.dick.mcw;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog;

/**
 * Startas när widgeten trycks. Markerar de missade samtalen som sedda (widgeten blir grå)
 * och öppnar telefonappens samtalslogg. Saknas behörighet öppnas appen i stället.
 */
public class OpenCallLogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!MissedCalls.hasPermission(this)) {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            MissedCalls.acknowledge(this);
            MissedCallWidget.updateAll(this);
            openCallLog(this);
        }
        finish();
        overridePendingTransition(0, 0);
    }

    /** Öppnar telefonappens lista med senaste samtal; faller tillbaka på knappsatsen. */
    static void openCallLog(Context context) {
        Intent log = new Intent(Intent.ACTION_VIEW)
                .setType(CallLog.Calls.CONTENT_TYPE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(log);
        } catch (ActivityNotFoundException e) {
            try {
                context.startActivity(new Intent(Intent.ACTION_DIAL)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException ignored) {
                // Ingen telefonapp alls.
            }
        }
    }
}
