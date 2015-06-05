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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;

public class SeasonsListFragment
	extends Fragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private int showId;
	private RecyclerView listView;
	private SeasonsListAdapter listAdapter;

	public interface OnSeasonSelectedListener {
		public void onSeasonSelected(int seasonNumber);
	}
	private OnSeasonSelectedListener onSeasonSelectedListener;

	private final SeasonsListAdapter.OnItemClickListener onItemClickListener =
		new SeasonsListAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int seasonNumber) {
				onSeasonSelectedListener.onSeasonSelected(seasonNumber);
			}
		};

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

	@Override
	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.seasons_list_fragment,
		                                container,
		                                false);

		listView = (RecyclerView)v.findViewById(R.id.list_view);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		listAdapter = new SeasonsListAdapter(getActivity(),
		                                     onItemClickListener);
		listView.setAdapter(listAdapter);
		listView.setLayoutManager(new LinearLayoutManager(getActivity()));

		showId = getArguments().getInt("showId");
		final Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	/* LoaderManager.LoaderCallbacks<Cursor> */
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

	private static class ViewHolder
		extends RecyclerView.ViewHolder
	{
		private final View itemContainer;
		private final TextView nameView;
		private final ProgressBar progressBar;
		private final TextView watchedCountView;

		public ViewHolder(View v) {
			super(v);

			itemContainer = v.findViewById(R.id.item_container);
			nameView = (TextView)v.findViewById(R.id.season_name_view);
			progressBar = (ProgressBar)v.findViewById(R.id.season_progress_bar);
			watchedCountView = (TextView)v.findViewById(R.id.watched_count_view);
		}

		public View getItemContainer() {
			return itemContainer;
		}

		public TextView getNameView() {
			return nameView;
		}

		public ProgressBar getProgressBar() {
			return progressBar;
		}

		public TextView getWatchedCountView() {
			return watchedCountView;
		}
	}

	private static class SeasonsListAdapter
		extends RecyclerView.Adapter<ViewHolder>
	{
		private interface OnItemClickListener {
			public void onItemClick(int position);
		}

		private final Context context;
		private final OnItemClickListener onItemClickListener;
		private EpisodesCounter episodesCounter;

		public SeasonsListAdapter(Context context,
		                          OnItemClickListener onItemClickListener) {
			this.context = context;
			this.onItemClickListener = onItemClickListener;

			episodesCounter =
				new EpisodesCounter(EpisodesTable.COLUMN_SEASON_NUMBER);
		}

		public void swapEpisodesCursor(Cursor episodesCursor) {
			episodesCounter.swapCursor(episodesCursor);

			notifyDataSetChanged();
		}

		/* RecyclerView.Adapter<ViewHolder> */
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
			final View v = LayoutInflater.from(context)
				.inflate(R.layout.seasons_list_item, viewGroup, false);

			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(ViewHolder viewHolder,
		                             final int position) {

			final Integer[] array =
				episodesCounter.getKeys().toArray(new Integer[] {});
			final int seasonNumber = array[position];

			final String nameText;
			if (seasonNumber == 0) {
				nameText = context.getString(R.string.season_name_specials);
			} else {
				nameText = context.getString(R.string.season_name, seasonNumber);
			}
			viewHolder.getNameView().setText(nameText);

			final int numAired =
				episodesCounter.getNumAiredEpisodes(seasonNumber);
			final int numWatched =
				episodesCounter.getNumWatchedEpisodes(seasonNumber);
			final int numUpcoming =
				episodesCounter.getNumUpcomingEpisodes(seasonNumber);

			viewHolder.getProgressBar().setMax(numAired);
			viewHolder.getProgressBar().setProgress(numWatched);

			String watchedCountText = context.getString(R.string.watched_count,
			                                            numWatched,
			                                            numAired);
			if (numUpcoming != 0) {
				watchedCountText += " " +
					context.getString(R.string.upcoming_count, numUpcoming);
			}
			viewHolder.getWatchedCountView().setText(watchedCountText);

			// Send clicks back to the fragment via the supplied listener.
			viewHolder.getItemContainer().setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onItemClickListener.onItemClick(seasonNumber);
					}
				});
		}

		@Override
		public int getItemCount() {
			if (episodesCounter != null) {
				final Integer[] array =
					episodesCounter.getKeys().toArray(new Integer[] {});
				return array.length;
			} else {
				return 0;
			}
		}
	}
}
