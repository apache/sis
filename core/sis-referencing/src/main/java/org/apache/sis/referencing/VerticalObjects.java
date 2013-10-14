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

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.apache.sis.referencing.datum.DefaultVerticalDatum;
import org.apache.sis.util.resources.Vocabulary;

import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.ALIAS_KEY;


/**
 * Frequently-used vertical CRS and datum that are guaranteed to be available in SIS.
 * Methods in this enumeration are shortcuts for object definitions in the EPSG database.
 * If there is no EPSG database available, or if the query failed, or if there is no EPSG definition for an object,
 * then {@code VerticalObjects} fallback on hard-coded values. Consequently, those methods never return {@code null}.
 *
 * <p>Referencing objects are cached after creation. Invoking the same method on the same {@code VerticalObjects}
 * instance twice will return the same {@link IdentifiedObject} instance, unless the internal cache has been cleared
 * (e.g. the application is running in a container environment, and some modules have been installed or uninstalled).</p>
 *
 * <p><b>Example:</b> the following code fetches a vertical Coordinate Reference System for height above the geoid:</p>
 *
 * {@preformat java
 *   VerticalCRS crs = VerticalObjects.GEOIDAL.crs();
 * }
 *
 * Below is an alphabetical list of object names available in this enumeration:
 *
 * <blockquote><table class="sis">
 *   <tr><th>Name or alias</th>       <th>Object type</th> <th>Enumeration value</th></tr>
 *   <tr><td>Barometric altitude</td> <td>CRS, Datum</td>  <td>{@link #BAROMETRIC}</td></tr>
 *   <tr><td>Geoidal height</td>      <td>CRS, Datum</td>  <td>{@link #GEOIDAL}</td></tr>
 *   <tr><td>Ellipsoidal height</td>  <td>CRS, Datum</td>  <td>{@link #ELLIPSOIDAL}</td></tr>
 *   <tr><td>Other surface</td>       <td>CRS, Datum</td>  <td>{@link #OTHER_SURFACE}</td></tr>
 * </table></blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public enum VerticalObjects {
    /**
     * Height measured by atmospheric pressure.
     *
     * @see VerticalDatumType#BAROMETRIC
     */
    BAROMETRIC(Vocabulary.Keys.BarometricAltitude),

    /**
     * Height measured above an equipotential surface.
     *
     * @see VerticalDatumType#GEOIDAL
     */
    GEOIDAL(Vocabulary.Keys.Geoidal),

    /**
     * Height measured along the normal to the ellipsoid used in the definition of horizontal datum.
     *
     * <p><b>This datum is not part of ISO 19111 international standard.</b>
     * Usage of this datum is generally not recommended since ellipsoidal heights make little sense without
     * their (<var>latitude</var>, <var>longitude</var>) locations. The ISO specification defines instead
     * three-dimensional {@code GeographicCRS} for that reason. However Apache SIS provides this value
     * because it is sometime useful to temporarily express ellipsoidal heights independently from other
     * ordinate values.</p>
     */
    ELLIPSOIDAL(Vocabulary.Keys.Ellipsoidal),

    /**
     * Height measured above other kind of surface, for example a geological feature.
     *
     * @see VerticalDatumType#OTHER_SURFACE
     */
    OTHER_SURFACE(Vocabulary.Keys.OtherSurface);

    /**
     * The resource keys for the name as one of the {@code Vocabulary.Keys} constants.
     */
    private final int key;

    /**
     * The cached object. This is initially {@code null}, then set to various kind of objects depending
     * on which method has been invoked. The kind of object stored in this field may change during the
     * application execution.
     */
    private transient volatile IdentifiedObject cached;

    /**
     * Creates a new enumeration value of the given name.
     *
     * {@note This constructor does not expect <code>VerticalDatumType</code> constant in order to avoid too
     *        early class initialization. In particular, we do not want early dependency to the SIS-specific
     *        <code>VerticalDatumTypes.ELLIPSOIDAL</code> constant.}
     */
    private VerticalObjects(final int name) {
        this.key = name;
    }

    /**
     * Returns the datum associated to this vertical object.
     * The following table summarizes the datum known to this class,
     * together with an enumeration value that can be used for fetching that datum:
     *
     * <blockquote><table class="sis">
     *   <tr><th>Name or alias</th>       <th>Enum</th></tr>
     *   <tr><td>Barometric altitude</td> <td>{@link #BAROMETRIC}</td></tr>
     *   <tr><td>Geoidal height</td>      <td>{@link #GEOIDAL}</td></tr>
     *   <tr><td>Ellipsoidal height</td>  <td>{@link #ELLIPSOIDAL}</td></tr>
     *   <tr><td>Other surface</td>       <td>{@link #OTHER_SURFACE}</td></tr>
     * </table></blockquote>
     *
     * @return The datum associated to this constant.
     *
     * @see DefaultVerticalDatum
     * @see DatumAuthorityFactory#createVerticalDatum(String)
     */
    public VerticalDatum datum() {
        VerticalDatum object = datum(cached);
        if (object == null) {
            synchronized (this) {
                object = datum(cached);
                if (object == null) {
                    final Map<String,Object> properties = new HashMap<>(4);
                    final InternationalString name = Vocabulary.formatInternational(key);
                    properties.put(NAME_KEY,  name.toString(Locale.ROOT));
                    properties.put(ALIAS_KEY, name);
                    object = new DefaultVerticalDatum(properties, VerticalDatumType.valueOf(name()));
                    cached = object;
                }
            }
        }
        return object;
    }

    /**
     * Returns the datum associated to the given object, or {@code null} if none.
     */
    private static VerticalDatum datum(final IdentifiedObject object) {
        if (object instanceof VerticalDatum) {
            return (VerticalDatum) object;
        }
        if (object instanceof VerticalCRS) {
            return ((VerticalCRS) object).getDatum();
        }
        return null;
    }
}
