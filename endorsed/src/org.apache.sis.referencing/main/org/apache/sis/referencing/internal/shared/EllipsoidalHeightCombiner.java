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
package org.apache.sis.referencing.internal.shared;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import org.opengis.util.FactoryException;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Constants;


/**
 * A class in charges of combining two-dimensional geographic or projected CRS with an ellipsoidal height into a
 * three-dimensional CRS. This is the converse of {@link org.apache.sis.referencing.EllipsoidalHeightSeparator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.referencing.EllipsoidalHeightSeparator
 */
public final class EllipsoidalHeightCombiner {
    /**
     * The factories to use for creating geodetic objects.
     */
    private final ReferencingFactoryContainer factories;

    /**
     * Creates a new instance initialized to the factories of the given container.
     *
     * @param  c  the container from which to fetch the factories, or {@code null} for the default factories.
     */
    public EllipsoidalHeightCombiner(final ReferencingFactoryContainer c) {
        factories = (c != null) ? c : new ReferencingFactoryContainer();
    }

    /**
     * Creates a new combiner which will use the given factories.
     * Any factory given in argument may be {@code null} if lazy instantiation is desired.
     *
     * @param  crsFactory  the factory to use for creating compound or three-dimensional geographic CRS.
     * @param  csFactory   the factory to use for creating three-dimensional ellipsoidal CS, if needed.
     * @param  opFactory   the factory to use for creating defining conversions, if needed.
     */
    public EllipsoidalHeightCombiner(final CRSFactory crsFactory, final CSFactory csFactory,
                                     final CoordinateOperationFactory opFactory)
    {
        factories = new ReferencingFactoryContainer(null, crsFactory, csFactory, null, opFactory, null);
    }

    /**
     * Creates a compound CRS using properties inferred from the given components.
     *
     * @param  components  ordered array of {@code CoordinateReferenceSystem} objects.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    public final CoordinateReferenceSystem createCompoundCRS(CoordinateReferenceSystem... components) throws FactoryException {
        return createCompoundCRS(properties(components), components);
    }

    /**
     * Creates a compound CRS, but we special processing for (two-dimensional Geographic + ellipsoidal heights) tuples.
     * If any such tuple is found, a three-dimensional geographic CRS is created instead of the compound CRS.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  components  ordered array of {@code CoordinateReferenceSystem} objects.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    public final CoordinateReferenceSystem createCompoundCRS(final Map<String, ?> properties,
            CoordinateReferenceSystem... components) throws FactoryException
    {
        for (int i=0; i<components.length; i++) {
            final CoordinateReferenceSystem vertical = components[i];
            if (vertical instanceof VerticalCRS) {
                final VerticalDatum datum = DatumOrEnsemble.asDatum((VerticalCRS) vertical);
                if (ReferencingUtilities.isEllipsoidalHeight(datum)) {
                    int axisPosition = 0;
                    CoordinateSystem cs2D;
                    CoordinateReferenceSystem crs = null;
                    if (i == 0 || (cs2D = getCsIfHorizontal2D(crs = components[i - 1])) == null) {
                        /*
                         * GeographicCRS are normally before VerticalCRS. But Apache SIS is tolerant to the
                         * opposite order (note however that such ordering is illegal according ISO 19162).
                         */
                        if (i+1 >= components.length || (cs2D = getCsIfHorizontal2D(crs = components[i + 1])) == null) {
                            continue;
                        }
                        axisPosition = 1;
                    }
                    /*
                     * At this point, we have the horizontal and vertical components. The horizontal component
                     * begins at `axisPosition`, which is almost always zero but following code accepts also 1.
                     * Try to get a 3D coordinate system from the EPSG database if possible because it provides
                     * better metadata (in particular a more appropriate CS name).
                     */
                    final var axes = new CoordinateSystemAxis[3];
                    axes[axisPosition++   ] = cs2D.getAxis(0);
                    axes[axisPosition++   ] = cs2D.getAxis(1);
                    axes[axisPosition %= 3] = vertical.getCoordinateSystem().getAxis(0);
                    final String epsgCode = getEpsgCode(cs2D, axes);
                    /*
                     * If the 3D CRS created in this block will be the CRS to return, the `properties` given in
                     * argument to this method are used directly. Otherwise we need to derive something else.
                     * Current implementation reuses the properties of the two-dimensional CRS.
                     */
                    final Map<String, ?> crsProps = (components.length == 2)
                            ? properties : ReferencingUtilities.getPropertiesForModifiedCRS(crs);
                    if (crs instanceof GeodeticCRS) {
                        final var geod = (GeodeticCRS) crs;
                        EllipsoidalCS cs3D = null;
                        if (epsgCode != null) try {
                            cs3D = factories.getCSAuthorityFactory().createEllipsoidalCS(epsgCode);
                        } catch (NoSuchAuthorityCodeException e) {
                            warning(e);
                        }
                        if (cs3D == null) {
                            cs3D = factories.getCSFactory().createEllipsoidalCS(
                                    ReferencingUtilities.getPropertiesForModifiedCRS(cs2D), axes[0], axes[1], axes[2]);
                        }
                        crs = factories.getCRSFactory().createGeographicCRS(crsProps, DatumOrEnsemble.asDatum(geod), cs3D);
                    } else if (crs instanceof ProjectedCRS) {
                        final var proj = (ProjectedCRS) crs;
                        GeographicCRS base = proj.getBaseCRS();
                        if (base.getCoordinateSystem().getDimension() == 2) {
                            base = (GeographicCRS) createCompoundCRS(ReferencingUtilities.getPropertiesForModifiedCRS(base), base, vertical);
                        }
                        /*
                         * In Apache SIS implementation, the Conversion contains the source and target CRS together with
                         * a MathTransform2D. We need to recreate the same conversion, but without CRS and MathTransform
                         * for letting SIS create or associate new ones, which will be three-dimensional now.
                         */
                        Conversion fromBase = proj.getConversionFromBase();
                        fromBase = factories.getCoordinateOperationFactory().createDefiningConversion(
                                    IdentifiedObjects.getProperties(fromBase),
                                    fromBase.getMethod(), fromBase.getParameterValues());

                        CartesianCS cs3D = null;
                        if (epsgCode != null) try {
                            cs3D = factories.getCSAuthorityFactory().createCartesianCS(epsgCode);
                        } catch (NoSuchAuthorityCodeException e) {
                            warning(e);
                        }
                        if (cs3D == null) {
                            cs3D = factories.getCSFactory().createCartesianCS(
                                    ReferencingUtilities.getPropertiesForModifiedCRS(cs2D), axes[0], axes[1], axes[2]);
                        }
                        crs  = factories.getCRSFactory().createProjectedCRS(crsProps, base, fromBase, cs3D);
                    } else {
                        throw new FactoryException(Errors.format(
                                Errors.Keys.UnsupportedType_1, Classes.getStandardClass(crs, CoordinateReferenceSystem.class)));
                    }
                    /*
                     * Remove the VerticalCRS and store the three-dimensional GeographicCRS in place of the previous
                     * two-dimensional GeographicCRS. Then let the loop continues in case there is other CRS to merge
                     * (should never happen, but we are paranoiac).
                     */
                    components = ArraysExt.remove(components, i, 1);
                    if (axisPosition != 0) i--;             // GeographicCRS before VerticalCRS (usual case).
                    components[i] = crs;
                }
            }
        }
        switch (components.length) {
            case 0:  return null;
            case 1:  return components[0];
            default: return factories.getCRSFactory().createCompoundCRS(properties, components);
        }
    }

    /**
     * Returns the coordinate system if the given CRS is a two-dimensional geographic or projected CRS,
     * or {@code null} otherwise. The returned coordinate system is either ellipsoidal or Cartesian;
     * no other type is returned.
     */
    private static CoordinateSystem getCsIfHorizontal2D(final CoordinateReferenceSystem crs) {
        final boolean isProjected = (crs instanceof ProjectedCRS);
        if (isProjected || crs instanceof GeodeticCRS) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs.getDimension() == 2 && (isProjected || cs instanceof EllipsoidalCS)) {
                /*
                 * ProjectedCRS are guaranteed to be associated with CartesianCS, so we do not test that.
                 * GeodeticCRS may be associated with either CartesianCS or EllipsoidalCS, but this method
                 * shall accept only EllipsoidalCS. Actually we should accept only GeographicCRS, but we
                 * relax this condition by accepting GeodeticCRS with EllipsoidalCS.
                 */
                return cs;
            }
        }
        return null;
    }

    /**
     * Returns the authority code for a variant of the given coordinate system with the given axes.
     * For consistency, we search for an authority code only if the given <abbr>CS</abbr> was itself
     * defined by an <abbr>EPSG</abbr>.
     *
     * @param  base  the coordinate system for which to get a variant.
     * @param  axes  the axes of the desired coordinate system.
     * @return a variant of the given coordinate system with the given axes.
     */
    private static String getEpsgCode(final CoordinateSystem base, final CoordinateSystemAxis[] axes) {
        if (IdentifiedObjects.getIdentifier(base, Citations.EPSG) != null) {
            final Integer epsgCode = CoordinateSystems.getEpsgCode(base.getClass(), axes);
            if (epsgCode != null) {
                return Constants.EPSG + ':' + epsgCode;
            }
        }
        return null;
    }

    /**
     * Logs a warnings for a non-fatal exception occurring while creating a coordinate system.
     */
    private static void warning(final NoSuchAuthorityCodeException e) {
        Logging.recoverableException(Logger.getLogger(Loggers.CRS_FACTORY),
                EllipsoidalHeightCombiner.class, "createCompoundCRS", e);
    }

    /**
     * Suggests properties for a compound CRS made of the given elements.
     * This method builds a default CRS name and domain of validity.
     *
     * @param  components  the components for which to get a default set of properties.
     * @return suggested properties in a modifiable map. Callers can modify the returned map.
     */
    static Map<String, Object> properties(final CoordinateReferenceSystem[] components) {
        final var name = new StringBuilder(40);
        Extent domain = null;
        for (int i=0; i<components.length; i++) {
            final CoordinateReferenceSystem crs = components[i];
            ArgumentChecks.ensureNonNullElement("components", i, crs);
            if (i != 0) name.append(" + ");
            name.append(crs.getName().getCode());
            domain = Extents.intersection(domain, crs.getDomainOfValidity());
        }
        final var properties = new HashMap<String, Object>(4);
        properties.put(CoordinateReferenceSystem.NAME_KEY, name.toString());
        properties.put(CoordinateReferenceSystem.DOMAIN_OF_VALIDITY_KEY, domain);
        return properties;
    }
}
