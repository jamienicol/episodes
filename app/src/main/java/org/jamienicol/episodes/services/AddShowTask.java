package org.jamienicol.episodes.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.jamienicol.episodes.EpisodesApplication;
import org.jamienicol.episodes.R;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.db.ShowsTable;
import org.jamienicol.episodes.tvdb.Client;
import org.jamienicol.episodes.tvdb.Episode;
import org.jamienicol.episodes.tvdb.Show;

import java.util.concurrent.Callable;

public class AddShowTask implements Callable<Void> {
    private static final String TAG = "AddShowTask";
    private final int tvdbId;
    private final String showName;
    private final String showLanguage;
    private final Context context;

    public AddShowTask(int tvdbId, String showName, String showLanguage) {
        this.tvdbId = tvdbId;
        this.showName = showName;
        this.showLanguage = showLanguage;
        this.context = EpisodesApplication.getInstance().getApplicationContext();
    }

    @Override
    public Void call() {
        if (!checkAlreadyAdded()) {
            this.showMessage(this.context.getString(R.string.adding_show, showName));
            final Client tvdbClient = new Client();
            final Show show = tvdbClient.getShow(this.tvdbId, this.showLanguage);
            if (show != null) {
                final int showId = insertShow(show);
                this.insertEpisodes(show.getEpisodes().toArray(new Episode[0]), showId);
                showMessage(this.context.getString(R.string.show_added, showName));
            } else {
                showMessage(this.context.getString(R.string.error_adding_show, showName));
            }
        } else {
            showMessage(this.context.getString(R.string.show_already_added, showName));
        }
        return null;
    }

    private boolean checkAlreadyAdded() {
        final String[] projection = {};
        final String selection = String.format("%s=?", ShowsTable.COLUMN_TVDB_ID);
        final String[] selectionArgs = {Integer.valueOf(this.tvdbId).toString()};
        final ContentResolver resolver = this.context.getContentResolver();
        final Cursor cursor = resolver.query(ShowsProvider.CONTENT_URI_SHOWS, projection, selection, selectionArgs,null);
        final boolean existing = cursor.moveToFirst();
        cursor.close();
        return existing;
    }

    private int insertShow(Show show){
        final ContentValues showValues = new ContentValues();
        showValues.put(ShowsTable.COLUMN_TVDB_ID, show.getId());
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
            value.put(EpisodesTable.COLUMN_TVDB_ID, episodes[i].getId());
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

        this.context.getContentResolver().bulkInsert(ShowsProvider.CONTENT_URI_EPISODES, values);
    }

    private void showMessage(String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}
