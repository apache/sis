/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sis.index.tree;

//JDK imports
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

//SIS imports
import org.apache.sis.geometry.DirectPosition2D;

//ROME imports
import com.sun.syndication.feed.module.georss.GeoRSSModule;
import com.sun.syndication.feed.rss.Item;

/**
 * Implements QuadTreeData to store GeoRSS items into quad tree. Provides
 * methods to save and load GeoRSS items to and from file.
 *
 */
public class GeoRSSData implements QuadTreeData {

    private String filename;
    private DirectPosition2D latLon;

    /**
     * Creates a GeoRSSData object that stores the name of the file that the
     * entry's information is written to and the geo location of the entry.
     *
     * @param filename
     *            filename where rss entry's info is stored
     * @param latLon
     *            geo location of the entry
     */
    public GeoRSSData(String filename, DirectPosition2D latLon) {
        this.filename = filename;
        this.latLon = latLon;
    }

    /**
     * Returns the Java 2D x-coordinate for the longitude.
     *
     * @return the Java 2D x-coordinate
     */
    public double getX() {
        return latLon.x + 180.0;
    }

    /**
     * Returns the Java 2D y-coordinate for the latitude.
     *
     * @return the Java 2D y-coordinate
     */
    public double getY() {
        return latLon.y + 90.0;
    }


    /* (non-Javadoc)
    * @see org.apache.sis.storage.QuadTreeData#getLatLon()
    */
    @Override
    public DirectPosition2D getLatLon() {
    return this.latLon;
    }

    /**
     * Returns the name of the file where the entry's info is saved.
     *
     * @return the name of the file where the entry's info is saved
     */
    public String getFileName() {
        return this.filename;
    }

    /**
     * Saves the GeoRSS entry to file.
     *
     * @param item
     *            the Item object from Java ROME API containing the GeoRSS entry
     * @param geoRSSModule
     *            the Java ROME API GeoRSSModule to parse geo location
     * @param directory
     *            the path of the directory in which to save the file
     */
    public void saveToFile(Item item, GeoRSSModule geoRSSModule,
            String directory) {
      if(!new File(directory).exists()) new File(directory).mkdirs();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(directory
                    + filename));
            if (item.getTitle() != null) {
                writer.write("title;" + item.getTitle().replace('\n', ' '));
                writer.newLine();
            }
            if (item.getLink() != null) {
                writer.write("link;" + item.getLink().replace('\n', ' '));
                writer.newLine();
            }
            if (item.getSource() != null) {
                writer.write("source;"
                        + item.getSource().getValue().replace('\n', ' '));
                writer.newLine();
            }
            if (item.getAuthor() != null) {
                writer.write("author;" + item.getAuthor().replace('\n', ' '));
                writer.newLine();
            }
            if (item.getDescription() != null) {
                writer.write("description;"
                        + item.getDescription().getValue().replace('\n', ' '));
                writer.newLine();
            }
            writer.write("pubDate;" + item.getPubDate().toString());
            writer.newLine();
            writer.write("lat;" + geoRSSModule.getPosition().getLatitude());
            writer.newLine();
            writer.write("lon;" + geoRSSModule.getPosition().getLongitude());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the file that contains the GeoRSS entry's information and returns a
     * HashMap of key, value pairs where the key is the name of the element e.g.
     * title, author.
     *
     * @param fullFileName
     *            the full path to the file
     * @return HashMap where the key is the name of the element and the value is
     *         the data inside the element's tag
     */
    public static HashMap<String, String> loadFromFile(String fullFileName) {
        HashMap<String, String> map = new HashMap<String, String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(
                    fullFileName));
            String line = "";
            while ((line = reader.readLine()) != null) {
                int delimIndex = line.indexOf(';');
                if (delimIndex != -1)
                    map.put(line.substring(0, delimIndex), line.substring(
                            delimIndex + 1, line.length()));
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;

    }

}
