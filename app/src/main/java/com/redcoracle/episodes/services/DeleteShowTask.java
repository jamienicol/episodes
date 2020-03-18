package com.redcoracle.episodes.services;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.redcoracle.episodes.EpisodesApplication;
import com.redcoracle.episodes.db.EpisodesTable;
import com.redcoracle.episodes.db.ShowsProvider;

import java.util.concurrent.Callable;

public class DeleteShowTask implements Callable<Void> {
    private static final String TAG = DeleteShowTask.class.getName();
    private int showId;
    private Context context;

    public DeleteShowTask(int showId) {
        this.showId = showId;
        this.context = EpisodesApplication.getInstance().getApplicationContext();
    }

    @Override
    public Void call() {
        final ContentResolver resolver = this.context.getContentResolver();
        final String selection = String.format("%s=?", EpisodesTable.COLUMN_SHOW_ID);
        final String[] selectionArgs = {String.valueOf(this.showId)};
        int episodes = resolver.delete(ShowsProvider.CONTENT_URI_EPISODES, selection, selectionArgs);
        Log.d(TAG, String.format("Deleted %s episodes", episodes));
        resolver.delete(Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS, String.valueOf(showId)), null, null);
        return null;
    }
}
