/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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

package org.jamienicol.episodes.tvdb;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import java.io.InputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

class GetShowParser
{
	private static final String TAG = "GetShowParser";

	// show which is being parsed
	Show show;

	// episode which is currently being parsed
	Episode episode;

	public Show parse(InputStream inputStream) {

		try {
			InputSource inputSource = new InputSource(inputStream);
			XMLReader xmlReader = XMLReaderFactory.createXMLReader("org.xmlpull.v1.sax2.Driver");

			RootElement rootElement = new RootElement("Data");

			Element seriesElement = rootElement.requireChild("Series");

			Element idElement = seriesElement.requireChild("id");
			idElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					int id = Integer.parseInt(body);

					Log.i(TAG, String.format("Parsed show ID: %d", id));
					show.setId(id);
				}
			});

			Element nameElement = seriesElement.requireChild("SeriesName");
			nameElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG, String.format("Parsed show name: %s", body));
					show.setName(body);
				}
			});

			Element languageElement = seriesElement.requireChild("Language");
			languageElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG, String.format("Parsed language: %s", body));
					show.setLanguage(body);
				}
			});

			Element overviewElement = seriesElement.getChild("Overview");
			overviewElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG,
					      String.format("Parsed show overview: %s", body));
					show.setOverview(body);
				}
			});

			Element firstAiredElement = seriesElement.getChild("FirstAired");
			firstAiredElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					try {
						DateFormat df = new SimpleDateFormat("yyyy-MM-dd",
						                                     Locale.US);
						Date firstAired = df.parse(body);

						Log.i(TAG,
						      String.format("Parsed show first aired date: %s",
						                    firstAired.toString()));
						show.setFirstAired(firstAired);

					} catch (ParseException e) {
						Log.w(TAG, "Error parsing first aired date: " + e.toString());
						show.setFirstAired(null);
					}
				}
			});

			Element bannerPathElement = seriesElement.getChild("banner");
			bannerPathElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG,
					      String.format("Parsed show banner path: %s", body));
					show.setBannerPath(body);
				}
			});

			final Element fanartPathElement = seriesElement.getChild("fanart");
			fanartPathElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG,
					      String.format("Parsed show fanart path: %s", body));
					show.setFanartPath(body);
				}
			});

			final Element posterPathElement = seriesElement.getChild("poster");
			posterPathElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG,
					      String.format("Parsed show poster path: %s", body));
					show.setPosterPath(body);
				}
			});

			Element episodeElement = rootElement.getChild("Episode");
			episodeElement.setStartElementListener(new StartElementListener() {
				public void start(Attributes attributes) {
					Log.i(TAG, "Begin parsing episode");
					episode = new Episode();
				}
			});
			episodeElement.setEndElementListener(new EndElementListener() {
				public void end() {
					Log.i(TAG, "End parsing episode");
					show.getEpisodes().add(episode);
					episode = null;
				}
			});

			Element episodeIdElement = episodeElement.requireChild("id");
			episodeIdElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					int id = Integer.parseInt(body);

					Log.i(TAG, String.format("Parsed episode ID: %d", id));
					episode.setId(id);
				}
			});

			Element episodeNameElement = episodeElement.getChild("EpisodeName");
			episodeNameElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG, String.format("Parsed episode name: %s", body));
					episode.setName(body);
				}
			});

			Element episodeOverviewElement = episodeElement.getChild("Overview");
			episodeOverviewElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG,
					      String.format("Parsed episode overview: %s", body));
					episode.setOverview(body);
				}
			});

			Element episodeEpisodeNumberElement = episodeElement.getChild("EpisodeNumber");
			episodeEpisodeNumberElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					int episodeNumber = Integer.parseInt(body);

					Log.i(TAG,
					      String.format("Parsed episode episode number: %d",
					                    episodeNumber));
					episode.setEpisodeNumber(episodeNumber);
				}
			});

			Element episodeSeasonNumberElement = episodeElement.getChild("SeasonNumber");
			episodeSeasonNumberElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					int seasonNumber = Integer.parseInt(body);

					Log.i(TAG,
					      String.format("Parsed episode season number: %d",
					                    seasonNumber));
					episode.setSeasonNumber(seasonNumber);
				}
			});

			Element episodeFirstAiredElement = episodeElement.getChild("FirstAired");
			episodeFirstAiredElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					try {
						DateFormat df = new SimpleDateFormat("yyyy-MM-dd",
						                                     Locale.US);
						Date firstAired = df.parse(body);

						Log.i(TAG,
						      String.format("Parsed episode first aired date: %s",
						                    firstAired.toString()));
						episode.setFirstAired(firstAired);

					} catch (ParseException e) {
						Log.w(TAG, "Error parsing first aired date: " + e.toString());
						episode.setFirstAired(null);
					}
				}
			});

			xmlReader.setContentHandler(rootElement.getContentHandler());

			show = new Show();
			show.setEpisodes(new LinkedList<Episode>());
			xmlReader.parse(inputSource);

			return show;

		} catch (SAXException e) {
			Log.w(TAG, "SAXException - parse: " + e.toString());
			return null;
		} catch (IOException e) {
			Log.w(TAG, "IOException - parse: " + e.toString());
			return null;
		}
	}
}
