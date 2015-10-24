/*
 * Copyright (C) 2013 Jamie Nicol <jamie@thenicols.net>
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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.db.ShowsTable;

public class ShowDetailsFragment
	extends Fragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private int showId;
	private TextView overviewView;
	private TextView firstAiredView;

	public static ShowDetailsFragment newInstance(int showId) {
		ShowDetailsFragment instance = new ShowDetailsFragment();

		Bundle args = new Bundle();
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
		View view = inflater.inflate(R.layout.show_details_fragment,
		                             container,
		                             false);

		overviewView = (TextView)view.findViewById(R.id.overview);
		firstAiredView = (TextView)view.findViewById(R.id.first_aired);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int showId = args.getInt("showId");
		Uri uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
		                               String.valueOf(showId));
		String[] projection = {
			ShowsTable.COLUMN_OVERVIEW,
			ShowsTable.COLUMN_FIRST_AIRED
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
		if (data != null && data.moveToFirst()) {

			int overviewColumnIndex =
				data.getColumnIndexOrThrow(ShowsTable.COLUMN_OVERVIEW);
			if (data.isNull(overviewColumnIndex)) {
				overviewView.setVisibility(View.GONE);
			} else {
				overviewView.setText(data.getString(overviewColumnIndex).trim());
				overviewView.setVisibility(View.VISIBLE);
			}

			int firstAiredColumnIndex =
				data.getColumnIndexOrThrow(ShowsTable.COLUMN_FIRST_AIRED);
			if (data.isNull(firstAiredColumnIndex)) {
				firstAiredView.setVisibility(View.GONE);
			} else {
				Date firstAired =
					new Date(data.getLong(firstAiredColumnIndex) * 1000);
				DateFormat df = DateFormat.getDateInstance();
				String firstAiredText = getString(R.string.first_aired,
				                                  df.format(firstAired));
				firstAiredView.setText(firstAiredText);
				firstAiredView.setVisibility(View.VISIBLE);
			}

		} else {
			overviewView.setVisibility(View.GONE);
			firstAiredView.setVisibility(View.GONE);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		onLoadFinished(loader, null);
	}
}
