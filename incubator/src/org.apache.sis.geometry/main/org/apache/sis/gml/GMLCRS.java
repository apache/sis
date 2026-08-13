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
package org.apache.sis.gml;

import java.util.Map;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.util.FactoryException;


/**
 * Resolution of the GML {@code srsName} attribute to a {@link CoordinateReferenceSystem} and back,
 * shared by the GML 2.0 and GML 3 readers and writers.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class GMLCRS {
    /**
     * Key under which the readers record the {@code srsName} attribute value verbatim, in the
     * {@linkplain org.apache.sis.geometries.Geometry#userProperties() user properties} of the
     * geometry they build. This lets a writer reproduce an {@code srsName} that
     * {@link #srsName srsName(…)} could not derive from the CRS alone, such as
     * {@code "urn:ogc:def:crs:EPSG::4326"} or an authority other than EPSG.
     *
     * <p>Purely advisory: writers must produce valid GML when it is absent.</p>
     */
    static final String SRS_NAME_KEY = "gml:srsName";

    /**
     * Key under which the readers record the local name of the GML element a geometry was read
     * from. Several GML elements map onto the same Apache SIS type — {@code gml:Curve} and
     * {@code gml:LineString} both become a {@code LineString} when the curve has a single linear
     * segment — so the element name is the only way a writer can reproduce the original spelling.
     *
     * <p>Purely advisory: writers must produce valid GML when it is absent.</p>
     */
    static final String ELEMENT_KEY = "gml:element";

    /**
     * Do not allow instantiation of this class.
     */
    private GMLCRS() {
    }

    /**
     * Resolves the given {@code srsName} attribute value to a coordinate reference system.
     *
     * @param  srsName    the {@code srsName} attribute value, or {@code null} if the attribute was absent.
     * @param  inherited  the CRS to return if {@code srsName} is null, or {@code null} if none.
     * @return the resolved CRS, or {@code null} if neither an {@code srsName} nor an inherited CRS exists.
     * @throws DataStoreReferencingException if {@code srsName} cannot be resolved.
     */
    static CoordinateReferenceSystem resolve(final String srsName, final CoordinateReferenceSystem inherited)
            throws DataStoreReferencingException
    {
        if (srsName == null) {
            return inherited;
        }
        try {
            return CRS.forCode(srsName);
        } catch (FactoryException e) {
            throw new DataStoreReferencingException(e);
        }
    }

    /**
     * Returns a coordinate reference system of exactly {@code dimension} dimensions, based on the
     * given CRS.
     *
     * <ul>
     *   <li>If {@code crs} is null, an {@linkplain Geometries#getUndefinedCRS undefined} CRS of the
     *       requested dimension is returned. This is the case of a GML document which declares no
     *       {@code srsName} at any enclosing level.</li>
     *   <li>If {@code crs} already has the requested number of dimensions, it is returned as-is.</li>
     *   <li>If the document has exactly one ordinate more than {@code crs} — a three-dimensional
     *       {@code posList} under {@code srsName="EPSG:4326"} is common in practice — the CRS is
     *       promoted: to the three-dimensional geographic CRS of the same geodetic reference frame
     *       when {@code crs} is geodetic, or to a compound CRS with an ellipsoidal height otherwise.</li>
     *   <li>Any other disagreement is a document error and is reported as such. Ordinates are never
     *       silently truncated, and an explicit {@code srsName} is never silently downgraded to an
     *       undefined CRS.</li>
     * </ul>
     *
     * @param  crs        the CRS resolved from the document, or {@code null} if none was declared.
     * @param  dimension  the number of ordinates per coordinate tuple found in the document.
     * @return a CRS of exactly {@code dimension} dimensions, never null.
     * @throws DataStoreContentException if the CRS and the coordinates disagree irreconcilably.
     * @throws DataStoreReferencingException if the promoted CRS cannot be created.
     */
    static CoordinateReferenceSystem forDimension(final CoordinateReferenceSystem crs, final int dimension)
            throws DataStoreContentException, DataStoreReferencingException
    {
        if (crs == null) {
            return Geometries.getUndefinedCRS(dimension);
        }
        final int actual = crs.getCoordinateSystem().getDimension();
        if (actual == dimension) {
            return crs;
        }
        if (dimension == actual + 1) {
            if (crs instanceof GeodeticCRS) try {
                return CommonCRS.forDatum(crs).geographic3D();
            } catch (IllegalArgumentException e) {
                // No `CommonCRS` for that reference frame: fall through to the compound CRS below.
            }
            try {
                return CRS.compound(crs, CommonCRS.Vertical.ELLIPSOIDAL.crs());
            } catch (FactoryException e) {
                throw new DataStoreReferencingException(e);
            }
        }
        throw new DataStoreContentException("The coordinate reference system \"" + crs.getName().getCode()
                + "\" has " + actual + " dimensions, but the GML coordinates have " + dimension
                + " ordinates per tuple.");
    }

    /**
     * Returns the {@code srsName} attribute value to write for the given coordinate reference
     * system, or {@code null} if no {@code srsName} attribute should be written at all.
     *
     * <p>{@code null} is returned for an {@linkplain Geometries#isUndefined undefined} CRS, which is
     * what the readers substitute for a document that declared no {@code srsName}: writing such a
     * CRS out would invent a reference frame the document never claimed.</p>
     *
     * @param  crs             the CRS to format, or {@code null} if none.
     * @param  userProperties  the user properties of the geometry being written, or {@code null}.
     *         When they carry a {@value #SRS_NAME_KEY} entry, it is reproduced verbatim.
     * @return the {@code srsName} value, or {@code null} if the attribute should be omitted.
     * @throws DataStoreReferencingException if the CRS has no EPSG identifier and no verbatim
     *         {@code srsName} was recorded.
     */
    static String srsName(final CoordinateReferenceSystem crs, final Map<String,Object> userProperties)
            throws DataStoreReferencingException
    {
        if (crs == null || Geometries.isUndefined(crs)) {
            return null;
        }
        if (userProperties != null && userProperties.get(SRS_NAME_KEY) instanceof String verbatim) {
            return verbatim;
        }
        final Identifier id = IdentifiedObjects.getIdentifier(crs, Citations.EPSG);
        if (id == null) {
            throw new DataStoreReferencingException("Cannot determine an EPSG code for the coordinate"
                    + " reference system \"" + crs.getName().getCode() + "\".");
        }
        return IdentifiedObjects.toString(id);
    }

    /**
     * Returns the local name of the GML element the given geometry was read from, or {@code null}
     * if unknown. Never throws: point views returned by
     * {@link org.apache.sis.geometries.PointSequence#getPoint(int)} implement
     * {@link org.apache.sis.geometries.Point} directly rather than through
     * {@code AbstractGeometry}, so their {@code userProperties()} is null.
     */
    static String sourceElement(final org.apache.sis.geometries.Geometry geometry) {
        final Map<String,Object> properties = geometry.userProperties();
        if (properties != null && properties.get(ELEMENT_KEY) instanceof String name) {
            return name;
        }
        return null;
    }
}
