package com.maciekjanusz.remoteserviceexample.service;


import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.google.android.gms.location.LocationListener;
import com.maciekjanusz.remoteserviceexample.MainActivity;
import com.maciekjanusz.tale.Tale;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.maciekjanusz.remoteserviceexample.service.TrackingService.ACTION_LOCATION_ACQUIRED;
import static com.maciekjanusz.remoteserviceexample.service.TrackingService.EXTRA_LOCATION;
import static com.maciekjanusz.remoteserviceexample.service.TrackingService.LOG_STRING_KEY;
import static com.maciekjanusz.remoteserviceexample.service.TrackingService.MSG_GET_CURRENT_STATE;
import static com.maciekjanusz.remoteserviceexample.service.TrackingService.MSG_LOG;

/**
 * This class isolates the remote service communication logic.
 * It is capable of automatic binding to the service, when registered as activity lifecycle
 * callbacks (see {@link MainActivity}).
 *
 * TODO: needs to be more generic
 */
public class ServiceProxy implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "ServiceProxy";

    /**
     * Context for registering/unregistering receivers, starting service etc.
     */
    private Context context;
    /**
     * Intent for starting service
     */
    private Intent serviceIntent;
    /**
     * Thread safe location listener list
     */
    private List<LocationListener> locationListeners = new CopyOnWriteArrayList<>();
    /**
     * Thread safe service callbacks list
     */
    private List<ServiceCallbacks> serviceCallbacks = new CopyOnWriteArrayList<>();

    /**
     * This flag denotes whether connection to the service has been established
     */
    private boolean serviceConnected = false;

    /**
     * Messenger for interprocess service communication
     */
    private Messenger serviceMessenger;
    /**
     * Broadcast receiver for service callbacks
     */
    private BroadcastReceiver serviceMessageReceiver = new ServiceMessageReceiver();
    /**
     * Service connection impl.
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // establish messenger
            serviceMessenger = new Messenger(iBinder);
            // send simple log message
            String logString = "Hello bound service!";
            Bundle data = new Bundle();
            data.putString(LOG_STRING_KEY, logString);
            sendMessage(MSG_LOG, data, null);

            // notify listeners of successful connection
            serviceConnected = true;
            notifyServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // notify listeners
            notifyServiceDisconnected();
            serviceConnected = false;
            // messenger is now obsolete
            serviceMessenger = null;
        }
    };

    public ServiceProxy(Context context) {
        this.context = context;
        this.serviceIntent = new Intent(context, TrackingService.class);
    }

    /**
     * Registers {@link #serviceMessageReceiver} with proper IntentFilter
     */
    private void registerServiceMessageReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_LOCATION_ACQUIRED);
        context.registerReceiver(serviceMessageReceiver, intentFilter);
    }

    /**
     * Unregisters {@link #serviceMessageReceiver}.
     * Call only once after registration.
     */
    private void unregisterServiceMessageReceiver() {
        context.unregisterReceiver(serviceMessageReceiver);
    }

    /**
     * Call this method to trigger callback from service with current location.
     */
    public void requestCurrentState() {
        sendMessage(MSG_GET_CURRENT_STATE, null, null);
    }

    /**
     * Sends message to service through bound messenger.
     *
     * @param what message identifier integer
     * @param data a data bundle (may be null)
     * @param object an additional object (may be null)
     * @return true if successful, false otherwise
     */
    public boolean sendMessage(int what, @Nullable Bundle data, @Nullable Object object) {
        if (serviceMessenger != null) {
            // obtain message and set data
            Message message = Message.obtain();
            message.what = what;
            message.setData(data);
            message.obj = object;

            // With IPC the extra object can only be a framework-implemented parcelable
            if (object != null && object instanceof Parcelable) {
                new Tale("object is not parcelable.").at().tell();
                return false;
            }

            // Proceed with sending
            try {
                new Tale("Sending message... " + what).at().tell();
                serviceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            new Tale("serviceMessenger uninitialized").at().tell();
            return false;
        }
    }

    /**
     * Unbind and then rebind service.
     */
    private void rebindService() {
        unbindService();
        bindService();
    }

    private void unbindService() {
        context.getApplicationContext().unbindService(serviceConnection);
    }

    private void bindService() {
        boolean result = context.getApplicationContext()
                .bindService(serviceIntent, serviceConnection, Context.BIND_ABOVE_CLIENT);
        new Tale(result ? "Succesfully bound to service." : "Failed to bind to service.").at().tell();
    }

    private void startService() {
        rebindService();
        context.startService(serviceIntent);
    }

    private void stopService() {
        context.stopService(serviceIntent);
    }

    /**
     * Call to toggle service state (started / stopped)
     */
    public void toggleService() {
        if (serviceConnected) {
            stopService();
        } else {
            startService();
        }
    }

    /*
        Activity lifecycle callbacks
     */
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (activity instanceof MainActivity) {
            bindService();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof MainActivity) {
            registerServiceMessageReceiver();
            if (serviceConnected) {
                requestCurrentState();
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity instanceof MainActivity) {
            unregisterServiceMessageReceiver();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (activity instanceof MainActivity) {
            unbindService();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // keep empty - this activity lifecycle callbacks gets
        // unregistered in MainActivity onDestroy
    }

    public void addLocationListener(LocationListener locationListener) {
        locationListeners.add(locationListener);
    }

    public void removeLocationListener(LocationListener locationListener) {
        locationListeners.remove(locationListener);
    }

    public void addServiceCallbacks(ServiceCallbacks serviceCallbacksImpl) {
        serviceCallbacks.add(serviceCallbacksImpl);
    }

    public void removeServiceCallbacks(ServiceCallbacks serviceCallbacksImpl) {
        serviceCallbacks.remove(serviceCallbacksImpl);
    }

    private void notifyListeners(Location location) {
        for(LocationListener locationListener : locationListeners) {
            locationListener.onLocationChanged(location);
        }
    }

    private void notifyServiceDisconnected() {
        for(ServiceCallbacks serviceCallbacksImpl : serviceCallbacks) {
            serviceCallbacksImpl.onServiceDisconnected();
        }
    }

    private void notifyServiceConnected() {
        for(ServiceCallbacks serviceCallbacksImpl : serviceCallbacks) {
            serviceCallbacksImpl.onServiceConnected();
        }
    }

    public interface ServiceCallbacks {
        void onServiceConnected();

        void onServiceDisconnected();
    }

    private class ServiceMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            /**
             * Remember to add actions to intent filter as well
             */
            switch (intent.getAction()) {
                case ACTION_LOCATION_ACQUIRED:
                    Location location = intent.getParcelableExtra(EXTRA_LOCATION);
                    notifyListeners(location);
                    break;
            }
        }
    }

}