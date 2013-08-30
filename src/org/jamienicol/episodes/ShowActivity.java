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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.db.ShowsTable;
import org.jamienicol.episodes.services.RefreshShowService;

public class ShowActivity extends SherlockFragmentActivity
	implements LoaderManager.LoaderCallbacks<Cursor>,
	           SeasonsListFragment.OnSeasonSelectedListener
{
	private int showId;
	private TabsAdapter pagerAdapter;
	private ViewPager pager;
	private boolean isShowStarred;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_activity);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		showId = intent.getIntExtra("showId", -1);
		if (showId == -1) {
			throw new IllegalArgumentException("must provide valid showId");
		}

		Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		getSupportLoaderManager().initLoader(0, loaderArgs, this);

		final ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		pager = (ViewPager)findViewById(R.id.pager);
		pagerAdapter = new TabsAdapter(this,
		                               getSupportFragmentManager(),
		                               bar,
		                               pager);

		Bundle fragmentArgs = new Bundle();
		fragmentArgs.putInt("showId", showId);

		ActionBar.Tab overviewTab = bar.newTab();
		overviewTab.setText(R.string.show_tab_overview);
		pagerAdapter.addTab(overviewTab,
		                    ShowDetailsFragment.class,
		                    fragmentArgs);
		ActionBar.Tab episodesTab = bar.newTab();
		episodesTab.setText(R.string.show_tab_episodes);
		pagerAdapter.addTab(episodesTab,
		                    SeasonsListFragment.class,
		                    fragmentArgs);

		pager.setAdapter(pagerAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.show_activity, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		final MenuItem toggleStarred =
			menu.findItem(R.id.menu_toggle_show_starred);
		if (isShowStarred) {
			toggleStarred.setIcon(R.drawable.ic_show_starred);
			toggleStarred.setTitle(R.string.menu_unstar_show);
		} else {
			toggleStarred.setIcon(R.drawable.ic_show_unstarred);
			toggleStarred.setTitle(R.string.menu_star_show);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int showId = args.getInt("showId");
		Uri uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
		                               new Integer(showId).toString());
		String[] projection = {
			ShowsTable.COLUMN_NAME,
			ShowsTable.COLUMN_STARRED
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
			int nameColumnIndex =
				data.getColumnIndexOrThrow(ShowsTable.COLUMN_NAME);
			setTitle(data.getString(nameColumnIndex));

			// maybe update the state of the toggle starred menu item
			int starredColumnIndex =
				data.getColumnIndexOrThrow(ShowsTable.COLUMN_STARRED);
			boolean starred =
				data.getInt(starredColumnIndex) > 0 ? true : false;
			if (isShowStarred != starred) {
				isShowStarred = starred;
				// toggle starred menu item needs updated
				supportInvalidateOptionsMenu();
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		setTitle("");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;

		case R.id.menu_toggle_show_starred:
			toggleShowStarred();
			return true;

		case R.id.menu_refresh_show:
			refreshShow();
			return true;

		case R.id.menu_mark_show_watched:
			markShowWatched(true);
			return true;

		case R.id.menu_mark_show_not_watched:
			markShowWatched(false);
			return true;

		case R.id.menu_delete_show:
			deleteShow();
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSeasonSelected(int seasonNumber) {
		Intent intent = new Intent(this,
		                           SeasonActivity.class);
		intent.putExtra("showId", showId);
		intent.putExtra("seasonNumber", seasonNumber);
		startActivity(intent);
	}

	private void toggleShowStarred() {
		ContentResolver contentResolver = getContentResolver();
		AsyncQueryHandler handler = new AsyncQueryHandler(contentResolver) {};
		ContentValues values = new ContentValues();
		values.put(ShowsTable.COLUMN_STARRED, !isShowStarred);
		String selection = String.format("%s=?",
		                                 ShowsTable.COLUMN_ID);
		String[] selectionArgs = {
			new Integer(showId).toString()
		};

		handler.startUpdate(0,
		                    null,
		                    ShowsProvider.CONTENT_URI_SHOWS,
		                    values,
		                    selection,
		                    selectionArgs);
	}

	private void refreshShow() {
		Intent intent = new Intent(this, RefreshShowService.class);
		intent.putExtra("showId", showId);

		startService(intent);
	}

	private void markShowWatched(boolean watched) {
		ContentResolver contentResolver = getContentResolver();
		AsyncQueryHandler handler = new AsyncQueryHandler(contentResolver) {};
		ContentValues epValues = new ContentValues();
		epValues.put(EpisodesTable.COLUMN_WATCHED, watched);
		String selection = String.format("%s=?",
		                                 EpisodesTable.COLUMN_SHOW_ID);
		String[] selectionArgs = {
			new Integer(showId).toString()
		};

		handler.startUpdate(0,
		                    null,
		                    ShowsProvider.CONTENT_URI_EPISODES,
		                    epValues,
		                    selection,
		                    selectionArgs);
	}

	private void deleteShow() {
		ContentResolver contentResolver = getContentResolver();
		AsyncQueryHandler handler = new AsyncQueryHandler(contentResolver) {};

		/* delete all the show's episodes */
		String epSelection = String.format("%s=?",
		                                   EpisodesTable.COLUMN_SHOW_ID);
		String[] epSelectionArgs = {
			new Integer(showId).toString()
		};

		handler.startDelete(0,
		                    null,
		                    ShowsProvider.CONTENT_URI_EPISODES,
		                    epSelection,
		                    epSelectionArgs);

		/* delete the show itself */
		Uri showUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
		                                   new Integer(showId).toString());
		handler.startDelete(0,
		                    null,
		                    showUri,
		                    null,
		                    null);
	}
}
