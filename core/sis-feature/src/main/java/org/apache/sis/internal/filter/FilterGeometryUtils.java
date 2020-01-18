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
package org.apache.sis.internal.filter;

import javax.measure.Unit;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.filter.CRSMatching;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.Utilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class FilterGeometryUtils {

    //store wkb/wkt readers/writers by thread since they are not concurrent.
    public static final GeometryFactory GF = new GeometryFactory();
    public static final ThreadLocal<WKBReader> WKB_READERS = new ThreadLocal<WKBReader>();
    public static final ThreadLocal<WKTReader> WKT_READERS = new ThreadLocal<WKTReader>();
    public static final ThreadLocal<WKBWriter> WKB_WRITERS = new ThreadLocal<WKBWriter>();
    public static final ThreadLocal<WKTWriter> WKT_WRITERS = new ThreadLocal<WKTWriter>();

    private FilterGeometryUtils() {
    }

    private static CoordinateReferenceSystem MERCATOR;

    /**
     * TODO: We should generify this. But too much code already depends on JTS,
     * so for now I hack this
     */
    public static final Geometries<Geometry> SIS_GEOMETRY_FACTORY = (Geometries<Geometry>) Geometries.implementation(GeometryLibrary.JTS);

    /**
     * Created when first required, not thread safe.
     *
     * @return WKBReader
     */
    public static WKBReader getWKBReader() {
        WKBReader wkbReader = WKB_READERS.get();
        if (wkbReader == null) {
            wkbReader = new WKBReader();
            WKB_READERS.set(wkbReader);
        }
        return wkbReader;
    }

    /**
     * Created when first requiered, not thread safe.
     *
     * @return WKTReader
     */
    public static WKTReader getWKTReader() {
        WKTReader wktReader = WKT_READERS.get();
        if (wktReader == null) {
            wktReader = new WKTReader();
            WKT_READERS.set(wktReader);
        }
        return wktReader;
    }

    /**
     * Created when first requiered, not thread safe.
     *
     * @return WKBReader
     */
    public static WKBWriter getWKBWriter() {
        WKBWriter candidate = FilterGeometryUtils.WKB_WRITERS.get();
        if (candidate == null) {
            candidate = new WKBWriter();
            FilterGeometryUtils.WKB_WRITERS.set(candidate);
        }
        return candidate;
    }

    /**
     * Created when first requiered, not thread safe.
     *
     * @return WKTReader
     */
    public static WKTWriter getWKTWriter() {
        WKTWriter candidate = FilterGeometryUtils.WKT_WRITERS.get();
        if (candidate == null) {
            candidate = new WKTWriter();
            FilterGeometryUtils.WKT_WRITERS.set(candidate);
        }
        return candidate;
    }

    public static CoordinateReferenceSystem getMercator() throws FactoryException {
        if (MERCATOR == null) {
            MERCATOR = CRS.forCode("EPSG:3395");
        }
        return MERCATOR;
    }

    public static Geometry toGeometry(final Object object, Expression exp) {
        Object value = null;
        if ((exp instanceof PropertyName) && object instanceof Feature && ((PropertyName) exp).getPropertyName().isEmpty()) {
            //Search for a default geometry.
            try {
                value = ((Feature) object).getPropertyValue(AttributeConvention.GEOMETRY_PROPERTY.toString());
            } catch (PropertyNotFoundException ex) {
                //no defined default geometry
            }
        } else {
            value = exp.evaluate(object);
        }

        if (value instanceof GeometryWrapper) {
            value = ((GeometryWrapper) value).geometry;
        } else if (value instanceof String) {
            try {
                //try to convert from WKT
                value = getWKTReader().read(value.toString());
            } catch (ParseException ex) {
                //we have try
            }
        } else if (value instanceof byte[]) {
            try {
                //try to convert from WKB
                value = getWKBReader().read((byte[]) value);
            } catch (ParseException ex) {
                //we have try
            }
        }

        Geometry candidate;
        if (value instanceof GridCoverage) {
            //use the coverage envelope
            final GridCoverage coverage = (GridCoverage) value;
            candidate = SIS_GEOMETRY_FACTORY.toGeometry2D(coverage.getGridGeometry().getEnvelope(), WraparoundMethod.SPLIT);
        } else {
            try {
                candidate = ObjectConverters.convert(value, Geometry.class);
            } catch (UnconvertibleObjectException ex) {
                //cound not convert value to a Geometry
                candidate = null;
            }
        }
        return candidate;
    }

    /**
     * Reproject geometries to the same CRS if needed and if possible.
     */
    public static Geometry[] toSameCRS(final Geometry leftGeom, final Geometry rightGeom)
            throws FactoryException, TransformException {

        final CoordinateReferenceSystem leftCRS = Geometries.getCoordinateReferenceSystem(leftGeom);
        final CoordinateReferenceSystem rightCRS = Geometries.getCoordinateReferenceSystem(rightGeom);

        final CRSMatching.Match match = CRSMatching
                .left(leftCRS)
                .right(rightCRS);

        // TODO: avoid casts.
        final CRSMatching.Transformer<Object> projectGeom = (g, op) -> Geometries.transform(g, op);
        return new Geometry[] {
            (Geometry) match.transformLeftToCommon(leftGeom, projectGeom),
            (Geometry) match.transformRightToCommon(rightGeom, projectGeom)
        };
    }

    /**
     * Reproject one or both geometries to the same crs, the matching crs will
     * be compatible with the requested unit. return Array[leftGeometry,
     * rightGeometry, matchingCRS];
     */
    public static Object[] toSameCRS(final Geometry leftGeom, final Geometry rightGeom, final Unit unit)
            throws NoSuchAuthorityCodeException, FactoryException, TransformException {

        final CoordinateReferenceSystem leftCRS = Geometries.getCoordinateReferenceSystem(leftGeom);
        final CoordinateReferenceSystem rightCRS = Geometries.getCoordinateReferenceSystem(rightGeom);

        if (leftCRS == null && rightCRS == null) {
            //bother geometries doesn't have a defined SRID, we assume that both
            //are in the same CRS
            return new Object[]{leftGeom, rightGeom, null};
        } else if (leftCRS == null || rightCRS == null || Utilities.equalsIgnoreMetadata(leftCRS, rightCRS)) {
            //both are in the same CRS

            final CoordinateReferenceSystem geomCRS = (leftCRS == null) ? rightCRS : leftCRS;

            if (geomCRS.getCoordinateSystem().getAxis(0).getUnit().isCompatible(unit)) {
                //the geometries crs is compatible with the requested unit, nothing to reproject
                return new Object[]{leftGeom, rightGeom, geomCRS};
            } else {
                //the crs unit is not compatible, we must reproject both geometries to a more appropriate crs
                if (Units.METRE.isCompatible(unit)) {
                    //in that case we reproject to mercator EPSG:3395
                    final CoordinateReferenceSystem mercator = getMercator();
                    final CoordinateOperation trs = CRS.findOperation(geomCRS, mercator, null);

                    return new Object[]{
                        Geometries.transform(leftGeom, trs),
                        Geometries.transform(rightGeom, trs),
                        mercator};

                } else {
                    //we can not find a matching projection in this case
                    throw new TransformException("Could not find a matching CRS for both geometries for unit :" + unit);
                }
            }

        } else {
            //both have different CRS, try to find the most appropriate crs amoung both

            final CoordinateReferenceSystem matchingCRS;
            final Object leftMatch;
            final Object rightMatch;

            if (leftCRS.getCoordinateSystem().getAxis(0).getUnit().isCompatible(unit)) {
                matchingCRS = leftCRS;
                final CoordinateOperation trs = CRS.findOperation(rightCRS, matchingCRS, null);
                rightMatch = Geometries.transform(rightGeom, trs);
                leftMatch = leftGeom;
            } else if (rightCRS.getCoordinateSystem().getAxis(0).getUnit().isCompatible(unit)) {
                matchingCRS = rightCRS;
                final CoordinateOperation trs = CRS.findOperation(leftCRS, matchingCRS, null);
                leftMatch = Geometries.transform(leftGeom, trs);
                rightMatch = rightGeom;
            } else {
                //the crs unit is not compatible, we must reproject both geometries to a more appropriate crs
                if (Units.METRE.isCompatible(unit)) {
                    //in that case we reproject to mercator EPSG:3395
                    matchingCRS = getMercator();

                    CoordinateOperation trs = CRS.findOperation(leftCRS, matchingCRS, null);
                    leftMatch = Geometries.transform(leftGeom, trs);
                    trs = CRS.findOperation(rightCRS, matchingCRS, null);
                    rightMatch = Geometries.transform(rightGeom, trs);

                } else {
                    //we can not find a matching projection in this case
                    throw new TransformException("Could not find a matching CRS for both geometries for unit :" + unit);
                }
            }

            return new Object[]{leftMatch, rightMatch, matchingCRS};
        }
    }
}
