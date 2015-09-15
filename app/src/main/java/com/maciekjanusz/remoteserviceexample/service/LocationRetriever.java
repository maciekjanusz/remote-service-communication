package com.maciekjanusz.remoteserviceexample.service;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.TimeUnit;

/**
 * This class isolates the logic of location objects retrieval through GoogleApiClient.
 */
public class LocationRetriever implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    /**
     * Location request time interval - with fused API 5 sec is minimum
     */
    private static final long REQUEST_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(5);

    /**
     * GoogleApiClient instance for retrieving locations through fused API
     */
    private GoogleApiClient googleApiClient;
    /**
     * LocationRequest object initialized within {@link #createLocationRequest()}
     */
    private LocationRequest locationRequest;
    /**
     * LocationListener for returning location through its callback
     */
    private LocationListener locationListener;

    /**
     * Last retrieved location. Can be null.
     */
    @Nullable
    private volatile Location currentLocation;

    public LocationRetriever(Context context) {
        buildGoogleApiClient(context.getApplicationContext());
    }

    /**
     * Sets up GoogleApiClient. Called in constructor {@link #LocationRetriever(Context)}
     * @param context Context for GoogleApiClient.Builder
     */
    private synchronized void buildGoogleApiClient(final Context context) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * This method initializes {@link #locationRequest} with {@link #REQUEST_INTERVAL_MILLIS}
     * as interval.
     */
    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(REQUEST_INTERVAL_MILLIS);
        locationRequest.setFastestInterval(REQUEST_INTERVAL_MILLIS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Starts updating location with googleApiClient.
     */
    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
    }

    /**
     * Stops googleApiClient from updating location.
     */
    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient, this);
    }

    /**
     * Call to start retrieving location
     * @param locationListener listener for location callbacks
     */
    public void startRetrievingLocation(LocationListener locationListener) {
        this.locationListener = locationListener;
        googleApiClient.connect();
    }

    /**
     * Call to stop retrieving location
     */
    public void stopRetrievingLocation() {
        stopLocationUpdates();
        googleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        // when client connects, start location updates
        createLocationRequest();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // when client connection gets suspended, stop location updates
        stopLocationUpdates();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        // set current location
        currentLocation = location;
        // notify listener
        locationListener.onLocationChanged(location);
    }

    /**
     * Getter for current location
     * @return current location. Might be null
     */
    @Nullable
    public Location getCurrentLocation() {
        return currentLocation;
    }
}