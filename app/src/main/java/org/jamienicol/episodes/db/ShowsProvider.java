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

package org.jamienicol.episodes.db;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import org.jamienicol.episodes.BuildConfig;

public class ShowsProvider extends ContentProvider
{
	private static final String TAG = "ShowsProvider";

	private static final String URI_AUTHORITY =
		BuildConfig.APPLICATION_ID + ".db.ShowsProvider";

	private static final Uri CONTENT_URI_BASE =
		Uri.parse(ContentResolver.SCHEME_CONTENT +
		          "://" +
		          ShowsProvider.URI_AUTHORITY);

	public static final Uri CONTENT_URI_SHOWS =
		Uri.parse(ContentResolver.SCHEME_CONTENT +
		          "://" +
		          ShowsProvider.URI_AUTHORITY +
		          "/" +
		          ShowsTable.TABLE_NAME);

	public static final Uri CONTENT_URI_EPISODES =
		Uri.parse(ContentResolver.SCHEME_CONTENT +
		          "://" +
		          ShowsProvider.URI_AUTHORITY +
		          "/" +
		          EpisodesTable.TABLE_NAME);

	public static final String CONTENT_TYPE_SHOW_DIR =
		ContentResolver.CURSOR_DIR_BASE_TYPE + "/show";
	public static final String CONTENT_TYPE_SHOW_ITEM =
		ContentResolver.CURSOR_ITEM_BASE_TYPE + "/show";
	public static final String CONTENT_TYPE_EPISODE_DIR =
		ContentResolver.CURSOR_DIR_BASE_TYPE + "/episode";
	public static final String CONTENT_TYPE_EPISODE_ITEM =
		ContentResolver.CURSOR_ITEM_BASE_TYPE + "/episode";

	private static final int URI_TYPE_SHOWS = 1;
	private static final int URI_TYPE_SHOWS_ID = 2;
	private static final int URI_TYPE_EPISODES = 3;
	private static final int URI_TYPE_EPISODES_ID = 4;

	private static final UriMatcher uriMatcher =
		new UriMatcher(UriMatcher.NO_MATCH);
	static {
		uriMatcher.addURI(URI_AUTHORITY,
		                  ShowsTable.TABLE_NAME,
		                  URI_TYPE_SHOWS);
		uriMatcher.addURI(URI_AUTHORITY,
		                  ShowsTable.TABLE_NAME + "/#",
		                  URI_TYPE_SHOWS_ID);
		uriMatcher.addURI(URI_AUTHORITY,
		                  EpisodesTable.TABLE_NAME,
		                  URI_TYPE_EPISODES);
		uriMatcher.addURI(URI_AUTHORITY,
		                  EpisodesTable.TABLE_NAME + "/#",
		                  URI_TYPE_EPISODES_ID);
	}

	private DatabaseOpenHelper databaseOpenHelper;

	@Override
	public Cursor query(Uri uri,
	                    String[] projection,
	                    String selection,
	                    String[] selectionArgs,
	                    String sortOrder) {
		String table;
		String sel;

		switch (uriMatcher.match(uri)) {
		case URI_TYPE_SHOWS:
			table = ShowsTable.TABLE_NAME;
			sel = selection;
			break;

		case URI_TYPE_SHOWS_ID:
			table = ShowsTable.TABLE_NAME;
			sel = String.format("%s=%s",
			                    ShowsTable.COLUMN_ID,
			                    uri.getLastPathSegment());
			if (selection != null) {
				sel += " AND " + selection;
			}
			break;

		case URI_TYPE_EPISODES:
			table = EpisodesTable.TABLE_NAME;
			sel = selection;
			break;

		case URI_TYPE_EPISODES_ID:
			table = EpisodesTable.TABLE_NAME;
			sel = String.format("%s=%s",
			                    EpisodesTable.COLUMN_ID,
			                    uri.getLastPathSegment());
			if (selection != null) {
				sel += " AND " + selection;
			}
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = databaseOpenHelper.getReadableDatabase();
		Cursor cursor = db.query(table,
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

		String table;
		Uri contentUri;
		if (uriMatcher.match(uri) == URI_TYPE_SHOWS) {
			table = ShowsTable.TABLE_NAME;
			contentUri = CONTENT_URI_SHOWS;
		} else if (uriMatcher.match(uri) == URI_TYPE_EPISODES) {
			table = EpisodesTable.TABLE_NAME;
			contentUri = CONTENT_URI_EPISODES;
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		try {
			long rowId = db.insertOrThrow(table, null, values);
			Log.i(TAG, String.format("succesfully inserted row. id: %d",
			                         rowId));
			Uri rowUri = ContentUris.withAppendedId(contentUri,
			                                        rowId);
			getContext().getContentResolver().notifyChange(rowUri, null);

			return rowUri;
		} catch (SQLiteConstraintException e) {
			Log.i(TAG, String.format("constraint error inserting row: %s",
			                         e.toString()));
			return null;
		}
	}

	@Override
	public int delete(Uri uri,
	                  String selection,
	                  String[] selectionArgs) {
		String table;
		String sel;

		switch (uriMatcher.match(uri)) {
		case URI_TYPE_SHOWS:
			table = ShowsTable.TABLE_NAME;
			sel = selection;
			break;

		case URI_TYPE_SHOWS_ID:
			table = ShowsTable.TABLE_NAME;
			sel = String.format("%s=%s",
			                    ShowsTable.COLUMN_ID,
			                    uri.getLastPathSegment());
			if (selection != null) {
				sel += " AND " + selection;
			}

			break;

		case URI_TYPE_EPISODES:
			table = EpisodesTable.TABLE_NAME;
			sel = selection;
			break;

		case URI_TYPE_EPISODES_ID:
			table = EpisodesTable.TABLE_NAME;
			sel = String.format("%s=%s",
			                    EpisodesTable.COLUMN_ID,
			                    uri.getLastPathSegment());
			if (selection != null) {
				sel += " AND " + selection;
			}

			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		int count = db.delete(table,
		                      sel,
		                      selectionArgs);

		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	@Override
	public int update(Uri uri,
	                  ContentValues values,
	                  String selection,
	                  String[] selectionArgs) {
		String table;
		String sel;

		switch (uriMatcher.match(uri)) {
		case URI_TYPE_SHOWS:
			table = ShowsTable.TABLE_NAME;
			sel = selection;
			break;

		case URI_TYPE_SHOWS_ID:
			table = ShowsTable.TABLE_NAME;
			sel = String.format("%s=%s",
			                    ShowsTable.COLUMN_ID,
			                    uri.getLastPathSegment());
			if (selection != null) {
				sel += " AND " + selection;
			}
			break;

		case URI_TYPE_EPISODES:
			table = EpisodesTable.TABLE_NAME;
			sel = selection;
			break;

		case URI_TYPE_EPISODES_ID:
			table = EpisodesTable.TABLE_NAME;
			sel = String.format("%s=%s",
			                    EpisodesTable.COLUMN_ID,
			                    uri.getLastPathSegment());
			if (selection != null) {
				sel += " AND " + selection;
			}
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		int count = db.update(table,
		                      values,
		                      sel,
		                      selectionArgs);

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

		case URI_TYPE_EPISODES:
			return CONTENT_TYPE_EPISODE_DIR;

		case URI_TYPE_EPISODES_ID:
			return CONTENT_TYPE_EPISODE_ITEM;

		default:
			return null;
		}
	}

	@Override
	public boolean onCreate() {
		databaseOpenHelper = new DatabaseOpenHelper(getContext());

		return true;
	}

	public static void reloadDatabase(Context context) {
		final ContentResolver resolver = context.getContentResolver();
		final ContentProviderClient client =
			resolver.acquireContentProviderClient(URI_AUTHORITY);
		final ShowsProvider provider =
			(ShowsProvider)client.getLocalContentProvider();

		provider.databaseOpenHelper.close();
		provider.databaseOpenHelper =
			new DatabaseOpenHelper(provider.getContext());

		resolver.notifyChange(CONTENT_URI_BASE, null);

		client.release();
	}
}
