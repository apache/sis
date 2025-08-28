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

import javax.measure.Unit;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.referencing.privy.WKTKeywords;

// Specific to the geoapi-4.0 branch:
import org.apache.sis.referencing.crs.DefaultGeocentricCRS;


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
 * the <abbr>SIS</abbr>-specific geocentric <abbr>CRS</abbr> class. This special case is implemented in the
 * {@link #where(EPSGDataAccess, IdentifiedObject, StringBuilder)} method.
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
            new Class<?>[] { ProjectedCRS.class,   GeographicCRS.class,   DefaultGeocentricCRS.class,
                             VerticalCRS.class,    CompoundCRS.class,     EngineeringCRS.class,
                             DerivedCRS.class,     TemporalCRS.class,     ParametricCRS.class},     // See comment below
            new String[]   {"projected",          "geographic",          "geocentric",
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
            new Class<?>[] { GeodeticDatum.class,  VerticalDatum.class,   EngineeringDatum.class,
                             TemporalDatum.class,  ParametricDatum.class},
            new String[]   {"geodetic",           "vertical",            "engineering",
                            "temporal",           "parametric"},         // Same comment as in the CRS case above.
            null, true),

    /**
     * Information about the "Conventional RS" table.
     * This enumeration usually needs to be ignored because the current type is too generic.
     *
     * @see #isCandidate()
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
    final boolean isCandidate() {
        return type != IdentifiedObject.class;
    }

    /**
     * Appends a {@code WHERE} clause together with a condition for searching the specified object.
     * This method delegates to {@link #where(EPSGDataAccess, Class, StringBuilder)} with the type
     * of the given object, except that some object properties may be inspected for resolving ambiguities.
     *
     * @param  factory  the factory which is writing a <abbr>SQL</abbr> statement.
     * @param  object   the object to search in the database.
     * @param  buffer   where to append the {@code WHERE} clause.
     */
    final void where(final EPSGDataAccess factory, final IdentifiedObject object, final StringBuilder buffer) {
        Class<?> userType = object.getClass();
        if (object instanceof GeodeticCRS) {
            final CoordinateSystem cs = ((GeodeticCRS) object).getCoordinateSystem();
            if (cs instanceof EllipsoidalCS) {
                userType = GeographicCRS.class;
            } else if (cs instanceof CartesianCS || cs instanceof SphericalCS) {
                userType = DefaultGeocentricCRS.class;
            }
        }
        where(factory, userType, buffer);
    }

    /**
     * Appends a {@code WHERE} clause together with a condition for searching the most specific subtype,
     * if such condition can be added. The clause appended by this method looks like the following example
     * (details may vary because of enumeration values):
     *
     * {@snippet lang="sql" :
     *     WHERE COORD_REF_SYS_KIND LIKE 'geographic%' AND
     *     }
     *
     * The caller shall add at least one condition after this method call.
     *
     * @param  factory   the factory which is writing a <abbr>SQL</abbr> statement.
     * @param  userType  the type specified by the user.
     * @param  buffer    where to append the {@code WHERE} clause.
     * @return the subtype, or {@link #type} if no subtype was found.
     */
    final Class<?> where(final EPSGDataAccess factory, final Class<?> userType, final StringBuilder buffer) {
        buffer.append(" WHERE ");
        if (typeColumn != null) {
            for (int i=0; i<subTypes.length; i++) {
                final Class<?> candidate = subTypes[i];
                if (candidate.isAssignableFrom(userType)) {
                    if (factory.translator.useEnumerations()) {
                        buffer.append("CAST(").append(typeColumn).append(" AS ")
                                .append(EPSGInstaller.ENUM_REPLACEMENT).append(')');
                    } else {
                        buffer.append(typeColumn);
                    }
                    buffer.append(" LIKE '").append(typeNames[i]).append("%' AND ");
                    return candidate;
                }
            }
        }
        return type;
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
