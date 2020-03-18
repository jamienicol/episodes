/*
 * Copyright (C) 2015 Jamie Nicol <jamie@thenicols.net>
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

package com.redcoracle.episodes.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class WrapContentListView
	extends ListView
{
	public WrapContentListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int height = 0;

		final ListAdapter adapter = getAdapter();
		if (adapter != null) {
			for (int i = 0; i < adapter.getCount(); i++) {
				View item = adapter.getView(i, null, this);

				if (item != null) {
					final int unspecified =
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
					item.measure(widthMeasureSpec, unspecified);

					height += item.getMeasuredHeight();
					height += getDividerHeight();
				}
			}
		}

		heightMeasureSpec = MeasureSpec.makeMeasureSpec(height,
		                                                MeasureSpec.EXACTLY);

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
