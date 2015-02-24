package com.firebirdberlin.longitude;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.serpro.library.String.MCrypt;

public class BuddyFragment extends PreferenceFragment {
	Context mContext = null;
	private int buddy_count = 0;
	private int selection_count = 0;
	MenuItem menuitem_delete = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		final Context context = getActivity();

		mContext = getActivity();
		setHasOptionsMenu(true);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.preferences_buddies);


		Preference createButton = (Preference) findPreference("pref_key_create_qrcode");
		createButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		    @Override
		    public boolean onPreferenceClick(Preference arg0) {

				String encode_data = createQRCodeString();
//				String userID = UploadService.getDeviceId(context);
				Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
				intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
				intent.putExtra("ENCODE_DATA", encode_data);
		        startActivity(intent);
		        return true;
		    }
		});

		Preference scanButton = (Preference) findPreference("pref_key_scan_qrcode");
		scanButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		    @Override
		    public boolean onPreferenceClick(Preference arg0) {
				try {
					Intent intent = new Intent("com.google.zxing.client.android.SCAN");
			        intent.setPackage("com.google.zxing.client.android");
			        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			        startActivityForResult(intent, 0);
				} catch (Exception e) {
	                Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
	                Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
	                startActivity(marketIntent);
	            }
		        return true;
		    }
		});
        PreferenceCategory buddies = (PreferenceCategory)findPreference("pref_key_buddies");

		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = settings.edit();
		Set<String> buddy_set =  settings.getStringSet("buddies", new HashSet<String>());
		Iterator<String> iterator = buddy_set.iterator();
	    while(iterator.hasNext()) {
	        String setElement = iterator.next();
	        Log.d(LongitudeUpdater.TAG, "BUDDY :" + setElement);
	        buddy_count++;
	        String key = "buddy_" + String.valueOf(buddy_count);
	        addBuddyPreference(key, setElement);
	    }
    }


	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.buddy_activity_actions, menu);
		menuitem_delete = menu.getItem(0);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_delete_selected_buddies:
				deleteBuddyPreferences();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void addBuddy(String buddyID){

		String key = "buddy_" + String.valueOf(buddy_count);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = settings.edit();

		Set<String> buddy_set =  settings.getStringSet("buddies", new HashSet<String>());

		if (buddy_set.contains(buddyID) == false) {
			buddy_set.add(buddyID);

			// workaround for a bug not saving stringsets correctly
			editor.remove("buddies");
			editor.commit();
			editor.putStringSet("buddies", buddy_set);
			editor.commit();

			buddy_count++;
			addBuddyPreference(key, buddyID);
		}
	}

	private void addBuddyPreference(String key, String buddyID){
		PreferenceCategory buddies = (PreferenceCategory)findPreference("pref_key_buddies");
		Preference pref = new BuddyPreference(mContext, key, buddyID);
		buddies.addPreference(pref);
	}

	public class BuddyPreference extends Preference {
		private boolean selected = false;
		private String ID = "";
		private String phoneNumber = "";

		public BuddyPreference(Context context, String key, String buddyID) {
			super(context);
			String[] separated = buddyID.split("\\|");
			if (separated.length == 2){
				ID = separated[0]; // user ID
				phoneNumber = separated[1]; // phone number
			}
			setKey(key);
			setTitle(phoneNumber);
			setSummary(ID);
		}

	    @Override
	    protected void onBindView(View view) {
	        super.onBindView(view);
//	        view.setOnClickListener(buddyClickListener);
	        view.setOnLongClickListener(buddyLongClickListener);
	        view.setLongClickable(true);
	    }

		public boolean is_selected_for_delete(){
			return selected;
		};

		public String getBuddyID(){
			return ID+"|"+phoneNumber;
		};

		public String getID(){
			return ID;
		};

		View.OnClickListener buddyClickListener = new View.OnClickListener() {
		    @Override
		    public void onClick(View v) {
				Toast.makeText(mContext, "Item clicked", Toast.LENGTH_LONG).show();
		    }
		};

		View.OnLongClickListener buddyLongClickListener = new View.OnLongClickListener() {
		    @Override
		    public boolean onLongClick(View v) {

				if (selected == false){
					int resid = android.R.attr.colorLongPressedHighlight;
					TypedValue a = new TypedValue();
					mContext.getTheme().resolveAttribute(resid, a, true);
					if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
					    v.setBackgroundColor(a.data);
					} else {
						v.setBackgroundColor(Color.parseColor("#00668F"));
					}
					selection_count++;
				} else {
					v.setBackgroundColor(android.R.color.transparent);
					selection_count--;
				}
				selected = ! selected;
				menuitem_delete.setVisible(selection_count > 0);
		        return true;
		    }
		};
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0) {
			Log.d(LongitudeUpdater.TAG, "result_code: " + resultCode);
			if (resultCode != 0){
				String contents = data.getStringExtra("SCAN_RESULT");
				Log.d(LongitudeUpdater.TAG, "MESSAGE: " + contents);

				String decrypted = MCrypt.decrypt_text(mContext, contents);
				int last_delimiter = decrypted.lastIndexOf("|");
				String[] separated = decrypted.split("\\|");
				if (separated.length == 2){
//					separated[0]; // user ID
//					separated[1]; // phone number

					addBuddy(decrypted);
					Log.d(LongitudeUpdater.TAG, "BUDDY FOUND: " + decrypted);
				}
			}
		}
	}

	private String createQRCodeString(){
		String userID = UploadService.getDeviceId(mContext);
		String phoneNumber = SettingsActivity.getPhoneNumber(mContext);
		String encrypted = MCrypt.encrypt_text(mContext, userID+"|"+phoneNumber);
		return encrypted;
	};


	private void deleteBuddyPreferences(){
		PreferenceCategory buddies = (PreferenceCategory)findPreference("pref_key_buddies");
		for (int i = buddies.getPreferenceCount()-1; i >= 0 ; i--){
			BuddyPreference pref = (BuddyPreference) buddies.getPreference(i);
			if (pref.is_selected_for_delete() == true){
				deleteBuddy(pref.getBuddyID());
				buddies.removePreference(pref);
			}
		}
		selection_count = 0;
		menuitem_delete.setVisible(false);
	}

	private void deleteBuddy(String buddyID){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = settings.edit();

		Set<String> buddy_set =  settings.getStringSet("buddies", new HashSet<String>());

		if (buddy_set.contains(buddyID) == true) {
			buddy_set.remove(buddyID);

			// workaround for a bug not saving stringsets correctly
			editor.remove("buddies");
			editor.commit();
			editor.putStringSet("buddies", buddy_set);
			editor.commit();

			buddy_count--;
		}
	}

	static public String getBuddyIDs(Context context){

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();

		Set<String> buddy_set =  settings.getStringSet("buddies", new HashSet<String>());
		String joined = "";
		int count = 0;
		Iterator<String> iterator = buddy_set.iterator();
		while(iterator.hasNext()) {
			String setElement = iterator.next();
			if (count++ > 0)
				joined += ",";
			int last_delimiter = setElement.lastIndexOf("|");
			if (last_delimiter > -1)
				joined += setElement.substring(0, last_delimiter);
			else
				joined += setElement;
		}
		return joined;
	}
}
