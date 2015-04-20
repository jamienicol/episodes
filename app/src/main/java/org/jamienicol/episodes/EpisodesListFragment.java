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
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;

public class EpisodesListFragment
	extends ListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private int showId;
	private int seasonNumber;
	private EpisodesCursorAdapter listAdapter;

	public interface OnEpisodeSelectedListener {
		public void onEpisodeSelected(int episodeId);
	}
	private OnEpisodeSelectedListener onEpisodeSelectedListener;

	public static EpisodesListFragment newInstance(int showId,
	                                               int seasonNumber) {
		final EpisodesListFragment instance = new EpisodesListFragment();

		final Bundle args = new Bundle();
		args.putInt("showId", showId);
		args.putInt("seasonNumber", seasonNumber);

		instance.setArguments(args);
		return instance;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			onEpisodeSelectedListener = (OnEpisodeSelectedListener)activity;
		} catch (ClassCastException e) {
			final String message =
				String.format("%s must implement OnEpisodeSelectedListener",
				              activity.toString());
			throw new ClassCastException(message);
		}
	}

	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		return inflater.inflate(R.layout.episodes_list_fragment,
		                        container,
		                        false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		listAdapter = new EpisodesCursorAdapter(getActivity(), null, 0);
		setListAdapter(listAdapter);

		showId = getArguments().getInt("showId");
		seasonNumber = getArguments().getInt("seasonNumber");

		final Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		loaderArgs.putInt("seasonNumber", seasonNumber);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final int showId = args.getInt("showId");
		final int seasonNumber = args.getInt("seasonNumber");

		final String[] projection = {
			EpisodesTable.COLUMN_ID,
			EpisodesTable.COLUMN_EPISODE_NUMBER,
			EpisodesTable.COLUMN_NAME,
			EpisodesTable.COLUMN_FIRST_AIRED,
			EpisodesTable.COLUMN_WATCHED
		};
		final String selection = String.format("%s=? AND %s=?",
		                                 EpisodesTable.COLUMN_SHOW_ID,
		                                 EpisodesTable.COLUMN_SEASON_NUMBER);
		final String[] selectionArgs = {
			String.valueOf(showId),
			String.valueOf(seasonNumber)
		};

		return new CursorLoader(getActivity(),
		                        ShowsProvider.CONTENT_URI_EPISODES,
		                        projection,
		                        selection,
		                        selectionArgs,
		                        EpisodesTable.COLUMN_EPISODE_NUMBER + " ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		listAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		onLoadFinished(loader, null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// pass the episode id to the listener.
		onEpisodeSelectedListener.onEpisodeSelected((int)id);
	}

	private class EpisodesCursorAdapter
		extends CursorAdapter
	{
		public EpisodesCursorAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final int idColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_ID);
			final int id = cursor.getInt(idColumnIndex);

			final ContentResolver contentResolver =
				context.getContentResolver();

			final int nameColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_NAME);
			final String name = cursor.getString(nameColumnIndex);

			final int episodeNumberColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_EPISODE_NUMBER);
			final int episodeNumber = cursor.getInt(episodeNumberColumnIndex);

			final TextView nameView =
				(TextView)view.findViewById(R.id.episode_name_view);
			if (seasonNumber == 0) {
				nameView.setText(name);
			} else {
				nameView.setText(String.format("%d - %s", episodeNumber, name));
			}

			final int firstAiredColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_FIRST_AIRED);
			final TextView dateView =
				(TextView)view.findViewById(R.id.episode_date_view);
			if (cursor.isNull(firstAiredColumnIndex)) {
				dateView.setVisibility(View.GONE);
			} else {
				final Date date =
					new Date(cursor.getLong(firstAiredColumnIndex) * 1000);
				final String dateText =
					DateFormat.getDateInstance().format(date);
				dateView.setText(dateText);
				dateView.setVisibility(View.VISIBLE);

				// grey out episode name if upcoming episode
				if (date.after(new Date())) {
					int color = context.getResources().getColor(android.R.color.tertiary_text_light);
					nameView.setTextColor(color);
					dateView.setTextColor(color);
				}
				else {
					int color = context.getResources().getColor(android.R.color.primary_text_light);
					nameView.setTextColor(color);
					dateView.setTextColor(color);
				}
			}

			final int watchedColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED);
			final int watched = cursor.getInt(watchedColumnIndex);
			final CheckBox watchedCheckBox =
				(CheckBox)view.findViewById(R.id.episode_watched_check_box);

			watchedCheckBox.setOnCheckedChangeListener(null);

			watchedCheckBox.setChecked(watched != 0);

			watchedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
				                             boolean isChecked) {
					final AsyncQueryHandler handler =
						new AsyncQueryHandler(contentResolver) {};
					final ContentValues epValues = new ContentValues();
					epValues.put(EpisodesTable.COLUMN_WATCHED, isChecked);

					final Uri epUri =
						Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES,
						                     String.valueOf(id));
					handler.startUpdate(0,
					                    null,
					                    epUri,
					                    epValues,
					                    null,
					                    null);
				}
			});
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LayoutInflater inflater = LayoutInflater.from(context);
			return inflater.inflate(R.layout.episodes_list_item, parent, false);
		}
	}
}
