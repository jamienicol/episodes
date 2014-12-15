/*
 * Copyright (C) 2014 Jamie Nicol <jamie@thenicols.net>
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

package org.jamienicol.episodes.db;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.jamienicol.episodes.R;

public class BackUpRestoreHelper
{
	private final static String TAG = BackUpRestoreHelper.class.getName();

	public static void backUp(Context context) {
		final BackUpTask task = new BackUpTask(context);
		task.execute();
	}

	public static void restore(Context context, String filename) {
		final RestoreTask task = new RestoreTask(context);
		task.execute(filename);
	}

	public static File getBackupDir() {
		return new File(Environment.getExternalStorageDirectory(), "episodes");
	}

	private static class BackUpTask
		extends AsyncTask<Void, Void, Boolean>
	{
		private final Context context;
		private String destFilePath;

		public BackUpTask(Context context) {
			this.context = context;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			Log.i(TAG, "Backing up library.");

			final File srcFile =
				context.getDatabasePath(DatabaseOpenHelper.getDbName());
			if (!srcFile.canRead()) {
				Log.e(TAG, String.format("Cannot read database file: '%s'.",
				                         srcFile.getPath()));
				return false;
			}

			final File destDir = getBackupDir();
			destDir.mkdirs();
			if (!destDir.isDirectory()) {
				Log.e(TAG,
				      String.format("Error creating backup directory '%s'.",
				                    destDir.getPath()));
				return false;
			}

			final File destFile = new File(destDir, getBackupFilename());
			destFilePath = destFile.getPath();

			try {
				FileChannel src = new FileInputStream(srcFile).getChannel();
				FileChannel dest = new FileOutputStream(destFile).getChannel();

				dest.transferFrom(src, 0, src.size());

				Log.i(TAG, String.format("Library backed up successfully: '%s'.",
				                         destFile.getPath()));

				return true;

			} catch (IOException e) {
				Log.e(TAG, String.format("Error backing up library: %s",
				                         e.toString()));

				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				final String message =
					context.getString(R.string.back_up_success_message,
					                  destFilePath);
				Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context,
				               R.string.back_up_error_message,
				               Toast.LENGTH_SHORT).show();
			}
		}

		private static String getBackupFilename() {
			final Date today = new Date();
			final DateFormat sdf =
				new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US);

			return "episodes_" + sdf.format(today) + ".db";
		}
	}

	private static class RestoreTask
		extends AsyncTask<String, Void, Boolean>
	{
		private final Context context;

		public RestoreTask(Context context) {
			this.context = context;
		}

		@Override
		protected Boolean doInBackground(String... filename) {
			final File srcFile = new File(filename[0]);
			final File destFile =
				context.getDatabasePath(DatabaseOpenHelper.getDbName());

			try {
				FileChannel src = new FileInputStream(srcFile).getChannel();
				FileChannel dest = new FileOutputStream(destFile).getChannel();

				dest.transferFrom(src, 0, src.size());

				Log.i(TAG, String.format("Library restored successfully.",
				                         destFile.getPath()));

				return true;

			} catch (IOException e) {
				Log.e(TAG, String.format("Error restoring library: %s",
				                         e.toString()));
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				Toast.makeText(context,
				               R.string.restore_success_message,
				               Toast.LENGTH_SHORT).show();

				ShowsProvider.reloadDatabase(context);
			} else {
				Toast.makeText(context,
				               R.string.restore_error_message,
				               Toast.LENGTH_SHORT).show();
			}
		}
	}
}
