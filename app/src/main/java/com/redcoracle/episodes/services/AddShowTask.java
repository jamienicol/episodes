package com.redcoracle.episodes.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.redcoracle.episodes.EpisodesApplication;
import com.redcoracle.episodes.R;
import com.redcoracle.episodes.db.EpisodesTable;
import com.redcoracle.episodes.db.ShowsProvider;
import com.redcoracle.episodes.db.ShowsTable;
import com.redcoracle.episodes.tvdb.Client;
import com.redcoracle.episodes.tvdb.Episode;
import com.redcoracle.episodes.tvdb.Show;

import java.util.LinkedList;
import java.util.concurrent.Callable;

public class AddShowTask implements Callable<Void> {
    private static final String TAG = "AddShowTask";
    private final int tmdbId;
    private final String showName;
    private final String showLanguage;
    private final Context context;

    public AddShowTask(int tmdbId, String showName, String showLanguage) {
        this.tmdbId = tmdbId;
        this.showName = showName;
        this.showLanguage = showLanguage;
        this.context = EpisodesApplication.getInstance().getApplicationContext();
    }

    @Override
    public Void call() {
        final Client tmdbClient = new Client();
        Show show = tmdbClient.getShow(this.tmdbId, this.showLanguage, false);

        if (!checkAlreadyAdded(show)) {
            this.showMessage(this.context.getString(R.string.adding_show, showName));
            show = tmdbClient.getShow(this.tmdbId, this.showLanguage, true);
            final int showId = insertShow(show);
            this.insertEpisodes(show.getEpisodes().toArray(new Episode[0]), showId);
            showMessage(this.context.getString(R.string.show_added, showName));
        } else {
            showMessage(this.context.getString(R.string.show_already_added, showName));
        }
        return null;
    }

    private boolean checkAlreadyAdded(Show show) {
        final String[] projection = {};
        String selection = String.format("%s=?", ShowsTable.COLUMN_TMDB_ID);
        LinkedList<String> selectionArgs = new LinkedList<>();
        selectionArgs.add(Integer.valueOf(show.getTmdbId()).toString());

        if (show.getTvdbId() > 0) {
            selection += String.format(" OR %s=?", ShowsTable.COLUMN_TVDB_ID);
            selectionArgs.add(Integer.valueOf(show.getTvdbId()).toString());
        }
        if (show.getImdbId() != null && !show.getImdbId().equals("")) {
            selection += String.format(" OR %s=?", ShowsTable.COLUMN_IMDB_ID);
            selectionArgs.add(show.getImdbId());
        }
        final ContentResolver resolver = this.context.getContentResolver();
        final Cursor cursor = resolver.query(
            ShowsProvider.CONTENT_URI_SHOWS,
            projection,
            selection,
            selectionArgs.toArray(new String[0]),
            null
        );
        final boolean existing = cursor.moveToFirst();
        cursor.close();
        return existing;
    }

    private int insertShow(Show show){
        final ContentValues showValues = new ContentValues();
        if (show.getTvdbId() != 0) {
            showValues.put(ShowsTable.COLUMN_TVDB_ID, show.getTvdbId());
        }
        showValues.put(ShowsTable.COLUMN_TMDB_ID, show.getTmdbId());
        showValues.put(ShowsTable.COLUMN_IMDB_ID, show.getImdbId());
        showValues.put(ShowsTable.COLUMN_NAME, show.getName());
        showValues.put(ShowsTable.COLUMN_LANGUAGE, show.getLanguage());
        showValues.put(ShowsTable.COLUMN_OVERVIEW, show.getOverview());
        if (show.getFirstAired() != null) {
            showValues.put(ShowsTable.COLUMN_FIRST_AIRED, show.getFirstAired().getTime() / 1000);
        }
        showValues.put(ShowsTable.COLUMN_BANNER_PATH, show.getBannerPath());
        showValues.put(ShowsTable.COLUMN_FANART_PATH, show.getFanartPath());
        showValues.put(ShowsTable.COLUMN_POSTER_PATH, show.getPosterPath());

        final Uri showUri = this.context.getContentResolver().insert(ShowsProvider.CONTENT_URI_SHOWS, showValues);
        final int showId = Integer.parseInt(showUri.getLastPathSegment());
        Log.i(TAG, String.format("show %s successfully added to database as row %d. adding episodes", show.getName(), showId));
        return showId;
    }

    private void insertEpisodes(Episode[] episodes, int showId) {
        final ContentValues[] values = new ContentValues[episodes.length];

        for (int i = 0; i < episodes.length; i++) {
            ContentValues value = new ContentValues();
            value.put(EpisodesTable.COLUMN_TVDB_ID, episodes[i].getTvdbId());
            value.put(EpisodesTable.COLUMN_TMDB_ID, episodes[i].getTmdbId());
            value.put(EpisodesTable.COLUMN_IMDB_ID, episodes[i].getImdbId());
            value.put(EpisodesTable.COLUMN_SHOW_ID, showId);
            value.put(EpisodesTable.COLUMN_NAME, episodes[i].getName());
            value.put(EpisodesTable.COLUMN_LANGUAGE, episodes[i].getLanguage());
            value.put(EpisodesTable.COLUMN_OVERVIEW, episodes[i].getOverview());
            value.put(EpisodesTable.COLUMN_EPISODE_NUMBER, episodes[i].getEpisodeNumber());
            value.put(EpisodesTable.COLUMN_SEASON_NUMBER, episodes[i].getSeasonNumber());
            if (episodes[i].getFirstAired() != null) {
                value.put(EpisodesTable.COLUMN_FIRST_AIRED, episodes[i].getFirstAired().getTime() / 1000);
            }
            values[i] = value;
        }

        for (ContentValues value : values) {
            this.context.getContentResolver().insert(ShowsProvider.CONTENT_URI_EPISODES, value);
        }
    }

    private void showMessage(String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}
