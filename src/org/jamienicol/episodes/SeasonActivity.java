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
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;

public class SeasonActivity
	extends ActionBarActivity
	implements EpisodesListFragment.OnEpisodeSelectedListener
{
	private int showId;
	private int seasonNumber;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.season_activity);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		showId = intent.getIntExtra("showId", -1);
		if (showId == -1) {
			throw new IllegalArgumentException("must provide valid showId");
		}
		seasonNumber = intent.getIntExtra("seasonNumber", -1);
		if (seasonNumber == -1) {
			throw new IllegalArgumentException("must provide valid seasonNumber");
		}

		if (seasonNumber == 0) {
			setTitle(getString(R.string.season_name_specials));
		} else {
			setTitle(String.format(getString(R.string.season_name,
			                                 seasonNumber)));
		}

		// create and add episodes list fragment,
		// but only on the first time the activity is created
		if (savedInstanceState == null) {
			EpisodesListFragment fragment =
				EpisodesListFragment.newInstance(showId, seasonNumber);
			FragmentTransaction transaction =
				getSupportFragmentManager().beginTransaction();
			transaction.add(R.id.episodes_list_fragment_container, fragment);
			transaction.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.season_activity, menu);

		return true;
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
		Intent intent = new Intent(this,
		                           EpisodeActivity.class);
		intent.putExtra("showId", showId);
		intent.putExtra("seasonNumber", seasonNumber);
		intent.putExtra("initialEpisodeId", episodeId);
		startActivity(intent);
	}

	private void markSeasonWatched(boolean watched) {
		ContentResolver contentResolver = getContentResolver();
		AsyncQueryHandler handler = new AsyncQueryHandler(contentResolver) {};
		ContentValues epValues = new ContentValues();
		epValues.put(EpisodesTable.COLUMN_WATCHED, watched);
		String selection = String.format("%s=? AND %s=?",
		                                 EpisodesTable.COLUMN_SHOW_ID,
		                                 EpisodesTable.COLUMN_SEASON_NUMBER);
		String[] selectionArgs = {
			new Integer(showId).toString(),
			new Integer(seasonNumber).toString()
		};

		handler.startUpdate(0,
		                    null,
		                    ShowsProvider.CONTENT_URI_EPISODES,
		                    epValues,
		                    selection,
		                    selectionArgs);
	}
}
