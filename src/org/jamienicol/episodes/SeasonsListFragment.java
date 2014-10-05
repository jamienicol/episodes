/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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
import android.support.v4.app.ListFragment;
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
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;

public class SeasonsListFragment
	extends ListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private int showId;
	private SeasonsListAdapter listAdapter;

	public interface OnSeasonSelectedListener {
		public void onSeasonSelected(int seasonNumber);
	}
	private OnSeasonSelectedListener onSeasonSelectedListener;

	public static SeasonsListFragment newInstance(int showId) {
		final SeasonsListFragment instance = new SeasonsListFragment();

		final Bundle args = new Bundle();
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
			final String message =
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
		final Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final int showId = args.getInt("showId");

		final String[] projection = {
			EpisodesTable.COLUMN_SEASON_NUMBER,
			EpisodesTable.COLUMN_FIRST_AIRED,
			EpisodesTable.COLUMN_WATCHED,
		};
		final String selection = EpisodesTable.COLUMN_SHOW_ID + "=?";
		final String[] selectionArgs = {
			String.valueOf(showId)
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
		onLoadFinished(loader, null);
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
		private EpisodesCounter episodesCounter;

		public SeasonsListAdapter(Context context, Cursor episodesCursor) {
			this.context = context;

			episodesCounter =
				new EpisodesCounter(EpisodesTable.COLUMN_SEASON_NUMBER);


			swapEpisodesCursor(episodesCursor);
		}

		public void swapEpisodesCursor(Cursor episodesCursor) {
			episodesCounter.swapCursor(episodesCursor);

			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			if (episodesCounter != null) {
				final Integer[] array =
					episodesCounter.getKeys().toArray(new Integer[] {});
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
			final Integer[] array =
				episodesCounter.getKeys().toArray(new Integer[] {});
			return array[position];
		}

		@Override
		public View getView(int position,
		                    View convertView,
		                    ViewGroup parent) {

			final LayoutInflater inflater = LayoutInflater.from(context);
			if(convertView == null) {
				convertView = inflater.inflate(R.layout.seasons_list_item,
				                               parent,
				                               false);
			}

			final Integer[] array =
				episodesCounter.getKeys().toArray(new Integer[] {});
			final int seasonNumber = array[position];

			final TextView numberView =
				(TextView)convertView.findViewById(R.id.season_number_view);
			String numberText;
			if (seasonNumber == 0) {
				numberText =
					context.getString(R.string.season_name_specials);
			} else {
				numberText = context.getString(R.string.season_name,
				                               seasonNumber);
			}
			numberView.setText(numberText);

			final int numAired =
				episodesCounter.getNumAiredEpisodes(seasonNumber);
			final int numWatched =
				episodesCounter.getNumWatchedEpisodes(seasonNumber);
			final int numUpcoming =
				episodesCounter.getNumUpcomingEpisodes(seasonNumber);

			final ProgressBar progressBar =
				(ProgressBar)convertView.findViewById(R.id.season_progress_bar);
			progressBar.setMax(numAired);
			progressBar.setProgress(numWatched);

			final TextView watchedCountView =
				(TextView)convertView.findViewById(R.id.watched_count_view);
			String watchedCountText = context.getString(R.string.watched_count,
			                                            numWatched,
			                                            numAired);
			if (numUpcoming != 0) {
				watchedCountText += " " +
					context.getString(R.string.upcoming_count,
					                  numUpcoming);
			}
			watchedCountView.setText(watchedCountText);

			return convertView;
		}
	}
}
