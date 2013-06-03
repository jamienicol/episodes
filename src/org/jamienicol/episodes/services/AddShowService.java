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
import android.database.Cursor;
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

		if (isShowAlreadyAdded(tvdbId) == false) {

			// fetch full show + episode information from tvdb
			Show show = tvdbClient.getShow(tvdbId);

			// add show and episodes to database
			int showId = insertShow(show);
			for (Episode episode : show.getEpisodes()) {
				insertEpisode(episode, showId);
			}
		}
	}

	private boolean isShowAlreadyAdded(int tvdbId) {
		final String[] projection = {
		};
		final String selection = String.format("%s=?",
		                                       ShowsTable.COLUMN_TVDB_ID);
		final String[] selectionArgs = {
			new Integer(tvdbId).toString()
		};
		Cursor cursor =
			getContentResolver().query(ShowsProvider.CONTENT_URI_SHOWS,
			                           projection,
			                           selection,
			                           selectionArgs,
			                           null);

		return cursor.moveToFirst();
	}

	private int insertShow(Show show) {
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

		// need to obtain the ID of the inserted show.
		// the ID is just the final segment of the URI
		int showId = Integer.parseInt(showUri.getLastPathSegment());

		Log.i(TAG, String.format("show %s successfully added to database as row %d. adding episodes",
		                         show.getName(),
		                         showId));

		return showId;
	}

	private void insertEpisode(Episode episode, int showId) {
		ContentValues episodeValues = new ContentValues();
		episodeValues.put(EpisodesTable.COLUMN_TVDB_ID, episode.getId());
		episodeValues.put(EpisodesTable.COLUMN_SHOW_ID, showId);
		episodeValues.put(EpisodesTable.COLUMN_NAME, episode.getName());
		episodeValues.put(EpisodesTable.COLUMN_OVERVIEW,
		             episode.getOverview());
		episodeValues.put(EpisodesTable.COLUMN_EPISODE_NUMBER,
		             episode.getEpisodeNumber());
		episodeValues.put(EpisodesTable.COLUMN_SEASON_NUMBER,
		             episode.getSeasonNumber());
		if (episode.getFirstAired() != null) {
			episodeValues.put(EpisodesTable.COLUMN_FIRST_AIRED,
			             episode.getFirstAired().getTime() / 1000);
		}

		getContentResolver().insert(ShowsProvider.CONTENT_URI_EPISODES,
		                            episodeValues);
	}
}
