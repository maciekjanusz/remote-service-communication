package com.maciekjanusz.remoteserviceexample.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.maciekjanusz.remoteserviceexample.MainActivity;
import com.maciekjanusz.remoteserviceexample.R;
import com.maciekjanusz.tale.Tale;

import java.lang.ref.WeakReference;

/**
 * This service runs in the foreground and in different process than rest of application,
 * for the purpose of making it less vulnerable to failure.
 * Its existence in different process makes communication with other app components harder,
 * as regular solutions such as ordinary binder or an event bus won't work across processes.
 * The solution is to use Messenger binder.
 *
 * As for the actual purpose, this service continuously updates device location
 * and broadcasts it to other app components, and can be controlled from within an activity
 * through aforementioned messenger.
 */
public class TrackingService extends Service implements LocationListener {

    public static final String LOG_STRING_KEY = "key:log_string";
    private static long instanceCount = 0;

    public static final int MSG_LOG = 0,
            MSG_GET_CURRENT_STATE = 1;

    public static final String
            ACTION_LOCATION_ACQUIRED = "action_location_acquired",
            EXTRA_LOCATION = "extra_location";

    /**
     * Messenger with {@link com.maciekjanusz.remoteserviceexample.service.TrackingService.ProxyMessageHandler}
     * for incoming IPC communication messages from serviceProxy
     */
    private final Messenger bindMessenger =
            new Messenger(new ProxyMessageHandler(new WeakReference<>(this)));
    /**
     * LocationRetriever for retrieving location. Duh
     */
    private LocationRetriever locationRetriever;

    /**
     * Intent for broadcasting location back to serviceProxy
     */
    private Intent locationBroadcastIntent = new Intent(ACTION_LOCATION_ACQUIRED);

    public TrackingService() {
        // increase instance count for debugging purposes
        instanceCount++;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return bindMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(startId, createServiceRunningNotification());

        // start retrieving location
        locationRetriever = new LocationRetriever(this);
        locationRetriever.startRetrievingLocation(this);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        locationRetriever.stopRetrievingLocation();
        super.onDestroy();
    }

    /**
     * Creates notification necessary for {@link Service#startForeground(int, Notification)}
     * with pending intent for launching {@link MainActivity}
     * @return created notification
     */
    private Notification createServiceRunningNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        PendingIntent pendingIntent = PendingIntent
                .getActivity(getApplicationContext(), 0,
                        new Intent(getApplicationContext(),
                                MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setContentTitle("Service running")
                .setContentText("PID " + android.os.Process.myPid());
        return builder.build();
    }

    /**
     * Send location broadcast to serviceProxy
     * @param location location object to send
     */
    private void broadcastLocation(final Location location) {
        locationBroadcastIntent.putExtra(EXTRA_LOCATION,
                location);
        sendBroadcast(locationBroadcastIntent);
    }

    /**
     * Currently - just broadcast current location.
     */
    private void broadcastCurrentState() {
        broadcastLocation(locationRetriever.getCurrentLocation());
    }

    @Override
    public void onLocationChanged(Location location) {
        broadcastLocation(location);
    }

    /**
     * Static handler for incoming messages from serviceProxy
     */
    private static class ProxyMessageHandler extends Handler {

        /**
         * Weak reference to {@link TrackingService} to avoid mem leaks.
         */
        private final WeakReference<TrackingService> serviceWeakReference;

        private ProxyMessageHandler(WeakReference<TrackingService> serviceWeakReference) {
            this.serviceWeakReference = serviceWeakReference;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_LOG:
                    // Simple log action
                    Bundle data = msg.getData();
                    String logString = data.getString(LOG_STRING_KEY);
                    new Tale("Service instance count: " + instanceCount).at().tell();
                    if(logString != null) {
                        new Tale("Incoming log message: " + logString).at().tell();
                    }
                    break;
                case MSG_GET_CURRENT_STATE:
                    // Retrieve current state and broadcast back to serviceProxy
                    try {
                        serviceWeakReference.get().broadcastCurrentState();
                    } catch (NullPointerException e) {
                        new Tale("Service has died. Couldn't process MSG_GET_CURRENT_STATE").at().tell();
                    }
                    break;
            }
        }
    }
}