package com.firebirdberlin.longitude;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.graphics.drawable.Drawable;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;

import android.preference.PreferenceManager;

import android.util.Log;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;

import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Iterator;
import java.util.LinkedList;

public class LongitudeUpdater extends Activity
{
	public static final String TAG = "LongitudeUpdater";
	public static final String PREFS_KEY = "LongitudeUpdater preferences";
	private Context mContext = this;
	private MapView mMapView;
	private MapController mMapController;
	private ResourceProxyImpl resProxyImp;
	private Handler handler;
//	private AlarmManager alarmMgr;
//	private PendingIntent alarmIntent;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

//		alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

//		startService(this);
		resProxyImp = new ResourceProxyImpl(this);
		handler     = new Handler();

		mMapView = (MapView) findViewById(R.id.mapview);
//		mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
		mMapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
		mMapView.setBuiltInZoomControls(true);
		mMapController = (MapController) mMapView.getController();
		mMapController.setZoom(17);
		handler.postDelayed(initMap, 300);
		handler.postDelayed(getFriendUpdates, 1000);

		enableBootReceiver(this);
		scheduleLocationService(this);
//		startDownloadService(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_friends:
				invalidateOptionsMenu();
				BuddyActivity.start(this);
				return true;
			case R.id.action_settings:
				SettingsActivity.openSettings(this);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mMessageReceiver, new IntentFilter("FriendLocationUpdates"));
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mMessageReceiver);
	}

	private Runnable initMap = new Runnable() {
		@Override
		public void run() {

			final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);

			// Define a listener that responds to location updates
			float lon = settings.getFloat("lon", 0);
			float lat = settings.getFloat("lat", 0);

			GeoPoint gPt = new GeoPoint((int)(lat * 1E6), (int)(lon * 1E6));
			mMapController.setCenter(gPt);

			// Add markers
			mMapView.getOverlays().clear();

			Drawable myMarker = getResources().getDrawable(R.drawable.ic_launcher);
			ItemizedIconOverlay markersOverlay = new ItemizedIconOverlay(new LinkedList(), myMarker, null, resProxyImp);
			mMapView.getOverlays().add(markersOverlay);

			OverlayItem ovm = new OverlayItem("Andi", "Droid", gPt);
			ovm.setMarker(myMarker);
			markersOverlay.addItem(ovm);
		}
	};

	private Runnable getFriendUpdates = new Runnable() {
		@Override
		public void run() {
			startDownloadService(mContext);
		}
	};


	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			String json = intent.getStringExtra("json");
			Log.d(TAG, "Message received" + json);


			final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);

			// Define a listener that responds to location updates
			double lon = (double) settings.getFloat("lon", 0);
			double lat = (double) settings.getFloat("lat", 0);

			GeoPoint gPt = new GeoPoint((int)(lat * 1E6), (int)(lon * 1E6));

			// Add markers
			mMapView.getOverlays().clear();

			Drawable myMarker = getResources().getDrawable(R.drawable.ic_launcher);
			ItemizedIconOverlay markersOverlay = new ItemizedIconOverlay(new LinkedList(), myMarker, null, resProxyImp);
			mMapView.getOverlays().add(markersOverlay);

			OverlayItem ovm = new OverlayItem("Andi", "Droid", gPt);
			ovm.setMarker(myMarker);
			markersOverlay.addItem(ovm);

			try{
				JSONObject jsonObject = new JSONObject(json);
				Iterator keys = jsonObject.keys();

				while(keys.hasNext()) {
					String key = (String)keys.next();

					JSONObject current = jsonObject.getJSONObject(key);
					Log.d(TAG, key + " = " + current);

					lat = current.getDouble("lat");
					lon = current.getDouble("lng");

					GeoPoint geoPoint = new GeoPoint((int)(lat * 1E6),(int)(lon * 1E6));
					OverlayItem ovitem = new OverlayItem("Andi", "Droid", geoPoint);

					ovitem.setMarker(myMarker);
					markersOverlay.addItem(ovitem);
				}

			} catch(Exception e){
				e.printStackTrace();
			}
			mMapView.invalidate();

			handler.postDelayed(getFriendUpdates,120000);
		}
	};

	public void enableBootReceiver(Context context){
		ComponentName receiver = new ComponentName(context, BootReceiver.class);
		PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(receiver,
									  PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
									  PackageManager.DONT_KILL_APP);
	}

	public void disableBootReceiver(Context context){
		ComponentName receiver = new ComponentName(context, BootReceiver.class);
		PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(receiver,
									  PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
									  PackageManager.DONT_KILL_APP);
	}

//	public void scheduleLocationService(Context context){
//		Intent intent = new Intent(context, LocationService.class);
//		alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
//		alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//									 AlarmManager.INTERVAL_FIFTEEN_MINUTES,
//									 AlarmManager.INTERVAL_FIFTEEN_MINUTES, alarmIntent);
//	}

	public static void scheduleLocationService(Context context) {
		Intent intent = new Intent(context, AlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

//	    long firstTime = SystemClock.elapsedRealtime();
//	    firstTime += 10000;//start 10s after first register.

		AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//						5000, 5000,
						AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						sender);
	}

	public static void  startService(Context context){
		Intent intent = new Intent(context, LocationService.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startService(intent);
	}

	public static void  startDownloadService(Context context){
		Intent intent = new Intent(context, DownloadService.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startService(intent);
	}
}
