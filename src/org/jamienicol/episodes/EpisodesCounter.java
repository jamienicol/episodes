/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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

import android.database.Cursor;
import android.util.SparseIntArray;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import org.jamienicol.episodes.db.EpisodesTable;

public class EpisodesCounter
{
	private String keyColumn;
	private Set<Integer> keys;
	private SparseIntArray numAiredEpisodesMap;
	private SparseIntArray numWatchedEpisodesMap;
	private SparseIntArray numUpcomingEpisodesMap;

	public EpisodesCounter(String keyColumn) {
		this.keyColumn = keyColumn;

		keys = new TreeSet<Integer>();
		numAiredEpisodesMap = new SparseIntArray();
		numWatchedEpisodesMap = new SparseIntArray();
		numUpcomingEpisodesMap = new SparseIntArray();
	}

	public void swapCursor(Cursor episodesCursor) {
		keys.clear();
		numAiredEpisodesMap.clear();
		numWatchedEpisodesMap.clear();
		numUpcomingEpisodesMap.clear();

		if (episodesCursor == null || episodesCursor.moveToFirst() == false) {
			return;
		}

		do {
			final int keyColumnIndex =
				episodesCursor.getColumnIndexOrThrow(keyColumn);
			final int key = episodesCursor.getInt(keyColumnIndex);

			// check if episode is aired, watched, or upcoming
			final int seasonNumberColumnIndex =
				episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER);
			final int seasonNumber =
				episodesCursor.getInt(seasonNumberColumnIndex);

			final int firstAiredColumnIndex =
				episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_FIRST_AIRED);
			Date firstAired = null;
			if (!episodesCursor.isNull(firstAiredColumnIndex)) {
				firstAired =
					new Date(episodesCursor.getLong(firstAiredColumnIndex)
					         * 1000);
			}

			final int watchedColumnIndex =
				episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED);
			final boolean watched =
				episodesCursor.getInt(watchedColumnIndex) > 0 ? true : false;


			if (keys.contains(key) == false) {
				keys.add(key);
			}

			// increment the appropriate counter(s) for this show.
			// count shows with no aired date as upcoming,
			// unless they're specials in which case count them as aired.
			if ((firstAired != null && firstAired.before(new Date()))
			    || seasonNumber == 0) {
				numAiredEpisodesMap.put(key,
				                        numAiredEpisodesMap.get(key) + 1);
				if (watched) {
					numWatchedEpisodesMap.put(key,
					                          numWatchedEpisodesMap.get(key) + 1);
				}
			} else {
				numUpcomingEpisodesMap.put(key,
				                           numUpcomingEpisodesMap.get(key) + 1);
			}
		} while (episodesCursor.moveToNext());
	}

	public Set<Integer> getKeys() {
		return keys;
	}

	public int getNumAiredEpisodes(int key) {
		final Integer value = numAiredEpisodesMap.get(key);
		if (value == null) {
			return 0;
		} else {
			return value;
		}
	}

	public int getNumWatchedEpisodes(int key) {
		final Integer value = numWatchedEpisodesMap.get(key);
		if (value == null) {
			return 0;
		} else {
			return value;
		}
	}

	public int getNumUpcomingEpisodes(int key) {
		final Integer value = numUpcomingEpisodesMap.get(key);
		if (value == null) {
			return 0;
		} else {
			return value;
		}
	}
}
