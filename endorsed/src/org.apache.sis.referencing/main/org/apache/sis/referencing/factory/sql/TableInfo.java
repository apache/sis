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
package org.apache.sis.referencing.factory.sql;

import java.util.Map;
import javax.measure.Unit;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.referencing.internal.shared.WKTKeywords;


/**
 * Information (such as columns of particular interest) about a specific <abbr>EPSG</abbr> table.
 * This class uses the mixed-case variant of the <abbr>EPSG</abbr> {@linkplain #table table names}.
 * The {@link #values()} can be tested in preference order for finding the table of an object.
 * Those tables are used by the {@link EPSGDataAccess#createObject(String)} method in order to
 * detect which of the following methods should be invoked for a given code:
 *
 * {@link EPSGDataAccess#createCoordinateReferenceSystem(String)}
 * {@link EPSGDataAccess#createCoordinateSystem(String)}
 * {@link EPSGDataAccess#createDatum(String)}
 * {@link EPSGDataAccess#createEllipsoid(String)}
 * {@link EPSGDataAccess#createUnit(String)}
 *
 * <h4>Ambiguity</h4>
 * As of ISO 19111:2019, we have no standard way to identify the geocentric case from a {@link Class} argument
 * because the standard does not provide the {@code GeocentricCRS} interface. This implementation fallbacks on
 * the GeoAPI-specific geocentric <abbr>CRS</abbr> class. This special case is implemented in the
 * {@link #appendWhere(EPSGDataAccess, Object, StringBuilder)} method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
enum TableInfo {
    /**
     * Information about the "Coordinate Reference System" table.
     */
    CRS(CoordinateReferenceSystem.class,
            "\"Coordinate Reference System\"",
            "COORD_REF_SYS_CODE",
            "COORD_REF_SYS_NAME",
            "COORD_REF_SYS_KIND",
            new Class<?>[] { ProjectedCRS.class,   GeographicCRS.class,   GeocentricCRS.class,
                             VerticalCRS.class,    CompoundCRS.class,     EngineeringCRS.class,
                             DerivedCRS.class,     TemporalCRS.class,     ParametricCRS.class},     // See comment below
            new String[]   {"projected",          "geographic 2D",       "geocentric",              // 3D case handled below
                            "vertical",           "compound",            "engineering",
                            "derived",            "temporal",            "parametric"},             // See comment below
            "SHOW_CRS", true),
            /*
             * Above declaration could omit Derived, Temporal and Parametric cases because they are not defined
             * by the EPSG repository (at least as of version 8.9). In particular we are not sure if EPSG would
             * chose to use "time" or "temporal". However, omitting those types slow down a lot the search for
             * CRS matching an existing one (even if it still work).
             */

    /**
     * Information about the "Datum" table.
     */
    DATUM(Datum.class,
            "\"Datum\"",
            "DATUM_CODE",
            "DATUM_NAME",
            "DATUM_TYPE",
            new Class<?>[] { DatumEnsemble.class,  // Need to be first because Apache SIS uses as mixin interface.
                             GeodeticDatum.class,  VerticalDatum.class,   EngineeringDatum.class,
                             TemporalDatum.class,  ParametricDatum.class},
            new String[]   {"ensemble",
                            "geodetic",           "vertical",            "engineering",
                            "temporal",           "parametric"},         // Same comment as in the CRS case above.
            null, true),

    /**
     * Information about the "Conventional RS" table.
     * This enumeration usually needs to be ignored because the current type is too generic.
     *
     * @see #isSpecificEnough()
     */
    CONVENTIONAL_RS(IdentifiedObject.class,
            "\"Conventional RS\"",
            "CONVENTIONAL_RS_CODE",
            "CONVENTIONAL_RS_NAME",
            null, null, null, null, false),

    /**
     * Information about the "Ellipsoid" table.
     */
    ELLIPSOID(Ellipsoid.class,
            "\"Ellipsoid\"",
            "ELLIPSOID_CODE",
            "ELLIPSOID_NAME",
            null, null, null, null, false),

    /**
     * Information about the "Prime Meridian" table.
     */
    PRIME_MERIDIAN(PrimeMeridian.class,
            "\"Prime Meridian\"",
            "PRIME_MERIDIAN_CODE",
            "PRIME_MERIDIAN_NAME",
            null, null, null, null, false),

    /**
     * Information about the "Coordinate_Operation" table.
     */
    OPERATION(CoordinateOperation.class,
            "\"Coordinate_Operation\"",
            "COORD_OP_CODE",
            "COORD_OP_NAME",
            "COORD_OP_TYPE",
            new Class<?>[] { Conversion.class, Transformation.class},
            new String[]   {"conversion",     "transformation"},
            "SHOW_OPERATION", true),

    /**
     * Information about the "Coordinate_Operation Method" table.
     */
    METHOD(OperationMethod.class,
            "\"Coordinate_Operation Method\"",
            "COORD_OP_METHOD_CODE",
            "COORD_OP_METHOD_NAME",
            null, null, null, null, false),

    /**
     * Information about the "Coordinate_Operation Parameter" table.
     */
    PARAMETER(ParameterDescriptor.class,
            "\"Coordinate_Operation Parameter\"",
            "PARAMETER_CODE",
            "PARAMETER_NAME",
            null, null, null, null, false),

    /**
     * Information about the "Extent" table.
     */
    EXTENT(Extent.class,
            "\"Extent\"",
            "EXTENT_CODE",
            "EXTENT_NAME",
            null, null, null, null, false),

    /**
     * Information about the "Coordinate System" table.
     */
    CS(CoordinateSystem.class,
            "\"Coordinate System\"",
            "COORD_SYS_CODE",
            "COORD_SYS_NAME",
            "COORD_SYS_TYPE",
            new Class<?>[] {CartesianCS.class,      EllipsoidalCS.class,      VerticalCS.class,      LinearCS.class,
                            SphericalCS.class,      PolarCS.class,            CylindricalCS.class,
                            TimeCS.class,           ParametricCS.class,       AffineCS.class},
            new String[]   {WKTKeywords.Cartesian,  WKTKeywords.ellipsoidal,  WKTKeywords.vertical,  WKTKeywords.linear,
                            WKTKeywords.spherical,  WKTKeywords.polar,        WKTKeywords.cylindrical,
                            WKTKeywords.temporal,   WKTKeywords.parametric,   WKTKeywords.affine},      // Same comment as in the CRS case above.
            null, false),

    /**
     * Information about the "Coordinate Axis" table.
     */
    AXIS(CoordinateSystemAxis.class,
            "\"Coordinate Axis\" AS CA INNER JOIN \"Coordinate Axis Name\" AS CAN " +
                                "ON CA.COORD_AXIS_NAME_CODE=CAN.COORD_AXIS_NAME_CODE",
            "COORD_AXIS_CODE",
            "COORD_AXIS_NAME",
            null, null, null, null, false),

    /**
     * Information about the "Unit of Measure" table.
     */
    UNIT(Unit.class,
            "\"Unit of Measure\"",
            "UOM_CODE",
            "UNIT_OF_MEAS_NAME",
            null, null, null, null, false);

    /**
     * Types to consider as synonymous for searching purposes. This map exists for historical reasons,
     * because dynamic datum and datum ensemble did not existed in older <abbr>ISO</abbr> 19111 standards.
     * If an object to search is "geodetic", there is a possibility that it is defined in the old way and
     * actually appears as a "dynamic geodetic" or "ensemble" in the <abbr>EPSG</abbr> geodetic dataset.
     *
     * <p>The "geographic 3D" case is handled in a special way. It is considered as a synonymous of
     * "geographic 2D" only when we don't know the number of dimensions.</p>
     */
    private static final Map<String, String[]> SYNONYMOUS_TYPES = Map.of(
            "geodetic",      new String[] {"dynamic geodetic", "ensemble"},
            "geographic 2D", new String[] {"geographic 3D"});

    /**
     * Types to replace by specialized types when the user-specified instance implements a mixin interface.
     * For example, {@link DynamicReferenceFrame} means to not search for any geodetic datum, but only for
     * dynamic geodetic datum.
     */
    private static final Map<String, String> DYNAMIC_TYPES = Map.of(
            "geodetic", "dynamic geodetic");
            // We would expect a "dynamic vertical" as well, but we don't see it yet in EPSG database.

    /**
     * The class of object to be created (usually a GeoAPI interface).
     */
    final Class<?> type;

    /**
     * The table name in mixed-case and without quotes.
     * This name can be converted to the actual table names by {@link SQLTranslator#toActualTableName(String)}.
     */
    final String table;

    /**
     * The <abbr>SQL</abbr> fragment to use in the {@code FROM} clause.
     * This is usually the table name, including the quotes for mixed-case names.
     * In sometime, this is a more complex clause including {@code JOIN} statements.
     */
    final String fromClause;

    /**
     * Column name for the code (usually with the {@code "_CODE"} suffix).
     */
    final String codeColumn;

    /**
     * Column name for the name (usually with the {@code "_NAME"} suffix).
     */
    final String nameColumn;

    /**
     * Column name for the type (usually with the {@code "_TYPE"} suffix), or {@code null}.
     */
    private final String typeColumn;

    /**
     * Sub-interfaces of {@link #type} to handle, or {@code null} if none.
     */
    private final Class<?>[] subTypes;

    /**
     * Names of {@link #subTypes} in the database, or {@code null} if none.
     * This array must have the same length as {@link #subTypes}.
     */
    private final String[] typeNames;

    /**
     * The column that specify if the object should be shown, or {@code null} if none.
     */
    final String showColumn;

    /**
     * Whether the table had an {@code "AREA_OF_USE_CODE"} column
     * in the legacy versions (before version 10) of the <abbr>EPSG</abbr> database.
     */
    final boolean areaOfUse;

    /**
     * Stores information about a specific table.
     *
     * @param type        the class of object to be created (usually a GeoAPI interface).
     * @param fromClause  The <abbr>SQL</abbr> fragment to use in the {@code FROM} clause, including quotes.
     * @param codeColumn  column name for the code (usually with the {@code "_CODE"} suffix).
     * @param nameColumn  column name for the name (usually with the {@code "_NAME"} suffix).
     * @param typeColumn  column type for the type (usually with the {@code "_TYPE"} suffix), or {@code null}.
     * @param subTypes    sub-interfaces of {@link #type} to handle, or {@code null} if none.
     * @param typeNames   names of {@code subTypes} in the database, or {@code null} if none.
     * @param showColumn  the column that specify if the object should be shown, or {@code null} if none.
     * @param areaOfUse   whether the table had an {@code "AREA_OF_USE_CODE"} column in the legacy versions.
     */
    private TableInfo(final Class<?> type,
                      final String fromClause, final String codeColumn, final String nameColumn,
                      final String typeColumn, final Class<?>[] subTypes, final String[] typeNames,
                      final String showColumn, final boolean areaOfUse)
    {
        this.type       = type;
        this.fromClause = fromClause;
        this.codeColumn = codeColumn;
        this.nameColumn = nameColumn;
        this.typeColumn = typeColumn;
        this.subTypes   = subTypes;
        this.typeNames  = typeNames;
        this.showColumn = showColumn;
        this.areaOfUse  = areaOfUse;
        final int start = fromClause.indexOf('"') + 1;
        table = fromClause.substring(start, fromClause.indexOf('"', start)).intern();
    }

    /**
     * Returns whether this enumeration value can be used when looking a table by an object type.
     * This method returns {@code false} for types that are too generic.
     */
    final boolean isSpecificEnough() {
        return type != IdentifiedObject.class;
    }

    /**
     * Returns a key which describes the type and/or the number of dimensions of the given object.
     * The current implementation relies on the fact that {@link GeographicCRS} is the only type
     * for which the current <abbr>EPSG</abbr> database distinguishes the number of dimensions,
     * but callers should not depend on this assumption as it may change in any future version.
     *
     * <h4>Maintenance note</h4>
     * If the implementation of this method is modified, then the extraction of {@code dimension} and
     * {@code userType} properties in the {@link #appendWhere(EPSGDataAccess, Object, StringBuilder)}
     * method body must be updated accordingly.
     */
    static Object toCacheKey(final IdentifiedObject object) {
        if (object instanceof GeodeticCRS) {
            final CoordinateSystem cs = ((GeodeticCRS) object).getCoordinateSystem();
            if (cs instanceof EllipsoidalCS) {
                return cs.getDimension();
            }
        }
        return object.getClass();
    }

    /**
     * Extracts the type from a value computed by {@link #toCacheKey(IdentifiedObject)}.
     *
     * @param  object  value computed by {@link #toCacheKey(IdentifiedObject)}.
     * @return the class of the object to search, ignoring the number of dimensions.
     * @throws ClassCastException if the given object has not been created by {@link #toCacheKey(IdentifiedObject)}.
     */
    static Class<?> typeOfCacheKey(final Object object) {
        if (object instanceof Integer) {
            return GeographicCRS.class;
        }
        return (Class<?>) object;
    }

    /**
     * Appends a {@code WHERE} clause together with a condition for searching the most specific subtype.
     * The clause appended by this method looks like the following example:
     *
     * {@snippet lang="sql" :
     *     WHERE (COORD_REF_SYS_KIND = 'geographic 2D' OR COORD_REF_SYS_KIND = 'geographic 3D') AND
     *     }
     *
     * The <abbr>SQL</abbr> fragment will have a trailing {@code WHERE} or {@code AND} keyword.
     * Therefore, the caller shall add at least one condition after this method call.
     *
     * <h4>Object type</h4>
     * The {@code object} argument shall be one of the following types:
     *
     * <ul>
     *   <li>An {@link IdentifiedObject} instance.</li>
     *   <li>The {@link Class} of an {@code IdentifiedObject}. It may be an implementation class.</li>
     *   <li>An opaque key computed by {@link #toCacheKey(IdentifiedObject)}.</li>
     * </ul>
     *
     * This method returns a generalization of the {@code object} argument: either a GeoAPI interface,
     * or {@code object} if it was a cache key computed by {@link #toCacheKey(IdentifiedObject)}.
     *
     * @param  factory  the factory which is writing a <abbr>SQL</abbr> statement.
     * @param  object   the instance, class or cache key to search in the database.
     * @param  buffer   where to append the {@code WHERE} clause.
     * @return the {@code object} argument, potentially generalized.
     */
    final Object appendWhere(final EPSGDataAccess factory, final Object object, final StringBuilder buffer) {
        final int dimension;            // 0 if not applicable. This is applicable only to `GeographicCRS`.
        final Class<?> userType;
        if (object instanceof Integer) {
            dimension = (Integer) object;
            userType  = GeographicCRS.class;
        } else if (object instanceof Class<?>) {
            userType  = (Class<?>) object;
            dimension = 0;
        } else if (object instanceof GeodeticCRS) {
            final CoordinateSystem cs = ((GeodeticCRS) object).getCoordinateSystem();
            if (cs instanceof EllipsoidalCS) {
                userType  = GeographicCRS.class;
                dimension = cs.getDimension();      // Intentionally restricted to this specific case.
            } else {
                if (cs instanceof CartesianCS || cs instanceof SphericalCS) {
                    userType = GeocentricCRS.class;
                } else {
                    userType = object.getClass();
                }
                dimension = 0;
            }
        } else {
            userType  = object.getClass();
            dimension = 0;
        }
        /*
         * Above code decomposed the given `object`.
         * The rest of this method builds the SQL.
         */
        buffer.append(" WHERE ");
        if (typeColumn != null) {
            for (int i=0; i<subTypes.length; i++) {
                final Class<?> subType = subTypes[i];
                if (subType.isAssignableFrom(userType)) {
                    /*
                     * Found the type to request in the `COORD_REF_SYS_KIND` or `DATUM_TYPE` columns.
                     * The mixin interfaces need to be handled in a special way.
                     */
                    String typeName = typeNames[i];
                    if (DynamicReferenceFrame.class.isAssignableFrom(userType)) {
                        typeName = DYNAMIC_TYPES.getOrDefault(typeName, typeName);
                    }
                    /*
                     * We may need to look for more than one type if some information are missing
                     * (for example, the dimension when EPSG distinguishes the 2D and 3D cases).
                     */
                    String[] synonymous = SYNONYMOUS_TYPES.get(typeName);
                    if (synonymous != null && dimension > 0 && dimension <= 9) {
                        final String suffix = "2D".replace('2',  (char) ('0' + dimension));
                        if (typeName.endsWith(suffix)) {
                            synonymous = null;
                        } else {
                            for (String alternative : synonymous) {
                                if (alternative.endsWith(suffix)) {
                                    typeName = alternative;
                                    synonymous = null;
                                    break;
                                }
                            }
                        }
                    }
                    /*
                     * Build the SQL `WHERE` clause.
                     */
                    buffer.append('(').append(typeColumn).append(" = '").append(typeName).append('\'');
                    if (synonymous != null) {
                        for (String alternative : synonymous) {
                            buffer.append(" OR ").append(typeColumn).append(" = '").append(alternative).append('\'');
                        }
                    }
                    buffer.append(") AND ");
                    return subType;
                }
            }
        }
        return (dimension != 0) ? dimension : type;
    }

    /**
     * Verifies that the given <abbr>SQL</abbr> statement contains the expected table and columns.
     * This method is for assertions only. It may either returns {@code false} if the query is not
     * valid, or throws an {@link AssertionError} itself for providing more details.
     *
     * @param  sql  the <abbr>SQL</abbr> statement to validate.
     * @return whether the statement is valid.
     */
    final boolean validate(final String sql) {
        if (sql.contains(table)) {
            if (type.isAssignableFrom(IdentifiedObject.class)) {
                if (!sql.contains(codeColumn)) throw new AssertionError(codeColumn);
                if (!sql.contains(nameColumn)) throw new AssertionError(nameColumn);
            }
            return true;
        }
        return false;
    }
}
