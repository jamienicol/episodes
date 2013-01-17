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

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import org.jamienicol.nextepisode.db.EpisodesTable;
import org.jamienicol.nextepisode.db.ShowsTable;
import org.jamienicol.nextepisode.db.ShowsProvider;
import org.jamienicol.nextepisode.tvdb.Client;
import org.jamienicol.nextepisode.tvdb.Episode;
import org.jamienicol.nextepisode.tvdb.Show;

public class AddShowPreviewFragment extends Fragment
{
	private Show show;

	public static AddShowPreviewFragment newInstance(int searchResultIndex) {
		AddShowPreviewFragment instance = new AddShowPreviewFragment();

		Bundle args = new Bundle();
		args.putInt("searchResultIndex", searchResultIndex);

		instance.setArguments(args);
		return instance;
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.add_show_preview_fragment,
		                             container,
		                             false);

		TextView overviewView = (TextView)view.findViewById(R.id.overview);
		TextView firstAiredView = (TextView)view.findViewById(R.id.first_aired);

		int searchResultIndex = getArguments().getInt("searchResultIndex");

		AddShowSearchResults results = AddShowSearchResults.getInstance();
		List<Show> resultsData = results.getData();

		// Ensure that there is actually data to display, because Android
		// may have destroyed it. If there is data display it, if there
		// isn't do nothing and the activity will handle the situation.
		if (resultsData != null) {
			show = resultsData.get(searchResultIndex);

			overviewView.setText(show.getOverview());

			Date firstAired = show.getFirstAired();
			if (firstAired != null) {
				DateFormat df = DateFormat.getDateInstance();
				String text = getString(R.string.first_aired);
				text += ": ";
				text += df.format(show.getFirstAired());
				firstAiredView.setText(text);
			} else {
				firstAiredView.setText(null);
			}
		}

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.add_show_preview_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_show:
			addShow();
			getActivity().finish();

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void addShow() {
		AddShowTask task = new AddShowTask();
		task.execute(show);
	}

	private class AddShowTask extends AsyncTask<Show, Void, Boolean>
	{
		ContentResolver contentResolver;

		protected void onPreExecute() {
			contentResolver = getActivity().getContentResolver();
		}

		protected Boolean doInBackground(Show... shows) {
			Client tvdbClient = new Client("25B864A8BC56AFAD");

			// fetch full show + episode information from tvdb
			Show show = tvdbClient.getShow(shows[0].getId());

			// fill in information about the show
			ContentValues showValues = new ContentValues();
			showValues.put(ShowsTable.COLUMN_TVDB_ID, show.getId());
			showValues.put(ShowsTable.COLUMN_NAME, show.getName());
			showValues.put(ShowsTable.COLUMN_OVERVIEW, show.getOverview());
			if (show.getFirstAired() != null) {
				showValues.put(ShowsTable.COLUMN_FIRST_AIRED,
				               show.getFirstAired().getTime() / 1000);
			}

			// insert the show into the database
			Uri showUri =
				contentResolver.insert(ShowsProvider.CONTENT_URI_SHOWS,
				                       showValues);

			// need to obtain the ID of the inserted show for the episodes'
			// show ID columns. the ID is just the final segment of the URI
			int showId = Integer.parseInt(showUri.getLastPathSegment());

			// insert each episode into the database
			for (Episode ep : show.getEpisodes()) {
				ContentValues epValues = new ContentValues();
				epValues.put(EpisodesTable.COLUMN_TVDB_ID, ep.getId());
				epValues.put(EpisodesTable.COLUMN_SHOW_ID, showId);
				epValues.put(EpisodesTable.COLUMN_NAME, ep.getName());
				epValues.put(EpisodesTable.COLUMN_OVERVIEW,
				             ep.getOverview());
				epValues.put(EpisodesTable.COLUMN_EPISODE_NUMBER,
				             ep.getEpisodeNumber());
				epValues.put(EpisodesTable.COLUMN_SEASON_NUMBER,
				             ep.getSeasonNumber());
				if (ep.getFirstAired() != null) {
					epValues.put(EpisodesTable.COLUMN_FIRST_AIRED,
					             ep.getFirstAired().getTime() / 1000);
				}

				contentResolver.insert(ShowsProvider.CONTENT_URI_EPISODES,
				                       epValues);
			}

			return true;
		}

		protected void onPostExecute(Boolean success) {
		}
	}
}
