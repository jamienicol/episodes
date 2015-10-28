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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.db.ShowsTable;
import org.jamienicol.episodes.services.RefreshShowService;

public class ShowActivity
	extends ActionBarActivity
	implements LoaderManager.LoaderCallbacks<Cursor>,
	           ViewPager.OnPageChangeListener,
	           SeasonsListFragment.OnSeasonSelectedListener
{
	private static final String KEY_DEFAULT_TAB = "default_tab";

	private int showId;
	private boolean isShowStarred;

	private ImageView headerImage;
	private Toolbar toolbar;
	private TextView titleView;
	private TabLayout tabStrip;
	private PagerAdapter pagerAdapter;
	private ViewPager pager;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_activity);

		final Intent intent = getIntent();
		showId = intent.getIntExtra("showId", -1);
		if (showId == -1) {
			throw new IllegalArgumentException("must provide valid showId");
		}

		final Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		getSupportLoaderManager().initLoader(0, loaderArgs, this);

		headerImage = (ImageView)findViewById(R.id.header_image);

		toolbar = (Toolbar)findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		titleView = (TextView)findViewById(R.id.title);

		pagerAdapter =
			new PagerAdapter(this, getSupportFragmentManager(), showId);

		pager = (ViewPager)findViewById(R.id.pager);
		pager.setAdapter(pagerAdapter);
		pager.setOnPageChangeListener(this);

		tabStrip = (TabLayout)findViewById(R.id.tab_strip);
		tabStrip.setTabTextColors(getResources().getColorStateList(R.color.tab_text));
		tabStrip.setupWithViewPager(pager);

		// Set the default tab from preferences.
		final SharedPreferences prefs =
				PreferenceManager.getDefaultSharedPreferences(this);
		pager.setCurrentItem(prefs.getInt(KEY_DEFAULT_TAB, 0));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
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

	/* LoaderManager.LoaderCallbacks<Cursor> */
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final int showId = args.getInt("showId");
		final Uri uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
		                                     String.valueOf(showId));
		final String[] projection = {
			ShowsTable.COLUMN_NAME,
			ShowsTable.COLUMN_STARRED,
			ShowsTable.COLUMN_FANART_PATH
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
			titleView.setText(data.getString(nameColumnIndex));

			// maybe update the state of the toggle starred menu item
			final int starredColumnIndex =
				data.getColumnIndexOrThrow(ShowsTable.COLUMN_STARRED);
			final boolean starred =
				data.getInt(starredColumnIndex) > 0 ? true : false;
			if (isShowStarred != starred) {
				isShowStarred = starred;
				// toggle starred menu item needs updated
				supportInvalidateOptionsMenu();
			}

			final int fanartPathColumnIndex =
				data.getColumnIndexOrThrow(ShowsTable.COLUMN_FANART_PATH);
			final String fanartPath = data.getString(fanartPathColumnIndex);
			if (fanartPath != null && !fanartPath.equals("")) {
				final String fanartUrl =
					String.format("http://thetvdb.com/banners/%s", fanartPath);

				final DisplayImageOptions options =
					new DisplayImageOptions.Builder()
					.cacheInMemory(true)
					.cacheOnDisk(true)
					.build();
				ImageLoader.getInstance().displayImage(fanartUrl,
				                                       headerImage,
				                                       options);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		onLoadFinished(loader, null);
	}

	/* ViewPager.OnPageChangeListener */
	@Override
	public void onPageScrolled(int position,
	                           float positionOffset,
	                           int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		final SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(KEY_DEFAULT_TAB, position);
		editor.apply();
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	/* SeasonsListFragment.OnSeasonSelectedListener */
	@Override
	public void onSeasonSelected(int seasonNumber) {
		final Intent intent = new Intent(this, SeasonActivity.class);
		intent.putExtra("showId", showId);
		intent.putExtra("seasonNumber", seasonNumber);
		startActivity(intent);
	}

	private void toggleShowStarred() {
		final ContentResolver contentResolver = getContentResolver();
		final AsyncQueryHandler handler =
			new AsyncQueryHandler(contentResolver) {};
		final ContentValues values = new ContentValues();
		values.put(ShowsTable.COLUMN_STARRED, !isShowStarred);
		final String selection = String.format("%s=?", ShowsTable.COLUMN_ID);
		final String[] selectionArgs = {
			String.valueOf(showId)
		};

		handler.startUpdate(0,
		                    null,
		                    ShowsProvider.CONTENT_URI_SHOWS,
		                    values,
		                    selection,
		                    selectionArgs);
	}

	private void refreshShow() {
		final Intent intent = new Intent(this, RefreshShowService.class);
		intent.putExtra("showId", showId);

		startService(intent);
	}

	private void markShowWatched(boolean watched) {
		final ContentResolver contentResolver = getContentResolver();
		final AsyncQueryHandler handler =
			new AsyncQueryHandler(contentResolver) {};
		final ContentValues epValues = new ContentValues();
		epValues.put(EpisodesTable.COLUMN_WATCHED, watched);
		final String selection =
			String.format("%s=? AND %s!=?",
			              EpisodesTable.COLUMN_SHOW_ID,
			              EpisodesTable.COLUMN_SEASON_NUMBER);
		final String[] selectionArgs = {
			String.valueOf(showId),
			"0"
		};

		handler.startUpdate(0,
		                    null,
		                    ShowsProvider.CONTENT_URI_EPISODES,
		                    epValues,
		                    selection,
		                    selectionArgs);
	}

	private void deleteShow() {
		final ContentResolver contentResolver = getContentResolver();
		final AsyncQueryHandler handler =
			new AsyncQueryHandler(contentResolver) {};

		/* delete all the show's episodes */
		final String epSelection =
			String.format("%s=?", EpisodesTable.COLUMN_SHOW_ID);
		final String[] epSelectionArgs = {
			String.valueOf(showId)
		};

		handler.startDelete(0,
		                    null,
		                    ShowsProvider.CONTENT_URI_EPISODES,
		                    epSelection,
		                    epSelectionArgs);

		/* delete the show itself */
		final Uri showUri =
			Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
			                     String.valueOf(showId));
		handler.startDelete(0,
		                    null,
		                    showUri,
		                    null,
		                    null);
	}

	private static class PagerAdapter
		extends FragmentPagerAdapter
	{
		private final Context context;
		private final int showId;

		public PagerAdapter(final Context context,
		                    final FragmentManager fragmentManager,
		                    final int showId) {
			super(fragmentManager);

			this.context = context;
			this.showId = showId;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(final int position) {
			switch (position) {
			case 0:
				return context.getString(R.string.show_tab_overview);
			case 1:
				return context.getString(R.string.show_tab_episodes);
			case 2:
				return context.getString(R.string.show_tab_next);
			default:
				return null;
			}
		}

		@Override
		public Fragment getItem(final int position) {
			switch (position) {
			case 0:
				return ShowDetailsFragment.newInstance(showId);
			case 1:
				return SeasonsListFragment.newInstance(showId);
			case 2:
				return NextEpisodeFragment.newInstance(showId);
			default:
				return null;
			}
		}
	}
}
