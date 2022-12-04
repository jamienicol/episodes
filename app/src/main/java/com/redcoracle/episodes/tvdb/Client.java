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

import com.redcoracle.episodes.EpisodesApplication;
import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.AppendToResponse;
import com.uwetrottmann.tmdb2.entities.BaseTvShow;
import com.uwetrottmann.tmdb2.entities.FindResults;
import com.uwetrottmann.tmdb2.entities.TvSeason;
import com.uwetrottmann.tmdb2.entities.TvShow;
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage;
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem;
import com.uwetrottmann.tmdb2.enumerations.ExternalSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import retrofit2.Response;

public class Client {
    private static final String TAG = Client.class.getName();
    private final Tmdb tmdb;

    public Client() {
        this.tmdb = EpisodesApplication.getInstance().getTmdbClient();
    }

    public List<Show> searchShows(String query, String language) {
        this.tmdb.searchService().tv(query, null, language, null, false);

        try {
            final TvShowResultsPage results = this.tmdb
                    .searchService()
                    .tv(query, null, language, null, false)
                    .execute()
                    .body();
            if (results != null) {
                final SearchShowsParser parser = new SearchShowsParser();
                return parser.parse(results, language);
            } else {
                return new LinkedList<>();
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            return new LinkedList<>();
        }
    }

    public Show getShow(HashMap<String, String> showIds, String language) {
        Show show = null;
        try {
            TvShow lookupResult = null;
            AppendToResponse includes = new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS);

            if (showIds.get("tmdbId") != null) {
                int tmdbId = Integer.parseInt(showIds.get("tmdbId"));
                Response<TvShow> seriesResponse = this.tmdb.tvService().tv(tmdbId, language, includes).execute();
                if (seriesResponse.isSuccessful() && seriesResponse.body() != null) {
                    lookupResult = seriesResponse.body();
                }
            }

            if (lookupResult == null && showIds.get("tvdbId") != null) {
                Response<FindResults> seriesResponse = this.tmdb.findService().find(
                        showIds.get("tvdbId"),
                        ExternalSource.TVDB_ID,
                        language
                ).execute();
                if (seriesResponse.isSuccessful() && seriesResponse.body() != null) {
                    if (seriesResponse.body().tv_results != null && seriesResponse.body().tv_results.size() > 0) {
                        BaseTvShow sparseShow = seriesResponse.body().tv_results.get(0);
                        lookupResult = tmdb.tvService().tv(sparseShow.id, language, includes).execute().body();
                    }
                }
            }

            if (lookupResult == null && showIds.get("imbId") != null) {
                Response<FindResults> seriesResponse = this.tmdb.findService().find(
                        showIds.get("imbId"),
                        ExternalSource.IMDB_ID,
                        language
                ).execute();
                if (seriesResponse.isSuccessful() && seriesResponse.body() != null) {
                    if (seriesResponse.body().tv_results != null && seriesResponse.body().tv_results.size() > 0) {
                        BaseTvShow sparseShow = seriesResponse.body().tv_results.get(0);
                        lookupResult = tmdb.tvService().tv(sparseShow.id, language, includes).execute().body();
                    }
                }
            }

            if (lookupResult != null) {
                final GetShowParser parser = new GetShowParser();
                show = parser.parse(lookupResult, language);
                show.setEpisodes(getEpisodesForShow(lookupResult, language));
            }

        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return show;
    }

    public Show getShow(int id, String language, boolean includeEpisodes) {
        try {
            AppendToResponse includes = new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS);
            Response<TvShow> seriesResponse = this.tmdb.tvService().tv(id, language, includes).execute();
            Log.d(TAG, String.format("Received response %d: %s", seriesResponse.code(), seriesResponse.message()));
            if (seriesResponse.isSuccessful() && seriesResponse.body() != null) {
                final GetShowParser parser = new GetShowParser();
                final TvShow series = seriesResponse.body();
                Show show = parser.parse(series, language);

                if (show != null && includeEpisodes) {
                    ArrayList<Episode> episodes = getEpisodesForShow(series, language);
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

    public ArrayList<Episode> getEpisodesForShow(TvShow series, String language) {
        int episode_count = series.number_of_episodes != null ? series.number_of_episodes : 64;
        ArrayList<Episode> episodes = new ArrayList<>(episode_count);
        final GetEpisodesParser episodesParser = new GetEpisodesParser();
        if (series.number_of_seasons != null) {
            for (TvSeason season : series.seasons) {
                try {
                    AppendToResponse includes = new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS);
                    season = this.tmdb.tvSeasonsService().season(series.id, season.season_number, language, includes).execute().body();
                    if (season != null) {
                        episodes.addAll(episodesParser.parse(season.episodes));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return episodes;
    }
}
