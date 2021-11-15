package com.redcoracle.episodes.services;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.redcoracle.episodes.EpisodesApplication;
import com.redcoracle.episodes.R;
import com.redcoracle.episodes.db.DatabaseOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;

public class BackupTask implements Callable<Void> {
    private final static String TAG = BackupTask.class.getName();
    private final Context context;
    private final String destinationFileName;

    public BackupTask(String destinationFileName) {
        this.destinationFileName = destinationFileName;
        this.context = EpisodesApplication.getInstance().getApplicationContext();
    }

    public Void call() {
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
        final File destinationFile = new File(destinationDirectory, this.destinationFileName);
        try {
            FileChannel src = new FileInputStream(databaseFile).getChannel();
            FileChannel dest = new FileOutputStream(destinationFile).getChannel();
            dest.transferFrom(src, 0, src.size());
            ContextCompat.getMainExecutor(this.context).execute(() -> Toast.makeText(
                this.context,
                String.format(this.context.getString(R.string.back_up_success_message), this.destinationFileName),
                Toast.LENGTH_LONG
            ).show());
            Log.i(TAG, String.format("Library backed up successfully: '%s'.", destinationFile.getPath()));
            src.close();
            dest.close();
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
