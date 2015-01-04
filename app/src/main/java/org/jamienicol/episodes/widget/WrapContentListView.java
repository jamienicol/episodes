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

package org.jamienicol.episodes.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class WrapContentListView
	extends ListView
{
	public WrapContentListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int maxHeightSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE,
		                                                MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, maxHeightSpec);
	}
}
