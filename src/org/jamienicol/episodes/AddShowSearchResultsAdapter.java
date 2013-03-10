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

import android.content.Context;
import java.util.List;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import org.jamienicol.episodes.tvdb.Show;

public class AddShowSearchResultsAdapter extends ArrayAdapter<Show>
{
	private LayoutInflater inflater;

	public AddShowSearchResultsAdapter(Context context,
	                                   List<Show> objects) {
		super(context, 0, 0, objects);

		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView =
				inflater.inflate(R.layout.add_show_search_results_list_item,
				                 parent,
				                 false);
		}

		TextView textView =
			(TextView)convertView.findViewById(R.id.show_name_view);
		textView.setText(getItem(position).getName());

		return convertView;
	}
}
