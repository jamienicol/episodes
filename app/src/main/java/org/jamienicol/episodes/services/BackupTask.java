package org.jamienicol.episodes.services;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.jamienicol.episodes.EpisodesApplication;
import org.jamienicol.episodes.db.DatabaseOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;

public class BackupTask implements Callable<Void> {
    private final static String TAG = BackupTask.class.getName();
    private final Context context;

    public BackupTask() {
        this.context = EpisodesApplication.getInstance().getApplicationContext();
    }

    public Void call(){
        Log.i(TAG, "Backing up library.");
        if (!isExternalStorageReadable()) {
            Log.i(TAG, "Storage is not readable.");
            return null;
        }
        final File databaseFile = this.context.getDatabasePath(DatabaseOpenHelper.getDbName());
        final File destinationDirectory = new File(this.context.getExternalFilesDir(null), "episodes");
        if (!destinationDirectory.mkdirs()) {
            Log.e(TAG, String.format("Error creating backup directory '%s'.", destinationDirectory.getPath()));
        }
        final Date today = new Date();
        final DateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault());
        final String destinationFileName = "episodes_" + formatter.format(today) + ".db";
        final File destinationFile = new File(destinationDirectory, destinationFileName);
        try {
            FileChannel src = new FileInputStream(databaseFile).getChannel();
            FileChannel dest = new FileOutputStream(destinationFile).getChannel();
            dest.transferFrom(src, 0, src.size());
            Log.i(TAG, String.format("Library backed up successfully: '%s'.", destinationFile.getPath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
