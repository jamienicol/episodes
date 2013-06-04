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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.db.ShowsTable;
import org.jamienicol.episodes.services.RefreshShowService;

public class ShowsListFragment extends SherlockListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private SimpleCursorAdapter listAdapter;
	private Cursor showsData;

	public interface OnShowSelectedListener {
		public void onShowSelected(int showId);
	}
	private OnShowSelectedListener onShowSelectedListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			onShowSelectedListener = (OnShowSelectedListener)activity;
		} catch (ClassCastException e) {
			String message =
				String.format("%s must implement OnShowSelectedListener",
				              activity.toString());
			throw new ClassCastException(message);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
    }

	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		return inflater.inflate(R.layout.shows_list_fragment, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		String[] from = new String[] {
			ShowsTable.COLUMN_NAME
		};
		int[] to = new int[] {
			R.id.show_name_view
		};

		listAdapter = new SimpleCursorAdapter(getActivity(),
		                                      R.layout.shows_list_item,
		                                      null,
		                                      from,
		                                      to,
		                                      0);
		setListAdapter(listAdapter);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.shows_list_fragment, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {

		// hide refresh all option if no shows exist
		boolean showsExist = (showsData != null && showsData.moveToFirst());

		menu.findItem(R.id.menu_refresh_all_shows).setVisible(showsExist);

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh_all_shows:
			refreshAllShows();

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
			ShowsTable.COLUMN_ID,
			ShowsTable.COLUMN_NAME
		};
		return new CursorLoader(getActivity(),
		                        ShowsProvider.CONTENT_URI_SHOWS,
		                        projection,
		                        null,
		                        null,
		                        ShowsTable.COLUMN_NAME + " ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		showsData = data;
		refreshViews();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		showsData = null;
		refreshViews();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		onShowSelectedListener.onShowSelected((int)id);
	}

	// ensures views are updated to display newest data.
	// to be called whenever a new cursor has been loaded
	private void refreshViews() {
		listAdapter.swapCursor(showsData);

		// force a new decision on whether to display certain menu items
		getActivity().supportInvalidateOptionsMenu();
	}

	private void refreshAllShows() {
		if (showsData != null && showsData.moveToFirst()) {
			do {
				int idColumnIndex =
					showsData.getColumnIndexOrThrow(ShowsTable.COLUMN_ID);

				int id = showsData.getInt(idColumnIndex);

				Intent intent = new Intent(getActivity(),
				                           RefreshShowService.class);
				intent.putExtra("showId", id);

				getActivity().startService(intent);

			} while (showsData.moveToNext());
		}
	}
}
