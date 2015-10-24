/*
 * Copyright (C) 2015 Jamie Nicol <jamie@thenicols.net>
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;

public class NextEpisodeFragment
	extends Fragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private static final String TAG = NextEpisodeFragment.class.getName();

	private int showId;
	private int episodeId;
	private View rootView;
	private TextView titleView;
	private TextView overviewView;
	private TextView dateView;
	private boolean watched = false;
	private CheckBox watchedCheckBox;

	public static NextEpisodeFragment newInstance(int showId) {
		final NextEpisodeFragment instance = new NextEpisodeFragment();

		final Bundle args = new Bundle();
		args.putInt("showId", showId);

		instance.setArguments(args);
		return instance;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		showId = getArguments().getInt("showId");
	}

	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.episode_details_fragment,
                                           container,
                                           false);

		rootView = view.findViewById(R.id.root);
		titleView = (TextView)view.findViewById(R.id.title);
		overviewView = (TextView)view.findViewById(R.id.overview);
		dateView = (TextView)view.findViewById(R.id.date);
		watchedCheckBox = (CheckBox)view.findViewById(R.id.watched);
		watchedCheckBox.setOnCheckedChangeListener(
			new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
				                             boolean isChecked) {
					// Make sure we have a next episode
					if (episodeId == -1) {
						Log.w(TAG, "Watched check changed but there is no next episode.");
						return;
					}

					final ContentResolver contentResolver =
						getActivity().getContentResolver();
					final AsyncQueryHandler handler =
						new AsyncQueryHandler(contentResolver) {};

					final Uri episodeUri =
						Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES,
						                     String.valueOf(episodeId));

					final ContentValues episodeValues = new ContentValues();
					episodeValues.put(EpisodesTable.COLUMN_WATCHED, isChecked);

					handler.startUpdate(0,
					                    null,
					                    episodeUri,
					                    episodeValues,
					                    null,
					                    null);
				}
			});

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final int showId = args.getInt("showId");
		final String[] projection = {
			EpisodesTable.COLUMN_ID,
			EpisodesTable.COLUMN_NAME,
			EpisodesTable.COLUMN_OVERVIEW,
			EpisodesTable.COLUMN_SEASON_NUMBER,
			EpisodesTable.COLUMN_EPISODE_NUMBER,
			EpisodesTable.COLUMN_FIRST_AIRED,
			EpisodesTable.COLUMN_WATCHED
		};
		final String selection =
			String.format("%s=? AND %s !=0 AND (%s==0 OR %s IS NULL)",
			              EpisodesTable.COLUMN_SHOW_ID,
			              EpisodesTable.COLUMN_SEASON_NUMBER,
			              EpisodesTable.COLUMN_WATCHED,
			              EpisodesTable.COLUMN_WATCHED);
		final String[] selectionArgs = {
			String.valueOf(showId)
		};
		final String sortOrder =
			String.format("%s ASC, %s ASC LIMIT %d",
			              EpisodesTable.COLUMN_SEASON_NUMBER,
			              EpisodesTable.COLUMN_EPISODE_NUMBER,
			              1);
		return new CursorLoader(getActivity(),
		                        ShowsProvider.CONTENT_URI_EPISODES,
		                        projection,
		                        selection,
		                        selectionArgs,
		                        sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data != null && data.moveToFirst()) {

			final int episodeIdColumnIndex =
				data.getColumnIndexOrThrow(EpisodesTable.COLUMN_ID);
			episodeId = data.getInt(episodeIdColumnIndex);

			final int seasonNumberColumnIndex =
				data.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER);
			final int seasonNumber = data.getInt(seasonNumberColumnIndex);
			final int episodeNumberColumnIndex =
					data.getColumnIndexOrThrow(EpisodesTable.COLUMN_EPISODE_NUMBER);
			final int episodeNumber = data.getInt(episodeNumberColumnIndex);

			final int titleColumnIndex =
				data.getColumnIndexOrThrow(EpisodesTable.COLUMN_NAME);
			final String title = data.getString(titleColumnIndex);

			String titleText = "";
			if (seasonNumber != 0) {
				titleText +=
					getActivity().getString(R.string.season_episode_prefix,
					                        seasonNumber,
					                        episodeNumber);
			}
			titleText += title;
			titleView.setText(titleText);

			final int overviewColumnIndex =
				data.getColumnIndexOrThrow(EpisodesTable.COLUMN_OVERVIEW);
			if (data.isNull(overviewColumnIndex)) {
				overviewView.setText("");
			} else {
				overviewView.setText(data.getString(overviewColumnIndex));
			}

			final int firstAiredColumnIndex =
				data.getColumnIndexOrThrow(EpisodesTable.COLUMN_FIRST_AIRED);
			if (data.isNull(firstAiredColumnIndex)) {
				dateView.setText("");
			} else {
				final Date date =
					new Date(data.getLong(firstAiredColumnIndex) * 1000);
				final String dateText =
					DateFormat.getDateInstance(DateFormat.LONG).format(date);
				dateView.setText(dateText);
			}

			final int watchedColumnIndex =
				data.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED);
			watched = data.getInt(watchedColumnIndex) > 0 ? true : false;
			watchedCheckBox.setChecked(watched);

			rootView.setVisibility(View.VISIBLE);
		} else {
			episodeId = -1;
			rootView.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		onLoadFinished(loader, null);
	}
}
