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

package com.redcoracle.episodes.tvdb;

import android.util.Log;

import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesResultsResponse;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import retrofit2.Response;

class SearchShowsParser {
    private static final String TAG = SearchShowsParser.class.getName();

    private List<Show> parsed;

    List<Show> parse(Response<SeriesResultsResponse> response, String language) {
        try {
            List<Series> series = response.body().data;
            parsed = new LinkedList<>();
            for(Series s : series) {
                Show show = new Show();
                show.setId(s.id);
                show.setName(s.seriesName);
                show.setLanguage(language);
                show.setOverview(s.overview);
                try {
                    if (s.firstAired == null) {
                        show.setFirstAired(null);
                    } else {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        Date firstAired = df.parse(s.firstAired);
                        show.setFirstAired(firstAired);
                    }

                } catch (ParseException e) {
                    Log.w(TAG, "Error parsing first aired date: " + e.toString());
                    show.setFirstAired(null);
                }
                parsed.add(show);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return parsed;
    }
}
