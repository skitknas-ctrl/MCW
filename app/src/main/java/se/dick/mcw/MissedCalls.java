package se.dick.mcw;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

/** Läser samtalsloggen och avgör om det finns missade samtal som inte har setts. */
public final class MissedCalls {

    public enum State { NO_PERMISSION, MISSED, NONE }

    private static final String PREFS = "mcw";
    /** Tidpunkt då samtalsloggen senast öppnades via widgeten; äldre missade samtal räknas som sedda. */
    private static final String KEY_ACK = "ack_time";

    private MissedCalls() {}

    public static boolean hasPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static State state(Context context) {
        if (!hasPermission(context)) return State.NO_PERMISSION;
        return count(context) > 0 ? State.MISSED : State.NONE;
    }

    /**
     * Antal missade samtal som är nya och olästa, och som kommit in efter att samtalsloggen
     * senast öppnades via widgeten. Telefonappen nollställer själv NEW/IS_READ när man tittar
     * i samtalsloggen eller på aviseringen, så widgeten släcks även då.
     */
    public static int count(Context context) {
        String sel = CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE
                + " AND " + CallLog.Calls.NEW + "=1"
                + " AND (" + CallLog.Calls.IS_READ + " IS NULL OR " + CallLog.Calls.IS_READ + "=0)"
                + " AND " + CallLog.Calls.DATE + ">?";
        String[] args = { String.valueOf(ackTime(context)) };
        try (Cursor c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[] { CallLog.Calls._ID }, sel, args, null)) {
            return c == null ? 0 : c.getCount();
        } catch (SecurityException e) {
            return 0;
        } catch (RuntimeException e) {
            // Vissa telefoner saknar IS_READ-kolumnen; försök utan den.
            String sel2 = CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE
                    + " AND " + CallLog.Calls.NEW + "=1"
                    + " AND " + CallLog.Calls.DATE + ">?";
            try (Cursor c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    new String[] { CallLog.Calls._ID }, sel2, args, null)) {
                return c == null ? 0 : c.getCount();
            } catch (RuntimeException e2) {
                return 0;
            }
        }
    }

    public static long ackTime(Context context) {
        return prefs(context).getLong(KEY_ACK, 0L);
    }

    /** Markerar alla hittills missade samtal som sedda. */
    public static void acknowledge(Context context) {
        prefs(context).edit().putLong(KEY_ACK, System.currentTimeMillis()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
