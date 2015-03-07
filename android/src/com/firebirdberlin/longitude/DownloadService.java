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


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.StatusLine;

import org.json.JSONObject;

import com.serpro.library.String.MCrypt;

public class DownloadService extends Service {
	private static String TAG = LongitudeUpdater.TAG + ".DownloadService";
	private Context mContext = null;

	@Override
	public void onCreate(){

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mContext = this;
		if (! MCrypt.secret_key_is_valid(mContext) ||
			SettingsActivity.getServerURL(mContext).length() == 0) {
			stopSelf();
		}

		Log.d(TAG, "DownloadService started");

		HttpPostAsyncTask sendPostReqAsyncTask = new HttpPostAsyncTask(this);
		sendPostReqAsyncTask.execute();

		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy(){

	}


	class HttpPostAsyncTask extends AsyncTask<String, Void, HttpResponse>{
		private Context mContext;

		public HttpPostAsyncTask (Context context){
			mContext = context;
		}

		@Override
		protected HttpResponse doInBackground(String... params) {
			String URL = SettingsActivity.getServerURL(mContext) + "getloc.php";

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(URL);
			HttpResponse response = null;

			String joined_userids = BuddyFragment.getBuddyIDs(mContext);
			String plaintext = "userids=" + joined_userids;
			String encrypted = MCrypt.encrypt_text(mContext, plaintext);

			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				nameValuePairs.add(new BasicNameValuePair("msg", encrypted));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				response = httpclient.execute(httppost);
			} catch (ClientProtocolException e) {

			} catch (IOException e) {

			} catch (Exception e){
				Log.e(TAG, "Exception: Could not retrieve locations");
//				e.printStackTrace();
			}

			return response;
		}

		@Override
		protected void onPostExecute(HttpResponse response) {
			super.onPostExecute(response);

			if (response == null){
				stopSelf();
				return;
			}

			Log.d(TAG, "Download finished.");

			StringBuilder builder = new StringBuilder();
			try{
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if(statusCode == 200){
					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(content));
					String line;
					while((line = reader.readLine()) != null){
						builder.append(line);
					}
				} else {
					Log.e(TAG,"Failed reading JSON object");
				}
			}catch(ClientProtocolException e){
				e.printStackTrace();
				Log.e(TAG, e.getMessage());
			} catch (IOException e){
				e.printStackTrace();
				Log.e(TAG, e.getMessage());
			}

			String json = builder.toString();
			String data = "";
			try{
				JSONObject jsonObject = new JSONObject(json);
				Log.d(TAG, jsonObject.getString("processing_time"));
				data = jsonObject.getString("data");
			} catch(Exception e){
				e.printStackTrace();
				Log.e(TAG, e.getMessage());
			}

			String decrypted = MCrypt.decrypt_text(mContext, data);
			sendMessageToActivity(mContext, decrypted);
			stopSelf();
		}
	}

	private static void sendMessageToActivity(Context context, String msg) {
		Intent intent = new Intent("FriendLocationUpdates");
		intent.putExtra("json", msg);
		context.sendBroadcast(intent);
	}
}
