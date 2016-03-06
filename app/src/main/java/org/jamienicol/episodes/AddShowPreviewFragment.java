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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import org.jamienicol.episodes.services.AddShowService;
import org.jamienicol.episodes.tvdb.Show;

public class AddShowPreviewFragment
	extends Fragment
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
				String text = getString(R.string.first_aired,
				                        df.format(show.getFirstAired()));
				firstAiredView.setText(text);
			} else {
				firstAiredView.setText("");
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
		Intent intent = new Intent(getActivity(), AddShowService.class);
		intent.putExtra("tvdbId", show.getId());
		intent.putExtra("language", show.getLanguage());
		intent.putExtra("showName", show.getName());

		getActivity().startService(intent);
	}
}
