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

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;
import org.jamienicol.nextepisode.db.EpisodesTable;
import org.jamienicol.nextepisode.db.ShowsProvider;

public class EpisodesListFragment extends ListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private EpisodesCursorAdapter listAdapter;

	public static EpisodesListFragment newInstance(int showId,
	                                               int seasonNumber) {
		EpisodesListFragment instance = new EpisodesListFragment();

		Bundle args = new Bundle();
		args.putInt("showId", showId);
		args.putInt("seasonNumber", seasonNumber);

		instance.setArguments(args);
		return instance;
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

		int showId = getArguments().getInt("showId");
		int seasonNumber = getArguments().getInt("seasonNumber");
		Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		loaderArgs.putInt("seasonNumber", seasonNumber);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int showId = args.getInt("showId");
		int seasonNumber = args.getInt("seasonNumber");

		String[] projection = {
			EpisodesTable.COLUMN_ID,
			EpisodesTable.COLUMN_NAME,
			EpisodesTable.COLUMN_WATCHED
		};
		String selection = String.format("%s=? AND %s=?",
		                                 EpisodesTable.COLUMN_SHOW_ID,
		                                 EpisodesTable.COLUMN_SEASON_NUMBER);
		String[] selectionArgs = {
			new Integer(showId).toString(),
			new Integer(seasonNumber).toString()
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
		listAdapter.swapCursor(null);
	}

	private static class EpisodesCursorAdapter
		extends CursorAdapter
	{
		public EpisodesCursorAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			int nameColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_NAME);
			String name = cursor.getString(nameColumnIndex);
			TextView nameView =
				(TextView)view.findViewById(R.id.episode_name_view);
			nameView.setText(name);

			int watchedColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED);
			int watched = cursor.getInt(watchedColumnIndex);
			CheckBox watchedCheckBox =
				(CheckBox)view.findViewById(R.id.episode_watched_check_box);
			watchedCheckBox.setChecked(watched != 0);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			return inflater.inflate(R.layout.episodes_list_item, parent, false);
		}
	}
}
