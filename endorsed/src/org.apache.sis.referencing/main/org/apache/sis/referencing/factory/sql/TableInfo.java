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

import java.util.Optional;
import javax.measure.Unit;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.util.CharSequences;

// Specific to the main branch:
import org.apache.sis.referencing.crs.DefaultParametricCRS;
import org.apache.sis.referencing.cs.DefaultParametricCS;
import org.apache.sis.referencing.datum.DefaultParametricDatum;


/**
 * Information about a specific table. This class uses the mixed-case variant of the <abbr>EPSG</abbr> table names.
 * If needed, those names will be converted by {@link SQLTranslator#apply(String)} to the actual table names.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class TableInfo {
    /**
     * The item used for coordinate reference systems.
     */
    static final TableInfo CRS;

    /**
     * The item used for datums.
     */
    static final TableInfo DATUM;

    /**
     * The item used for ellipsoids.
     */
    static final TableInfo ELLIPSOID;

    /**
     * List of tables and columns to test for existence of codes values.
     * Those tables are used by the {@link EPSGDataAccess#createObject(String)} method
     * in order to detect which of the following methods should be invoked for a given code:
     *
     * {@link EPSGDataAccess#createCoordinateReferenceSystem(String)}
     * {@link EPSGDataAccess#createCoordinateSystem(String)}
     * {@link EPSGDataAccess#createDatum(String)}
     * {@link EPSGDataAccess#createEllipsoid(String)}
     * {@link EPSGDataAccess#createUnit(String)}
     *
     * The order is significant: it is the key for a {@code switch} statement.
     */
    static final TableInfo[] EPSG = {
        CRS = new TableInfo(CoordinateReferenceSystem.class,
                "\"Coordinate Reference System\"",
                "COORD_REF_SYS_CODE",
                "COORD_REF_SYS_NAME",
                "COORD_REF_SYS_KIND",
                new Class<?>[] { ProjectedCRS.class,   GeographicCRS.class,   GeocentricCRS.class,
                                 VerticalCRS.class,    CompoundCRS.class,     EngineeringCRS.class,
                                 DerivedCRS.class,     TemporalCRS.class,     DefaultParametricCRS.class},     // See comment below
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

        new TableInfo(CoordinateSystem.class,
                "\"Coordinate System\"",
                "COORD_SYS_CODE",
                "COORD_SYS_NAME",
                "COORD_SYS_TYPE",
                new Class<?>[] {CartesianCS.class,      EllipsoidalCS.class,      VerticalCS.class,      LinearCS.class,
                                SphericalCS.class,      PolarCS.class,            CylindricalCS.class,
                                TimeCS.class,           DefaultParametricCS.class, AffineCS.class},
                new String[]   {WKTKeywords.Cartesian,  WKTKeywords.ellipsoidal,  WKTKeywords.vertical,  WKTKeywords.linear,
                                WKTKeywords.spherical,  WKTKeywords.polar,        WKTKeywords.cylindrical,
                                WKTKeywords.temporal,   WKTKeywords.parametric,   WKTKeywords.affine},      // Same comment as in the CRS case above.
                null, false),

        new TableInfo(CoordinateSystemAxis.class,
                "\"Coordinate Axis\" AS CA INNER JOIN \"Coordinate Axis Name\" AS CAN " +
                                    "ON CA.COORD_AXIS_NAME_CODE=CAN.COORD_AXIS_NAME_CODE",
                "COORD_AXIS_CODE",
                "COORD_AXIS_NAME",
                null, null, null, null, false),

        DATUM = new TableInfo(Datum.class,
                "\"Datum\"",
                "DATUM_CODE",
                "DATUM_NAME",
                "DATUM_TYPE",
                new Class<?>[] { GeodeticDatum.class,  VerticalDatum.class,   EngineeringDatum.class,
                                 TemporalDatum.class,  DefaultParametricDatum.class},
                new String[]   {"geodetic",           "vertical",            "engineering",
                                "temporal",           "parametric"},         // Same comment as in the CRS case above.
                null, true),

        ELLIPSOID = new TableInfo(Ellipsoid.class,
                "\"Ellipsoid\"",
                "ELLIPSOID_CODE",
                "ELLIPSOID_NAME",
                null, null, null, null, false),

        new TableInfo(PrimeMeridian.class,
                "\"Prime Meridian\"",
                "PRIME_MERIDIAN_CODE",
                "PRIME_MERIDIAN_NAME",
                null, null, null, null, false),

        new TableInfo(CoordinateOperation.class,
                "\"Coordinate_Operation\"",
                "COORD_OP_CODE",
                "COORD_OP_NAME",
                "COORD_OP_TYPE",
                new Class<?>[] { Conversion.class, Transformation.class},
                new String[]   {"conversion",     "transformation"},
                "SHOW_OPERATION", true),

        new TableInfo(OperationMethod.class,
                "\"Coordinate_Operation Method\"",
                "COORD_OP_METHOD_CODE",
                "COORD_OP_METHOD_NAME",
                null, null, null, null, false),

        new TableInfo(ParameterDescriptor.class,
                "\"Coordinate_Operation Parameter\"",
                "PARAMETER_CODE",
                "PARAMETER_NAME",
                null, null, null, null, false),

        new TableInfo(Unit.class,
                "\"Unit of Measure\"",
                "UOM_CODE",
                "UNIT_OF_MEAS_NAME",
                null, null, null, null, false),
    };

    /**
     * The class of object to be created (usually a GeoAPI interface).
     */
    final Class<?> type;

    /**
     * The table name for SQL queries, including schema name and quotes for mixed-case names.
     * May contain a {@code JOIN} clause.
     *
     * @see #unquoted()
     */
    final String table;

    /**
     * Column name for the code (usually with the {@code "_CODE"} suffix).
     */
    final String codeColumn;

    /**
     * Column name for the name (usually with the {@code "_NAME"} suffix), or {@code null}.
     */
    final String nameColumn;

    /**
     * Column type for the type (usually with the {@code "_TYPE"} suffix), or {@code null}.
     * {@link EPSGDataAccess} and {@link AuthorityCodes} assume that values in this column
     * are not longer than the maximal length specified in {@value #ENUM_REPLACEMENT}.
     */
    private final String typeColumn;

    /**
     * The SQL type to use as a replacement for enumerated values on databases that do not support enumerations.
     */
    static final String ENUM_REPLACEMENT = "VARCHAR(80)";

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
     * @param table       the table name for SQL queries, including quotes for mixed-case names.
     * @param codeColumn  column name for the code (usually with the {@code "_CODE"} suffix).
     * @param nameColumn  column name for the name (usually with the {@code "_NAME"} suffix), or {@code null}.
     * @param typeColumn  column type for the type (usually with the {@code "_TYPE"} suffix), or {@code null}.
     * @param subTypes    sub-interfaces of {@link #type} to handle, or {@code null} if none.
     * @param typeNames   names of {@code subTypes} in the database, or {@code null} if none.
     * @param showColumn  the column that specify if the object should be shown, or {@code null} if none.
     * @param areaOfUse   whether the table had an {@code "AREA_OF_USE_CODE"} column in the legacy versions.
     */
    private TableInfo(final Class<?> type,
                      final String table, final String codeColumn, final String nameColumn,
                      final String typeColumn, final Class<?>[] subTypes, final String[] typeNames,
                      final String showColumn, final boolean areaOfUse)
    {
        this.type       = type;
        this.table      = table;
        this.codeColumn = codeColumn;
        this.nameColumn = nameColumn;
        this.typeColumn = typeColumn;
        this.subTypes   = subTypes;
        this.typeNames  = typeNames;
        this.showColumn = showColumn;
        this.areaOfUse  = areaOfUse;
    }

    /**
     * Returns the table name without schema name and without quotes.
     */
    final String unquoted() {
        return table.substring(table.indexOf('"') + 1, table.lastIndexOf('"'));
    }

    /**
     * Returns the class of objects created from the given table. The given table name should be one of
     * the values enumerated in the {@code epsg_table_name} types of the {@code EPSG_Prepare.sql} file.
     * The name may be prefixed by {@code "epsg_"} and may contain abbreviations of the full name.
     * For example, {@code "epsg_coordoperation"} is considered as a match for {@code "Coordinate_Operation"}.
     *
     * @param  table  mixed-case name of an <abbr>EPSG</abbr> table.
     * @return name of the class of objects created from the given table.
     */
    static Optional<String> getObjectClassName(String table) {
        if (table != null) {
            if (table.startsWith(SQLTranslator.TABLE_PREFIX)) {
                table = table.substring(SQLTranslator.TABLE_PREFIX.length());
            }
            for (final TableInfo info : EPSG) {
                if (CharSequences.isAcronymForWords(table, info.unquoted())) {
                    return Optional.of(info.type.getSimpleName());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Appends a {@code WHERE} clause together with a condition for searching the specified object.
     * This method delegates to {@link #where(Class, StringBuilder)} with the type of the given object,
     * except that some object properties may be inspected for resolving ambiguities.
     *
     * @param  object  the object to search in the database.
     * @param  buffer  where to append the {@code WHERE} clause.
     */
    final void where(final IdentifiedObject object, final StringBuilder buffer) {
        Class<?> userType = object.getClass();
        if (object instanceof GeodeticCRS) {
            final CoordinateSystem cs = ((GeodeticCRS) object).getCoordinateSystem();
            if (cs instanceof EllipsoidalCS) {
                userType = GeographicCRS.class;
            } else if (cs instanceof CartesianCS || cs instanceof SphericalCS) {
                userType = GeocentricCRS.class;
            }
        }
        where(userType, buffer);
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
     * @param  userType  the type specified by the user.
     * @param  buffer    where to append the {@code WHERE} clause.
     * @return the subtype, or {@link #type} if no subtype was found.
     */
    final Class<?> where(final Class<?> userType, final StringBuilder buffer) {
        buffer.append(" WHERE ");
        if (typeColumn != null) {
            for (int i=0; i<subTypes.length; i++) {
                final Class<?> candidate = subTypes[i];
                if (candidate.isAssignableFrom(userType)) {
                    if (ENUM_REPLACEMENT != null) {
                        buffer.append("CAST(").append(typeColumn).append(" AS ").append(ENUM_REPLACEMENT).append(')');
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
}
