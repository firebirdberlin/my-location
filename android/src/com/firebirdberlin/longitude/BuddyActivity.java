package com.firebirdberlin.longitude;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class BuddyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new BuddyFragment())
                .commit();
    }

    public static void start(Context context) {
		Intent myIntent = new Intent(context, BuddyActivity.class);
		context.startActivity(myIntent);
	}
}
