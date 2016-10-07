package io.rover;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.util.Log;




import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.rover.model.BeaconConfiguration;
import io.rover.model.BeaconTransitionEvent;
import io.rover.model.Device;
import io.rover.model.DeviceUpdateEvent;
import io.rover.model.Event;
import io.rover.model.GimbalPlaceTransitionEvent;
import io.rover.model.LocationUpdateEvent;
import io.rover.model.Place;

/**
 * Created by ata_n on 2016-03-21.
 */
public class Rover implements EventSubmitTask.Callback {

    protected static String VERSION = "1.1.0";
    protected static Rover mSharedInstance = new Rover();

    private Context mApplicationContext;
    private PendingIntent mLocationPendingIntent;
    private PendingIntent mGeofencePendingIntent;
    private PendingIntent mNearbyMessagesPendingIntent;
    private PendingIntent mAppLaunchPendingIntent;
    private ExecutorService mEventExecutorService = Executors.newSingleThreadExecutor();
    private ArrayList<RoverObserver> mObservers = new ArrayList<>();
    private NotificationProvider mNotificationProvider;
    private boolean mGimbalMode;

    private Rover() {}

    public static void setup(Application application, RoverConfig config) {
        mSharedInstance.mApplicationContext = application.getApplicationContext();
        mSharedInstance.mNotificationProvider = config.mNotificationProvider;
        Router.setApiKey(config.mAppToken);
        Router.setDeviceId(Device.getInstance().getIdentifier(mSharedInstance.mApplicationContext));

        // Gimbal check
        try {
            Class gmblPlaceManagerClass = Class.forName("com.gimbal.android.PlaceManager");
            mSharedInstance.mGimbalMode = true;
        } catch (ClassNotFoundException e) {
            mSharedInstance.mGimbalMode = false;
        }

        Device.getInstance().setGimbalMode(mSharedInstance.mGimbalMode);

        // Set Cache

        try {
            if (HttpResponseCache.getInstalled() == null) {
                File httpCacheDir = new File(mSharedInstance.mApplicationContext.getCacheDir(), "http");
                long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
            }
        } catch (IOException e) {
            Log.i("Rover", "HTTP response cache installation failed:" + e);
        }
    }

    public static void addObserver(RoverObserver observer) {
        mSharedInstance.mObservers.add(observer);
    }

    public static void deleteObserver(RoverObserver observer) {
        mSharedInstance.mObservers.remove(observer);
    }

    public interface OnInboxReloadListener {
        void onSuccess(List<io.rover.model.Message> messages);
        void onFailure();
    }

    public static void reloadInbox(final OnInboxReloadListener listener) {
        FetchInboxTask task = new FetchInboxTask();
        task.setCallback(new FetchInboxTask.Callback() {
            @Override
            public void onSuccess(List<io.rover.model.Message> messages) {
                if (listener != null) {
                    listener.onSuccess(messages);
                }
            }
        });
        task.execute();
    }

    public static void submitEvent(Event event) {
        mSharedInstance.sendEvent(event);
    }


    private PendingIntent getAppLaunchPendingIntent() {
        // TODO: intent needs to contain message id
        if (mAppLaunchPendingIntent == null) {
            String packageName = mApplicationContext.getPackageName();
            Intent intent = mApplicationContext.getPackageManager().getLaunchIntentForPackage(packageName);
            mAppLaunchPendingIntent = PendingIntent.getActivity(mApplicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mAppLaunchPendingIntent;
    }


    protected void sendEvent(Event event) {
        EventSubmitTask eventTask = new EventSubmitTask(mApplicationContext, event);
        eventTask.setCallback(this);
        // TODO: this may have to be synchronous
        mEventExecutorService.execute(eventTask);
    }

    @Override
    public void onEventRegistered(Event event) {

    }

    @Override
    public void onReceivedGeofences(final List geofences) {

    }


}
