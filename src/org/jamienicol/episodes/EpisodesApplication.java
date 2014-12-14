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
import android.preference.PreferenceManager;
import android.util.Log;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import java.io.IOException;

public class EpisodesApplication
	extends Application
{
	private static final String TAG = EpisodesApplication.class.getName();

	private static EpisodesApplication instance;

	private AutoRefreshHelper autoRefreshHelper;
	private OkHttpClient httpClient;

	@Override
	public void onCreate() {
		super.onCreate();

		instance = this;

		// ensure the default settings are initialised at first launch,
		// rather than waiting for the settings screen to be opened.
		// do this before anything that needs these settings is instantiated.
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		autoRefreshHelper = AutoRefreshHelper.getInstance(this);

		httpClient = new OkHttpClient();
		try {
			Cache httpCache = new Cache(getCacheDir(), 1024 * 1024);
			httpClient.setCache(httpCache);
		} catch (IOException e) {
			Log.w(TAG, "Error initialising okhttp cache", e);
		}

		final ImageLoaderConfiguration imageLoaderConfig =
			new ImageLoaderConfiguration.Builder(this)
			.build();

		ImageLoader.getInstance().init(imageLoaderConfig);
	}

	public static EpisodesApplication getInstance() {
		return instance;
	}

	public OkHttpClient getHttpClient() {
		return httpClient;
	}
}
