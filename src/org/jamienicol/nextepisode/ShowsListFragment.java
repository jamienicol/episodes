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

package org.jamienicol.nextepisode;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import org.jamienicol.nextepisode.db.ShowsProvider;
import org.jamienicol.nextepisode.db.ShowsTable;

public class ShowsListFragment extends ListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private SimpleCursorAdapter listAdapter;

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
		listAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		listAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		onShowSelectedListener.onShowSelected((int)id);
	}
}
