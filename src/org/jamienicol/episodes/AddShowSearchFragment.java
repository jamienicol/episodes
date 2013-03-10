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
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import java.util.List;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.db.ShowsTable;
import org.jamienicol.episodes.tvdb.Client;
import org.jamienicol.episodes.tvdb.Show;

public class AddShowSearchFragment extends ListFragment
	implements LoaderManager.LoaderCallbacks<List<Show>>
{
	public static AddShowSearchFragment newInstance(String query) {
		AddShowSearchFragment instance = new AddShowSearchFragment();

		Bundle args = new Bundle();
		args.putString("query", query);

		instance.setArguments(args);
		return instance;
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		return inflater.inflate(R.layout.add_show_search_fragment,
		                        container,
		                        false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// if the loader exists and it's busy loading
		// then spin the progress bar.
		Loader loader = getLoaderManager().getLoader(0);
		if (loader != null) {
			getActivity().setProgressBarIndeterminateVisibility(loader.isStarted());
		}

		String query = getArguments().getString("query");
		Bundle loaderArgs = new Bundle();
		loaderArgs.putString("query", query);

		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<List<Show>> onCreateLoader(int id, Bundle args) {
		getActivity().setProgressBarIndeterminateVisibility(true);

		SearchLoader loader = new SearchLoader(getActivity(),
		                                       args.getString("query"));
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<List<Show>> loader,
	                           List<Show> data) {
		AddShowSearchResults results = AddShowSearchResults.getInstance();
		results.setData(data);

		Activity activity = getActivity();
		activity.setProgressBarIndeterminateVisibility(false);

		ListAdapter adapter = null;
		if (data != null) {
			adapter = new AddShowSearchResultsAdapter(activity, data);
		}
		setListAdapter(adapter);
	}

	@Override
	public void onLoaderReset(Loader<List<Show>> loader) {
		AddShowSearchResults results = AddShowSearchResults.getInstance();
		results.setData(null);

		setListAdapter(null);
	}

	private static class SearchLoader
		extends AsyncTaskLoader<List<Show>>
	{
		private final String query;
		private List<Show> cachedResult;

		public SearchLoader(Context context, String query) {
			super(context);

			this.query = query;
			cachedResult = null;
		}

		@Override
		public List<Show> loadInBackground() {
			Client tvdbClient = new Client("25B864A8BC56AFAD");

			List<Show> results = tvdbClient.searchShows(query);

			return results;
		}

		@Override
		public void deliverResult(List<Show> data) {
			cachedResult = data;

			if (isStarted()) {
				super.deliverResult(data);
			}
		}

		@Override
		public void onStartLoading() {
			if (cachedResult != null) {
				deliverResult(cachedResult);
			} else {
				forceLoad();
			}
		}

		@Override
		public void onStopLoading() {
			cancelLoad();
		}

		@Override
		public void onReset() {
			onStopLoading();
			cachedResult = null;
		}
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(),
		                           AddShowPreviewActivity.class);
		intent.putExtra("searchResultIndex", position);
		startActivity(intent);
	}
}
