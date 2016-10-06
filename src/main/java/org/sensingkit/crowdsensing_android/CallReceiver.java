package org.sensingkit.crowdsensing_android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import java.util.Date;

/**
 * Created by 703145805 on 9/22/2016.
 */

public class CallReceiver extends PhonecallReceiver {

    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start)
    {
        showNotification(ctx, "onIncomingCallReceived : "+ number, start);
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start)
    {
        showNotification(ctx, "onIncomingCallAnswered : "+ number, start);
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end)
    {
        showNotification(ctx, "onIncomingCallEnded : "+ number, start);
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start)
    {
        showNotification(ctx, "onOutgoingCallStarted : "+ number, start);
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end)
    {
        showNotification(ctx, "onOutgoingCallEnded : "+ number, start);
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start)
    {
        showNotification(ctx, "onMissedCall : "+ number, start);
    }

    private void showNotification(Context context, String number, Date date) {

        // Set Notification Title
        String strtitle = "Crowd Sensing";
        // Open NotificationView Class on Notification Click
        Intent intent = new Intent(context, CrowdSensing.class);
        // Send data to NotificationView Class
//        intent.putExtra("title", strtitle);
//        intent.putExtra("text", number);

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // Open NotificationView.java Activity
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Create Notification using NotificationCompat.Builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context)
                // Set Icon
                .setSmallIcon(R.drawable.ic_launcher)
                // Set Ticker Message
                .setTicker(number)
                // Set Title
                .setContentTitle("Crowd Sensing")
                // Set Text
                .setContentText(number)
                // Add an Action Button below Notification
                //.addAction(R.drawable.ic_launcher, "Action Button", pIntent)
                // Set PendingIntent into Notification
                .setContentIntent(pIntent)
                // Dismiss Notification
                .setAutoCancel(true);

        // Create Notification Manager
        NotificationManager notificationmanager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // Build Notification with Notification Manager
        notificationmanager.notify(0, builder.build());

    }

}