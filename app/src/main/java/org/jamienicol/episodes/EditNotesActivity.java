/*
 * Copyright (C) 2015 Daniele Ricci <daniele.athome@gmail.com>
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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class EditNotesActivity
    extends AppCompatActivity
    implements LoaderManager.LoaderCallbacks<Cursor>
{
    private int showId;

    private EditText notesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_notes_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Intent intent = getIntent();
        showId = intent.getIntExtra("showId", -1);
        if (showId == -1) {
            throw new IllegalArgumentException("must provide valid showId");
        }

        notesView = (EditText) findViewById(R.id.notes_edit);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Bundle loaderArgs = new Bundle();
        loaderArgs.putInt("showId", showId);
        getSupportLoaderManager().initLoader(0, loaderArgs, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_notes_activity, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_save_notes:
                saveNotes();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_down);
    }

    private void saveNotes() {
        notesView.setEnabled(false);

        final ContentResolver contentResolver = getContentResolver();
        final AsyncQueryHandler handler =
            new AsyncQueryHandler(contentResolver) {
                @Override
                protected void onUpdateComplete(int token, Object cookie, int result) {
                    if (result > 0) {
                        onBackPressed();
                    }
                    else {
                        notesView.setEnabled(true);
                        Toast.makeText(EditNotesActivity.this,
                            R.string.error_saving_notes,
                            Toast.LENGTH_LONG).show();
                    }
                }


            };
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
        // disable editor
        notesView.setEnabled(false);

        final int showId = args.getInt("showId");
        final Uri uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS,
            String.valueOf(showId));
        final String[] projection = {
            ShowsTable.COLUMN_NOTES,
            ShowsTable.COLUMN_NAME,
        };
        return new CursorLoader(this,
            uri,
            projection,
            null,
            null,
            null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {

            final int nameColumnIndex =
                data.getColumnIndexOrThrow(ShowsTable.COLUMN_NAME);
            if (data.isNull(nameColumnIndex)) {
                setTitle("");
            } else {
                final String text = data.getString(nameColumnIndex);
                setTitle(text);
            }

            final int notesColumnIndex =
                data.getColumnIndexOrThrow(ShowsTable.COLUMN_NOTES);
            if (data.isNull(notesColumnIndex)) {
                notesView.setText("");
            } else {
                final String text = data.getString(notesColumnIndex);
                notesView.setText(text);
            }

            // enable editor
            notesView.setEnabled(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        onLoadFinished(loader, null);
    }

}
