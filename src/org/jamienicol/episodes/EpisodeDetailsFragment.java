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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import java.text.DateFormat;
import java.util.Date;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;

public class EpisodeDetailsFragment extends SherlockFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private int episodeId;
	private TextView overviewView;
	private TextView seasonEpisodeView;
	private TextView firstAiredView;
	private CheckBox watchedCheckBox;
	private Cursor episodeData;

	public static EpisodeDetailsFragment newInstance(int episodeId) {
		EpisodeDetailsFragment instance = new EpisodeDetailsFragment();

		Bundle args = new Bundle();
		args.putInt("episodeId", episodeId);

		instance.setArguments(args);
		return instance;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		episodeId = getArguments().getInt("episodeId");

		setHasOptionsMenu(true);
	}

	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.episode_details_fragment,
		                             container,
		                             false);

		overviewView = (TextView)view.findViewById(R.id.overview);
		seasonEpisodeView = (TextView)view.findViewById(R.id.season_episode);
		firstAiredView = (TextView)view.findViewById(R.id.first_aired);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("episodeId", episodeId);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.episode_details_fragment, menu);

		watchedCheckBox =
			(CheckBox)menu.findItem(R.id.menu_watched).getActionView();

		final ContentResolver contentResolver =
			getActivity().getContentResolver();
		watchedCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean isChecked = ((CheckBox)view).isChecked();

				AsyncQueryHandler handler =
					new AsyncQueryHandler(contentResolver) {};

				Uri episodeUri =
					Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES,
					                     new Integer(episodeId).toString());

				ContentValues episodeValues = new ContentValues();
				episodeValues.put(EpisodesTable.COLUMN_WATCHED, isChecked);

				handler.startUpdate(0,
				                    null,
				                    episodeUri,
				                    episodeValues,
				                    null,
				                    null);
			}});
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		refreshViews();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int episodeId = args.getInt("episodeId");
		Uri uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES,
		                               new Integer(episodeId).toString());
		String[] projection = {
			EpisodesTable.COLUMN_OVERVIEW,
			EpisodesTable.COLUMN_SEASON_NUMBER,
			EpisodesTable.COLUMN_EPISODE_NUMBER,
			EpisodesTable.COLUMN_FIRST_AIRED,
			EpisodesTable.COLUMN_WATCHED
		};
		return new CursorLoader(getActivity(),
		                        uri,
		                        projection,
		                        null,
		                        null,
		                        null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		episodeData = data;
		refreshViews();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		episodeData = null;
		refreshViews();
	}

	private void refreshViews() {
		if (episodeData != null && episodeData.moveToFirst()) {

			int overviewColumnIndex =
				episodeData.getColumnIndexOrThrow(EpisodesTable.COLUMN_OVERVIEW);
			if (episodeData.isNull(overviewColumnIndex)) {
				overviewView.setVisibility(View.INVISIBLE);
			} else {
				overviewView.setText(episodeData.getString(overviewColumnIndex));
				overviewView.setVisibility(View.VISIBLE);
			}

			int seasonNumberColumnIndex =
				episodeData.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER);
			int seasonNumber = episodeData.getInt(seasonNumberColumnIndex);
			if (seasonNumber == 0) {
				seasonEpisodeView.setVisibility(View.INVISIBLE);
			} else {
				int episodeNumberColumnIndex =
					episodeData.getColumnIndexOrThrow(EpisodesTable.COLUMN_EPISODE_NUMBER);
				String seasonEpisodeText =
					String.format(getActivity().getString(R.string.season_episode),
					              episodeData.getInt(seasonNumberColumnIndex),
					              episodeData.getInt(episodeNumberColumnIndex));
				seasonEpisodeView.setText(seasonEpisodeText);
				seasonEpisodeView.setVisibility(View.VISIBLE);
			}

			int firstAiredColumnIndex =
				episodeData.getColumnIndexOrThrow(EpisodesTable.COLUMN_FIRST_AIRED);
			if (episodeData.isNull(firstAiredColumnIndex)) {
				firstAiredView.setVisibility(View.INVISIBLE);
			} else {
				Date firstAired =
					new Date(episodeData.getLong(firstAiredColumnIndex) * 1000);
				DateFormat df = DateFormat.getDateInstance();
				String firstAiredText =
					String.format(getString(R.string.first_aired),
					              df.format(firstAired));
				firstAiredView.setText(firstAiredText);
				firstAiredView.setVisibility(View.VISIBLE);
			}

			// watchedCheckBox might not be inflated yet
			if (watchedCheckBox != null) {
				int watchedColumnIndex =
					episodeData.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED);
				int watched = episodeData.getInt(watchedColumnIndex);
				watchedCheckBox.setChecked(watched != 0);
				watchedCheckBox.setVisibility(View.VISIBLE);
			}

		} else {
			// on old android versions (on 8, not on 17) the first load
			// will complete before these are inflated,
			// so we must check for null
			if (overviewView != null) {
				overviewView.setVisibility(View.INVISIBLE);
			}
			if (seasonEpisodeView != null) {
				seasonEpisodeView.setVisibility(View.INVISIBLE);
			}
			if (firstAiredView != null) {
				firstAiredView.setVisibility(View.INVISIBLE);
			}
			// even on newer android versions watchedCheckBox
			// wont be inflated before the first load completes
			if (watchedCheckBox != null) {
				watchedCheckBox.setVisibility(View.INVISIBLE);
			}
		}
	}
}
