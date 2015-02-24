package com.firebirdberlin.longitude;


import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.telephony.TelephonyManager;

import android.os.Bundle;
import android.os.IBinder;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.lang.String;
import java.lang.StringBuffer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import com.serpro.library.String.MCrypt;

public class UploadService extends Service {
	private static String TAG = LongitudeUpdater.TAG + ".UploadService";
	private Context mContext = null;

	@Override
	public void onCreate(){

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mContext = this;
		Bundle extras = intent.getExtras();
		if(extras != null) {

			String lat = extras.getString("lat");
			String lon = extras.getString("lon");
			String userid = this.getDeviceId(mContext);

			HttpPostAsyncTask sendPostReqAsyncTask = new HttpPostAsyncTask();
			sendPostReqAsyncTask.execute(userid, lat, lon);

		}

		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy(){

	}

	class HttpPostAsyncTask extends AsyncTask<String, Void, String>{

		@Override
		protected String doInBackground(String... params) {

			String URL = SettingsActivity.getServerURL(mContext) + "setloc.php";
			String paramUserID = params[0];
			String paramLat = params[1];
			String paramLon = params[2];


			 // Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(URL);

			String plaintext = "user_id=" + paramUserID
								+ "|lat=" + paramLat
								+ "|lon=" + paramLon;
			String encrypted = MCrypt.encrypt_text(mContext, plaintext);

			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				nameValuePairs.add(new BasicNameValuePair("msg", encrypted));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = httpclient.execute(httppost);
			} catch (ClientProtocolException e) {

			} catch (IOException e) {

			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			Log.d(TAG, "Upload finished.");
			stopSelf();
		}
	}

	static public String getDeviceId(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		String deviceID = telephonyManager.getDeviceId();

		try {
			deviceID = UploadService.computeHash(deviceID);
		}
		catch (NoSuchAlgorithmException e){

		}
		catch (UnsupportedEncodingException e){

		}

		return deviceID;
	};

	static public String computeHash(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException{
	    MessageDigest digest = MessageDigest.getInstance("SHA-256");
	    digest.reset();

	    byte[] byteData = digest.digest(input.getBytes("UTF-8"));
	    StringBuffer sb = new StringBuffer();

	    for (int i = 0; i < byteData.length; i++){
	      sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
	    }
	    return sb.toString();
	}

}
