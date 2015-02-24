package com.firebirdberlin.longitude;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import android.preference.PreferenceManager;

import android.util.Log;

import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.String;

public class LocationService extends Service {
	private static String TAG = LongitudeUpdater.TAG + ".LocationService";
	private LocationManager locationManager;
	NotificationManager notificationManager;

	private Context mContext = null;
	private Notification note;
	private int NOTIFICATION_ID = 1337;
	private PendingIntent pIntentLocationService;
	private LocationListener locationListener;

	@Override
	public void onCreate(){
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (! isNetworkAvailable() ) {
			Log.d(TAG, "No network available");
			stopSelf();
		}

		mContext = this;
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		showNotification();

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
			  // Called when a new location is found by the network location provider.
				String provider = location.getProvider();
				Location prev_location = new Location(provider);
				prev_location.setLongitude(settings.getFloat("lon", 0));
				prev_location.setLatitude(settings.getFloat("lat", 0));
				prev_location.setAccuracy(settings.getFloat("accuracy", 1000));
				float distance_in_meters = location.distanceTo(prev_location);
				Log.d(TAG, "Distance to prev location " + distance_in_meters + " m");
				Log.d(TAG, "old accuracy " + prev_location.getAccuracy() + " new accuracy " + location.getAccuracy());

				if (distance_in_meters > 200. || location.getAccuracy() < prev_location.getAccuracy()){
					double lon = location.getLongitude();
					double lat = location.getLatitude();
					float accuracy = location.getAccuracy();

					uploadLocation(lat, lon);
					updateNotification(lat, lon);

					SharedPreferences.Editor prefEditor = settings.edit();
					prefEditor.putFloat("lon", (float) lon);
					prefEditor.putFloat("lat", (float) lat);
					prefEditor.putFloat("accuracy", accuracy);
					prefEditor.commit();
				}
				stopSelf();
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {}

			public void onProviderDisabled(String provider) {}
		  };


		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

		return Service.START_NOT_STICKY;
	}

	private Notification buildNotification(String title, String text) {
	 	note  = new Notification.Builder(this)
			.setContentTitle(title)
			.setContentText(text)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentIntent(pIntentLocationService)
			.setAutoCancel(true).build();
//			.addAction(R.drawable.ic_launcher, "unmute", pIntentUnmute).build();
//			.addAction(R.drawable.icon, "More", pIntent)
//			.addAction(R.drawable.icon, "And more", pIntent).build();
		note.flags|=Notification.FLAG_NO_CLEAR;
		return note;
	}

	public void showNotification(){

		Intent IntentLocationService = new Intent(this, LocationService.class);
		IntentLocationService.putExtra("action", "click");
		pIntentLocationService = PendingIntent.getService(this, 0, IntentLocationService, 0);

		note = buildNotification("LongitudeUpdater", "... is running.");
		note.setLatestEventInfo(this, "Longitude Updater",
								"... is running",
								pIntentLocationService);
		note.flags|=Notification.FLAG_FOREGROUND_SERVICE;
		startForeground(1337, note);
	}

	public void updateNotification(double lat, double lon){
		note = buildNotification("LongitudeUpdater", "Lat : " + String.valueOf(lat) +
													 "| Lon :" + String.valueOf(lon));
		notificationManager.notify(NOTIFICATION_ID, note);
	}

	private void uploadLocation(double lat, double lon){
		Intent intent = new Intent(this, UploadService.class);
		intent.putExtra("lat", String.valueOf(lat));
		intent.putExtra("lon", String.valueOf(lon));
		startService(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		//TODO for communication return IBinder implementation
		return null;
	}

	@Override
	public void onDestroy(){
		locationManager.removeUpdates(locationListener);
	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
		// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
		  return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager
			  = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
}
