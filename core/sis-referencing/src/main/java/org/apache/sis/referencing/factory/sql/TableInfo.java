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

import javax.measure.unit.Unit;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.internal.metadata.WKTKeywords;


/**
 * Information about a specific table. The MS-Access dialect of SQL is assumed;
 * it will be translated into ANSI SQL later by {@link SQLTranslator#apply(String)} if needed.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class TableInfo {
    /**
     * List of tables and columns to test for codes values.
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
        new TableInfo(CoordinateReferenceSystem.class,
                "[Coordinate Reference System]",
                "COORD_REF_SYS_CODE",
                "COORD_REF_SYS_NAME",
                "COORD_REF_SYS_KIND",
                new Class<?>[] { ProjectedCRS.class,   GeographicCRS.class,   GeocentricCRS.class,
                                 VerticalCRS.class,    CompoundCRS.class,     EngineeringCRS.class},
                              // TemporalCRS.class,    ParametricCRS.class    (See comment below)
                new String[]   {"projected",          "geographic",          "geocentric",
                                "vertical",           "compound",            "engineering"},
                             // "temporal",           "parametric"
                "SHOW_CRS"),
                /*
                 * Above declaration omitted Temporal and Parametric cases because they are not defined
                 * by the EPSG registry (at least as of version 8.9). In particular, we are not sure if
                 * EPSG would chose to use "time" or "temporal".  Omitting those types for now does not
                 * prevent SIS to find CRS of those types; the operation will only be more costly.
                 */

        new TableInfo(CoordinateSystem.class,
                "[Coordinate System]",
                "COORD_SYS_CODE",
                "COORD_SYS_NAME",
                "COORD_SYS_TYPE",
                new Class<?>[] {CartesianCS.class,      EllipsoidalCS.class,      VerticalCS.class,      LinearCS.class,
                                SphericalCS.class,      PolarCS.class,            CylindricalCS.class},
                             // TimeCS.class,           ParametricCS.class,       AffineCS.class         (see above comment)
                new String[]   {WKTKeywords.Cartesian,  WKTKeywords.ellipsoidal,  WKTKeywords.vertical,  WKTKeywords.linear,
                                WKTKeywords.spherical,  WKTKeywords.polar,        WKTKeywords.cylindrical},
                             // WKTKeywords.temporal,   WKTKeywords.parametric,   WKTKeywords.affine
                null),

        new TableInfo(CoordinateSystemAxis.class,
                "[Coordinate Axis] AS CA INNER JOIN [Coordinate Axis Name] AS CAN" +
                                 " ON CA.COORD_AXIS_NAME_CODE=CAN.COORD_AXIS_NAME_CODE",
                "COORD_AXIS_CODE",
                "COORD_AXIS_NAME",
                null, null, null, null),

        new TableInfo(Datum.class,
                "[Datum]",
                "DATUM_CODE",
                "DATUM_NAME",
                "DATUM_TYPE",
                new Class<?>[] { GeodeticDatum.class,  VerticalDatum.class,   EngineeringDatum.class},
                              // TemporalDatum.class,  ParametricDatum.class  (see above comment),
                new String[]   {"geodetic",           "vertical",            "engineering"},
                             // "temporal",           "parametric",
                null),

        new TableInfo(Ellipsoid.class,
                "[Ellipsoid]",
                "ELLIPSOID_CODE",
                "ELLIPSOID_NAME",
                null, null, null, null),

        new TableInfo(PrimeMeridian.class,
                "[Prime Meridian]",
                "PRIME_MERIDIAN_CODE",
                "PRIME_MERIDIAN_NAME",
                null, null, null, null),

        new TableInfo(CoordinateOperation.class,
                "[Coordinate_Operation]",
                "COORD_OP_CODE",
                "COORD_OP_NAME",
                "COORD_OP_TYPE",
                new Class<?>[] { Projection.class, Conversion.class, Transformation.class},
                new String[]   {"conversion",     "conversion",     "transformation"},
                "SHOW_OPERATION"),
                // Note: Projection is handled in a special way.

        new TableInfo(OperationMethod.class,
                "[Coordinate_Operation Method]",
                "COORD_OP_METHOD_CODE",
                "COORD_OP_METHOD_NAME",
                null, null, null, null),

        new TableInfo(ParameterDescriptor.class,
                "[Coordinate_Operation Parameter]",
                "PARAMETER_CODE",
                "PARAMETER_NAME",
                null, null, null, null),

        new TableInfo(Unit.class,
                "[Unit of Measure]",
                "UOM_CODE",
                "UNIT_OF_MEAS_NAME",
                null, null, null, null),
    };

    /**
     * The class of object to be created.
     */
    final Class<?> type;

    /**
     * The table name for SQL queries. May contains a {@code "JOIN"} clause.
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
     */
    final String typeColumn;

    /**
     * Sub-interfaces of {@link #type} to handle, or {@code null} if none.
     */
    final Class<?>[] subTypes;

    /**
     * Names of {@link #subTypes} in the database, or {@code null} if none.
     */
    final String[] typeNames;

    /**
     * The column that specify if the object should be shown, or {@code null} if none.
     */
    final String showColumn;

    /**
     * Stores information about a specific table.
     */
    private TableInfo(final Class<?> type,
                      final String table, final String codeColumn, final String nameColumn,
                      final String typeColumn, final Class<?>[] subTypes, final String[] typeNames,
                      final String showColumn)
    {
        this.type       = type;
        this.table      = table;
        this.codeColumn = codeColumn;
        this.nameColumn = nameColumn;
        this.typeColumn = typeColumn;
        this.subTypes   = subTypes;
        this.typeNames  = typeNames;
        this.showColumn = showColumn;
    }
}
