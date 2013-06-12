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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import java.util.HashMap;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.db.ShowsTable;
import org.jamienicol.episodes.services.RefreshShowService;

public class ShowsListFragment extends SherlockListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private static final int LOADER_ID_SHOWS = 0;
	private static final int LOADER_ID_EPISODES = 1;

	private ShowsListAdapter listAdapter;
	private Cursor showsData;
	private Cursor episodesData;

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

		listAdapter = new ShowsListAdapter(getActivity(),
		                                   null,
		                                   null);
		setListAdapter(listAdapter);

		getLoaderManager().initLoader(LOADER_ID_SHOWS, null, this);
		getLoaderManager().initLoader(LOADER_ID_EPISODES, null, this);
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
		if (id == LOADER_ID_SHOWS) {
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

		} else if (id == LOADER_ID_EPISODES) {
			String[] projection = {
				EpisodesTable.COLUMN_SHOW_ID,
				EpisodesTable.COLUMN_WATCHED
			};
			return new CursorLoader(getActivity(),
			                        ShowsProvider.CONTENT_URI_EPISODES,
			                        projection,
			                        null,
			                        null,
			                        null);

		} else {
			throw new IllegalArgumentException("invalid loader id");
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch (loader.getId()) {
		case LOADER_ID_SHOWS:
			showsData = data;
			break;

		case LOADER_ID_EPISODES:
			episodesData = data;
			break;
		}

		refreshViews();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case LOADER_ID_SHOWS:
			showsData = null;
			break;

		case LOADER_ID_EPISODES:
			episodesData = null;
			break;
		}

		refreshViews();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		onShowSelectedListener.onShowSelected((int)id);
	}

	// ensures views are updated to display newest data.
	// to be called whenever a new cursor has been loaded
	private void refreshViews() {
		listAdapter.swapShowsCursor(showsData);
		listAdapter.swapEpisodesCursor(episodesData);

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

	private static class ShowsListAdapter
		extends BaseAdapter
	{
		private Context context;
		private HashMap<Integer, Integer> numEpisodesMap;
		private HashMap<Integer, Integer> numWatchedEpisodesMap;
		private Cursor showsCursor;

		public ShowsListAdapter(Context context,
		                        Cursor showsCursor,
		                        Cursor episodesCursor) {
			this.context = context;

			countEpisodes(episodesCursor);
		}

		public void swapShowsCursor(Cursor showsCursor) {
			this.showsCursor = showsCursor;

			notifyDataSetChanged();
		}

		public void swapEpisodesCursor(Cursor episodesCursor) {
			countEpisodes(episodesCursor);

			if (showsCursor != null) {
				notifyDataSetChanged();
			}
		}

		private void countEpisodes(Cursor episodesCursor) {
			numEpisodesMap = new HashMap<Integer, Integer>();
			numWatchedEpisodesMap = new HashMap<Integer, Integer>();

			/* extract the total number and number of watched episodes
			 * for each show. */
			if (episodesCursor != null && episodesCursor.moveToFirst()) {
				do {
					int showIdColumnIndex =
						episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_SHOW_ID);
					int showId = episodesCursor.getInt(showIdColumnIndex);

					// ensure entries exist in maps for this show id
					if (numEpisodesMap.containsKey(showId) == false) {
						numEpisodesMap.put(showId, 0);
					}
					if (numWatchedEpisodesMap.containsKey(showId) == false) {
						numWatchedEpisodesMap.put(showId, 0);
					}

					// increment num episodes for this show
					numEpisodesMap.put(showId, numEpisodesMap.get(showId) + 1);

					// if episode is watched, increment value for this show
					int watchedColumnIndex =
						episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED);
					boolean watched =
						episodesCursor.getInt(watchedColumnIndex) > 0 ? true : false;
					if (watched) {
						numWatchedEpisodesMap.put(showId,
						                          numWatchedEpisodesMap.get(showId) + 1);
					}
				} while (episodesCursor.moveToNext());
			}
		}

		@Override
		public int getCount() {
			if (showsCursor == null) {
				return 0;
			} else {
				return showsCursor.getCount();
			}
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			showsCursor.moveToPosition(position);

			int idColumnIndex =
				showsCursor.getColumnIndexOrThrow(ShowsTable.COLUMN_ID);
			return showsCursor.getInt(idColumnIndex);
		}

		@Override
		public View getView(int position,
		                    View convertView,
		                    ViewGroup parent) {

			LayoutInflater inflater = LayoutInflater.from(context);
			if(convertView == null) {
				convertView = inflater.inflate(R.layout.shows_list_item,
				                               parent,
				                               false);
			}

			showsCursor.moveToPosition(position);

			int idColumnIndex =
				showsCursor.getColumnIndexOrThrow(ShowsTable.COLUMN_ID);
			int id = showsCursor.getInt(idColumnIndex);

			TextView nameView =
				(TextView)convertView.findViewById(R.id.show_name_view);
			int nameColumnIndex =
				showsCursor.getColumnIndexOrThrow(ShowsTable.COLUMN_NAME);
			String name = showsCursor.getString(nameColumnIndex);
			nameView.setText(name);

			ProgressBar progressBar =
				(ProgressBar)convertView.findViewById(R.id.show_progress_bar);
			int numEpisodes = 0;
			if (numEpisodesMap.containsKey(id)) {
				numEpisodes = numEpisodesMap.get(id);
			}
			int numWatched = 0;
			if (numWatchedEpisodesMap.containsKey(id)) {
				numWatched = numWatchedEpisodesMap.get(id);
			}
			progressBar.setMax(numEpisodes);
			progressBar.setProgress(numWatched);

			return convertView;
		}
	}
}
