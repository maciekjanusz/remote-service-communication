package com.maciekjanusz.remoteserviceexample;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;
import com.maciekjanusz.remoteserviceexample.service.ServiceProxy;
import com.maciekjanusz.remoteserviceexample.service.ServiceProxy.ServiceCallbacks;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks, LocationListener {

    /**
     * Service toggle button
     */
    @Bind(R.id.start_stop_button)       Button      startStopButton;
    /**
     * Text view for displaying location latitude and longitude
     */
    @Bind(R.id.location_textview)       TextView    locationView;
    /**
     * Text View for displaying speed
     */
    @Bind(R.id.speed_textview)          TextView    speedView;
    /**
     * Text view for displaying location accuracy (in meters)
     */
    @Bind(R.id.accuracy_textview)       TextView    accuracyView;

    /**
     * ServiceProxy object for IPC messenger logic
     */
    private ServiceProxy serviceProxy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        serviceProxy = new ServiceProxy(this);
        serviceProxy.addLocationListener(this);
        serviceProxy.addServiceCallbacks(this);

        // provides serviceProxy with lifecycle callbacks for auto-binding
        getApplication().registerActivityLifecycleCallbacks(serviceProxy);
    }

    /**
     * Handle start / stop button click
     */
    @OnClick(R.id.start_stop_button)
    public void startStopButtonClick() {
        // Toggle service and disable button (wait mode)
        startStopButton.setEnabled(false);
        serviceProxy.toggleService();
    }

    @Override
    protected void onDestroy() {
        // stop acquiring locations and service callbacks
        serviceProxy.removeLocationListener(this);
        serviceProxy.removeServiceCallbacks(this);
        // unregister serviceProxy lifecycle callbacks
        getApplication().unregisterActivityLifecycleCallbacks(serviceProxy);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected() {
        Toast.makeText(MainActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
        // Update button
        startStopButton.setText(R.string.service_stop);
        startStopButton.setEnabled(true);

        // Automatically request current location
        serviceProxy.requestCurrentState();
    }

    @Override
    public void onServiceDisconnected() {
        Toast.makeText(MainActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
        // Update button
        startStopButton.setText(R.string.service_start);
        startStopButton.setEnabled(true);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            // Update UI
            locationView.setText(location.getLatitude() + ", " + location.getLongitude());
            accuracyView.setText(String.valueOf(location.getAccuracy()));
            speedView.setText(String.valueOf(location.getSpeed()) + " m/s");
        }
    }
}