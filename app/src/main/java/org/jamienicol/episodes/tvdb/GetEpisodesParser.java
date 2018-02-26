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

import com.uwetrottmann.thetvdb.entities.EpisodesResponse;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

class GetEpisodesParser
{
    private static final String TAG = "GetShowParser";

    // TODO: Cleanup the variable names.
    ArrayList<Episode> parse(EpisodesResponse episodesResponse) {
        try {
            ArrayList<Episode> episodes = new ArrayList<>();

            for (com.uwetrottmann.thetvdb.entities.Episode episode : episodesResponse.data) {
                Episode e = new Episode();
                e.setId(episode.id);
                e.setName(episode.episodeName);
                e.setOverview(episode.overview);
                e.setEpisodeNumber(episode.airedEpisodeNumber);
                e.setSeasonNumber(episode.airedSeason);
                try {
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date firstAired = df.parse(episode.firstAired);
                    Log.i(TAG, String.format("Parsed first aired date: %s", firstAired.toString()));
                    e.setFirstAired(firstAired);

                } catch (ParseException ex) {
                    Log.w(TAG, "Error parsing first aired date: " + ex.toString());
                    e.setFirstAired(null);
                }
                episodes.add(e);
            }
            return episodes;
        } catch (Exception ex) {
            Log.w(TAG, ex);
            return null;
        }
    }
}
