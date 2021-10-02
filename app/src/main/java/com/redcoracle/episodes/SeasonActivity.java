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

package com.redcoracle.episodes;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.redcoracle.episodes.db.EpisodesTable;
import com.redcoracle.episodes.db.ShowsProvider;
import com.redcoracle.episodes.db.ShowsTable;

import java.util.ArrayList;
import java.util.Date;

public class SeasonActivity
	extends AppCompatActivity
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
		LoaderManager.getInstance(this).initLoader(0, loaderArgs, this);

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
		int item_id = item.getItemId();
		if (item_id == R.id.home) {
			finish();
			return true;
		} else if (item_id == R.id.menu_mark_season_watched) {
			markSeasonWatched(true);
			return true;
		} else if (item_id == R.id.menu_mark_season_not_watched) {
			markSeasonWatched(false);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onEpisodeSelected(int episodeId) {
		final Intent intent = new Intent(this, EpisodeActivity.class);
		intent.putExtra("showId", showId);
		intent.putExtra("seasonNumber", seasonNumber);
		intent.putExtra("initialEpisodeId", episodeId);
		startActivity(intent);
	}

	private void markSeasonWatched(boolean watched) {
		final ContentResolver contentResolver = getContentResolver();
		final AsyncQueryHandler handler = new AsyncQueryHandler(contentResolver) {};
		final ContentValues epValues = new ContentValues();
		final Date now = new Date();
		epValues.put(EpisodesTable.COLUMN_WATCHED, watched);
		String selection = String.format(
			"%s=? AND %s=?",
			EpisodesTable.COLUMN_SHOW_ID,
			EpisodesTable.COLUMN_SEASON_NUMBER
		);
		ArrayList<String> selectionArgs = new ArrayList<String>(){
			{
				add(String.valueOf(showId));
				add(String.valueOf(seasonNumber));
			}
		};

		if (watched) {
			// Only mark episodes that have aired.
			selection = String.format(
				"%s AND %s <= ? AND %s IS NOT NULL",
				selection,
				EpisodesTable.COLUMN_FIRST_AIRED,
				EpisodesTable.COLUMN_FIRST_AIRED
			);
			selectionArgs.add(String.valueOf(now.getTime() / 1000));
		}

		handler.startUpdate(0,
		                    null,
		                    ShowsProvider.CONTENT_URI_EPISODES,
		                    epValues,
		                    selection,
		                    selectionArgs.toArray(new String[0]));
	}
}
