package org.sensingkit.crowdsensing_android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.Toast;

/**
 * Created by 703145805 on 9/22/2016.
 */
public class CrowdSensorBootReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationTrackerBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent gpsTrackerIntent = new Intent(context, CallSmsSensorsReciever.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, 0);

        Toast.makeText(context, "BootReceiver onReceive:: ", Toast.LENGTH_LONG).show();

        int intervalInMinutes = 1;

//        if (currentlyTracking) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    intervalInMinutes * 60000, // 60000 = 1 minute,
                    pendingIntent);
//        } else {
//            alarmManager.cancel(pendingIntent);
//        }
    }
}