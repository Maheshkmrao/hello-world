/*
 * Copyright (c) 2015. Queen Mary University of London
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

import android.content.Context;
import android.os.Environment;

import org.sensingkit.sensingkitlib.SKException;
import org.sensingkit.sensingkitlib.SKExceptionErrorCode;
import org.sensingkit.sensingkitlib.SKSensorModuleType;
import org.sensingkit.sensingkitlib.SensingKitLib;
import org.sensingkit.sensingkitlib.SensingKitLibInterface;

import java.io.File;

public class SensingSession {

    @SuppressWarnings("unused")
    private static final String TAG = "SensingSession";

    // SensingKit
    private SensingKitLibInterface mSensingKitLib;
    private boolean isSensing = false;

    // Session Folder
    private File mSessionFolder;

    // Models
    private ModelWriter mAudioLevelModelWriter;
    private ModelWriter mAccelerometerModelWriter;
    private ModelWriter mGravityModelWriter;
    private ModelWriter mLinearAccelerationModelWriter;
    private ModelWriter mGyroscopeModelWriter;
    private ModelWriter mRotationModelWriter;
    private ModelWriter mMagnetometerModelWriter;
    private ModelWriter mActivityModelWriter;
    private ModelWriter mAmbTempModelWriter;
    private ModelWriter mBatteryModelWriter;
    private ModelWriter mBluetoothModelWriter;
    private ModelWriter mLightModelWriter;
    private ModelWriter mLocationModelWriter;
    private ModelWriter mScreenStatusModelWriter;
    private ModelWriter mStepCounterModelWriter;
    private ModelWriter mStepDetectorModelWriter;

    public SensingSession(final Context context, final String folderName) throws SKException {

        // Init SensingKit
        mSensingKitLib = SensingKitLib.getSensingKitLib(context);

//        Toast.makeText(context, "SensingSession mSensingKitLib" , Toast.LENGTH_LONG).show();

        // Create the folder
        mSessionFolder = createFolder(folderName);
//        Toast.makeText(context, "SensingSession mSessionFolder" , Toast.LENGTH_LONG).show();
        // Init ModelWriters
        mAudioLevelModelWriter = new ModelWriter(SKSensorModuleType.AUDIO_LEVEL, mSessionFolder, "Audio");
        mAccelerometerModelWriter = new ModelWriter(SKSensorModuleType.ACCELEROMETER, mSessionFolder, "Accelerometer");
        mGravityModelWriter = new ModelWriter(SKSensorModuleType.GRAVITY, mSessionFolder, "Gravity");
        mLinearAccelerationModelWriter = new ModelWriter(SKSensorModuleType.LINEAR_ACCELERATION, mSessionFolder, "LinearAcceleration");
        mGyroscopeModelWriter = new ModelWriter(SKSensorModuleType.GYROSCOPE, mSessionFolder, "Gyroscope");
        mRotationModelWriter = new ModelWriter(SKSensorModuleType.ROTATION, mSessionFolder, "Rotation");
        mMagnetometerModelWriter = new ModelWriter(SKSensorModuleType.MAGNETOMETER, mSessionFolder, "Magnetometer");
        mActivityModelWriter = new ModelWriter(SKSensorModuleType.ACTIVITY, mSessionFolder, "Activity");
        mAmbTempModelWriter = new ModelWriter(SKSensorModuleType.AMBIENT_TEMPERATURE, mSessionFolder, "AmbientTemperature");
        mBatteryModelWriter = new ModelWriter(SKSensorModuleType.BATTERY, mSessionFolder, "Battery");
        mBluetoothModelWriter = new ModelWriter(SKSensorModuleType.BLUETOOTH, mSessionFolder, "Bluetooth");
        mLightModelWriter = new ModelWriter(SKSensorModuleType.LIGHT, mSessionFolder, "Light");
        mLocationModelWriter = new ModelWriter(SKSensorModuleType.LOCATION, mSessionFolder, "Location");
        mScreenStatusModelWriter = new ModelWriter(SKSensorModuleType.SCREEN_STATUS, mSessionFolder, "ScreenStatus");
        mStepCounterModelWriter = new ModelWriter(SKSensorModuleType.STEP_COUNTER, mSessionFolder, "StepCounter");
        mStepDetectorModelWriter = new ModelWriter(SKSensorModuleType.STEP_DETECTOR, mSessionFolder, "StepDetector");

        // Register Sensors
        mSensingKitLib.registerSensorModule(SKSensorModuleType.AUDIO_LEVEL);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.ACCELEROMETER);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.GRAVITY);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.LINEAR_ACCELERATION);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.GYROSCOPE);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.ROTATION);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.MAGNETOMETER);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.ACTIVITY);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.AMBIENT_TEMPERATURE);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.BATTERY);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.BLUETOOTH);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.LIGHT);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.LOCATION);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.SCREEN_STATUS);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.STEP_COUNTER);
        mSensingKitLib.registerSensorModule(SKSensorModuleType.STEP_DETECTOR);

        // Subscribe ModelWriters
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.AUDIO_LEVEL, mAudioLevelModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACCELEROMETER, mAccelerometerModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.GRAVITY, mGravityModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.LINEAR_ACCELERATION, mLinearAccelerationModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.GYROSCOPE, mGyroscopeModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ROTATION, mRotationModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.MAGNETOMETER, mMagnetometerModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mActivityModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mAmbTempModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mBatteryModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mBluetoothModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mLightModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mLocationModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mScreenStatusModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mStepCounterModelWriter);
        mSensingKitLib.subscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mStepDetectorModelWriter);

//        Toast.makeText(context, "After SensingSession" , Toast.LENGTH_LONG).show();
    }

    public void start() throws SKException {

        this.isSensing = true;

        // Start
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.AUDIO_LEVEL);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.ACCELEROMETER);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.GRAVITY);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.LINEAR_ACCELERATION);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.GYROSCOPE);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.ROTATION);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.MAGNETOMETER);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.ACTIVITY);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.AMBIENT_TEMPERATURE);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.BATTERY);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.BLUETOOTH);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.LIGHT);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.LOCATION);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.SCREEN_STATUS);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.STEP_COUNTER);
        mSensingKitLib.startContinuousSensingWithSensor(SKSensorModuleType.STEP_DETECTOR);
    }

    public void stop() throws SKException {

        this.isSensing = false;

        // Stop
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.AUDIO_LEVEL);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.ACCELEROMETER);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.GRAVITY);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.LINEAR_ACCELERATION);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.GYROSCOPE);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.ROTATION);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.MAGNETOMETER);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.ACTIVITY);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.AMBIENT_TEMPERATURE);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.BATTERY);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.BLUETOOTH);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.LIGHT);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.LOCATION);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.SCREEN_STATUS);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.STEP_COUNTER);
        mSensingKitLib.stopContinuousSensingWithSensor(SKSensorModuleType.STEP_DETECTOR);

        // Flush
        mAudioLevelModelWriter.flush();
        mAccelerometerModelWriter.flush();
        mGravityModelWriter.flush();
        mLinearAccelerationModelWriter.flush();
        mGyroscopeModelWriter.flush();
        mRotationModelWriter.flush();
        mMagnetometerModelWriter.flush();
        mActivityModelWriter.flush();
        mAmbTempModelWriter.flush();
        mBatteryModelWriter.flush();
        mBluetoothModelWriter.flush();
        mLightModelWriter.flush();
        mLocationModelWriter.flush();
        mScreenStatusModelWriter.flush();
        mStepCounterModelWriter.flush();
        mStepDetectorModelWriter.flush();
    }

    public void close() throws SKException {

        // Unsubscribe ModelWriters
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.AUDIO_LEVEL, mAudioLevelModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACCELEROMETER, mAccelerometerModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.GRAVITY, mGravityModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.LINEAR_ACCELERATION, mLinearAccelerationModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.GYROSCOPE, mGyroscopeModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ROTATION, mRotationModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.MAGNETOMETER, mMagnetometerModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mActivityModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mAmbTempModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mBatteryModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mBluetoothModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mLightModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mLocationModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mScreenStatusModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mStepCounterModelWriter);
        mSensingKitLib.unsubscribeSensorDataListener(SKSensorModuleType.ACTIVITY, mStepDetectorModelWriter);

        // Deregister Sensors
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.AUDIO_LEVEL);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.ACCELEROMETER);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.GRAVITY);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.LINEAR_ACCELERATION);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.GYROSCOPE);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.ROTATION);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.MAGNETOMETER);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.BLUETOOTH);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.ACTIVITY);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.AMBIENT_TEMPERATURE);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.BATTERY);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.BLUETOOTH);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.LIGHT);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.LOCATION);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.SCREEN_STATUS);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.STEP_COUNTER);
        mSensingKitLib.deregisterSensorModule(SKSensorModuleType.STEP_DETECTOR);

        // Close
        mAudioLevelModelWriter.close();
        mAccelerometerModelWriter.close();
        mGravityModelWriter.close();
        mLinearAccelerationModelWriter.close();
        mGyroscopeModelWriter.close();
        mRotationModelWriter.close();
        mMagnetometerModelWriter.close();
        mActivityModelWriter.close();
        mAmbTempModelWriter.close();
        mBatteryModelWriter.close();
        mBluetoothModelWriter.close();
        mLightModelWriter.close();
        mLocationModelWriter.close();
        mScreenStatusModelWriter.close();
        mStepCounterModelWriter.close();
        mStepDetectorModelWriter.close();
    }

    public boolean isSensing() {
        return this.isSensing;
    }

    private File createFolder(final String folderName) throws SKException {

        // Create App folder: CrowdSensing
        File appFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MySensorsDemo/");

        if (!appFolder.exists()) {
            if (!appFolder.mkdir()) {
                throw new SKException(TAG, "Folder could not be created.", SKExceptionErrorCode.UNKNOWN_ERROR);
            }
        }

        // Create session folder
        File folder = new File(appFolder, folderName);

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                throw new SKException(TAG, "Folder could not be created.", SKExceptionErrorCode.UNKNOWN_ERROR);
            }
        }

        return folder;
    }

}
