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

package com.redcoracle.episodes.tvdb;

import java.util.Date;

public class Episode {
	private int id;
	private Integer tvdbId;
	private Integer tmdbId;
	private String imdbId;
	private String name;
	private String language;
	private String overview;
	private int episodeNumber;
	private int seasonNumber;
	private Date firstAired;

	Episode() {
	}

	public String identifier() {
		return String.format("%s-%s", this.seasonNumber, this.episodeNumber);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Integer getTvdbId() {
		return this.tvdbId;
	}

	public void setTvdbId(Integer id) {
		this.tvdbId = id;
	}

	public int getTmdbId() {
		return this.tmdbId;
	}

	public void setTmdbId(int id) {
		this.tmdbId = id;
	}

	public String getImdbId() {
		return this.imdbId;
	}

	public void setImdbId(String id) {
		this.imdbId = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String overview) {
		this.overview = overview;
	}

	public int getEpisodeNumber() {
		return episodeNumber;
	}

	void setEpisodeNumber(int episodeNumber) {
		this.episodeNumber = episodeNumber;
	}

	public int getSeasonNumber() {
		return seasonNumber;
	}

	void setSeasonNumber(int seasonNumber) {
		this.seasonNumber = seasonNumber;
	}

	public Date getFirstAired() {
		return firstAired;
	}

	void setFirstAired(Date firstAired) {
		this.firstAired = firstAired;
	}
}
