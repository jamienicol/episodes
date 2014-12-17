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

package org.jamienicol.episodes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import org.jamienicol.episodes.db.BackUpRestoreHelper;

public class SelectBackupDialog
	extends DialogFragment
{
	public interface OnBackupSelectedListener {
		public void onBackupSelected(String backupFilename);
	}
	private OnBackupSelectedListener onBackupSelectedListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			onBackupSelectedListener = (OnBackupSelectedListener)activity;
		} catch (ClassCastException e) {
			final String message =
				String.format("%s must implement OnBackupSelectedListener",
				              activity.toString());
			throw new ClassCastException(message);
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog.Builder builder =
			new AlertDialog.Builder(getActivity());

		final File[] backups = getBackupFiles();

		if (backups != null && backups.length > 0) {
			return createDialogBackups(builder, backups);
		} else {
			return createDialogNoBackups(builder);
		}
	}

	private File[] getBackupFiles() {
		final File[] files = BackUpRestoreHelper.getBackupDir().listFiles();

		if (files != null) {
			Arrays.sort(files, new Comparator<File>() {
				public int compare(File lhs, File rhs) {
					return Long.valueOf(rhs.lastModified()).
						compareTo(lhs.lastModified());
				}
			});
			return files;
		} else {
			return null;
		}
	}

	private Dialog createDialogBackups(AlertDialog.Builder builder,
	                                   final File[] backups) {
		final String[] names = new String[backups.length];
		for (int i = 0; i < backups.length; i++) {
			names[i] = backups[i].getName();
		}

		builder.setTitle(R.string.restore_dialog_title)
			.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					final String path = backups[which].getPath();
					onBackupSelectedListener.onBackupSelected(path);
				}
			});

		return builder.create();
	}

	private Dialog createDialogNoBackups(AlertDialog.Builder builder) {
		final String message =
			getActivity().getString(R.string.restore_dialog_no_backups_message,
			                        BackUpRestoreHelper.getBackupDir());

		builder.setTitle(R.string.restore_dialog_title)
			.setMessage(message);

		return builder.create();
	}
}
