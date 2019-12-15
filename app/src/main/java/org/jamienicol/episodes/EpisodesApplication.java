/*
 * Copyright (C) 2014 Jamie Nicol <jamie@thenicols.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jamienicol.episodes;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.uwetrottmann.thetvdb.TheTvdb;

public class EpisodesApplication extends Application {
	private static final String TAG = EpisodesApplication.class.getName();
	private static EpisodesApplication instance;
    private TheTvdb tvdbClient;

	@Override
	public void onCreate() {
		super.onCreate();

		instance = this;

		// ensure the default settings are initialised at first launch,
		// rather than waiting for the settings screen to be opened.
		// do this before anything that needs these settings is instantiated.
		// TODO: Is this needed?
		//PreferenceManager.setDefaultValues(this, R.xml.preferences, false);


        try {
			tvdbClient = new TheTvdb(BuildConfig.TVDB_KEY);
		} catch (Exception e) {
			Log.d(TAG, "Error initialising TvdbClient", e);
		}

		createNotificationChannel();
	}

	public static EpisodesApplication getInstance() {
		return instance;
	}

	public TheTvdb getTvdbClient() {
		return tvdbClient;
	}

	private void createNotificationChannel(){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.channel_name);
			String description = getString(R.string.channel_description);
			int importance = NotificationManager.IMPORTANCE_LOW;
			NotificationChannel channel = new NotificationChannel("episodes_channel_id", name, importance);
			channel.setDescription(description);
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}
}
