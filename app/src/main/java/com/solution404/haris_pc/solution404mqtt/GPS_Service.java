package com.solution404.haris_pc.solution404mqtt;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


@SuppressWarnings("MissingPermission")
public class GPS_Service extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private LocationManager locationManager;
    private LocationListener listener;

    MqttAndroidClient client;

    MqttMessage message;


    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                handleUncaughtException(thread, e);
            }
        });

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                Intent i = new Intent("location_update");
                i.putExtra("coordinates", location.getLongitude() + "," + location.getLatitude());
                Toast.makeText(getApplicationContext(), "updated_location", Toast.LENGTH_SHORT).show();

                String clientId = MqttClient.generateClientId();
                client = new MqttAndroidClient(getApplicationContext(), "tcp://solution404.io:1884", clientId);

                try {
                    IMqttToken token = client.connect();
                    token.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Toast.makeText(getApplicationContext(), "connected", Toast.LENGTH_SHORT).show();
                            message = new MqttMessage(String.valueOf(String.valueOf(location.getLatitude() + "," + location.getLongitude())).getBytes());
                            message.setQos(1);
                            message.setRetained(false);

                            try {
                                Toast.makeText(getApplicationContext(), String.valueOf(message), Toast.LENGTH_SHORT).show();
                                client.publish("location/583048e227ea4d2c35a18e46", message);

                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Toast.makeText(getApplicationContext(), "connection failed!", Toast.LENGTH_SHORT).show();

                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (locationManager != null) {
            locationManager.removeUpdates(listener);
        }

        startService(new Intent(this, GPS_Service.class));
    }

    public void handleUncaughtException(Thread thread, Throwable e) {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically

        Intent intent = new Intent();
        intent.setAction("com.solution404.haris_pc.mqttpublisher.SEND_LOG"); // see step 5.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
        startActivity(intent);

        System.exit(1); // kill off the crashed app


    }
}

