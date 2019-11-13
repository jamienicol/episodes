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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.View;

public class WrapContentViewPager
	extends ViewPager
{
	public WrapContentViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final FragmentPagerAdapter adapter = (FragmentPagerAdapter)getAdapter();
		final Fragment fragment =
			(Fragment)getAdapter().instantiateItem(this, getCurrentItem());

		if (fragment != null) {
			final View view = fragment.getView();

			if (view != null) {
				view.measure(widthMeasureSpec,
				             MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

				final int height = Math.max(view.getMeasuredHeight(),
				                            getSuggestedMinimumHeight());
				heightMeasureSpec =
					MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
			}
		}

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	public int getMinimumHeightPlease() {
		// getMinimumHeight() is only API level >= 16, and
		// getSuggestedMinimumHeight() is protected.
		return getSuggestedMinimumHeight();
	}
}
