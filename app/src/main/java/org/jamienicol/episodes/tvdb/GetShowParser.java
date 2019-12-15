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

import com.uwetrottmann.thetvdb.entities.Series;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class GetShowParser
{
	private static final String TAG = "GetShowParser";

    Show parse(Series series, String language) {
        Show show;
        try {
            show = new Show();

            show.setId(series.id);
            show.setName(series.seriesName);
            show.setLanguage(language);
            show.setOverview(series.overview);
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd",
                        Locale.US);
                Date firstAired = df.parse(series.firstAired);

                Log.i(TAG, String.format("Parsed first aired date: %s",
                        firstAired.toString()));
                show.setFirstAired(firstAired);

            } catch (ParseException e) {
                Log.w(TAG, "Error parsing first aired date: " + e.toString());
                show.setFirstAired(null);
            }
            show.setBannerPath(series.banner);
            show.setFanartPath(series.fanart);
            show.setPosterPath(series.poster);
        } catch (Exception e) {
	        Log.w(TAG, e);
            return null;
        }
        return show;
    }
}
