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

package org.jamienicol.episodes;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.jamienicol.episodes.db.BackUpRestoreHelper;

public class MainActivity
    extends AppCompatActivity
    implements ShowsListFragment.OnShowSelectedListener,
               SelectBackupDialog.OnBackupSelectedListener,
               ActivityCompat.OnRequestPermissionsResultCallback
{
	private static Context context;
	private static final int PERMISSION_REQUEST_CODE = 0;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		MainActivity.context = getApplicationContext();

		// ensure that the auto-refresh alarm is scheduled.
		// this should mainly be useful the first time the app is ran.
		AutoRefreshHelper.getInstance(getApplicationContext())
			.rescheduleAlarm();
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

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
	    String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
	    ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private void back_up() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkStoragePermission()) {
                requestStoragePermission();
            }
        }

        BackUpRestoreHelper.backUp(getApplicationContext());
    }

    private void restore() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkStoragePermission()) {
                requestStoragePermission();
            }
        }

        final FragmentManager fm = getSupportFragmentManager();
        final SelectBackupDialog dialog = new SelectBackupDialog();
        dialog.show(fm, "select_backup_dialog");
    }

	@Override
	public void onBackupSelected(String backupFilename) {
		BackUpRestoreHelper.restore(getApplicationContext(), backupFilename);
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
