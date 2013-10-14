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
package org.apache.sis.referencing;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.apache.sis.referencing.datum.DefaultTemporalDatum;
import org.apache.sis.util.resources.Vocabulary;

import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.ALIAS_KEY;


/**
 * Frequently-used temporal CRS and datum that are guaranteed to be available in SIS.
 *
 * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code TemporalObjects}
 * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
 * (e.g. the application is running in a container environment, and some modules have been installed or uninstalled).</p>
 *
 * <p><b>Example:</b> the following code fetches a temporal Coordinate Reference System using the Julian calendar:</p>
 *
 * {@preformat java
 *   TemporalCRS crs = TemporalObjects.JULIAN.crs();
 * }
 *
 * Below is an alphabetical list of object names available in this enumeration:
 *
 * <blockquote><table class="sis">
 *   <tr><th>Name or alias</th>    <th>Object type</th> <th>Enumeration value</th></tr>
 *   <tr><td>Dublin Julian</td>    <td>CRS, Datum</td>  <td>{@link #DUBLIN_JULIAN}</td></tr>
 *   <tr><td>Java time</td>        <td>CRS, Datum</td>  <td>{@link #JAVA}</td></tr>
 *   <tr><td>Julian</td>           <td>CRS, Datum</td>  <td>{@link #JULIAN}</td></tr>
 *   <tr><td>Modified Julian</td>  <td>CRS, Datum</td>  <td>{@link #MODIFIED_JULIAN}</td></tr>
 *   <tr><td>Truncated Julian</td> <td>CRS, Datum</td>  <td>{@link #TRUNCATED_JULIAN}</td></tr>
 *   <tr><td>Unix time</td>        <td>CRS, Datum</td>  <td>{@link #UNIX}</td></tr>
 * </table></blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public enum TemporalObjects {
    /**
     * Time measured as days since January 1st, 4713 BC at 12:00 UTC.
     */
    JULIAN(Vocabulary.Keys.Julian, -2440588 * (24*60*60*1000L) + (12*60*60*1000L)),

    /**
     * Time measured as days since November 17, 1858 at 00:00 UTC.
     * A <cite>Modified Julian day</cite> (MJD) is defined relative to
     * <cite>Julian day</cite> (JD) as {@code MJD = JD − 2400000.5}.
     */
    MODIFIED_JULIAN(Vocabulary.Keys.ModifiedJulian, -40587 * (24*60*60*1000L)),

    /**
     * Time measured as days since May 24, 1968 at 00:00 UTC.
     * This epoch was introduced by NASA for the space program.
     * A <cite>Truncated Julian day</cite> (TJD) is defined relative to
     * <cite>Julian day</cite> (JD) as {@code TJD = JD − 2440000.5}.
     */
    TRUNCATED_JULIAN(Vocabulary.Keys.TruncatedJulian, -587 * (24*60*60*1000L)),

    /**
     * Time measured as days since December 31, 1899 at 12:00 UTC.
     * A <cite>Dublin Julian day</cite> (DJD) is defined relative to
     * <cite>Julian day</cite> (JD) as {@code DJD = JD − 2415020}.
     */
    DUBLIN_JULIAN(Vocabulary.Keys.DublinJulian, -25568 * (24*60*60*1000L) + (12*60*60*1000L)),

    /**
     * Time measured as seconds since January 1st, 1970 at 00:00 UTC.
     */
    UNIX(-1, 0),

    /**
     * Time measured as milliseconds since January 1st, 1970 at 00:00 UTC.
     */
    JAVA(-1, 0);

    /**
     * The resource keys for the name as one of the {@code Vocabulary.Keys} constants,
     * or -1 for using the enumeration name.
     */
    private final int key;

    /**
     * The date and time origin of this temporal datum.
     */
    private final long epoch;

    /**
     * The cached object. This is initially {@code null}, then set to various kind of objects depending
     * on which method has been invoked. The kind of object stored in this field may change during the
     * application execution.
     */
    private transient volatile IdentifiedObject cached;

    /**
     * Creates a new enumeration value of the given name with time counted since the given epoch.
     */
    private TemporalObjects(final int name, final long epoch) {
        this.key   = name;
        this.epoch = epoch;
    }

    /**
     * Returns the datum associated to this temporal object.
     * The following table summarizes the datum known to this class,
     * together with an enumeration value that can be used for fetching that datum:
     *
     * <blockquote><table class="sis">
     *   <tr><th>Name or alias</th>    <th>Enum</th></tr>
     *   <tr><td>Dublin Julian</td>    <td>{@link #DUBLIN_JULIAN}</td></tr>
     *   <tr><td>Julian</td>           <td>{@link #JULIAN}</td></tr>
     *   <tr><td>Modified Julian</td>  <td>{@link #MODIFIED_JULIAN}</td></tr>
     *   <tr><td>Truncated Julian</td> <td>{@link #TRUNCATED_JULIAN}</td></tr>
     *   <tr><td>Unix / Java</td>      <td>{@link #JAVA}</td></tr>
     * </table></blockquote>
     *
     * @return The datum associated to this constant.
     *
     * @see DefaultTemporalDatum
     * @see DatumAuthorityFactory#createTemporalDatum(String)
     */
    public TemporalDatum datum() {
        TemporalDatum object = datum(cached);
        if (object == null) {
            synchronized (this) {
                object = datum(cached);
                if (object == null) {
                    if (this == UNIX) {
                        object = JAVA.datum(); // Share the same instance for UNIX and JAVA.
                    } else {
                        final Map<String,Object> properties;
                        if (key >= 0) {
                            properties = new HashMap<>(4);
                            final InternationalString name = Vocabulary.formatInternational(key);
                            properties.put(NAME_KEY,  name.toString(Locale.ROOT));
                            properties.put(ALIAS_KEY, name);
                        } else {
                            properties = Collections.<String,Object>singletonMap(NAME_KEY, name());
                        }
                        object = new DefaultTemporalDatum(properties, new Date(epoch));
                    }
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the datum associated to the given object, or {@code null} if none.
     */
    private static TemporalDatum datum(final IdentifiedObject object) {
        if (object instanceof TemporalDatum) {
            return (TemporalDatum) object;
        }
        if (object instanceof TemporalCRS) {
            return ((TemporalCRS) object).getDatum();
        }
        return null;
    }
}
