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

import android.app.Application;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class EpisodesApplication
	extends Application
{
	private static EpisodesApplication instance;

	@Override
	public void onCreate() {
		super.onCreate();

		instance = this;

		final ImageLoaderConfiguration imageLoaderConfig =
			new ImageLoaderConfiguration.Builder(this)
			.build();

		ImageLoader.getInstance().init(imageLoaderConfig);
	}
}
