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

class SearchShowsParser
{
	private static final String TAG = "SearchShowsParser";

	// show which is currently being parsed
	Show current;

	// shows which have finished being parsed
	List<Show> parsed;

	public List<Show> parse(InputStream inputStream) {

		try {
			InputSource inputSource = new InputSource(inputStream);
			XMLReader xmlReader = XMLReaderFactory.createXMLReader("org.xmlpull.v1.sax2.Driver");

			RootElement rootElement = new RootElement("Data");
			Element seriesElement = rootElement.getChild("Series");
			seriesElement.setStartElementListener(new StartElementListener() {
				public void start(Attributes attributes) {
					Log.i(TAG, "Begin parsing show");
					current = new Show();
				}
			});
			seriesElement.setEndElementListener(new EndElementListener() {
				public void end() {
					Log.i(TAG, "End parsing show");
					parsed.add(current);
					current = null;
				}
			});

			Element idElement = seriesElement.requireChild("id");
			idElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					int id = Integer.parseInt(body);

					Log.i(TAG, String.format("Parsed ID: %d", id));
					current.setId(id);
				}
			});

			Element nameElement = seriesElement.requireChild("SeriesName");
			nameElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG, String.format("Parsed name: %s", body));
					current.setName(body);
				}
			});

			Element languageElement = seriesElement.requireChild("language");
			languageElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG, String.format("Parsed language: %s", body));
					current.setLanguage(body);
				}
			});

			Element overviewElement = seriesElement.getChild("Overview");
			overviewElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					Log.i(TAG, String.format("Parsed overview: %s", body));
					current.setOverview(body);
				}
			});

			Element firstAiredElement = seriesElement.getChild("FirstAired");
			firstAiredElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					try {
						DateFormat df = new SimpleDateFormat("yyyy-MM-dd",
						                                     Locale.US);
						Date firstAired = df.parse(body);

						Log.i(TAG, String.format("Parsed first aired date: %s",
						                         firstAired.toString()));
						current.setFirstAired(firstAired);

					} catch (ParseException e) {
						Log.w(TAG, "Error parsing first aired date: " + e.toString());
						current.setFirstAired(null);
					}
				}
			});

			xmlReader.setContentHandler(rootElement.getContentHandler());

			current = null;
			parsed = new LinkedList<Show>();
			xmlReader.parse(inputSource);

			return parsed;

		} catch (SAXException e) {
			Log.w(TAG, "SAXException - parse: " + e.toString());
			return null;
		} catch (IOException e) {
			Log.w(TAG, "IOException - parse: " + e.toString());
			return null;
		}
	}
}
