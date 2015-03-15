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


import org.jamienicol.episodes.db.ShowsProvider;
import org.jamienicol.episodes.db.ShowsTable;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class ShowNotesFragment
	extends Fragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private int showId;
	private EditText notesView;

	public static ShowNotesFragment newInstance(int showId) {
		ShowNotesFragment instance = new ShowNotesFragment();

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
		View view = inflater.inflate(R.layout.show_notes_fragment,
		                             container,
		                             false);

		notesView = (EditText)view.findViewById(R.id.notes);

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
	public void onPause() {
		super.onPause();

		saveNotes();
	}

	private void saveNotes() {
		final ContentResolver contentResolver = getActivity().getContentResolver();
		final AsyncQueryHandler handler =
			new AsyncQueryHandler(contentResolver) {};
		final ContentValues values = new ContentValues();
		values.put(ShowsTable.COLUMN_NOTES, notesView.getText().toString());
		final String selection = String.format("%s=?", ShowsTable.COLUMN_ID);
		final String[] selectionArgs = {
			String.valueOf(showId)
		};

		handler.startUpdate(0,
			null,
			ShowsProvider.CONTENT_URI_SHOWS,
			values,
			selection,
			selectionArgs);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int showId = args.getInt("showId");
		Uri uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
		                               String.valueOf(showId));
		String[] projection = {
			ShowsTable.COLUMN_NOTES
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
				data.getColumnIndexOrThrow(ShowsTable.COLUMN_NOTES);
			if (data.isNull(overviewColumnIndex)) {
				notesView.setText("");
			} else {
				notesView.setText(data.getString(overviewColumnIndex));
			}

		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		onLoadFinished(loader, null);
	}
}
