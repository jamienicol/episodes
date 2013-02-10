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
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.jamienicol.nextepisode.db.EpisodesTable;
import org.jamienicol.nextepisode.db.ShowsProvider;

public class EpisodeDetailsFragment extends Fragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private TextView overviewView;
	private TextView seasonEpisodeView;

	public static EpisodeDetailsFragment newInstance(int episodeId) {
		EpisodeDetailsFragment instance = new EpisodeDetailsFragment();

		Bundle args = new Bundle();
		args.putInt("episodeId", episodeId);

		instance.setArguments(args);
		return instance;
	}

	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.episode_details_fragment,
		                             container,
		                             false);

		overviewView = (TextView)view.findViewById(R.id.overview);
		seasonEpisodeView = (TextView)view.findViewById(R.id.season_episode);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int episodeId = getArguments().getInt("episodeId");

		Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("episodeId", episodeId);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int episodeId = args.getInt("episodeId");
		Uri uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES,
		                               new Integer(episodeId).toString());
		String[] projection = {
			EpisodesTable.COLUMN_OVERVIEW,
			EpisodesTable.COLUMN_SEASON_NUMBER,
			EpisodesTable.COLUMN_EPISODE_NUMBER
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
		if (data.getCount() >= 1) {
			data.moveToFirst();

			int overviewColumnIndex =
				data.getColumnIndexOrThrow(EpisodesTable.COLUMN_OVERVIEW);
			overviewView.setText(data.getString(overviewColumnIndex));

			int seasonNumberColumnIndex =
				data.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER);
			int episodeNumberColumnIndex =
				data.getColumnIndexOrThrow(EpisodesTable.COLUMN_EPISODE_NUMBER);
			String seasonEpisodeText =
				String.format(getActivity().getString(R.string.season_episode),
				              data.getInt(seasonNumberColumnIndex),
				              data.getInt(episodeNumberColumnIndex));
			seasonEpisodeView.setText(seasonEpisodeText);
		} else {
			overviewView.setText("");
			seasonEpisodeView.setText("");
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		overviewView.setText("");
		seasonEpisodeView.setText("");
	}
}
