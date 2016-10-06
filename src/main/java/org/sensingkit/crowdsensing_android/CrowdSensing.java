/*
 * Copyright (c) 2014. Queen Mary University of London
 * Kleomenis Katevas, k.katevas@qmul.ac.uk
 *
 * This file is part of CrowdSensing software.
 * For more information, please visit http://www.sensingkit.org
 *
 * CrowdSensing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CrowdSensing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with CrowdSensing.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sensingkit.crowdsensing_android;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CrowdSensing extends ActionBarActivity {

    protected CrashReportExceptionHandler mDamageReport;

    private CrowdSensing mActivity;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;
    public static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101;
    public static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 102;
    public static final int MY_PERMISSIONS_REQUEST_READ_CALL_LOG = 103;
    public static final int MY_PERMISSIONS_REQUEST_READ_SMS = 104;

    @SuppressWarnings("unused")
    private static final String TAG = "CrowdSensing";

    private enum SensingStatus {
        Stopped,
        Sensing,
        Paused
    }

    // UI Elements
    private TextView mStatus;
    private Button mSensingButton;
    private Button mPauseButton;

    // Button Statuses
    SensingStatus mSensingStatus;

    // Services
    SensingService mSensingService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crowd_sensing);

        mActivity = this;

        Intent bundle = getIntent();
        if (bundle != null && bundle.hasExtra("CALL_TIME_MESSAGE")) {
            String notificationMessage = bundle.getStringExtra("CALL_TIME_MESSAGE");
            Toast.makeText(mActivity, "Call Details :" + notificationMessage, Toast.LENGTH_LONG).show();
        }

        mDamageReport = new CrashReportExceptionHandler(this);
        //ModuleManager.addActivityInstance(this);
        mDamageReport.initialize();

        checkPermission(mActivity, "Storage");
        checkPermission(mActivity, "AudioRecord");
        checkPermission(mActivity, "PHONE_STATE");
        checkPermission(mActivity, "READ_CALL_LOG");
        checkPermission(mActivity, "READ_SMS");

        // get refs to the Status TextView
        mStatus = (TextView)findViewById(R.id.status);

        mSensingButton = (Button)findViewById(R.id.sensing);
        mSensingButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
//                Toast.makeText(CrowdSensing.this, "mSensingStatus " + mSensingStatus.name(), Toast.LENGTH_LONG).show();

                switch (mSensingStatus) {

                    case Stopped:
                        startSensing();

                        startAlarmManager();

                        setSensingStatus(SensingStatus.Sensing);
                        break;

                    case Paused:
                    case Sensing:
                        stopSensing();

                        cancelAlarmManager();

                        setSensingStatus(SensingStatus.Stopped);
                        break;

                }

            }
        });

        mPauseButton = (Button)findViewById(R.id.pause);
        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                switch (mSensingStatus) {

                    case Stopped:
                        Log.e(TAG, "Case Stopped should not be available.");

                        break;

                    case Paused:
                        continueSensing();
                        setSensingStatus(SensingStatus.Sensing);
                        break;

                    case Sensing:
                        pauseSensing();
                        setSensingStatus(SensingStatus.Paused);
                        break;

                }

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.crowd_sensing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {

            case R.id.action_settings:
                //newGame();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(this, SensingService.class);

        if (!isSensingServiceRunning()) {
//            Toast.makeText(CrowdSensing.this, "Strating Sensing Service", Toast.LENGTH_LONG).show();
            // Start the SensingService
            startService(intent);
        }

        // Bind SensingService
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            Log.i(TAG, "onServiceConnected()");
//            Toast.makeText(CrowdSensing.this, "onServiceConnected ServiceConnection", Toast.LENGTH_LONG).show();

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SensingService.LocalBinder binder = (SensingService.LocalBinder) service;
            mSensingService = binder.getService();
            mBound = true;

            // Update the UI
            switch (mSensingService.getSensingStatus()) {

                case Stopped:
                    setSensingStatus(SensingStatus.Stopped);
                    break;

                case Sensing:
                    setSensingStatus(SensingStatus.Sensing);
                    break;

                case Paused:
                    setSensingStatus(SensingStatus.Paused);
                    break;

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            Log.i(TAG, "onServiceDisconnected()");

            mBound = false;
        }

    };

    private boolean isSensingServiceRunning() {

        ActivityManager manager = (ActivityManager) getSystemService (Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (SensingService.class.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;

    }

    private void setSensingStatus(SensingStatus status) {

        switch (status) {

            case Stopped:

                mStatus.setText("Stopped");
                mSensingButton.setText("Start Sensing");
                mPauseButton.setText("Pause");
                mPauseButton.setEnabled(false);
                break;

            case Sensing:

                mStatus.setText("Sensing...");
                mSensingButton.setText("Stop Sensing");
                mPauseButton.setText("Pause");
                mPauseButton.setEnabled(true);
                break;

            case Paused:

                mStatus.setText("Paused");
                mSensingButton.setText("Stop Sensing");
                mPauseButton.setText("Continue");
                mPauseButton.setEnabled(true);
                break;

            default:
                Log.i(TAG, "Unknown SensingStatus: " + status);

        }

        mSensingStatus = status;
    }

    private void startSensing() {
        // Start Sensing
        try{
            CallReceiver reciever = new CallReceiver();
            IntentFilter filter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
            filter.addAction(Intent.ACTION_CALL);
            filter.addAction(Intent.ACTION_CALL_BUTTON);
            filter.addAction(Intent.ACTION_DIAL);

            registerReceiver(reciever, filter);

            mSensingService.startSensing();
        }catch(Exception ex){}
    }

    private void pauseSensing() {

        // Pause Sensing
        try{
            mSensingService.pauseSensing();
        }catch(Exception ex){}
    }

    private void continueSensing() {

        // Continue Sensing
        try{
            mSensingService.continueSensing();
        }catch(Exception ex){}
    }

    private void stopSensing() {

        // Stop Sensing
        try{
            mSensingService.stopSensing();
        }catch(Exception ex){}

    }

    @Override
    protected void onDestroy() {
        mDamageReport.restoreOriginalHandler();
        mDamageReport = null;
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkPermission(final Context context, String permission)
    {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if(currentAPIVersion>= Build.VERSION_CODES.M)
        {
            if(permission.equalsIgnoreCase("Storage")) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                        alertBuilder.setCancelable(true);
                        alertBuilder.setTitle("Permission necessary");
                        alertBuilder.setMessage("External storage permission is necessary");
                        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                            }
                        });
                        AlertDialog alert = alertBuilder.create();
                        alert.show();

                    } else {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                    return false;
                } else {
                    return true;
                }
            }
            else if(permission.equalsIgnoreCase("AudioRecord")) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.RECORD_AUDIO)) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                        alertBuilder.setCancelable(true);
                        alertBuilder.setTitle("Permission necessary");
                        alertBuilder.setMessage("Audio Record permission is necessary");
                        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                            }
                        });
                        AlertDialog alert = alertBuilder.create();
                        alert.show();

                    } else {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                    }
                    return false;
                } else {
                    return true;
                }
            }
            else if(permission.equalsIgnoreCase("PHONE_STATE")) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_PHONE_STATE)) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                        alertBuilder.setCancelable(true);
                        alertBuilder.setTitle("Permission necessary");
                        alertBuilder.setMessage("Phone State permission is necessary");
                        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
                            }
                        });
                        AlertDialog alert = alertBuilder.create();
                        alert.show();

                    } else {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
                    }
                    return false;
                } else {
                    return true;
                }
            }
            else if(permission.equalsIgnoreCase("READ_CALL_LOG")) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_CALL_LOG)) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                        alertBuilder.setCancelable(true);
                        alertBuilder.setTitle("Permission necessary");
                        alertBuilder.setMessage("Read Call Log permission is necessary");
                        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_CALL_LOG}, MY_PERMISSIONS_REQUEST_READ_CALL_LOG);
                            }
                        });
                        AlertDialog alert = alertBuilder.create();
                        alert.show();

                    } else {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_CALL_LOG}, MY_PERMISSIONS_REQUEST_READ_CALL_LOG);
                    }
                    return false;
                } else {
                    return true;
                }
            }
            else if(permission.equalsIgnoreCase("READ_SMS")) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_SMS)) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                        alertBuilder.setCancelable(true);
                        alertBuilder.setTitle("Permission necessary");
                        alertBuilder.setMessage("Read SMS permission is necessary");
                        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_SMS}, MY_PERMISSIONS_REQUEST_READ_SMS);
                            }
                        });
                        AlertDialog alert = alertBuilder.create();
                        alert.show();

                    } else {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_SMS}, MY_PERMISSIONS_REQUEST_READ_SMS);
                    }
                    return false;
                } else {
                    return true;
                }
            }


        } else {
            return true;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if(requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
//                Toast.makeText(mActivity, "External Storage Permission Granted.", Toast.LENGTH_LONG).show();
            }
            else if(requestCode == MY_PERMISSIONS_REQUEST_RECORD_AUDIO) {
//                Toast.makeText(mActivity, "Record Audio Permission Granted.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(mActivity, "Permission not granted.", Toast.LENGTH_LONG).show();
        }
    }

    private void startAlarmManager() {
        Log.d(TAG, "startAlarmManager");

        Context context = getBaseContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent gpsTrackerIntent = new Intent(context, CallSmsSensorsReciever.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, 0);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                1 * 60000, // 60000 = 1 minute
                pendingIntent);
    }

    private void cancelAlarmManager() {
        Log.d(TAG, "cancelAlarmManager");

        Context context = getBaseContext();
        Intent gpsTrackerIntent = new Intent(context, CallSmsSensorsReciever.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, 0);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

}
