package com.redcoracle.episodes;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtilities {
    public static String get_suggested_filename() {
        final Date today = new Date();
        final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault());
        return String.format("episodes_%s.db", formatter.format(today));
    }

    public static String uri_to_filename(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String filename = cursor.getString(nameIndex);
        cursor.close();
        return filename;
    }

    public static void copy_file(FileChannel source, FileChannel destination) {
        try {
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
