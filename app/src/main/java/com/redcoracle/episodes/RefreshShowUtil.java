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

package com.redcoracle.episodes;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import com.redcoracle.episodes.db.EpisodesTable;
import com.redcoracle.episodes.db.ShowsProvider;
import com.redcoracle.episodes.db.ShowsTable;
import com.redcoracle.episodes.tvdb.Client;
import com.redcoracle.episodes.tvdb.Episode;
import com.redcoracle.episodes.tvdb.Show;

import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class RefreshShowUtil {
	private static final String TAG = RefreshShowUtil.class.getName();

	public static void refreshShow(int showId, ContentResolver contentResolver) {
		Log.i(TAG, String.format("Refreshing show %d", showId));

		final Client tmdbClient = new Client();
		SharedPreferences preferences = Preferences.getSharedPreferences();

		final String showLanguage = preferences.getString("pref_language", "en");
		final HashMap<String, String> showIds = getShowIds(showId, contentResolver);
		final Show show = tmdbClient.getShow(showIds, showLanguage);

		if (show != null) {
			updateShow(showId, show, contentResolver);
			if (show.getEpisodes() != null) {
				updateEpisodes(showId, show.getEpisodes(), contentResolver);
			}
		}
	}

	private static HashMap<String, String> getShowIds(int showId, ContentResolver contentResolver) {
		final Uri showUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS, String.valueOf(showId));
		final String[] projection = {
			ShowsTable.COLUMN_TVDB_ID,
			ShowsTable.COLUMN_TMDB_ID,
			ShowsTable.COLUMN_IMDB_ID
		};
		final Cursor showCursor = contentResolver.query(showUri, projection, null, null, null);
		showCursor.moveToFirst();
		final int tvdbIdColumnIndex = showCursor.getColumnIndexOrThrow(ShowsTable.COLUMN_TVDB_ID);
		final int tmdbIdColumnIndex = showCursor.getColumnIndexOrThrow(ShowsTable.COLUMN_TMDB_ID);
		final int imdbIdColumnIndex = showCursor.getColumnIndexOrThrow(ShowsTable.COLUMN_IMDB_ID);
		HashMap<String, String> showIds = new HashMap<String, String>() {{
			put("tvdbId", showCursor.getString(tvdbIdColumnIndex));
			put("tmdbId", showCursor.getString(tmdbIdColumnIndex));
			put("imdbId", showCursor.getString(imdbIdColumnIndex));
		}};
		showCursor.close();
		return showIds;
	}

	private static void updateShow(int showId, Show show, ContentResolver contentResolver) {
		final ContentValues showValues = new ContentValues();
		if (show.getTvdbId() != 0) {
			showValues.put(ShowsTable.COLUMN_TVDB_ID, show.getTvdbId());
		}
		showValues.put(ShowsTable.COLUMN_TMDB_ID, show.getTmdbId());
		showValues.put(ShowsTable.COLUMN_IMDB_ID, show.getImdbId());
		showValues.put(ShowsTable.COLUMN_NAME, show.getName());
		showValues.put(ShowsTable.COLUMN_LANGUAGE, show.getLanguage());
		showValues.put(ShowsTable.COLUMN_OVERVIEW, show.getOverview());
		if (show.getFirstAired() != null) {
			showValues.put(ShowsTable.COLUMN_FIRST_AIRED, show.getFirstAired().getTime() / 1000);
		}
		showValues.put(ShowsTable.COLUMN_BANNER_PATH, show.getBannerPath());
		showValues.put(ShowsTable.COLUMN_FANART_PATH, show.getFanartPath());
		showValues.put(ShowsTable.COLUMN_POSTER_PATH, show.getPosterPath());

		final Uri showUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS, String.valueOf(showId));
		contentResolver.update(showUri, showValues, null, null);
	}

	private static void updateEpisodes(int showId, List<Episode> episodes, ContentResolver contentResolver) {
		// TODO: likely performance gains to be had in here
		final MultiKeyMap seasonPairMap = new MultiKeyMap();
		final HashSet<String> seen = new HashSet<>();
		final SparseArray<Episode> episodeMap = new SparseArray<>();
		final ArrayList<ContentValues> updates = new ArrayList<>();

		for (Episode episode : episodes) {
			episodeMap.append(episode.getTmdbId(), episode);
			seasonPairMap.put(episode.getSeasonNumber(), episode.getEpisodeNumber(), episode);
		}

		final Cursor cursor = getEpisodesCursor(showId, contentResolver);

		while (cursor.moveToNext()) {
			final int idColumnIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_ID);
			final int episodeId = cursor.getInt(idColumnIndex);
			final int tmdbColumnIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_TMDB_ID);
			final int episodeTmdbId = cursor.getInt(tmdbColumnIndex);

			Episode episode = episodeMap.get(episodeTmdbId);
			final Uri episodeUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES, String.valueOf(episodeId));

			if (episode == null) {
				// Unable to find episode by ID; try season/episode pair instead.
				// I think this should only happen when a show needs to migrate from TVDB->TMDB
				episode = (Episode) seasonPairMap.get(
					cursor.getInt(cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER)),
					cursor.getInt(cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_EPISODE_NUMBER))
				);
				if (episode != null) {
					Log.d(TAG, String.format("Matched by season/episode number: %s", episodeId));
					if (seen.contains(episode.identifier())) {
						// Already matched a different episode by season/episode pair,
						// so this will fail on insert and should be deleted instead.
						Log.d(TAG, String.format("Deleting duplicate episode %s (%s)",episode.identifier(), episodeId));
						contentResolver.delete(episodeUri, null, null);
					} else {
						seen.add(episode.identifier());
						episodes.remove(episode);
						continue;
					}
				}
			} else if (seen.contains(episode.identifier())) {
				Log.d(TAG, String.format("Deleting previously seen episode %s (%s)", episode.identifier(), episode.getId()));
				contentResolver.delete(episodeUri, null, null);
				continue;
			} else {
				Log.d(TAG, String.format("Found match by TMDB ID: %s", episodeId));
				seen.add(episode.identifier());
			}

			if (episode == null) {
				Log.i(TAG, String.format("No matches found. Deleting episode: %d", episodeId));
				contentResolver.delete(episodeUri, null, null);
			} else {
				final ContentValues epValues = new ContentValues();
				epValues.put(EpisodesTable.COLUMN_ID, episodeId);
				epValues.put(EpisodesTable.COLUMN_SHOW_ID, showId);
				epValues.put(EpisodesTable.COLUMN_TVDB_ID, episode.getTvdbId());
				epValues.put(EpisodesTable.COLUMN_TMDB_ID, episode.getTmdbId());
				epValues.put(EpisodesTable.COLUMN_IMDB_ID, episode.getImdbId());
				epValues.put(EpisodesTable.COLUMN_NAME, episode.getName());
				epValues.put(EpisodesTable.COLUMN_LANGUAGE, episode.getLanguage());
				epValues.put(EpisodesTable.COLUMN_OVERVIEW, episode.getOverview());
				epValues.put(EpisodesTable.COLUMN_EPISODE_NUMBER, episode.getEpisodeNumber());
				epValues.put(EpisodesTable.COLUMN_SEASON_NUMBER, episode.getSeasonNumber());
				if (episode.getFirstAired() != null) {
					epValues.put(EpisodesTable.COLUMN_FIRST_AIRED, episode.getFirstAired().getTime() / 1000);
				}

				Log.i(TAG, String.format("Updating episode %d.", episodeId));
				updates.add(epValues);

				/* remove episode from list of episodes
				 * returned by tvdb. by the end of this function
				 * this list will only contain new episodes */
				episodes.remove(episode);
			}
		}
		cursor.close();
		contentResolver.bulkInsert(ShowsProvider.CONTENT_URI_EPISODES, updates.toArray(new ContentValues[0]));


		for (Episode episode : episodes) {
			final ContentValues epValues = new ContentValues();
			epValues.put(EpisodesTable.COLUMN_SHOW_ID, showId);
			epValues.put(EpisodesTable.COLUMN_TVDB_ID, episode.getTvdbId());
			epValues.put(EpisodesTable.COLUMN_TMDB_ID, episode.getTmdbId());
			epValues.put(EpisodesTable.COLUMN_IMDB_ID, episode.getImdbId());
			epValues.put(EpisodesTable.COLUMN_NAME, episode.getName());
			epValues.put(EpisodesTable.COLUMN_LANGUAGE, episode.getLanguage());
			epValues.put(EpisodesTable.COLUMN_OVERVIEW, episode.getOverview());
			epValues.put(EpisodesTable.COLUMN_EPISODE_NUMBER, episode.getEpisodeNumber());
			epValues.put(EpisodesTable.COLUMN_SEASON_NUMBER, episode.getSeasonNumber());
			if (episode.getFirstAired() != null) {
				epValues.put(EpisodesTable.COLUMN_FIRST_AIRED, episode.getFirstAired().getTime() / 1000);
			}

			contentResolver.insert(ShowsProvider.CONTENT_URI_EPISODES, epValues);
		}
	}

	private static Cursor getEpisodesCursor(int showId, ContentResolver contentResolver) {
		final String[] projection = {
			EpisodesTable.COLUMN_ID,
			EpisodesTable.COLUMN_TVDB_ID,
			EpisodesTable.COLUMN_TMDB_ID,
			EpisodesTable.COLUMN_IMDB_ID,
			EpisodesTable.COLUMN_SEASON_NUMBER,
			EpisodesTable.COLUMN_EPISODE_NUMBER
		};
		final String selection = String.format("%s=?", EpisodesTable.COLUMN_SHOW_ID);
		final String[] selectionArgs = {
			String.valueOf(showId)
		};

		return contentResolver.query(ShowsProvider.CONTENT_URI_EPISODES, projection, selection, selectionArgs, null);
	}
}
