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

package com.redcoracle.episodes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.util.Arrays;

public class SelectBackupDialog extends DialogFragment {
	public interface OnBackupSelectedListener {
		void onBackupSelected(String backupFilename);
	}
	private OnBackupSelectedListener onBackupSelectedListener;
	private Context context;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context = context;
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			onBackupSelectedListener = (OnBackupSelectedListener) activity;
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final File[] backups = getBackupFiles();

		if (backups != null && backups.length > 0) {
			return createDialogBackups(builder, backups);
		} else {
			return createDialogNoBackups(builder);
		}
	}

	private File[] getBackupFiles() {
		final File[] files = new File(this.context.getExternalFilesDir(null), "episodes").listFiles();

		if (files != null) {
			Arrays.sort(files, (lhs, rhs) -> Long.compare(rhs.lastModified(), lhs.lastModified()));
			return files;
		} else {
			return null;
		}
	}

	private Dialog createDialogBackups(AlertDialog.Builder builder, final File[] backups) {
		final String[] names = new String[backups.length];
		for (int i = 0; i < backups.length; i++) {
			names[i] = backups[i].getName();
		}

		builder.setTitle(R.string.restore_dialog_title)
			.setItems(names, (dialog, which) -> {
				final String path = backups[which].getPath();
				onBackupSelectedListener.onBackupSelected(path);
			});

		return builder.create();
	}

	private Dialog createDialogNoBackups(AlertDialog.Builder builder) {
		final File directory = new File(this.context.getExternalFilesDir(null), "episodes");
		final String message = getActivity().getString(R.string.restore_dialog_no_backups_message, directory);
		builder.setTitle(R.string.restore_dialog_title).setMessage(message);
		return builder.create();
	}
}
