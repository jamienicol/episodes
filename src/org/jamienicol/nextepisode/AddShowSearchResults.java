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

package org.jamienicol.nextepisode;

import java.util.List;
import org.jamienicol.nextepisode.tvdb.SearchResult;

public class AddShowSearchResults
{
	// singleton instance
	private static AddShowSearchResults instance = new AddShowSearchResults();

	private List<SearchResult> data;

	private AddShowSearchResults() {
	}

	public static AddShowSearchResults getInstance() {
		return instance;
	}

	public List<SearchResult> getData() {
		return data;
	}

	public void setData(List<SearchResult> data) {
		this.data = data;
	}
}
