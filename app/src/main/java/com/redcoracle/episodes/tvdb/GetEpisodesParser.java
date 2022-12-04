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

import com.uwetrottmann.tmdb2.entities.TvEpisode;

import java.util.ArrayList;
import java.util.List;

class GetEpisodesParser {
    private static final String TAG = GetEpisodesParser.class.getName();

    ArrayList<Episode> parse(List<TvEpisode> tmdbEpisodes) {
        try {
            ArrayList<Episode> episodes = new ArrayList<>(tmdbEpisodes.size());
            for (TvEpisode episode : tmdbEpisodes) {
                Episode e = new Episode();
                e.setId(episode.id);
                e.setTmdbId(episode.id);
                if (episode.external_ids != null) {
                    if (episode.external_ids.tvdb_id != null) {
                        e.setTvdbId(episode.external_ids.tvdb_id);
                    }
                    if (episode.external_ids.imdb_id != null) {
                        e.setImdbId(episode.external_ids.imdb_id);
                    }
                }
                e.setName(episode.name != null ? episode.name : "");
                e.setOverview(episode.overview);
                e.setSeasonNumber(episode.season_number);
                e.setEpisodeNumber(episode.episode_number);
                e.setFirstAired(episode.air_date);
                episodes.add(e);
            }
            return episodes;
        } catch (Exception ex) {
            Log.w(TAG, ex);
            return null;
        }
    }
}
