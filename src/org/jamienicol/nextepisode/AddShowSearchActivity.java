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

import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.ListView;
import java.util.List;
import org.jamienicol.nextepisode.db.ShowsProvider;
import org.jamienicol.nextepisode.db.ShowsTable;
import org.jamienicol.nextepisode.tvdb.Client;
import org.jamienicol.nextepisode.tvdb.SearchResult;

public class AddShowSearchActivity extends ListActivity
	implements LoaderManager.LoaderCallbacks<List<SearchResult>>
{
	private List<SearchResult> searchResults;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.add_show_search_activity);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			setTitle(query);

			// if the loader exists and it's busy loading
			// then spin the progress bar.
			Loader loader = getLoaderManager().getLoader(0);
			if (loader != null) {
				setProgressBarIndeterminateVisibility(loader.isStarted());
			}

			Bundle loaderArgs = new Bundle();
			loaderArgs.putString("query", query);
			getLoaderManager().initLoader(0, loaderArgs, this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
			                Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);

			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Loader<List<SearchResult>> onCreateLoader(int id, Bundle args) {
		setProgressBarIndeterminateVisibility(true);

		SearchLoader loader = new SearchLoader(this, args.getString("query"));
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<List<SearchResult>> loader,
	                           List<SearchResult> data) {
		setProgressBarIndeterminateVisibility(false);

		searchResults = data;

		ListAdapter adapter = null;
		if (data != null) {
			adapter = new SearchResultsAdapter(this, data);
		}
		setListAdapter(adapter);
	}

	@Override
	public void onLoaderReset(Loader<List<SearchResult>> loader) {
		setListAdapter(null);
	}

	private static class SearchLoader
		extends AsyncTaskLoader<List<SearchResult>>
	{
		private final String query;
		private List<SearchResult> cachedResult;

		public SearchLoader(Context context, String query) {
			super(context);

			this.query = query;
			cachedResult = null;
		}

		@Override
		public List<SearchResult> loadInBackground() {
			Client tvdbClient = new Client("25B864A8BC56AFAD");

			List<SearchResult> results = tvdbClient.searchShows(query);

			return results;
		}

		@Override
		public void deliverResult(List<SearchResult> data) {
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

	protected void onListItemClick (ListView l, View v, int position, long id) {
		SearchResult clickedResult = searchResults.get(position);

		ContentValues values = new ContentValues();
		values.put(ShowsTable.COLUMN_TVDB_ID, clickedResult.getId());
		values.put(ShowsTable.COLUMN_NAME, clickedResult.getName());
		values.put(ShowsTable.COLUMN_OVERVIEW, clickedResult.getOverview());
		getContentResolver().insert(ShowsProvider.CONTENT_URI_SHOWS, values);

		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
		                Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		finish();
	}
}
