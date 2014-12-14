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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import java.util.List;
import org.jamienicol.episodes.tvdb.Show;

public class AddShowPreviewActivity
	extends ActionBarActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_show_preview_activity);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		int searchResultIndex = intent.getIntExtra("searchResultIndex", 0);

		AddShowSearchResults results = AddShowSearchResults.getInstance();
		List<Show> resultsData = results.getData();

		// Ensure that there is actually data to display,
		// because Android may have destroyed it.
		if (resultsData == null) {
			// Android has killed the singleton instance which held the
			// data. There's nothing this activity can do, so finish.
			finish();
			return;
		}

		Show show = resultsData.get(searchResultIndex);
		getSupportActionBar().setTitle(show.getName());

		// If this is the first time the activity has been created,
		// create and add the preview fragment.
		if (savedInstanceState == null) {
			AddShowPreviewFragment fragment =
				AddShowPreviewFragment.newInstance(searchResultIndex);
			FragmentTransaction transaction =
				getSupportFragmentManager().beginTransaction();
			transaction.add(R.id.preview_fragment_container, fragment);
			transaction.commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
