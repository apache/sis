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
package org.apache.sis.storage.gdal;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.measure.Units;


/**
 * Creates Coordinate Reference System instances form {@link PJ} objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class ReferencingFactory extends AbstractFactory {
    /**
     * The default factory instance.
     */
    static final ReferencingFactory INSTANCE = new ReferencingFactory();

    /**
     * The factory for coordinate reference system objects.
     */
    private final CRSFactory crsFactory;

    /**
     * The factory for coordinate system objects.
     */
    private final CSFactory csFactory;

    /**
     * The factory for datum objects.
     */
    private final DatumFactory datumFactory;

    /**
     * Poll of identifiers created by this factory.
     */
    private final Map<String,Identifier> identifiers;

    /**
     * Pool of {@literal Proj.4} objects created so far. The keys are the Proj.4 definition strings.
     * The same {@link PJ} instance may appear more than once if various definition strings resulted
     * in the same Proj.4 object.
     */
    private final WeakValueHashMap<String,PJ> pool;

    /**
     * Creates a new factory.
     */
    private ReferencingFactory() {
        crsFactory   = DefaultFactories.forBuildin(CRSFactory.class);
        csFactory    = DefaultFactories.forBuildin(CSFactory.class);
        datumFactory = DefaultFactories.forBuildin(DatumFactory.class);
        identifiers  = new HashMap<>();
        pool         = new WeakValueHashMap<>(String.class);
    }

    /**
     * Returns the value of the given parameter as an identifier, or {@code null} if none.
     * The given parameter key shall include the {@code '+'} prefix and {@code '='} suffix,
     * for example {@code "+proj="}. This is a helper method for providing the {@code name}
     * property value in constructors.
     *
     * @param  definition  the Proj.4 definition string to parse.
     * @param  keyword     the parameter name.
     * @return the parameter value as an identifier.
     */
    private Map<String,Identifier> identifier(final String definition, final String keyword) {
        String value = "";
        if (keyword != null) {
            int i = definition.indexOf(keyword);
            if (i >= 0) {
                i += keyword.length();
                final int stop = definition.indexOf(' ', i);
                value = (stop >= 0) ? definition.substring(i, stop) : definition.substring(i);
                value = value.trim();
            }
        }
        if (value.isEmpty()) {
            value = "Unnamed";
        }
        return identifier(value);
    }

    /**
     * Returns the identifier for the given code in {@literal Proj.4} namespace.
     */
    private Map<String,Identifier> identifier(final String code) {
        Identifier id = identifiers.computeIfAbsent(code, (k) -> {
            short i18n = 0;
            if (k.equalsIgnoreCase("Unnamed")) i18n = Vocabulary.Keys.Unnamed;
            if (k.equalsIgnoreCase("Unknown")) i18n = Vocabulary.Keys.Unknown;
            return new ImmutableIdentifier(Citations.PROJ4, Constants.PROJ4, k, null,
                    (i18n != 0) ? Vocabulary.formatInternational(i18n) : null);
        });
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, id);
    }

    /**
     * Creates a geodetic datum from the given {@literal Proj.4} wrapper.
     *
     * @param  pj  the Proj.4 object to wrap.
     */
    private GeodeticDatum createDatum(final PJ pj) throws FactoryException {
        final PrimeMeridian pm;
        final double greenwichLongitude = pj.getGreenwichLongitude();
        if (greenwichLongitude == 0) {
            pm = CommonCRS.WGS84.datum().getPrimeMeridian();
        } else {
            pm = datumFactory.createPrimeMeridian(identifier("Unnamed"), greenwichLongitude, Units.DEGREE);
        }
        final String definition = pj.getCode();
        return datumFactory.createGeodeticDatum(identifier(definition, "+datum="),
               datumFactory.createEllipsoid    (identifier(definition, "+ellps="),
                    pj.getSemiMajorAxis(), pj.getSemiMinorAxis(), Units.METRE), pm);
    }

    /**
     * Creates a coordinate reference system from the given {@literal Proj.4} wrapper.
     * The given {@code pj} will be stored as the CRS identifier.
     *
     * @param  pj          the Proj.4 object to wrap.
     * @param  withHeight  whether to include a height axis.
     */
    private CoordinateReferenceSystem createCRS(final PJ pj, final boolean withHeight) throws FactoryException {
        final PJ.Type type = pj.getType();
        final boolean geographic = PJ.Type.GEOGRAPHIC.equals(type);
        final char[] dir = pj.getAxisDirections();
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[withHeight ? dir.length : 2];
        for (int i=0; i<axes.length; i++) {
            final char d = Character.toLowerCase(dir[i]);
            char abbreviation = Character.toUpperCase(d);
            boolean vertical = false;
            final AxisDirection c;
            final String name;
            switch (d) {
                case 'e': c = AxisDirection.EAST;  name = geographic ? "Geodetic longitude" : "Easting";  break;
                case 'w': c = AxisDirection.WEST;  name = geographic ? "Geodetic longitude" : "Westing";  break;
                case 'n': c = AxisDirection.NORTH; name = geographic ? "Geodetic latitude"  : "Northing"; break;
                case 's': c = AxisDirection.SOUTH; name = geographic ? "Geodetic latitude"  : "Southing"; break;
                case 'u': c = AxisDirection.UP;    name = "Height";  vertical = true; abbreviation = 'h'; break;
                case 'd': c = AxisDirection.DOWN;  name = "Depth";   vertical = true; break;
                default:  c = AxisDirection.OTHER; name = "Unknown"; break;
            }
            if (geographic && AxisDirections.isCardinal(c)) {
                abbreviation = (d == 'e' || d == 'w') ? 'λ' : 'φ';
            }
            final Unit<?> unit = (vertical || !geographic) ? Units.METRE.divide(pj.getLinearUnitToMetre(vertical)) : Units.DEGREE;
            axes[i] = csFactory.createCoordinateSystemAxis(identifier(name), String.valueOf(abbreviation).intern(), c, unit);
        }
        /*
         * At this point we got the coordinate system axes. Now create the CRS. The given Proj.4 object
         * will be stored as the CRS identifier for allowing OperationFactory to get it back before to
         * attempt to create a new one for a given CRS.
         */
        final Map<String,Identifier> csName = identifier("Unnamed");
        final Map<String,Identifier> name = new HashMap<>(identifier(pj.getName()));
        name.put(CoordinateReferenceSystem.IDENTIFIERS_KEY, pj);
        switch (type) {
            case GEOGRAPHIC: {
                return crsFactory.createGeographicCRS(name, createDatum(pj), withHeight ?
                        csFactory.createEllipsoidalCS(csName, axes[0], axes[1], axes[2]) :
                        csFactory.createEllipsoidalCS(csName, axes[0], axes[1]));
            }
            case GEOCENTRIC: {
                return crsFactory.createGeocentricCRS(name, createDatum(pj),
                        csFactory.createCartesianCS(csName, axes[0], axes[1], axes[2]));
            }
            case PROJECTED: {
                final CoordinateReferenceSystem base = createCRS(unique(new PJ(pj)), withHeight);
                final Conversion fromBase = null;   // TODO
                return crsFactory.createProjectedCRS(name, (GeographicCRS) base, fromBase, withHeight ?
                        csFactory.createCartesianCS(csName, axes[0], axes[1], axes[2]) :
                        csFactory.createCartesianCS(csName, axes[0], axes[1]));
            }
            default: {
                throw new FactoryException(Errors.format(Errors.Keys.UnknownEnumValue_2, type, PJ.Type.class));
            }
        }
    }

    /**
     * Gets the {@literal Proj.4} object from the given coordinate reference system. If an existing {@code PJ}
     * instance is found, returns it. Otherwise creates a new {@code PJ} instance from a Proj.4 definition
     * inferred from the given CRS. This method is the converse of {@link #createCRS(PJ, boolean)}.
     */
    private PJ unwrapOrCreate(final CoordinateReferenceSystem crs) throws FactoryException {
        for (final Identifier id : crs.getIdentifiers()) {
            if (id instanceof PJ) {
                return (PJ) id;
            }
        }
        return unique(new PJ(Proj4.definition(crs)));
    }

    /**
     * Returns a unique instance of the given {@literal Proj.4} object.
     */
    @SuppressWarnings("FinalizeCalledExplicitly")
    private PJ unique(PJ pj) {
        final PJ existing = pool.putIfAbsent(pj.getCode(), pj);
        if (existing != null) {
            pj.finalize();          // Release Proj.4 resources.
            return existing;
        }
        return pj;
    }

    /**
     * Creates a coordinate reference system from the given {@literal Proj.4} definition string.
     *
     * @param  definition  the Proj.4 definition.
     * @param  withHeight  whether to include a height axis.
     */
    public CoordinateReferenceSystem createCRS(final String definition, final boolean withHeight) throws FactoryException {
        PJ pj = pool.get(definition);
        if (pj == null) {
            pj = unique(new PJ(definition));
            pool.putIfAbsent(definition, pj);
        }
        return createCRS(pj, withHeight);
    }

    /**
     * Creates an operation for conversion or transformation between two coordinate reference systems.
     *
     * @param  sourceCRS  the source coordinate reference system.
     * @param  targetCRS  the target coordinate reference system.
     * @return a coordinate operation for transforming coordinates from the given source CRS to the given target CRS.
     * @throws FactoryException if the given CRS are not instances recognized by this class.
     */
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        Identifier id;
        String src = null, tgt = null, name = "Unnamed";
        if ((id = sourceCRS.getName()) != null) src = id.getCode();
        if ((id = targetCRS.getName()) != null) tgt = id.getCode();
        if (src != null || tgt != null) {
            final StringBuilder buffer = new StringBuilder();
            if (src != null) buffer.append("From ").append(src);
            if (tgt != null) buffer.append(buffer.length() == 0 ? "To " : " to ").append(tgt);
            name = buffer.toString();
        }
        final Transform tr = new Transform(unwrapOrCreate(sourceCRS), is3D("sourceCRS", sourceCRS),
                                           unwrapOrCreate(targetCRS), is3D("targetCRS", targetCRS));
        /*
         * TODO: should create a more specific coordinate operation.
         */
        return new AbstractCoordinateOperation(identifier(name), sourceCRS, targetCRS, null, tr);
    }

    /**
     * Returns whether the given CRS is three-dimensional.
     * Thrown an exception if the number of dimension is unsupported.
     */
    private static boolean is3D(final String arg, final CoordinateReferenceSystem crs) throws FactoryException {
        final int dim = crs.getCoordinateSystem().getDimension();
        final boolean is3D = (dim >= 3);
        if (dim < 2 || dim > 3) {
            throw new FactoryException(Errors.format(Errors.Keys.MismatchedDimension_3, arg, is3D ? 3 : 2, dim));
        }
        return is3D;
    }
}
