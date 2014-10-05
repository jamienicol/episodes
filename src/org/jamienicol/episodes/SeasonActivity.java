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

package org.jamienicol.episodes;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.db.ShowsTable;

public class SeasonActivity
	extends ActionBarActivity
	implements LoaderManager.LoaderCallbacks<Cursor>,
	           EpisodesListFragment.OnEpisodeSelectedListener
{
	private int showId;
	private int seasonNumber;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.season_activity);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final Intent intent = getIntent();
		showId = intent.getIntExtra("showId", -1);
		if (showId == -1) {
			throw new IllegalArgumentException("must provide valid showId");
		}
		seasonNumber = intent.getIntExtra("seasonNumber", -1);
		if (seasonNumber == -1) {
			throw new IllegalArgumentException("must provide valid seasonNumber");
		}

		final Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		getSupportLoaderManager().initLoader(0, loaderArgs, this);

		final ActionBar actionBar = getSupportActionBar();
		if (seasonNumber == 0) {
			actionBar.setSubtitle(getString(R.string.season_name_specials));
		} else {
			actionBar.setSubtitle(getString(R.string.season_name,
			                                seasonNumber));
		}

		// create and add episodes list fragment,
		// but only on the first time the activity is created
		if (savedInstanceState == null) {
			final EpisodesListFragment fragment =
				EpisodesListFragment.newInstance(showId, seasonNumber);
			final FragmentTransaction transaction =
				getSupportFragmentManager().beginTransaction();
			transaction.add(R.id.episodes_list_fragment_container, fragment);
			transaction.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.season_activity, menu);

		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final int showId = args.getInt("showId");
		final Uri uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
		                                     String.valueOf(showId));
		final String[] projection = {
			ShowsTable.COLUMN_NAME
		};
		return new CursorLoader(this,
		                        uri,
		                        projection,
		                        null,
		                        null,
		                        null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data != null && data.moveToFirst()) {
			// make activity title the show name
			final int nameColumnIndex =
				data.getColumnIndexOrThrow(ShowsTable.COLUMN_NAME);
			getSupportActionBar().setTitle(data.getString(nameColumnIndex));
		} else {
			getSupportActionBar().setTitle("");
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		onLoadFinished(loader, null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;

		case R.id.menu_mark_season_watched:
			markSeasonWatched(true);
			return true;

		case R.id.menu_mark_season_not_watched:
			markSeasonWatched(false);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onEpisodeSelected(int episodeId) {
		final Intent intent = new Intent(this,
		                                 EpisodeActivity.class);
		intent.putExtra("showId", showId);
		intent.putExtra("seasonNumber", seasonNumber);
		intent.putExtra("initialEpisodeId", episodeId);
		startActivity(intent);
	}

	private void markSeasonWatched(boolean watched) {
		final ContentResolver contentResolver = getContentResolver();
		final AsyncQueryHandler handler =
			new AsyncQueryHandler(contentResolver) {};
		final ContentValues epValues = new ContentValues();
		epValues.put(EpisodesTable.COLUMN_WATCHED, watched);
		final String selection =
			String.format("%s=? AND %s=?",
			              EpisodesTable.COLUMN_SHOW_ID,
			              EpisodesTable.COLUMN_SEASON_NUMBER);
		final String[] selectionArgs = {
			String.valueOf(showId),
			String.valueOf(seasonNumber)
		};

		handler.startUpdate(0,
		                    null,
		                    ShowsProvider.CONTENT_URI_EPISODES,
		                    epValues,
		                    selection,
		                    selectionArgs);
	}
}
