/*
 * Copyright (C) 2012-2015 Jamie Nicol <jamie@thenicols.net>
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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;

public class EpisodeActivity
	extends ActionBarActivity
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	int initialEpisodeId;
	private ViewPager episodeDetailsPager;
	private Cursor episodesData;
	private EpisodeDetailsPagerAdapter pagerAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.episode_activity);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		int showId = intent.getIntExtra("showId", -1);
		if (showId == -1) {
			throw new IllegalArgumentException("must provide valid showId");
		}
		int seasonNumber = intent.getIntExtra("seasonNumber", -1);
		if (seasonNumber == -1) {
			throw new IllegalArgumentException("must provide valid seasonNumber");
		}
		initialEpisodeId = intent.getIntExtra("initialEpisodeId", -1);
		if (initialEpisodeId == -1) {
			throw new IllegalArgumentException("must provide valid initialEpisodeId");
		}

		episodeDetailsPager =
			(ViewPager)findViewById(R.id.episode_details_pager);
		episodesData = null;
		pagerAdapter =
			new EpisodeDetailsPagerAdapter(getSupportFragmentManager(),
			                               episodesData);
		episodeDetailsPager.setAdapter(pagerAdapter);

		Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		loaderArgs.putInt("seasonNumber", seasonNumber);
		getSupportLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int showId = args.getInt("showId");
		int seasonNumber = args.getInt("seasonNumber");

		String[] projection = {
			EpisodesTable.COLUMN_ID
		};
		String selection = String.format("%s=? AND %s=?",
		                                 EpisodesTable.COLUMN_SHOW_ID,
		                                 EpisodesTable.COLUMN_SEASON_NUMBER);
		String[] selectionArgs = {
			String.valueOf(showId),
			String.valueOf(seasonNumber)
		};

		return new CursorLoader(this,
		                        ShowsProvider.CONTENT_URI_EPISODES,
		                        projection,
		                        selection,
		                        selectionArgs,
		                        EpisodesTable.COLUMN_EPISODE_NUMBER + " ASC");
	}

	private int getEpisodePositionFromId(Cursor episodesData, int id) {
		int position;

		episodesData.moveToPosition(-1);
		while (episodesData.moveToNext()) {
			int idColumnIndex =
				episodesData.getColumnIndexOrThrow(EpisodesTable.COLUMN_ID);

			if (id == episodesData.getInt(idColumnIndex)) {
				return episodesData.getPosition();
			}
		}

		return 0;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		/* we'll need to move the view pager to the initial episode
		   if this is the first time data has been loaded */
		final boolean moveToInitialPosition =
			(data != null) && (episodesData == null);

		episodesData = data;
		pagerAdapter.swapCursor(episodesData);

		if (moveToInitialPosition) {
			int initialPosition = getEpisodePositionFromId(episodesData,
			                                               initialEpisodeId);
			episodeDetailsPager.setCurrentItem(initialPosition, false);
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

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private static class EpisodeDetailsPagerAdapter
		extends FragmentStatePagerAdapter
	{
		Cursor episodesData;

		public EpisodeDetailsPagerAdapter(FragmentManager fragmentManager,
		                                  Cursor episodesData) {
			super(fragmentManager);

			swapCursor(episodesData);
		}

		public void swapCursor(Cursor episodesData) {
			this.episodesData = episodesData;
			this.notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			if (episodesData != null) {
				return episodesData.getCount();
			} else {
				return 0;
			}
		}

		@Override
		public Fragment getItem(int position) {
			int idColumnIndex =
				episodesData.getColumnIndexOrThrow(EpisodesTable.COLUMN_ID);
			episodesData.moveToPosition(position);
			int episodeId = episodesData.getInt(idColumnIndex);
			return EpisodeDetailsFragment.newInstance(episodeId);
		}
	}
}
