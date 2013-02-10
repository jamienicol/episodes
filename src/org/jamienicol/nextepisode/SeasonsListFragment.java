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
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;
import org.jamienicol.nextepisode.db.EpisodesTable;
import org.jamienicol.nextepisode.db.ShowsProvider;

public class SeasonsListFragment extends ListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private int showId;
	private SimpleCursorAdapter listAdapter;

	public interface OnSeasonSelectedListener {
		public void onSeasonSelected(int seasonNumber);
	}
	private OnSeasonSelectedListener onSeasonSelectedListener;

	public static SeasonsListFragment newInstance(int showId) {
		SeasonsListFragment instance = new SeasonsListFragment();

		Bundle args = new Bundle();
		args.putInt("showId", showId);

		instance.setArguments(args);
		return instance;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			onSeasonSelectedListener = (OnSeasonSelectedListener)activity;
		} catch (ClassCastException e) {
			String message =
				String.format("%s must implement OnSeasonSelectedListener",
				              activity.toString());
			throw new ClassCastException(message);
		}
	}

	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		return inflater.inflate(R.layout.seasons_list_fragment,
		                        container,
		                        false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		String[] from = new String[] {
			EpisodesTable.COLUMN_SEASON_NUMBER
		};
		int[] to = new int[] {
			R.id.season_number_view
		};

		listAdapter = new SimpleCursorAdapter(getActivity(),
		                                      R.layout.seasons_list_item,
		                                      null,
		                                      from,
		                                      to,
		                                      0);
		listAdapter.setViewBinder(new SeasonsViewBinder());
		setListAdapter(listAdapter);

		showId = getArguments().getInt("showId");
		Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		// FIXME: initLoader causes no data to be displayed after a
		// screen rotation, for example. Must be to do with us using a
		// custom MatrixCursor instead of Cursor returned from the loader.
		// Using restartLoader causes the MatrixCursor to be recreated so
		// this works fine for now, but there's probably a better fix.
		getLoaderManager().restartLoader(0, loaderArgs, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.seasons_list_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_mark_all_watched:
			markAllWatched(true);
			return true;

		case R.id.menu_mark_all_not_watched:
			markAllWatched(false);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int showId = args.getInt("showId");

		String[] projection = {
			EpisodesTable.COLUMN_SEASON_NUMBER
		};
		String selection = EpisodesTable.COLUMN_SHOW_ID + "=?";
		String[] selectionArgs = {
			new Integer(showId).toString()
		};

		return new CursorLoader(getActivity(),
		                        ShowsProvider.CONTENT_URI_EPISODES,
		                        projection,
		                        selection,
		                        selectionArgs,
		                        EpisodesTable.COLUMN_SEASON_NUMBER + " ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// The Cursor data contains a row for each episode of the show.
		// We just want a row for each season, so we need to create a new
		// Cursor and add a row to it for each season number

		int seasonNumberColumnIndex =
			data.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER);

		// CursorAdapter requires an _id column to work,
		// so we'll just use the season number for that too.
		String[] columns = {
			BaseColumns._ID,
			EpisodesTable.COLUMN_SEASON_NUMBER
		};
		MatrixCursor seasonsCursor = new MatrixCursor(columns);

		// holds the season numbers that we've already seen
		Set<Integer> seen = new HashSet<Integer>();

		while (data.moveToNext()) {
			int seasonNumber = data.getInt(seasonNumberColumnIndex);

			if (!seen.contains(seasonNumber)) {
				seen.add(seasonNumber);

				Object[] row = {
					seasonNumber,
					seasonNumber
				};
				seasonsCursor.addRow(row);
			}
		}

		listAdapter.swapCursor(seasonsCursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		listAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// the id has been set to be the season number,
		// so pass it to the listener.
		onSeasonSelectedListener.onSeasonSelected((int)id);
	}

	private void markAllWatched(boolean watched) {
		ContentResolver contentResolver = getActivity().getContentResolver();
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

	private class SeasonsViewBinder implements SimpleCursorAdapter.ViewBinder
	{
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			int seasonNumberColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER);

			if (columnIndex == seasonNumberColumnIndex) {
				int seasonNumber = cursor.getInt(seasonNumberColumnIndex);
				String text;
				if (seasonNumber == 0) {
					text = getString(R.string.season_name_specials);
				} else {
					text = String.format(getString(R.string.season_name,
					                               seasonNumber));
				}
				TextView textView = (TextView)view;
				textView.setText(text);

				return true;

			} else {
				return false;
			}
		}
	}
}
