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
import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListAdapter;
import java.util.List;
import org.jamienicol.nextepisode.tvdb.Client;
import org.jamienicol.nextepisode.tvdb.SearchResult;

public class AddShowSearchActivity extends ListActivity
{
	private SearchTask searchTask;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_show_search);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	        String query = intent.getStringExtra(SearchManager.QUERY);

	        setTitle(query);

	        searchTask = new SearchTask(this);
	        searchTask.execute(query);
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

	private static class SearchTask extends AsyncTask<String, Void, Boolean> {
		private AddShowSearchActivity activity;
		private Client tvdbClient;
		private List<SearchResult> results;

		public SearchTask(AddShowSearchActivity activity) {
			this.activity = activity;
			tvdbClient = new Client("25B864A8BC56AFAD");
			results = null;
		}

		@Override
		protected Boolean doInBackground(String... query) {
			results = tvdbClient.searchShows(query[0]);
			if (results != null) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {

			ListAdapter adapter = null;
			if (result) {
				adapter = new SearchResultsAdapter(activity, results);
			}

			activity.setListAdapter(adapter);
		}
	}
}
