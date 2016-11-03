/*
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
package org.apache.sis.internal.gpx;

import com.esri.core.geometry.Point;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.opengis.geometry.Envelope;
import org.opengis.feature.Feature;

import javax.xml.stream.XMLStreamReader;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.xml.StaxStreamReader;

import static javax.xml.stream.XMLStreamReader.*;
import static org.apache.sis.internal.gpx.GPXConstants.*;


/**
 * Stax reader class for GPX 1.0 and 1.1 files.
 *
 * Usage :<br>
 * <pre>
 * {@code
 * final GPXReader reader = new GPXReader();
 * reader.setInput(gpxInput);
 *
 * final GPXVersion version = reader.getVersion();
 * final Metadata metadata = reader.getMetadata();
 *
 * while(reader.hasNext()) {
 *     Feature feature = reader.next();
 * }
 *
 * }
 * </pre>
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
public class GPXReader extends StaxStreamReader {

    private MetaData metadata;
    private Feature current;
    private int wayPointInc = 0;
    private int routeInc = 0;
    private int trackInc = 0;
    private GPXVersion version = null;
    private String baseNamespace = GPX_NAMESPACE_V11;

    /**
     * {@inheritDoc }
     *
     * @param input input object
     * @throws IOException if input failed to be opened for any IO reason
     * @throws XMLStreamException if input is not a valid XML stream
     */
    public GPXReader(final Object input, final StorageConnector storage) throws DataStoreException, IOException, XMLStreamException {
        super(input, storage);
        final XMLStreamReader reader = getReader();

        //search for the bound tag to generate the envelope
        searchLoop :
        while(reader.hasNext()){
            final int type = reader.next();

            switch (type) {
                case START_ELEMENT:
                    final String typeName = reader.getLocalName();
                    if(TAG_GPX.equalsIgnoreCase(typeName)){

                        String str = "1.1"; //consider 1.1 by default
                        for(int i=0,n=reader.getAttributeCount(); i<n;i++){
                            if(ATT_GPX_VERSION.equalsIgnoreCase(reader.getAttributeLocalName(i))){
                                str = reader.getAttributeValue(i);
                            }
                        }

                        try{
                            this.version = GPXVersion.toVersion(str);
                        }catch(NumberFormatException ex){
                            throw new XMLStreamException(ex);
                        }

                        if(version == GPXVersion.v1_0_0){
                            baseNamespace = GPX_NAMESPACE_V10;
                            //we wont found a metadata tag, must read the tags here.
                            metadata = parseMetaData100();
                            break searchLoop;
                        }else{
                            baseNamespace = GPX_NAMESPACE_V11;
                        }

                    }else if(TAG_METADATA.equalsIgnoreCase(typeName)){
                        metadata = parseMetaData110();
                        break searchLoop;
                    }else if(  TAG_WPT.equalsIgnoreCase(typeName)
                            || TAG_TRK.equalsIgnoreCase(typeName)
                            || TAG_RTE.equalsIgnoreCase(typeName)){
                        //there is no metadata tag
                        break searchLoop;
                    }
            }
        }

    }

    /**
     * Get GPX file version.
     * This method will return a result only if called only after the input has been set.
     *
     * @return GPXVersion or null if input is not set.
     */
    public GPXVersion getVersion() {
        return version;
    }

    /**
     * Get GPX metadata.
     * This method will return a result only if called only after the input has been set.
     *
     * @return Metadata or null if input is not set.
     */
    public MetaData getMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() throws IOException, XMLStreamException {
        super.close();
        metadata = null;
        current = null;
        wayPointInc = 0;
        routeInc = 0;
        trackInc = 0;
    }

    /**
     * Returns true if there is a next feature in the stream.
     *
     * @return true if there is next feature
     * @throws javax.xml.stream.XMLStreamException if xml parser encounter an invalid
     *                          element or underlying stream caused an exception
     */
    public boolean hasNext() throws IOException, XMLStreamException {
        findNext();
        return current != null;
    }

    /**
     * Get next feature in the stream.
     *
     * @return GPX WayPoint, Route or track
     * @throws javax.xml.stream.XMLStreamException if xml parser encounter an invalid
     *                          element or underlying stream caused an exception
     */
    public Feature next() throws IOException, XMLStreamException {
        findNext();
        final Feature ele = current;
        current = null;
        return ele;
    }

    /**
     * Search for the next feature in the stax stream.
     * This method will set the current local property if there is one.
     */
    private void findNext() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        if(current != null) return;

        boolean first = true;
        while ( first || (current == null && reader.hasNext()) ) {
            final int type;
            if(first){
                type = reader.getEventType();
                first = false;
            }else{
                type = reader.next();
            }

            if(type == START_ELEMENT) {
                final String localName = reader.getLocalName();
                if(TAG_WPT.equalsIgnoreCase(localName)){
                    current = parseWayPoint(wayPointInc++);
                    break;
                }else if(TAG_RTE.equalsIgnoreCase(localName)){
                    current = parseRoute(routeInc++);
                    break;
                }else if(TAG_TRK.equalsIgnoreCase(localName)){
                    current = parseTrack(trackInc++);
                    break;
                }
            }
        }

    }

    /**
     * Parse current metadata element.
     * The stax reader must be placed to the start element of the metadata.
     *
     * @return MetaData
     */
    private MetaData parseMetaData100() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();

        final MetaData metadata = new MetaData();

        searchLoop:
        while (reader.hasNext()) {
            final int type = reader.next();

            switch (type) {
                case START_ELEMENT:
                    final String localName = reader.getLocalName();
                    if(TAG_NAME.equalsIgnoreCase(localName)){
                        metadata.name = reader.getElementText();
                    }else if(TAG_DESC.equalsIgnoreCase(localName)) {
                        metadata.description = reader.getElementText();
                    }else if(TAG_AUTHOR.equalsIgnoreCase(localName)) {
                        if(metadata.person == null) metadata.person = new Person();
                        metadata.person.name = reader.getElementText();
                    }else if(TAG_AUTHOR_EMAIL.equalsIgnoreCase(localName)) {
                        if(metadata.person == null) metadata.person = new Person();
                        metadata.person.email = reader.getElementText();
                    }else if(TAG_URL.equalsIgnoreCase(localName)){
                        try {
                            metadata.links.add(new URI(reader.getElementText()));
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }else if(TAG_URLNAME.equalsIgnoreCase(localName)){
                        //reader.getElementText();
                    }else if(TAG_METADATA_TIME.equalsIgnoreCase(localName)){
                        metadata.time = parseTime(reader.getElementText());
                    }else if(TAG_METADATA_KEYWORDS.equalsIgnoreCase(localName)){
                        metadata.keywords = reader.getElementText();
                    }else if(TAG_BOUNDS.equalsIgnoreCase(localName)){
                        metadata.bounds = parseBound();
                    }else if(  TAG_WPT.equalsIgnoreCase(localName)
                            || TAG_TRK.equalsIgnoreCase(localName)
                            || TAG_RTE.equalsIgnoreCase(localName)){
                        //there is no more metadata tags
                        break searchLoop;
                    }
                    break;
            }
        }

        return metadata;
    }

    /**
     * Parse current metadata element.
     * The stax reader must be placed to the start element of the metadata.
     *
     * @return MetaData
     */
    private MetaData parseMetaData110() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final MetaData metadata = new MetaData();

        while (reader.hasNext()) {
            final int type = reader.next();

            switch (type) {
                case START_ELEMENT:
                    final String localName = reader.getLocalName();
                    if(TAG_NAME.equalsIgnoreCase(localName)){
                        metadata.name = reader.getElementText();
                    }else if(TAG_DESC.equalsIgnoreCase(localName)){
                        metadata.description = reader.getElementText();
                    }else if(TAG_AUTHOR.equalsIgnoreCase(localName)){
                        metadata.person = parsePerson();
                    }else if(TAG_COPYRIGHT.equalsIgnoreCase(localName)){
                        metadata.copyRight = parseCopyright();
                    }else if(TAG_LINK.equalsIgnoreCase(localName)){
                        metadata.links.add(parseLink());
                    }else if(TAG_METADATA_TIME.equalsIgnoreCase(localName)){
                        metadata.time = parseTime(reader.getElementText());
                    }else if(TAG_METADATA_KEYWORDS.equalsIgnoreCase(localName)){
                        metadata.keywords = reader.getElementText();
                    }else if(TAG_BOUNDS.equalsIgnoreCase(localName)){
                        metadata.bounds = parseBound();
                    }
                    break;
                case END_ELEMENT:
                    if(TAG_METADATA.equalsIgnoreCase(reader.getLocalName())){
                        //end of the metadata element
                        return metadata;
                    }
                    break;
            }
        }

        throw new XMLStreamException("Error in xml file, relation tag without end.");
    }

    /**
     * Parse current copyright element.
     * The stax reader must be placed to the start element of the copyright.
     *
     * @return Copyright
     */
    private Copyright parseCopyright() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Copyright copyright = new Copyright();
        copyright.author = reader.getAttributeValue(null, ATT_COPYRIGHT_AUTHOR);

        while (reader.hasNext()) {
            final int type = reader.next();

            switch (type) {
                case START_ELEMENT:
                    final String localName = reader.getLocalName();
                    if(TAG_COPYRIGHT_YEAR.equalsIgnoreCase(localName)){
                        copyright.year = Integer.valueOf(reader.getElementText());
                    }else if(TAG_COPYRIGHT_LICENSE.equalsIgnoreCase(localName)){
                        try {
                            copyright.license = new URI(reader.getElementText());
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
                case END_ELEMENT:
                    if(TAG_COPYRIGHT.equalsIgnoreCase(reader.getLocalName())){
                        return copyright;
                    }
                    break;
            }
        }

        throw new XMLStreamException("Error in xml file, copyright tag without end.");
    }

    /**
     * Parse current URI element.
     * The stax reader must be placed to the start element.
     *
     * @return URI
     */
    private URI parseLink() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        String text = reader.getAttributeValue(null, ATT_LINK_HREF);
        String mime = null;

        while (reader.hasNext()) {
            final int type = reader.next();

            switch (type) {
                case START_ELEMENT:
                    final String localName = reader.getLocalName();
                    if(TAG_LINK_TEXT.equalsIgnoreCase(localName) && text==null){
                        text = reader.getElementText();
                    }else if(TAG_LINK_TYPE.equalsIgnoreCase(localName)){
                        mime = reader.getElementText();
                    }
                    break;
                case END_ELEMENT:
                    if(TAG_LINK.equalsIgnoreCase(reader.getLocalName())){
                        try {
                            //end of the link element
                            return new URI(text);
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
            }
        }

        throw new XMLStreamException("Error in xml file, link tag without end.");
    }

    /**
     * Parse current Person element.
     * The stax reader must be placed to the start element.
     *
     * @return Person
     */
    private Person parsePerson() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Person person = new Person();

        while (reader.hasNext()) {
            final int type = reader.next();

            switch (type) {
                case START_ELEMENT:
                    final String localName = reader.getLocalName();
                    if(TAG_NAME.equalsIgnoreCase(localName)){
                        person.name = reader.getElementText();
                    }else if(TAG_AUTHOR_EMAIL.equalsIgnoreCase(localName)){
                        person.email = reader.getElementText();
                    }else if(TAG_LINK.equalsIgnoreCase(localName)){
                        person.link = parseLink();
                    }
                    break;
                case END_ELEMENT:
                    if(TAG_AUTHOR.equalsIgnoreCase(reader.getLocalName())){
                        //end of the author element
                        return person;
                    }
                    break;
            }
        }

        throw new XMLStreamException("Error in xml file, person tag without end.");
    }

    /**
     * Parse current Envelope element.
     * The stax reader must be placed to the start element.
     *
     * @return Envelope
     */
    private Envelope parseBound() throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final String xmin = reader.getAttributeValue(null, ATT_BOUNDS_MINLON);
        final String xmax = reader.getAttributeValue(null, ATT_BOUNDS_MAXLON);
        final String ymin = reader.getAttributeValue(null, ATT_BOUNDS_MINLAT);
        final String ymax = reader.getAttributeValue(null, ATT_BOUNDS_MAXLAT);

        if(xmin == null || xmax == null || ymin == null || ymax == null){
            throw new XMLStreamException("Error in xml file, metadata bounds not defined correctly");
        }

        skipUntilEnd(TAG_BOUNDS);

        return new ImmutableEnvelope(new double[] {Double.parseDouble(xmin), Double.parseDouble(ymin)},
                                     new double[] {Double.parseDouble(xmax), Double.parseDouble(ymax)},
                                     CommonCRS.WGS84.normalizedGeographic());
    }

    /**
     * Parse way point type feature element.
     * The stax reader must be placed to the start element.
     *
     * @return Feature
     */
    private Feature parseWayPoint(final int index) throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Feature feature = TYPE_WAYPOINT.newInstance();
        feature.setPropertyValue("index", index);

        //way points might be located in different tag names : wpt, rtept and trkpt
        //we kind the current tag name to know when we reach the end.
        final String tagName = reader.getLocalName();

        List<URI> links = null;

        final String lat = reader.getAttributeValue(null, ATT_WPT_LAT);
        final String lon = reader.getAttributeValue(null, ATT_WPT_LON);

        if(lat == null || lon == null){
            throw new XMLStreamException("Error in xml file, way point lat/lon not defined correctly");
        }else{
            feature.setPropertyValue("@geometry", new Point(Double.parseDouble(lon), Double.parseDouble(lat)));
        }

        while (reader.hasNext()) {
            final int eventType = reader.next();

            switch (eventType) {
                case START_ELEMENT:
                    final String localName = reader.getLocalName();
                    if(TAG_WPT_ELE.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_ELE, Double.valueOf(reader.getElementText()));
                    }else if(TAG_WPT_TIME.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_TIME, parseTime(reader.getElementText()));
                    }else if(TAG_WPT_MAGVAR.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_MAGVAR, Double.valueOf(reader.getElementText()));
                    }else if(TAG_WPT_GEOIHEIGHT.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_GEOIHEIGHT, Double.valueOf(reader.getElementText()));
                    }else if(TAG_NAME.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_NAME, reader.getElementText());
                    }else if(TAG_CMT.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_CMT,reader.getElementText());
                    }else if(TAG_DESC.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_DESC, reader.getElementText());
                    }else if(TAG_SRC.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_SRC, reader.getElementText());
                    }else if(TAG_LINK.equalsIgnoreCase(localName)){
                        if(links == null) links = new ArrayList<>();
                        links.add(parseLink());
                    }else if(TAG_WPT_SYM.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_SYM, reader.getElementText());
                    }else if(TAG_TYPE.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_TYPE, reader.getElementText());
                    }else if(TAG_WPT_FIX.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_FIX, reader.getElementText());
                    }else if(TAG_WPT_SAT.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_SAT, Integer.valueOf(reader.getElementText()));
                    }else if(TAG_WPT_HDOP.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_HDOP, Double.valueOf(reader.getElementText()));
                    }else if(TAG_WPT_PDOP.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_PDOP, Double.valueOf(reader.getElementText()));
                    }else if(TAG_WPT_VDOP.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_VDOP, Double.valueOf(reader.getElementText()));
                    }else if(TAG_WPT_AGEOFGPSDATA.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_AGEOFGPSDATA, Double.valueOf(reader.getElementText()));
                    }else if(TAG_WPT_DGPSID.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_WPT_DGPSID, Integer.valueOf(reader.getElementText()));
                    }else if(version == GPXVersion.v1_0_0 && TAG_URL.equalsIgnoreCase(localName)){
                        //GPX 1.0 only
                        if(links == null) links = new ArrayList<>();
                        try {
                            links.add(new URI(reader.getElementText()));
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
                case END_ELEMENT:
                    if(tagName.equalsIgnoreCase(reader.getLocalName())){
                        //end of the way point element
                        if(links!=null) feature.setPropertyValue(TAG_LINK, links);
                        return feature;
                    }
                    break;
            }
        }

        throw new XMLStreamException("Error in xml file, "+tagName+" tag without end.");
    }

    /**
     * Parse route type feature element.
     * The stax reader must be placed to the start element.
     *
     * @return Feature
     */
    private Feature parseRoute(final int index) throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Feature feature = TYPE_ROUTE.newInstance();
        feature.setPropertyValue("index", index);

        int ptInc = 0;
        List<URI> links = null;
        List<Feature> wayPoints = null;

        while (reader.hasNext()) {
            final int eventType = reader.next();

            switch (eventType) {
                case START_ELEMENT:
                    final String localName = reader.getLocalName();
                    if(TAG_RTE_RTEPT.equalsIgnoreCase(localName)){
                        if(wayPoints == null) wayPoints = new ArrayList<>();
                        wayPoints.add(parseWayPoint(ptInc++));
                    }else if(TAG_NAME.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_NAME, reader.getElementText());
                    }else if(TAG_CMT.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_CMT, reader.getElementText());
                    }else if(TAG_DESC.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_DESC, reader.getElementText());
                    }else if(TAG_SRC.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_SRC, reader.getElementText());
                    }else if(TAG_LINK.equalsIgnoreCase(localName)){
                        if(links == null) links = new ArrayList<>();
                        links.add(parseLink());
                    }else if(TAG_NUMBER.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_NUMBER, Integer.valueOf(reader.getElementText()));
                    }else if(TAG_TYPE.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_TYPE, reader.getElementText());
                    }else if(version == GPXVersion.v1_0_0 && TAG_URL.equalsIgnoreCase(localName)){
                        //GPX 1.0 only
                        if(links == null) links = new ArrayList<>();
                        try {
                            links.add(new URI(reader.getElementText()));
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
                case END_ELEMENT:
                    if(TAG_RTE.equalsIgnoreCase(reader.getLocalName())){
                        //end of the route element
                        if(links!=null) feature.setPropertyValue(TAG_LINK, links);
                        if(wayPoints!=null) feature.setPropertyValue(TAG_RTE_RTEPT, wayPoints);
                        return feature;
                    }
                    break;
            }
        }

        throw new XMLStreamException("Error in xml file, "+TAG_RTE+" tag without end.");
    }

    /**
     * Parse track segment type feature element.
     * The stax reader must be placed to the start element.
     *
     * @return Feature
     */
    private Feature parseTrackSegment(final int index) throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Feature feature = TYPE_TRACK_SEGMENT.newInstance();
        feature.setPropertyValue("index", index);
        int ptInc = 0;
        List<Feature> wayPoints = null;

        while (reader.hasNext()) {
            final int eventType = reader.next();

            switch (eventType) {
                case START_ELEMENT:
                    final String localName = reader.getLocalName();
                    if(TAG_TRK_SEG_PT.equalsIgnoreCase(localName)){
                        if(wayPoints == null) wayPoints = new ArrayList<>();
                        wayPoints.add(parseWayPoint(ptInc++));
                    }
                    break;
                case END_ELEMENT:
                    if(TAG_TRK_SEG.equalsIgnoreCase(reader.getLocalName())){
                        //end of the track segment element
                        if(wayPoints!=null) feature.setPropertyValue(TAG_TRK_SEG_PT, wayPoints);
                        return feature;
                    }
                    break;
            }
        }

        throw new XMLStreamException("Error in xml file, "+TAG_TRK_SEG+" tag without end.");
    }

    /**
     * Parse track type feature element.
     * The stax reader must be placed to the start element.
     *
     * @return Feature
     */
    private Feature parseTrack(final int index) throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        final Feature feature = TYPE_TRACK.newInstance();
        feature.setPropertyValue("index", index);
        int segInc = 0;
        List<URI> links = null;
        List<Feature> segments = null;

        while (reader.hasNext()) {
            final int eventType = reader.next();

            switch (eventType) {
                case START_ELEMENT:
                    final String localName = reader.getLocalName();
                    if(TAG_TRK_SEG.equalsIgnoreCase(localName)){
                        if(segments == null) segments = new ArrayList<>();
                        segments.add(parseTrackSegment(segInc++));
                    }else if(TAG_NAME.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_NAME, reader.getElementText());
                    }else if(TAG_CMT.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_CMT, reader.getElementText());
                    }else if(TAG_DESC.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_DESC, reader.getElementText());
                    }else if(TAG_SRC.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_SRC, reader.getElementText());
                    }else if(TAG_LINK.equalsIgnoreCase(localName)){
                        if(links == null) links = new ArrayList<>();
                        links.add(parseLink());
                    }else if(TAG_NUMBER.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_NUMBER, Integer.valueOf(reader.getElementText()));
                    }else if(TAG_TYPE.equalsIgnoreCase(localName)){
                        feature.setPropertyValue(TAG_TYPE, reader.getElementText());
                    }else if(version == GPXVersion.v1_0_0 && TAG_URL.equalsIgnoreCase(localName)){
                        //GPX 1.0 only
                        if(links == null) links = new ArrayList<>();
                        try {
                            links.add(new URI(reader.getElementText()));
                        } catch (URISyntaxException ex) {
                            throw new XMLStreamException(ex);
                        }
                    }
                    break;
                case END_ELEMENT:
                    if(TAG_TRK.equalsIgnoreCase(reader.getLocalName())){
                        //end of the track element
                        if(links!=null) feature.setPropertyValue(TAG_LINK, links);
                        if(segments!=null) feature.setPropertyValue(TAG_TRK_SEG, segments);
                        return feature;
                    }
                    break;
            }
        }

        throw new XMLStreamException("Error in xml file, "+TAG_TRK+" tag without end.");
    }

    /**
     * Parse date or date time from string.
     * The method support only ISO 8601 Date and DateTime formats.
     *
     * @param dateStr date in ISO date or data time format
     * @return Temporal
     */
    private static Temporal parseTime(String dateStr) {
        try{
            final DateTimeFormatter format = DateTimeFormatter.ISO_INSTANT;
            final TemporalAccessor accessor = format.parse(dateStr);
            return Instant.from(accessor);
        }catch(UnsupportedTemporalTypeException | DateTimeParseException ex){
            try{
                final DateTimeFormatter format = DateTimeFormatter.ISO_DATE;
                final TemporalAccessor accessor = format.parse(dateStr);
                return LocalDate.from(accessor);
            }catch(UnsupportedTemporalTypeException | DateTimeParseException e){
                final DateTimeFormatter format = DateTimeFormatter.ISO_DATE_TIME;
                final TemporalAccessor accessor = format.parse(dateStr);
                return LocalDateTime.from(accessor);
            }
        }
    }

}
