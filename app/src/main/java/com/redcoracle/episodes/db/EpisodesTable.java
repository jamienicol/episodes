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

import java.util.Arrays;

public class EpisodesTable {
    private static final String TAG = "EpisodesTable";

    static final String TABLE_NAME = "episodes";

    public static final String COLUMN_ID = BaseColumns._ID;
    public static final String COLUMN_TVDB_ID = "tvdb_id";
    public static final String COLUMN_TMDB_ID = "tmdb_id";
    public static final String COLUMN_IMDB_ID = "imdb_id";
    public static final String COLUMN_SHOW_ID = "show_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LANGUAGE = "language";
    public static final String COLUMN_OVERVIEW = "overview";
    public static final String COLUMN_EPISODE_NUMBER = "episode_number";
    public static final String COLUMN_SEASON_NUMBER = "season_number";
    public static final String COLUMN_FIRST_AIRED = "first_aired";
    public static final String COLUMN_WATCHED = "watched";

    public static String createTableSQL(String table_name) {
        return String.format(
                "CREATE TABLE %s ("  +
                "%s INTEGER PRIMARY KEY," +
                "%s INTEGER UNIQUE," +
                "%s INTEGER UNIQUE," +
                "%s TEXT UNIQUE," +
                "%s INTEGER NOT NULL," +
                "%s VARCHAR(200) NOT NULL," +
                "%s TEXT," +
                "%s TEXT," +
                "%s INTEGER," +
                "%s INTEGER," +
                "%s DATE," +
                "%s BOOLEAN" +
                ");",
                table_name,
                COLUMN_ID,
                COLUMN_TVDB_ID,
                COLUMN_TMDB_ID,
                COLUMN_IMDB_ID,
                COLUMN_SHOW_ID,
                COLUMN_NAME,
                COLUMN_LANGUAGE,
                COLUMN_OVERVIEW,
                COLUMN_EPISODE_NUMBER,
                COLUMN_SEASON_NUMBER,
                COLUMN_FIRST_AIRED,
                COLUMN_WATCHED
        );
    }

    public static void onCreate(SQLiteDatabase db) {
        String create = createTableSQL(TABLE_NAME);

        Log.d(TAG, String.format("creating episodes table: %s", create));

        db.execSQL(create);
    }

    static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 7) {
            // Add language column
            Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
            String[] columns = cursor.getColumnNames();
            if (!Arrays.asList(columns).contains(COLUMN_LANGUAGE)) {
                Log.d(TAG, "upgrading episodes table: adding language column");
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT", TABLE_NAME, COLUMN_LANGUAGE));
            }
            cursor.close();
        }

        if (oldVersion <= 8) {
            // Add TMDB/IMDB columns
            db.beginTransaction();
            try {
                final String temp_table_name = String.format("new_%s", TABLE_NAME);
                String create_table = createTableSQL(temp_table_name);
                String insert_columns = String.format(
                        "%s, %s, %s, %s, %s, %s, %s, %s, %s, %s",
                        COLUMN_ID, COLUMN_TVDB_ID, COLUMN_SHOW_ID, COLUMN_NAME, COLUMN_LANGUAGE,
                        COLUMN_OVERVIEW, COLUMN_EPISODE_NUMBER, COLUMN_SEASON_NUMBER,
                        COLUMN_FIRST_AIRED, COLUMN_WATCHED
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
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }
}
