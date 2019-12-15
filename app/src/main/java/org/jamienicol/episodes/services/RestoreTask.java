package org.jamienicol.episodes.services;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.bumptech.glide.Glide;

import org.jamienicol.episodes.EpisodesApplication;
import org.jamienicol.episodes.db.DatabaseOpenHelper;
import org.jamienicol.episodes.db.ShowsProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;

public class RestoreTask implements Callable<Void> {
    private final static String TAG = RestoreTask.class.getName();
    private final Context context;
    private final String filename;


    public RestoreTask(String filename) {
        this.context = EpisodesApplication.getInstance().getApplicationContext();
        this.filename = filename;
    }

    public Void call() {
        if (!isExternalStorageWritable()) {
            return null;
        }
        final File backupFile = new File(this.filename);
        final File databaseFile = this.context.getDatabasePath(DatabaseOpenHelper.getDbName());
        try {
            FileChannel src = new FileInputStream(backupFile).getChannel();
            FileChannel dest = new FileOutputStream(databaseFile).getChannel();
            dest.transferFrom(src, 0, src.size());
            Glide.get(this.context).clearDiskCache();
            Log.i(TAG, "Library restored successfully.");
        } catch (IOException e) {
            Log.e(TAG, String.format("Error restoring library: %s", e.toString()));
        } finally {
            ShowsProvider.reloadDatabase(this.context);
        }
        return null;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
