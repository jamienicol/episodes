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

package com.redcoracle.episodes.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

public class ShowsTable {
    private static final String TAG = "ShowsTable";

    public static final String TABLE_NAME = "shows";

    public static final String COLUMN_ID = BaseColumns._ID;
    public static final String COLUMN_TVDB_ID = "tvdb_id";
    public static final String COLUMN_TMDB_ID = "tmdb_id";
    public static final String COLUMN_IMDB_ID = "imdb_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LANGUAGE = "language";
    public static final String COLUMN_OVERVIEW = "overview";
    public static final String COLUMN_FIRST_AIRED = "first_aired";
    public static final String COLUMN_STARRED = "starred";
    public static final String COLUMN_ARCHIVED = "archived";
    public static final String COLUMN_BANNER_PATH = "banner_path";
    public static final String COLUMN_FANART_PATH = "fanart_path";
    public static final String COLUMN_POSTER_PATH = "poster_path";
    public static final String COLUMN_NOTES = "notes";

    public static final String COLUMN_TYPE_ID = "INTEGER PRIMARY KEY";
    public static final String COLUMN_TYPE_TVDB_ID = "INTEGER UNIQUE";
    public static final String COLUMN_TYPE_TMDB_ID = "INTEGER UNIQUE";
    public static final String COLUMN_TYPE_IMDB_ID = "STRING UNIQUE";
    public static final String COLUMN_TYPE_NAME = "TEXT NOT NULL";
    public static final String COLUMN_TYPE_LANGUAGE = "TEXT";
    public static final String COLUMN_TYPE_OVERVIEW = "TEXT";
    public static final String COLUMN_TYPE_FIRST_AIRED = "DATE";
    public static final String COLUMN_TYPE_STARRED = "BOOLEAN DEFAULT 0";
    public static final String COLUMN_TYPE_ARCHIVED = "BOOLEAN DEFAULT 0";
    public static final String COLUMN_TYPE_BANNER_PATH = "TEXT";
    public static final String COLUMN_TYPE_FANART_PATH = "TEXT";
    public static final String COLUMN_TYPE_POSTER_PATH = "TEXT";
    public static final String COLUMN_TYPE_NOTES = "TEXT";

    public static String createTableSQL(String table_name) {
        return String.format(
            "CREATE TABLE %s ("  +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s," +
            "    %s %s" +
            ");",
            table_name,
            COLUMN_ID, COLUMN_TYPE_ID,
            COLUMN_TVDB_ID, COLUMN_TYPE_TVDB_ID,
            COLUMN_TMDB_ID, COLUMN_TYPE_TMDB_ID,
            COLUMN_IMDB_ID, COLUMN_TYPE_IMDB_ID,
            COLUMN_NAME, COLUMN_TYPE_NAME,
            COLUMN_LANGUAGE, COLUMN_TYPE_LANGUAGE,
            COLUMN_OVERVIEW, COLUMN_TYPE_OVERVIEW,
            COLUMN_FIRST_AIRED, COLUMN_TYPE_FIRST_AIRED,
            COLUMN_STARRED, COLUMN_TYPE_STARRED,
            COLUMN_ARCHIVED, COLUMN_TYPE_ARCHIVED,
            COLUMN_BANNER_PATH, COLUMN_TYPE_BANNER_PATH,
            COLUMN_FANART_PATH, COLUMN_TYPE_FANART_PATH,
            COLUMN_POSTER_PATH, COLUMN_TYPE_POSTER_PATH,
            COLUMN_NOTES, COLUMN_TYPE_NOTES
        );
    }

    public static void onCreate(SQLiteDatabase db) {
        String create = createTableSQL(TABLE_NAME);
        Log.d(TAG, String.format("creating shows table: %s", create));
        db.execSQL(create);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add starred column
            Log.d(TAG, "upgrading shows table: adding starred column");
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s %s",
                    TABLE_NAME,
                    COLUMN_STARRED,
                    COLUMN_TYPE_STARRED));
        }

        if (oldVersion < 3) {
            // Add banner path column
            Log.d(TAG, "upgrading shows table: adding banner path column");
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s %s",
                    TABLE_NAME,
                    COLUMN_BANNER_PATH,
                    COLUMN_TYPE_BANNER_PATH));
        }

        if (oldVersion < 4) {
            // Add fanart path and poster path columns
            Log.d(TAG, "upgrading shows table: adding fanart path column");
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s %s",
                    TABLE_NAME,
                    COLUMN_FANART_PATH,
                    COLUMN_TYPE_FANART_PATH));

            Log.d(TAG, "upgrading shows table: adding poster path column");
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s %s",
                    TABLE_NAME,
                    COLUMN_POSTER_PATH,
                    COLUMN_TYPE_POSTER_PATH));
        }

        if (oldVersion < 5) {
            // Add notes column
            Log.d(TAG, "upgrading shows table: adding notes column");
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s %s",
                    TABLE_NAME,
                    COLUMN_NOTES,
                    COLUMN_TYPE_NOTES));
        }

        if (oldVersion < 6) {
            // Add language column
            Log.d(TAG, "upgrading shows table: adding language column");
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s %s",
                    TABLE_NAME,
                    COLUMN_LANGUAGE,
                    COLUMN_TYPE_LANGUAGE));
        }

        if (oldVersion < 7) {
            // Add archived column
            Log.d(TAG, "upgrading shows table: adding archived column");
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s %s",
                    TABLE_NAME,
                    COLUMN_ARCHIVED,
                    COLUMN_TYPE_ARCHIVED));
        }

        if (oldVersion < 9) {
            // Add TMDB/IMDB columns
            db.beginTransaction();
            try {
                final String temp_table_name = String.format("new_%s", TABLE_NAME);

                String create_table = createTableSQL(temp_table_name);
                String insert_columns = String.format(
                        "%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s",
                        COLUMN_ID, COLUMN_TVDB_ID, COLUMN_NAME, COLUMN_LANGUAGE, COLUMN_OVERVIEW,
                        COLUMN_FIRST_AIRED, COLUMN_STARRED, COLUMN_ARCHIVED, COLUMN_BANNER_PATH,
                        COLUMN_FANART_PATH, COLUMN_POSTER_PATH, COLUMN_NOTES
                );

                db.execSQL(create_table);
                db.execSQL(String.format(
                        "INSERT INTO %s (%s) SELECT %s FROM %s",
                        temp_table_name, insert_columns, insert_columns, TABLE_NAME
                ));

                db.execSQL(String.format("DROP TABLE %s", TABLE_NAME));
                db.execSQL(String.format(
                        "ALTER TABLE %s RENAME TO %s",
                        temp_table_name, TABLE_NAME
                ));

                final Cursor fk_query = db.rawQuery("PRAGMA foreign_key_check", null);
                final int fk_check = fk_query.getCount();
                fk_query.close();
                if (fk_check == 0) {
                    db.setTransactionSuccessful();
                }
            } finally {
                db.endTransaction();
            }
        }
    }
}
