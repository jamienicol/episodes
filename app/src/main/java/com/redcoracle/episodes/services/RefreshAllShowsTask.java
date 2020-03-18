package com.redcoracle.episodes.services;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.redcoracle.episodes.EpisodesApplication;
import com.redcoracle.episodes.R;
import com.redcoracle.episodes.db.ShowsProvider;
import com.redcoracle.episodes.db.ShowsTable;

import java.util.concurrent.Callable;

import static com.redcoracle.episodes.RefreshShowUtil.refreshShow;

public class RefreshAllShowsTask implements Callable<Void> {
    @Override
    public Void call() {
        Context context = EpisodesApplication.getInstance().getApplicationContext();
        ContentResolver resolver = context.getContentResolver();
        final Uri showUri = ShowsProvider.CONTENT_URI_SHOWS;
        final String[] projection = {
                ShowsTable.COLUMN_ID,
                ShowsTable.COLUMN_NAME
        };
        final String sort = ShowsTable.COLUMN_NAME + " ASC";
        final Cursor cursor = resolver.query(showUri, projection, null, null, sort);
        final int idColumnIndex = cursor.getColumnIndex(ShowsTable.COLUMN_ID);
        final int nameColumnIndex = cursor.getColumnIndex(ShowsTable.COLUMN_NAME);
        final int total = cursor.getCount();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "episodes_channel_id");
        notificationBuilder
                .setContentTitle("Refreshing Shows")
                .setSmallIcon(R.drawable.ic_show_starred)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        int current = 0;
        notificationBuilder.setProgress(total, current, false);
        notificationManager.notify(0, notificationBuilder.build());

        int showId;
        String showName;
        cursor.moveToFirst();
        do {
            showId = cursor.getInt(idColumnIndex);
            showName = cursor.getString(nameColumnIndex);
            notificationBuilder.setContentText(showName);
            notificationBuilder.setProgress(total, current, false);
            notificationManager.notify(0, notificationBuilder.build());
            refreshShow(showId, resolver);
            current += 1;
        } while (cursor.moveToNext());
        cursor.close();
        notificationBuilder.setContentText("Refresh complete!").setProgress(0, 0, false);
        notificationManager.notify(0, notificationBuilder.build());
        return null;
    }
}
