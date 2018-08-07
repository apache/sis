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
package org.apache.sis.internal.metadata;

import java.util.Map;
import java.util.HashMap;
import org.opengis.metadata.extent.Extent;
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
import org.opengis.util.FactoryException;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * A class in charges of combining two-dimensional geographic or projected CRS with an ellipsoidal height
 * into a three-dimensional CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class EllipsoidalHeightCombiner {
    /**
     * The kind of factory initialized by {@link #initialize(int)}.
     */
    protected static final int CRS=1, CS=2, OPERATION=4;

    /**
     * The factory to use for creating compound or three-dimensional geographic CRS.
     */
    protected CRSFactory crsFactory;

    /**
     * The factory to use for creating three-dimensional ellipsoidal CS, if needed.
     */
    protected CSFactory csFactory;

    /**
     * The factory to use for creating defining conversions, if needed.
     */
    protected CoordinateOperationFactory opFactory;

    /**
     * Creates a new combiner with no initial factory.
     * Subclasses must override the {@link #initialize(int)} method.
     */
    protected EllipsoidalHeightCombiner() {
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
        this.crsFactory = crsFactory;
        this.csFactory  = csFactory;
        this.opFactory  = opFactory;
    }

    /**
     * Initializes the factory identified by the given code. This is used for lazy initialization if any factory given
     * to the constructor was null. In such case, subclass must override. If the same {@code EllipsoidalHeightCombiner}
     * instance is used more than once, than it is subclass responsibility to verify that the {@link #crsFactory},
     * {@link #csFactory} or {@link #opFactory} field has not already been set.
     *
     * @param  factoryTypes  a bitwise combination of {@link #CRS}, {@link #CS} and {@link #OPERATION}.
     */
    protected void initialize(final int factoryTypes) {
    }

    /**
     * Creates a compound CRS, but we special processing for (two-dimensional Geographic + ellipsoidal heights) tuples.
     * If any such tuple is found, a three-dimensional geographic CRS is created instead than the compound CRS.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  components  ordered array of {@code CoordinateReferenceSystem} objects.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    public final CoordinateReferenceSystem createCompoundCRS(final Map<String,?> properties,
            CoordinateReferenceSystem... components) throws FactoryException
    {
        for (int i=0; i<components.length; i++) {
            final CoordinateReferenceSystem vertical = components[i];
            if (vertical instanceof VerticalCRS) {
                final VerticalDatum datum = ((VerticalCRS) vertical).getDatum();
                if (datum != null && datum.getVerticalDatumType() == VerticalDatumTypes.ELLIPSOIDAL) {
                    int axisPosition = 0;
                    CoordinateSystem cs = null;
                    CoordinateReferenceSystem crs = null;
                    if (i == 0 || (cs = getCsIfHorizontal2D(crs = components[i - 1])) == null) {
                        /*
                         * GeographicCRS are normally before VerticalCRS. But Apache SIS is tolerant to the
                         * opposite order (note however that such ordering is illegal according ISO 19162).
                         */
                        if (i+1 >= components.length || (cs = getCsIfHorizontal2D(crs = components[i + 1])) == null) {
                            continue;
                        }
                        axisPosition = 1;
                    }
                    /*
                     * At this point we have the horizontal and vertical components. The horizontal component
                     * begins at 'axisPosition', which is almost always zero. Create the three-dimensional CRS.
                     * If the result is the CRS to be returned directly by this method (components.length == 2),
                     * use the properties given in argument. Otherwise we need to use other properties; current
                     * implementation recycles the properties of the existing two-dimensional CRS.
                     */
                    final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[3];
                    axes[axisPosition++   ] = cs.getAxis(0);
                    axes[axisPosition++   ] = cs.getAxis(1);
                    axes[axisPosition %= 3] = vertical.getCoordinateSystem().getAxis(0);
                    final ReferencingServices referencing = ReferencingServices.getInstance();
                    final Map<String,?> csProps = referencing.getProperties(cs, false);
                    final Map<String,?> crsProps = (components.length == 2) ? properties : referencing.getProperties(crs, false);
                    if (crs instanceof GeodeticCRS) {
                        initialize(CS | CRS);
                        cs = csFactory.createEllipsoidalCS(csProps, axes[0], axes[1], axes[2]);
                        crs = crsFactory.createGeographicCRS(crsProps, ((GeodeticCRS) crs).getDatum(), (EllipsoidalCS) cs);
                    } else {
                        initialize(CS | CRS | OPERATION);
                        final ProjectedCRS proj = (ProjectedCRS) crs;
                        GeographicCRS base = proj.getBaseCRS();
                        if (base.getCoordinateSystem().getDimension() == 2) {
                            base = (GeographicCRS) createCompoundCRS(referencing.getProperties(base, false), base, vertical);
                        }
                        /*
                         * In Apache SIS implementation, the Conversion contains the source and target CRS together with
                         * a MathTransform2D. We need to recreate the same conversion, but without CRS and MathTransform
                         * for letting SIS create or associate new ones, which will be three-dimensional now.
                         */
                        Conversion fromBase = proj.getConversionFromBase();
                        fromBase = opFactory.createDefiningConversion(referencing.getProperties(fromBase, true),
                                    fromBase.getMethod(), fromBase.getParameterValues());
                        cs = csFactory.createCartesianCS(csProps, axes[0], axes[1], axes[2]);
                        crs = crsFactory.createProjectedCRS(crsProps, base, fromBase, (CartesianCS) cs);
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
            default: initialize(CRS); return crsFactory.createCompoundCRS(properties, components);
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
                 * ProjectedCRS are guaranteed to be associated to CartesianCS, so we do not test that.
                 * GeodeticCRS may be associated to either CartesianCS or EllipsoidalCS, but this method
                 * shall accept only EllipsoidalCS. Actually we should accept only GeographicCRS, but we
                 * relax this condition by accepting GeodeticCRS with EllipsoidalCS.
                 */
                return cs;
            }
        }
        return null;
    }

    /**
     * Suggests properties for a compound CRS made of the given elements.
     * This method builds a default CRS name and domain of validity.
     *
     * @param  components  the components for which to get a default set of properties.
     * @return suggested properties in a modifiable map. Callers can modify the returned map.
     */
    public static Map<String,Object> properties(final CoordinateReferenceSystem... components) {
        final StringBuilder name = new StringBuilder(40);
        Extent domain = null;
        for (int i=0; i<components.length; i++) {
            final CoordinateReferenceSystem crs = components[i];
            ArgumentChecks.ensureNonNullElement("components", i, crs);
            if (i != 0) name.append(" + ");
            name.append(crs.getName().getCode());
            domain = Extents.intersection(domain, crs.getDomainOfValidity());
        }
        final Map<String,Object> properties = new HashMap<>(2);
        properties.put(CoordinateReferenceSystem.NAME_KEY, name.toString());
        properties.put(CoordinateReferenceSystem.DOMAIN_OF_VALIDITY_KEY, domain);
        return properties;
    }
}
