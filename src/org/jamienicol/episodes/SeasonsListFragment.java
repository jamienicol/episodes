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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;

public class SeasonsListFragment extends SherlockListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private int showId;
	private SeasonsListAdapter listAdapter;

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

		listAdapter = new SeasonsListAdapter(getActivity(),
		                                     null);
		setListAdapter(listAdapter);

		showId = getArguments().getInt("showId");
		Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int showId = args.getInt("showId");

		String[] projection = {
			EpisodesTable.COLUMN_SEASON_NUMBER,
			EpisodesTable.COLUMN_WATCHED
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
		listAdapter.swapEpisodesCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		listAdapter.swapEpisodesCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// the id has been set to be the season number,
		// so pass it to the listener.
		onSeasonSelectedListener.onSeasonSelected((int)id);
	}

	private static class SeasonsListAdapter
		extends BaseAdapter
	{
		private Context context;
		Set<Integer> seasonNumbersSet;
		private HashMap<Integer, Integer> numEpisodesMap;
		private HashMap<Integer, Integer> numWatchedEpisodesMap;

		public SeasonsListAdapter(Context context, Cursor episodesCursor) {
			this.context = context;

			processCursor(episodesCursor);
		}

		public void swapEpisodesCursor(Cursor episodesCursor) {
			processCursor(episodesCursor);

			notifyDataSetChanged();
		}

		private void processCursor(Cursor episodesCursor) {
			seasonNumbersSet = new TreeSet<Integer>();
			numEpisodesMap = new HashMap<Integer, Integer>();
			numWatchedEpisodesMap = new HashMap<Integer, Integer>();

			if (episodesCursor != null && episodesCursor.moveToFirst()) {
				do {
					int seasonNumberColumnIndex =
						episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER);
					int seasonNumber =
						episodesCursor.getInt(seasonNumberColumnIndex);

					seasonNumbersSet.add(seasonNumber);

					// ensure entries exist in maps for this season
					if (numEpisodesMap.containsKey(seasonNumber) == false) {
						numEpisodesMap.put(seasonNumber, 0);
					}
					if (numWatchedEpisodesMap.containsKey(seasonNumber) == false) {
						numWatchedEpisodesMap.put(seasonNumber, 0);
					}

					// increment num episodes for this season
					numEpisodesMap.put(seasonNumber,
					                   numEpisodesMap.get(seasonNumber) + 1);

					// if episode is watched, increment value for this season
					int watchedColumnIndex =
						episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED);
					boolean watched =
						episodesCursor.getInt(watchedColumnIndex) > 0 ? true : false;
					if (watched) {
						numWatchedEpisodesMap.put(seasonNumber,
						                          numWatchedEpisodesMap.get(seasonNumber) + 1);
					}
				} while (episodesCursor.moveToNext());
			}
		}

		@Override
		public int getCount() {
			if (seasonNumbersSet != null) {
				Integer[] array = seasonNumbersSet.toArray(new Integer[] {});
				return array.length;
			} else {
				return 0;
			}
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			// use season number as id
			Integer[] array = seasonNumbersSet.toArray(new Integer[] {});
			return array[position];
		}

		@Override
		public View getView(int position,
		                    View convertView,
		                    ViewGroup parent) {

			LayoutInflater inflater = LayoutInflater.from(context);
			if(convertView == null) {
				convertView = inflater.inflate(R.layout.seasons_list_item,
				                               parent,
				                               false);
			}

			Integer[] array = seasonNumbersSet.toArray(new Integer[] {});
			int seasonNumber = array[position];

			TextView numberView =
				(TextView)convertView.findViewById(R.id.season_number_view);
			String numberText;
			if (seasonNumber == 0) {
				numberText =
					context.getString(R.string.season_name_specials);
			} else {
				numberText =
					String.format(context.getString(R.string.season_name),
					              seasonNumber);
			}
			numberView.setText(numberText);

			ProgressBar progressBar =
				(ProgressBar)convertView.findViewById(R.id.season_progress_bar);
			int numEpisodes = 0;
			if (numEpisodesMap.containsKey(seasonNumber)) {
				numEpisodes = numEpisodesMap.get(seasonNumber);
			}
			int numWatched = 0;
			if (numWatchedEpisodesMap.containsKey(seasonNumber)) {
				numWatched = numWatchedEpisodesMap.get(seasonNumber);
			}
			progressBar.setMax(numEpisodes);
			progressBar.setProgress(numWatched);

			return convertView;
		}
	}
}
