package com.firebirdberlin.longitude;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {
	public static final String PREF_KEY_SERVER_ADDRESS = "pref_key_server_address";
	public static final String PREF_KEY_SERVER_PASSWORD = "pref_key_server_password";
	public static final String PREF_KEY_PHONE_NUMBER = "pref_key_phone_number";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static void openSettings(Context context) {
		Intent myIntent = new Intent(context, SettingsActivity.class);
		context.startActivity(myIntent);
	}

	public static String getServerURL(Context context){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		return settings.getString(SettingsActivity.PREF_KEY_SERVER_ADDRESS, "");
	}

	public static String getPhoneNumber(Context context){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		return settings.getString(SettingsActivity.PREF_KEY_PHONE_NUMBER, "");
	}

}
