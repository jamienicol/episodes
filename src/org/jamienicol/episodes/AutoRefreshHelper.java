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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import org.jamienicol.episodes.db.ShowsTable;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.RefreshShowUtil;

public class AutoRefreshHelper
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private static final String TAG = AutoRefreshHelper.class.getName();

	private static final String KEY_PREF_AUTO_REFRESH_ENABLED =
		"pref_auto_refresh_enabled";
	private static final String KEY_PREF_AUTO_REFRESH_PERIOD =
		"pref_auto_refresh_period";

	private static final String KEY_LAST_AUTO_REFRESH_TIME =
		"last_auto_refresh_time";

	private static AutoRefreshHelper instance;

	private final Context context;
	private final SharedPreferences preferences;

	public AutoRefreshHelper(Context context) {
		this.context = context;

		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.registerOnSharedPreferenceChangeListener(this);
	}

	public static synchronized AutoRefreshHelper getInstance(Context context) {
		if (instance == null) {
			instance = new AutoRefreshHelper(context);
		}
		return instance;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
	                                      String key) {
		if (key.equals(KEY_PREF_AUTO_REFRESH_ENABLED)) {
			onAutoRefreshEnabledChanged();
		} else if (key.equals(KEY_PREF_AUTO_REFRESH_PERIOD)) {
			onAutoRefreshPeriodChanged();
		}
	}

	private void onAutoRefreshEnabledChanged() {
		rescheduleAlarm();
	}

	private void onAutoRefreshPeriodChanged() {
		rescheduleAlarm();
	}

	private boolean getAutoRefreshEnabled() {
		return preferences.getBoolean(KEY_PREF_AUTO_REFRESH_ENABLED, false);
	}

	private long getAutoRefreshPeriod() {
		final String hours =
			preferences.getString(KEY_PREF_AUTO_REFRESH_PERIOD, "0");

		// convert hours to milliseconds
		return Long.parseLong(hours) * 60 * 60 * 1000;
	}

	private long getPrevAutoRefreshTime() {
		return preferences.getLong(KEY_LAST_AUTO_REFRESH_TIME, 0);
	}

	private void setPrevAutoRefreshTime(long time) {
		final SharedPreferences.Editor editor =
			preferences.edit();
		editor.putLong(KEY_LAST_AUTO_REFRESH_TIME, time);
		editor.commit();
	}

	public void rescheduleAlarm() {
		final AlarmManager alarmManager =
			(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		final Intent intent =
			new Intent(context, AutoRefreshHelper.Service.class);
		final PendingIntent pendingIntent =
			PendingIntent.getService(context, 0, intent, 0);

		if (getAutoRefreshEnabled() && getAutoRefreshPeriod() != 0) {
			final long alarmTime =
				getPrevAutoRefreshTime() + getAutoRefreshPeriod();

			Log.i(TAG, String.format("Scheduling auto refresh alarm for %d.", alarmTime));

			alarmManager.set(AlarmManager.RTC,
			                 alarmTime,
			                 pendingIntent);
		} else {
			Log.i(TAG, "Cancelling auto refresh alarm.");

			alarmManager.cancel(pendingIntent);
		}
	}

	public static class Service
		extends IntentService
	{
		private static final String TAG = Service.class.getName();

		public Service() {
			super(Service.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			Log.i(TAG, "Refreshing all shows.");

			final ContentResolver contentResolver = getContentResolver();
			final Cursor cursor = getShowsCursor(contentResolver);

			while (cursor.moveToNext()) {
				final int showIdColumnIndex =
					cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_ID);
				final int showId = cursor.getInt(showIdColumnIndex);

				RefreshShowUtil.refreshShow(showId, contentResolver);
			}

			final AutoRefreshHelper helper =
				AutoRefreshHelper.getInstance(getApplicationContext());

			helper.setPrevAutoRefreshTime(System.currentTimeMillis());
			helper.rescheduleAlarm();
		}

		private static Cursor getShowsCursor(ContentResolver contentResolver) {
			final String[] projection = {
				ShowsTable.COLUMN_ID
			};

			final Cursor cursor =
				contentResolver.query(ShowsProvider.CONTENT_URI_SHOWS,
				                      projection,
				                      null,
				                      null,
				                      null);

			return cursor;
		}
	}
}
