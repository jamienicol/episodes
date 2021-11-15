/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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

package com.redcoracle.episodes;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.redcoracle.episodes.db.DatabaseOpenHelper;
import com.redcoracle.episodes.db.ShowsProvider;
import com.redcoracle.episodes.services.AsyncTask;
import com.redcoracle.episodes.services.BackupTask;
import com.redcoracle.episodes.services.RestoreTask;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class MainActivity
    extends AppCompatActivity
    implements ShowsListFragment.OnShowSelectedListener,
               SelectBackupDialog.OnBackupSelectedListener,
               ActivityCompat.OnRequestPermissionsResultCallback {

    private static Context context;
    private static final int WRITE_REQUEST_CODE = 0;
    private static final int READ_REQUEST_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        MainActivity.context = getApplicationContext();
        AutoRefreshHelper.getInstance(getApplicationContext()).rescheduleAlarm();
    }

	public static Context getAppContext() {
		return MainActivity.context;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		final MenuItem menuItem = menu.findItem(R.id.menu_add_new_show);
		final SearchView addShow =
			(SearchView)MenuItemCompat.getActionView(menuItem);
		addShow.setQueryHint(getString(R.string.menu_add_show_search_hint));
		addShow.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				@Override
				public boolean onQueryTextChange(String query) {
					return true;
				}

				@Override
				public boolean onQueryTextSubmit(String query) {
					final Intent intent =
						new Intent(MainActivity.this,
						           AddShowSearchActivity.class);
				intent.putExtra("query", query);
				startActivity(intent);
				MenuItemCompat.collapseActionView(menuItem);
				return true;
				}
			});

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_back_up:
			back_up();
			return true;

		case R.id.menu_restore:
			restore();
			return true;

		case R.id.menu_settings:
			showSettings();
			return true;

		case R.id.menu_about:
			showAbout();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onShowSelected(int showId) {
		final Intent intent = new Intent(this, ShowActivity.class);
		intent.putExtra("showId", showId);
		startActivity(intent);
	}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void back_up() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/x-sqlite3");
            intent.putExtra(Intent.EXTRA_TITLE, FileUtilities.get_suggested_filename());
            startActivityForResult(intent, WRITE_REQUEST_CODE);
        } else {
            // For now, keep the existing functionality on pre-API19
            if (hasStoragePermission()) {
                new AsyncTask().executeAsync(new BackupTask(FileUtilities.get_suggested_filename()));
            }
        }
    }

    private void restore() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/x-sqlite3");
            // On API 31 the file was not selectable without this
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"application/octet-stream"});
            startActivityForResult(intent, READ_REQUEST_CODE);
        } else {
            // For now, keep the existing functionality on pre-API19
            if (hasStoragePermission()) {
                final FragmentManager fm = getSupportFragmentManager();
                final SelectBackupDialog dialog = new SelectBackupDialog();
                dialog.show(fm, "select_backup_dialog");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                FileUtilities.copy_file(
                    new FileInputStream(this.getDatabasePath(DatabaseOpenHelper.getDbName())).getChannel(),
                    new FileOutputStream(getContentResolver().openFileDescriptor(uri, "w").getFileDescriptor()).getChannel()
                );
                Toast.makeText(
                    this,
                    String.format(this.getString(R.string.back_up_success_message), FileUtilities.uri_to_filename(this, uri)),
                    Toast.LENGTH_LONG
                ).show();
            } else if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                FileUtilities.copy_file(
                    new FileInputStream(getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor()).getChannel(),
                    new FileOutputStream(this.getDatabasePath(DatabaseOpenHelper.getDbName())).getChannel()
                );
                ShowsProvider.reloadDatabase(this);
                android.os.AsyncTask.execute(() -> Glide.get(getApplicationContext()).clearDiskCache());
                Toast.makeText(this, this.getString(R.string.restore_success_message), Toast.LENGTH_LONG).show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

	@Override
	public void onBackupSelected(String backupFilename) {
		new AsyncTask().executeAsync(new RestoreTask(backupFilename));
	}

	private void showSettings() {
		final Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	private void showAbout() {
		final Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}
}
