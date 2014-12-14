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

package org.jamienicol.episodes.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

public class EpisodesTable
{
	private static final String TAG = "EpisodesTable";

	public static final String TABLE_NAME = "episodes";

	public static final String COLUMN_ID = BaseColumns._ID;
	public static final String COLUMN_TVDB_ID = "tvdb_id";
	public static final String COLUMN_SHOW_ID = "show_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_OVERVIEW = "overview";
	public static final String COLUMN_EPISODE_NUMBER = "episode_number";
	public static final String COLUMN_SEASON_NUMBER = "season_number";
	public static final String COLUMN_FIRST_AIRED = "first_aired";
	public static final String COLUMN_WATCHED = "watched";

	public static void onCreate(SQLiteDatabase db) {
		String create =
			String.format("CREATE TABLE %s ("  +
			              "    %s INTEGER PRIMARY KEY," +
			              "    %s INTEGER UNIQUE NOT NULL," +
			              "    %s INTEGER NOT NULL," +
			              "    %s VARCHAR(200) NOT NULL," +
			              "    %s TEXT," +
			              "    %s INTEGER," +
			              "    %s INTEGER," +
			              "    %s DATE," +
			              "    %s BOOLEAN" +
			              ");",
			              TABLE_NAME,
			              COLUMN_ID,
			              COLUMN_TVDB_ID,
			              COLUMN_SHOW_ID,
			              COLUMN_NAME,
			              COLUMN_OVERVIEW,
			              COLUMN_EPISODE_NUMBER,
			              COLUMN_SEASON_NUMBER,
			              COLUMN_FIRST_AIRED,
			              COLUMN_WATCHED);

		Log.d(TAG, String.format("creating episodes table: %s", create));

		db.execSQL(create);
	}

	public static void onUpgrade(SQLiteDatabase db,
	                             int oldVersion,
	                             int newVersion) {
	}
}
