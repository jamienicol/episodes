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

import com.uwetrottmann.tmdb2.entities.BaseTvShow;
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage;

import java.util.LinkedList;
import java.util.List;

class SearchShowsParser {
    private static final String TAG = SearchShowsParser.class.getName();

    private List<Show> parsed;

    List<Show> parse(TvShowResultsPage results, String language) {
        try {
            List<BaseTvShow> series = results.results;
            parsed = new LinkedList<>();
            for(BaseTvShow s : series) {
                Show show = new Show();
                show.setId(s.id);
                show.setTmdbId(s.id);
                show.setName(s.name);
                show.setLanguage(language);
                show.setOverview(s.overview);
                show.setFirstAired(s.first_air_date);
                parsed.add(show);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return parsed;
    }

}
