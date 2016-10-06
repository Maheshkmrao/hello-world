package org.sensingkit.crowdsensing_android;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by 703145805 on 9/22/2016.
 */
public class CallSmsSensorsReciever extends BroadcastReceiver {

    private Context mContext;

    private int mCallLogCount;
    private int mSmsLogCount;

    private Date mLastCallDate;
    private Date mLastSmsDate;

    private long mLastCallMin;
    private long mLastSmsMin;

    private List<Sms> mSmsList;
    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;

        getCallDetails();

        mSmsList = getAllSms(mContext);

//        if(mSmsList != null && mSmsList.size() > 0){
//            Sms bean = mSmsList.get(mSmsList.size()-1);
//            mLastSmsDate = new Date();
//            try{
//                mLastSmsDate.setTime(Long.parseLong(bean.getTime()));
//            }catch(Exception ex){
//                Toast.makeText(context, "Last SMS Exception : " + ex.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        }

        Calendar mCurrentCal = Calendar.getInstance();
        Date mCurrentDate = mCurrentCal.getTime();

        String lastCallon = timeDifference("Call", mLastCallDate, mCurrentDate);
        String lastSmsOn = timeDifference("SMS", mLastSmsDate, mCurrentDate);

        if(mLastCallMin > 5 && mLastSmsMin > 5)
            showNotification(mContext, "call Since :" + lastCallon +". \nSMS Since :" + lastSmsOn);
        else if(mLastCallMin > 5)
            showNotification(mContext, "call Since :" + lastCallon);
        else if(mLastSmsMin > 5)
            showNotification(mContext, "SMS Since :" + lastSmsOn);

//        Toast.makeText(context, "Last call recieved on : " + lastCallon, Toast.LENGTH_LONG).show();
//
//        Toast.makeText(context, "Last SMS recieved on : " + lastSmsOn, Toast.LENGTH_LONG).show();

//        context.startService(new Intent(context, LocationTrackerService.class));

    }

    private void getCallDetails() {

        StringBuffer sb = new StringBuffer();
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Cursor managedCursor = mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI, null,
                null, null, null);
        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int name = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        sb.append("Call Log :");
//        while (managedCursor.moveToNext()) {
//            String phNumber = managedCursor.getString(number);
//            String PhnName = managedCursor.getString(name);
//            String callType = managedCursor.getString(type);
//            String callDate = managedCursor.getString(date);
//            Date callDayTime = new Date(Long.valueOf(callDate));
//            String callDuration = managedCursor.getString(duration);
//            String dir = null;
//            int dircode = Integer.parseInt(callType);
//            switch (dircode) {
//                case CallLog.Calls.OUTGOING_TYPE:
//                    dir = "OUTGOING";
//                    break;
//
//                case CallLog.Calls.INCOMING_TYPE:
//                    dir = "INCOMING";
//                    break;
//
//                case CallLog.Calls.MISSED_TYPE:
//                    dir = "MISSED";
//                    break;
//            }
//            sb.append("\nPhone Number:--- " + phNumber + " \nName:--- "
//                    + PhnName + " \nCall Type:--- "
//                    + dir + " \nCall Date:--- " + callDayTime
//                    + " \nCall duration in sec :--- " + callDuration);
//            sb.append("\n----------------------------------");
//
//            mCallLogCount++;
//        }

        if( managedCursor.moveToLast() == true ) {
            String phNumber = managedCursor.getString( number );
            String callDuration = managedCursor.getString( duration );
            String dir = null;
            String callDate = managedCursor.getString( date );
            mLastCallDate = new Date(Long.valueOf(callDate));
            sb.append( "\nPhone Number:--- "+phNumber +" \nCall duration in sec :--- "+callDuration );
            sb.append("\n----------------------------------");
        }
        managedCursor.close();

        managedCursor.close();
    }

    private List<Sms> getAllSms(Context mActivity) {
        List<Sms> lstSms = new ArrayList<Sms>();
        Sms objSms = new Sms();
        Uri message = Uri.parse("content://sms/inbox");
        ContentResolver cr = mActivity.getContentResolver();

        Cursor c = cr.query(message, null, null, null, "date ASC");

        int totalSMS = c.getCount();

        if (c.moveToLast()) {
//            for (int i = 0; i < totalSMS; i++) {

                objSms = new Sms();
                objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                objSms.setAddress(c.getString(c
                        .getColumnIndexOrThrow("address")));
                objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                objSms.setReadState(c.getString(c.getColumnIndex("read")));
                objSms.setTime(c.getString(c.getColumnIndexOrThrow("date")));

                String smsDate = c.getString(c.getColumnIndexOrThrow("date"));
                mLastSmsDate = new Date(Long.valueOf(smsDate));

                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    objSms.setFolderName("inbox");
                } else {
                    objSms.setFolderName("sent");
                }

                lstSms.add(objSms);
//                c.moveToNext();
//            }
        }
        // else {
        // throw new RuntimeException("You have no SMS");
        // }
        c.close();

        return lstSms;
    }

    private void showNotification(Context context, String message) {

        // Set Notification Title
        String strtitle = "Call/SMS Sensing ";
        // Open NotificationView Class on Notification Click
        Intent intent = new Intent(context, CrowdSensing.class);
        // Send data to NotificationView Class
        intent.putExtra("CALL_TIME_MESSAGE", message);

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
                .setTicker(message)
                // Set Title
                .setContentTitle("Crowd Sensing")
                // Set Text
                .setContentText(message)
                // Add an Action Button below Notification
                .addAction(R.drawable.ic_launcher, "Action Button", pIntent)
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

    //1 minute = 60 seconds
    //1 hour = 60 x 60 = 3600
    //1 day = 3600 x 24 = 86400
    public String timeDifference(String callsms, Date startDate, Date endDate){

        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        System.out.println("startDate : " + startDate);
        System.out.println("endDate : "+ endDate);
        System.out.println("different : " + different);

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

//        System.out.printf(
//                "%d days, %d hours, %d minutes, %d seconds%n",
//                elapsedDays,
//                elapsedHours, elapsedMinutes, elapsedSeconds);

        StringBuffer timeDifference = new StringBuffer();
        if(elapsedDays > 0)
            timeDifference.append(elapsedDays+ " days,");
        if(elapsedHours > 0)
            timeDifference.append(elapsedHours+ " hours,");
        if(elapsedMinutes > 0) {
            if(TextUtils.equals(callsms, "Call")){
                mLastCallMin = elapsedMinutes;
            }
            else if(TextUtils.equals(callsms, "SMS")){
                mLastSmsMin = elapsedMinutes;
            }
            timeDifference.append(elapsedMinutes + " minutes,");
        }
        if(elapsedSeconds > 0)
            timeDifference.append(elapsedSeconds+ " seconds,");

        return timeDifference.toString();
    }

}
