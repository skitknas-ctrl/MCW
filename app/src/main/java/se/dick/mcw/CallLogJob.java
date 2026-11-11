package se.dick.mcw;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.provider.CallLog;

/**
 * Håller widgeten aktuell:
 * - ett jobb som systemet startar så fort samtalsloggen ändras (nytt samtal, samtal markerat som sett),
 * - ett periodiskt jobb var 15:e minut som reserv,
 * - en fördröjd kontroll strax efter att ett samtal avslutats.
 */
public class CallLogJob extends JobService {

    private static final int JOB_CONTENT = 1;
    private static final int JOB_PERIODIC = 2;
    private static final int JOB_DELAYED = 3;

    public static void scheduleAll(Context context) {
        scheduleContent(context);
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js.getPendingJob(JOB_PERIODIC) == null) {
            js.schedule(new JobInfo.Builder(JOB_PERIODIC, component(context))
                    .setPeriodic(JobInfo.getMinPeriodMillis())
                    .setPersisted(true)
                    .build());
        }
    }

    static void scheduleContent(Context context) {
        if (!MissedCalls.hasPermission(context)) return;
        JobScheduler js = context.getSystemService(JobScheduler.class);
        try {
            js.schedule(new JobInfo.Builder(JOB_CONTENT, component(context))
                    .addTriggerContentUri(new JobInfo.TriggerContentUri(CallLog.Calls.CONTENT_URI,
                            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                    .setTriggerContentUpdateDelay(500)
                    .setTriggerContentMaxDelay(3000)
                    .build());
        } catch (RuntimeException ignored) {
            // Det periodiska jobbet och PHONE_STATE räcker som reserv.
        }
    }

    public static void scheduleDelayed(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        js.schedule(new JobInfo.Builder(JOB_DELAYED, component(context))
                .setMinimumLatency(3000)
                .setOverrideDeadline(10000)
                .build());
    }

    public static void cancelAll(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        js.cancel(JOB_CONTENT);
        js.cancel(JOB_PERIODIC);
        js.cancel(JOB_DELAYED);
    }

    private static ComponentName component(Context context) {
        return new ComponentName(context, CallLogJob.class);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        MissedCallWidget.updateAll(this);
        // Innehållsjobb gäller bara en gång och måste läggas upp på nytt.
        if (params.getJobId() == JOB_CONTENT || params.getJobId() == JOB_PERIODIC) {
            scheduleContent(this);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
