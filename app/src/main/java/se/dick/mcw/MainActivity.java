package se.dick.mcw;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;

/** Startsida: behörigheter, lägga till widgeten, batterioptimering och versionsinfo. */
public class MainActivity extends Activity {

    private static final int REQ_PERMS = 1;
    private static final String[] PERMS = {
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
    };

    private TextView status;
    private Button permButton, batteryButton, pinButton;
    /** Sant när användaren redan nekat och systemet inte längre visar frågan. */
    private boolean askedOnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Marginal mot statusfält / kameraurtag och navigeringsfält (edge-to-edge)
        View root = findViewById(R.id.main_root);
        int basePad = root.getPaddingTop();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(basePad + bars.left, basePad + bars.top,
                    basePad + bars.right, basePad + bars.bottom);
            return insets;
        });

        status = findViewById(R.id.status);
        permButton = findViewById(R.id.button_permission);
        batteryButton = findViewById(R.id.button_battery);
        pinButton = findViewById(R.id.button_pin);

        permButton.setOnClickListener(v -> requestPerms());
        batteryButton.setOnClickListener(v -> requestBatteryExemption());
        pinButton.setOnClickListener(v -> pinWidget());
        findViewById(R.id.button_test).setOnClickListener(v -> OpenCallLogActivity.openCallLog(this));

        ((TextView) findViewById(R.id.version)).setText(getString(R.string.version,
                BuildConfig.VERSION_NAME, BuildConfig.BUILD_DATE));

        if (!MissedCalls.hasPermission(this)) requestPerms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean perm = MissedCalls.hasPermission(this);
        if (perm) {
            int n = MissedCalls.count(this);
            status.setText(n > 0 ? getString(R.string.status_missed, n) : getString(R.string.status_ok));
            CallLogJob.scheduleAll(this);
        } else {
            status.setText(R.string.status_noperm);
        }
        permButton.setVisibility(perm && allGranted() ? View.GONE : View.VISIBLE);

        PowerManager pm = getSystemService(PowerManager.class);
        batteryButton.setVisibility(pm.isIgnoringBatteryOptimizations(getPackageName())
                ? View.GONE : View.VISIBLE);

        pinButton.setVisibility(AppWidgetManager.getInstance(this).isRequestPinAppWidgetSupported()
                ? View.VISIBLE : View.GONE);

        MissedCallWidget.updateAll(this);
    }

    private boolean allGranted() {
        for (String p : PERMS) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private void requestPerms() {
        boolean canAsk = !askedOnce;
        for (String p : PERMS) {
            if (shouldShowRequestPermissionRationale(p)) canAsk = true;
        }
        if (canAsk) {
            askedOnce = true;
            requestPermissions(PERMS, REQ_PERMS);
        } else {
            // Frågan har nekats för gott; behörigheten måste ges i appinställningarna.
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        refresh();
    }

    private void requestBatteryExemption() {
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.fromParts("package", getPackageName(), null)));
        } catch (RuntimeException e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private void pinWidget() {
        AppWidgetManager.getInstance(this).requestPinAppWidget(
                new ComponentName(this, MissedCallWidget.class), null, null);
    }
}
