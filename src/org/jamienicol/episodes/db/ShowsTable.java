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

public class ShowsTable
{
	public static final String TABLE_NAME = "shows";

	public static final String COLUMN_ID = BaseColumns._ID;
	public static final String COLUMN_TVDB_ID = "tvdb_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_OVERVIEW = "overview";
	public static final String COLUMN_FIRST_AIRED = "first_aired";
	public static final String COLUMN_STARRED = "starred";
	public static final String COLUMN_BANNER_PATH = "banner_path";

	public static final String COLUMN_TYPE_ID = "INTEGER PRIMARY KEY";
	public static final String COLUMN_TYPE_TVDB_ID = "INTEGER UNIQUE NOT NULL";
	public static final String COLUMN_TYPE_NAME = "TEXT NOT NULL";
	public static final String COLUMN_TYPE_OVERVIEW = "TEXT";
	public static final String COLUMN_TYPE_FIRST_AIRED = "DATE";
	public static final String COLUMN_TYPE_STARRED = "BOOLEAN DEFAULT 0";
	public static final String COLUMN_TYPE_BANNER_PATH = "TEXT";

	public static void onCreate(SQLiteDatabase db) {
		String create =
			String.format("CREATE TABLE %s ("  +
			              "    %s %s," +
			              "    %s %s," +
			              "    %s %s," +
			              "    %s %s," +
			              "    %s %s," +
			              "    %s %s," +
			              "    %s %s" +
			              ");",
			              TABLE_NAME,
			              COLUMN_ID, COLUMN_TYPE_ID,
			              COLUMN_TVDB_ID, COLUMN_TYPE_TVDB_ID,
			              COLUMN_NAME, COLUMN_TYPE_NAME,
			              COLUMN_OVERVIEW, COLUMN_TYPE_OVERVIEW,
			              COLUMN_FIRST_AIRED, COLUMN_TYPE_FIRST_AIRED,
			              COLUMN_STARRED, COLUMN_TYPE_STARRED,
			              COLUMN_BANNER_PATH, COLUMN_TYPE_BANNER_PATH);
		db.execSQL(create);
	}

	public static void onUpgrade(SQLiteDatabase db,
	                             int oldVersion,
	                             int newVersion) {
		switch (oldVersion) {
		case 1:
			// Add starred column
			db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s %s",
			                         TABLE_NAME,
			                         COLUMN_STARRED,
			                         COLUMN_TYPE_STARRED));

			// fall through
		case 2:
			// Add banner path column
			db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s %s",
			                         TABLE_NAME,
			                         COLUMN_BANNER_PATH,
			                         COLUMN_TYPE_BANNER_PATH));

			// fall through
		}
	}
}
