package com.redcoracle.episodes;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	public static SharedPreferences getSharedPreferences() {
		Context context = MainActivity.getAppContext();
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
}
