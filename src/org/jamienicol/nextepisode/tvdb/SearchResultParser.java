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

package org.jamienicol.nextepisode.tvdb;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import java.io.InputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

class SearchResultParser
{
	private static final String TAG = "SearchResultParser";

	// search result which is currently being parsed
	SearchResult current;

	// search results which have finished being parsed
	List<SearchResult> parsed;

	public List<SearchResult> parse(InputStream inputStream) {

		try {
			InputSource inputSource = new InputSource(inputStream);
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();

			RootElement rootElement = new RootElement("Data");
			Element seriesElement = rootElement.getChild("Series");
			seriesElement.setStartElementListener(new StartElementListener() {
				public void start(Attributes attributes) {
					current = new SearchResult();
				}
			});
			seriesElement.setEndElementListener(new EndElementListener() {
				public void end() {
					parsed.add(current);
					current = null;
				}
			});

			Element idElement = seriesElement.requireChild("seriesid");
			idElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					current.setId(Integer.parseInt(body));
				}
			});

			Element nameElement = seriesElement.requireChild("SeriesName");
			nameElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					current.setName(body);
				}
			});

			Element overviewElement = seriesElement.getChild("Overview");
			overviewElement.setEndTextElementListener(new EndTextElementListener() {
				public void end(String body) {
					current.setOverview(body);
				}
			});

			xmlReader.setContentHandler(rootElement.getContentHandler());

			current = null;
			parsed = new LinkedList<SearchResult>();
			xmlReader.parse(inputSource);

			return parsed;

		} catch (ParserConfigurationException e) {
			Log.w(TAG, "ParserConfigurationException - parse: " + e.toString());
			return null;
		} catch (SAXException e) {
			Log.w(TAG, "SAXException - parse: " + e.toString());
			return null;
		} catch (IOException e) {
			Log.w(TAG, "IOException - parse: " + e.toString());
			return null;
		}
	}
}
