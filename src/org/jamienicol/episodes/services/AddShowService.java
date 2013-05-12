/*
 * Copyright (C) 2012 Jamie Nicol <jamie@thenicols.net>
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

package org.jamienicol.episodes.services;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsTable;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.tvdb.Client;
import org.jamienicol.episodes.tvdb.Episode;
import org.jamienicol.episodes.tvdb.Show;

public class AddShowService extends IntentService
{
	private static final String TAG = "AddShowService";

	public AddShowService() {
		super("AddShowService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Client tvdbClient = new Client("25B864A8BC56AFAD");

		int tvdbId = intent.getIntExtra("tvdbId", 0);

		// fetch full show + episode information from tvdb
		Show show = tvdbClient.getShow(tvdbId);

		// fill in information about the show
		ContentValues showValues = new ContentValues();
		showValues.put(ShowsTable.COLUMN_TVDB_ID, show.getId());
		showValues.put(ShowsTable.COLUMN_NAME, show.getName());
		showValues.put(ShowsTable.COLUMN_OVERVIEW, show.getOverview());
		if (show.getFirstAired() != null) {
			showValues.put(ShowsTable.COLUMN_FIRST_AIRED,
			               show.getFirstAired().getTime() / 1000);
		}

		// insert the show into the database
		Uri showUri =
			getContentResolver().insert(ShowsProvider.CONTENT_URI_SHOWS,
			                            showValues);

		if (showUri != null) {
			// need to obtain the ID of the inserted show.
			// the ID is just the final segment of the URI
			int showId = Integer.parseInt(showUri.getLastPathSegment());

			Log.i(TAG, String.format("show %s successfully added to database as row %d. adding episodes",
			                         show.getName(),
			                         showId));

			// insert each episode into the database
			for (Episode ep : show.getEpisodes()) {
				ContentValues epValues = new ContentValues();
				epValues.put(EpisodesTable.COLUMN_TVDB_ID, ep.getId());
				epValues.put(EpisodesTable.COLUMN_SHOW_ID, showId);
				epValues.put(EpisodesTable.COLUMN_NAME, ep.getName());
				epValues.put(EpisodesTable.COLUMN_OVERVIEW,
				             ep.getOverview());
				epValues.put(EpisodesTable.COLUMN_EPISODE_NUMBER,
				             ep.getEpisodeNumber());
				epValues.put(EpisodesTable.COLUMN_SEASON_NUMBER,
				             ep.getSeasonNumber());
				if (ep.getFirstAired() != null) {
					epValues.put(EpisodesTable.COLUMN_FIRST_AIRED,
					             ep.getFirstAired().getTime() / 1000);
				}

				getContentResolver().insert(ShowsProvider.CONTENT_URI_EPISODES,
				                            epValues);
			}

		} else {
			Log.i(TAG, String.format("show %s not added to database. skipping episodes",
			                         show.getName()));
		}
	}
}
