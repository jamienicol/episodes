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

package com.redcoracle.episodes.tvdb;

import android.util.Log;

import com.uwetrottmann.tmdb2.entities.TvShow;

class GetShowParser {
	private static final String TAG = "GetShowParser";

    Show parse(TvShow series, String language) {
        Show show;
        try {
            show = new Show();
            if (series.id != null) {
                show.setId(series.id);
                show.setTmdbId(series.id);
            } else {
                Log.w(TAG, String.format("Show does not have an ID: %s", series.name));
                return null;
            }
            if (series.external_ids != null) {
                if (series.external_ids.tvdb_id != null) {
                    show.setTvdbId(series.external_ids.tvdb_id);
                }
                if (series.external_ids.imdb_id != null) {
                    show.setImdbId(series.external_ids.imdb_id);
                }
            }
            show.setName(series.name);
            show.setLanguage(language);
            show.setOverview(series.overview);
            show.setFirstAired(series.first_air_date);
            show.setBannerPath(series.backdrop_path);
            show.setPosterPath(series.poster_path);
        } catch (Exception e) {
	        Log.w(TAG, e);
            return null;
        }
        return show;
    }
}
