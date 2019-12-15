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

package org.jamienicol.episodes.tvdb;

import android.util.Log;

import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.entities.EpisodesResponse;
import com.uwetrottmann.thetvdb.entities.SeriesResponse;
import com.uwetrottmann.thetvdb.entities.SeriesResultsResponse;

import org.jamienicol.episodes.EpisodesApplication;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Client
{
	private static final String TAG = Client.class.getName();

    private final TheTvdb tvdb;

	public Client() {
        tvdb = EpisodesApplication.getInstance().getTvdbClient();
	}

	public List<Show> searchShows(String query, String language) {

		try {
			final String escapedQuery = URLEncoder.encode(query, "UTF-8");
			final retrofit2.Response<SeriesResultsResponse> response =
					tvdb.search().series(escapedQuery, null, null, null, language).execute();
			if (response.isSuccessful()) {
				final SearchShowsParser parser = new SearchShowsParser();
				return parser.parse(response, language);
			} else {
				return new LinkedList<>();
			}
		} catch (IOException e) {
			Log.w(TAG, e);
			return null;
		}
	}

	public Show getShow(int id, String language) {
		try {
			final retrofit2.Response<SeriesResponse> seriesResponse = tvdb.series().series(id, language).execute();
			Log.d(TAG, String.format("Received response %d: %s", seriesResponse.code(), seriesResponse.message()));
			if (seriesResponse.isSuccessful()) {
				final GetShowParser parser = new GetShowParser();
				Show show = parser.parse(seriesResponse.body().data, language);

				if (show != null) {
                    ArrayList<Episode> episodes = new ArrayList<>();
				    final GetEpisodesParser episodesParser = new GetEpisodesParser();
					Integer page = 1;
					while (page != null) {
						EpisodesResponse episodesResponse = tvdb.series().episodes(show.getId(), page, language).execute().body();
						episodes.addAll(episodesParser.parse(episodesResponse));
						page = episodesResponse.links.next;
					}
					show.setEpisodes(episodes);
                }
                return show;
			} else {
				return null;
			}
		} catch (IOException e) {
			Log.w(TAG, e);
			return null;
		}
	}
}
