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

package org.jamienicol.nextepisode.tvdb;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

public class Client
{
	private static final String TAG = "Client";
	private static final String baseUrl = "http://thetvdb.com/api";

	private final String apiKey;

	public Client(String apiKey) {
		this.apiKey = apiKey;
	}

	public List<Show> searchShows(String query) {

		try {
			String escapedQuery = URLEncoder.encode(query, "UTF-8");
			StringBuilder urlBuilder = new StringBuilder();
			urlBuilder.append(baseUrl);
			urlBuilder.append("/GetSeries.php?seriesname=");
			urlBuilder.append(escapedQuery);

			URL url = new URL(urlBuilder.toString());
			URLConnection connection = url.openConnection();
			InputStream inputStream = new BufferedInputStream(connection.getInputStream());

			SearchShowsParser parser = new SearchShowsParser();
			return parser.parse(inputStream);

		} catch (IOException e) {
			Log.w(TAG, "IOException - searchShows: " + e.toString());
			return null;
		}
	}
}
