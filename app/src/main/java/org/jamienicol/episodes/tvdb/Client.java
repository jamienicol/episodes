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
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import org.jamienicol.episodes.EpisodesApplication;

public class Client
{
	private static final String TAG = Client.class.getName();
	private static final String baseUrl = "http://thetvdb.com/api";

	private final String apiKey;
	private final OkHttpClient http;

	public Client(String apiKey) {
		this.apiKey = apiKey;
		http = EpisodesApplication.getInstance().getHttpClient();
	}

	public List<Show> searchShows(String query) {

		try {
			final String escapedQuery = URLEncoder.encode(query, "UTF-8");
			final String url =
				String.format("%s/GetSeries.php?seriesname=%s&language=all",
				              baseUrl,
				              escapedQuery);
			Log.d(TAG, String.format("Sending request to %s", url));

			final Request request = new Request.Builder().url(url).build();

			final Response response = http.newCall(request).execute();

			Log.d(TAG, String.format("Received response %d: %s",
			                         response.code(),
			                         response.message()));

			if (response.isSuccessful()) {
				final SearchShowsParser parser = new SearchShowsParser();

				return parser.parse(response.body().byteStream());
			} else {
				return null;
			}
		} catch (IOException e) {
			Log.w(TAG, e);
			return null;
		}
	}

	public Show getShow(int id, String language) {
		try {
			// fall back to english if no language specified
			if (language == null) {
				language = "en";
			}

			final String url = String.format(Locale.US,
			                                 "%s/%s/series/%d/all/%s.xml",
			                                 baseUrl,
			                                 apiKey,
			                                 id,
			                                 language);
			Log.d(TAG, String.format("Sending request to %s", url));

			final Request request = new Request.Builder().url(url).build();

			final Response response = http.newCall(request).execute();

			Log.d(TAG, String.format("Received response %d: %s",
			                         response.code(),
			                         response.message()));

			if (response.isSuccessful()) {
				final GetShowParser parser = new GetShowParser();

				return parser.parse(response.body().byteStream());
			} else {
				return null;
			}
		} catch (IOException e) {
			Log.w(TAG, e);
			return null;
		}
	}
}
