/*
 * Copyright (C) 2012 Jamie Nicol <jamie@thenicols.net>
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

package org.jamienicol.nextepisode.db;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class ShowsProvider extends ContentProvider
{
	private static final String URI_AUTHORITY =
		"org.jamienicol.nextepisode.db.ShowsProvider";

	public static final Uri CONTENT_URI_SHOWS =
		Uri.parse(ContentResolver.SCHEME_CONTENT +
		          "://" +
		          ShowsProvider.URI_AUTHORITY +
		          "/" +
		          ShowsTable.TABLE_NAME);

	public static final String CONTENT_TYPE_SHOW_DIR =
		ContentResolver.CURSOR_DIR_BASE_TYPE + "/show";
	public static final String CONTENT_TYPE_SHOW_ITEM =
		ContentResolver.CURSOR_ITEM_BASE_TYPE + "/show";

	private static final int URI_TYPE_SHOWS = 1;
	private static final int URI_TYPE_SHOWS_ID = 2;

	private static final UriMatcher uriMatcher =
		new UriMatcher(UriMatcher.NO_MATCH);
	static {
		uriMatcher.addURI(URI_AUTHORITY,
		                  ShowsTable.TABLE_NAME,
		                  URI_TYPE_SHOWS);
		uriMatcher.addURI(URI_AUTHORITY,
		                  ShowsTable.TABLE_NAME + "/#",
		                  URI_TYPE_SHOWS_ID);
	}

	private DatabaseOpenHelper databaseOpenHelper;

	@Override
	public Cursor query(Uri uri,
	                    String[] projection,
	                    String selection,
	                    String[] selectionArgs,
	                    String sortOrder) {
		String sel;

		switch (uriMatcher.match(uri)) {
		case URI_TYPE_SHOWS:
			sel = selection;
			break;

		case URI_TYPE_SHOWS_ID:
			sel = String.format("%s=%s",
			                    ShowsTable.COLUMN_ID,
			                    uri.getLastPathSegment());
			if (selection != null) {
				sel += " AND " + selection;
			}
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = databaseOpenHelper.getReadableDatabase();
		Cursor cursor = db.query(ShowsTable.TABLE_NAME,
		                         projection,
		                         sel,
		                         selectionArgs,
		                         null,
		                         null,
		                         sortOrder,
		                         null);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		if (uriMatcher.match(uri) != URI_TYPE_SHOWS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		long rowId = db.insert(ShowsTable.TABLE_NAME,
		                       null,
		                       values);

		if (rowId > 0) {
			Uri showUri = ContentUris.withAppendedId(CONTENT_URI_SHOWS,
			                                         rowId);
			getContext().getContentResolver().notifyChange(showUri, null);

			return showUri;
		} else {
			throw new SQLException("Failed to insert row into " + uri);
		}
	}

	@Override
	public int delete(Uri uri,
	                  String selection,
	                  String[] selectionArgs) {
		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		int count;

		switch (uriMatcher.match(uri)) {
		case URI_TYPE_SHOWS:
			count = db.delete(ShowsTable.TABLE_NAME,
			                  selection,
			                  selectionArgs);
			break;

		case URI_TYPE_SHOWS_ID:
			String sel = String.format("%s=%s",
			                           ShowsTable.COLUMN_ID,
			                           uri.getLastPathSegment());
			if (selection != null) {
				sel += " AND " + selection;
			}

			count = db.delete(ShowsTable.TABLE_NAME,
			                  sel,
			                  selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	@Override
	public int update(Uri uri,
	                  ContentValues values,
	                  String selection,
	                  String[] selectionArgs) {
		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		int count;

		switch (uriMatcher.match(uri)) {
		case URI_TYPE_SHOWS:
			count = db.update(ShowsTable.TABLE_NAME,
			                  values,
			                  selection,
			                  selectionArgs);
			break;

		case URI_TYPE_SHOWS_ID:
			String sel = String.format("%s=%s",
			                           ShowsTable.COLUMN_ID,
			                           uri.getLastPathSegment());
			if (selection != null) {
				sel += " AND " + selection;
			}

			count = db.update(ShowsTable.TABLE_NAME,
			                  values,
			                  sel,
			                  selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case URI_TYPE_SHOWS:
			return CONTENT_TYPE_SHOW_DIR;

		case URI_TYPE_SHOWS_ID:
			return CONTENT_TYPE_SHOW_ITEM;

		default:
			return null;
		}
	}

	@Override
	public boolean onCreate() {
		databaseOpenHelper = new DatabaseOpenHelper(getContext());

		return true;
	}
}
