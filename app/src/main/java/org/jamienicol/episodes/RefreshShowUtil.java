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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import java.util.List;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsTable;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.tvdb.Client;
import org.jamienicol.episodes.tvdb.Episode;
import org.jamienicol.episodes.tvdb.Show;

public class RefreshShowUtil
{
	private static final String TAG = RefreshShowUtil.class.getName();

	// Data needed in order to make show refresh request
	private static class RefreshRequestData {
		public int tvdbId;
		public String language;
	}

	public static void refreshShow(int showId,
	                               ContentResolver contentResolver) {
		final Client tvdbClient = new Client("25B864A8BC56AFAD");

		Log.i(TAG, String.format("Refreshing show %d", showId));

		final RefreshRequestData data =
			getRefreshRequestData(showId, contentResolver);

		// fetch full show + episode information from tvdb
		final Show show = tvdbClient.getShow(data.tvdbId, data.language);

		if (show != null) {
			updateShow(showId, show, contentResolver);
			updateExistingEpisodes(showId, show.getEpisodes(), contentResolver);
			addNewEpisodes(showId, show.getEpisodes(), contentResolver);
		}
	}

	private static RefreshRequestData getRefreshRequestData(
		int showId, ContentResolver contentResolver) {

		final Uri showUri =
			Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
			                     String.valueOf(showId));
		final String[] projection = {
			ShowsTable.COLUMN_TVDB_ID,
			ShowsTable.COLUMN_LANGUAGE,
		};

		final Cursor showCursor = contentResolver.query(showUri,
		                                                projection,
		                                                null,
		                                                null,
		                                                null);

		final int tvdbIdColumnIndex =
			showCursor.getColumnIndexOrThrow(ShowsTable.COLUMN_TVDB_ID);
		final int languageColumnIndex =
			showCursor.getColumnIndexOrThrow(ShowsTable.COLUMN_LANGUAGE);

		showCursor.moveToFirst();

		RefreshRequestData data = new RefreshRequestData();
		data.tvdbId = showCursor.getInt(tvdbIdColumnIndex);
		data.language = showCursor.getString(languageColumnIndex);
		return data;
	}

	private static void updateShow(int showId,
	                               Show show,
	                               ContentResolver contentResolver) {

		final ContentValues showValues = new ContentValues();
		showValues.put(ShowsTable.COLUMN_TVDB_ID, show.getId());
		showValues.put(ShowsTable.COLUMN_NAME, show.getName());
		showValues.put(ShowsTable.COLUMN_LANGUAGE, show.getLanguage());
		showValues.put(ShowsTable.COLUMN_OVERVIEW, show.getOverview());
		if (show.getFirstAired() != null) {
			showValues.put(ShowsTable.COLUMN_FIRST_AIRED,
			               show.getFirstAired().getTime() / 1000);
		}
		showValues.put(ShowsTable.COLUMN_BANNER_PATH, show.getBannerPath());
		showValues.put(ShowsTable.COLUMN_FANART_PATH, show.getFanartPath());
		showValues.put(ShowsTable.COLUMN_POSTER_PATH, show.getPosterPath());

		final Uri showUri =
			Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
			                     String.valueOf(showId));
		contentResolver.update(showUri, showValues, null, null);
	}

	private static void updateExistingEpisodes(int showId,
	                                           List<Episode> episodes,
	                                           ContentResolver contentResolver) {
		final Cursor episodesCursor =
			getEpisodesCursor(showId, contentResolver);

		while (episodesCursor.moveToNext()) {

			final int idColumnIndex =
				episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_ID);
			final int episodeId = episodesCursor.getInt(idColumnIndex);
			final Uri episodeUri =
				Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES,
				                     String.valueOf(episodeId));

			final int tvdbIdColumnIndex =
				episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_TVDB_ID);
			final int episodeTvdbId = episodesCursor.getInt(tvdbIdColumnIndex);
			final Episode episode = findEpisodeWithTvdbId(episodes,
			                                              episodeTvdbId);

			if (episode == null) {
				/* the episode no longer exists in tvdb. delete */
				Log.i(TAG, String.format("Deleting episode %d: no longer exists in tvdb.", episodeId));
				contentResolver.delete(episodeUri, null, null);

			} else {
				/* update the episode row with the new values */
				final ContentValues epValues = new ContentValues();
				epValues.put(EpisodesTable.COLUMN_TVDB_ID, episode.getId());
				epValues.put(EpisodesTable.COLUMN_SHOW_ID, showId);
				epValues.put(EpisodesTable.COLUMN_NAME, episode.getName());
				epValues.put(EpisodesTable.COLUMN_OVERVIEW,
				             episode.getOverview());
				epValues.put(EpisodesTable.COLUMN_EPISODE_NUMBER,
				             episode.getEpisodeNumber());
				epValues.put(EpisodesTable.COLUMN_SEASON_NUMBER,
				             episode.getSeasonNumber());
				if (episode.getFirstAired() != null) {
					epValues.put(EpisodesTable.COLUMN_FIRST_AIRED,
					             episode.getFirstAired().getTime() / 1000);
				}

				Log.i(TAG, String.format("Updating episode %d.", episodeId));
				contentResolver.update(episodeUri, epValues, null, null);

				/* remove episode from list of episodes
				 * returned by tvdb. by the end of this function
				 * this list will only contain new episodes */
				episodes.remove(episode);
			}
		}
	}

	private static Cursor getEpisodesCursor(int showId,
	                                        ContentResolver contentResolver) {
		final String[] projection = {
			EpisodesTable.COLUMN_ID,
			EpisodesTable.COLUMN_TVDB_ID
		};
		final String selection = String.format("%s=?",
		                                       EpisodesTable.COLUMN_SHOW_ID);
		final String[] selectionArgs = {
			String.valueOf(showId)
		};

		final Cursor cursor =
			contentResolver.query(ShowsProvider.CONTENT_URI_EPISODES,
			                      projection,
			                      selection,
			                      selectionArgs,
			                      null);

		return cursor;
	}

	private static Episode findEpisodeWithTvdbId(List<Episode> episodes,
	                                             int episodeTvdbId) {
		for (Episode ep : episodes) {
			if (ep.getId() == episodeTvdbId) {
				return ep;
			}
		}

		return null;
	}

	private static void addNewEpisodes(int showId,
	                                   List<Episode> episodes,
	                                   ContentResolver contentResolver) {

		for (Episode episode : episodes) {
			final ContentValues epValues = new ContentValues();
			epValues.put(EpisodesTable.COLUMN_TVDB_ID, episode.getId());
			epValues.put(EpisodesTable.COLUMN_SHOW_ID, showId);
			epValues.put(EpisodesTable.COLUMN_NAME, episode.getName());
			epValues.put(EpisodesTable.COLUMN_OVERVIEW,
			             episode.getOverview());
			epValues.put(EpisodesTable.COLUMN_EPISODE_NUMBER,
			             episode.getEpisodeNumber());
			epValues.put(EpisodesTable.COLUMN_SEASON_NUMBER,
			             episode.getSeasonNumber());
			if (episode.getFirstAired() != null) {
				epValues.put(EpisodesTable.COLUMN_FIRST_AIRED,
				             episode.getFirstAired().getTime() / 1000);
			}

			Log.i(TAG, "Adding new episode.");
			contentResolver.insert(ShowsProvider.CONTENT_URI_EPISODES,
			                       epValues);
		}
	}
}
