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


/**
 * Information about a specific table. The MS-Access dialect of SQL is assumed;
 * it will be translated into ANSI SQL later by {@link EPSGDataAccess#adaptSQL(String)} if needed.
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
                new Class<?>[] { ProjectedCRS.class, GeographicCRS.class, GeocentricCRS.class,
                                 VerticalCRS.class,  CompoundCRS.class,   EngineeringCRS.class},
                new String[]   {"projected",        "geographic",        "geocentric",
                                "vertical",         "compound",          "engineering"}),

        new TableInfo(CoordinateSystem.class,
                "[Coordinate System]",
                "COORD_SYS_CODE",
                "COORD_SYS_NAME",
                "COORD_SYS_TYPE",
                new Class<?>[] { CartesianCS.class, EllipsoidalCS.class, SphericalCS.class, VerticalCS.class},
                new String[]   {"Cartesian",       "ellipsoidal",       "spherical",       "vertical"}),
                               //Really upper-case C.
        new TableInfo(CoordinateSystemAxis.class,
                "[Coordinate Axis] AS CA INNER JOIN [Coordinate Axis Name] AS CAN" +
                                 " ON CA.COORD_AXIS_NAME_CODE=CAN.COORD_AXIS_NAME_CODE",
                "COORD_AXIS_CODE",
                "COORD_AXIS_NAME"),

        new TableInfo(Datum.class,
                "[Datum]",
                "DATUM_CODE",
                "DATUM_NAME",
                "DATUM_TYPE",
                new Class<?>[] { GeodeticDatum.class, VerticalDatum.class, EngineeringDatum.class},
                new String[]   {"geodetic",          "vertical",          "engineering"}),

        new TableInfo(Ellipsoid.class,
                "[Ellipsoid]",
                "ELLIPSOID_CODE",
                "ELLIPSOID_NAME"),

        new TableInfo(PrimeMeridian.class,
                "[Prime Meridian]",
                "PRIME_MERIDIAN_CODE",
                "PRIME_MERIDIAN_NAME"),

        new TableInfo(CoordinateOperation.class,
                "[Coordinate_Operation]",
                "COORD_OP_CODE",
                "COORD_OP_NAME",
                "COORD_OP_TYPE",
                new Class<?>[] { Projection.class, Conversion.class, Transformation.class},
                new String[]   {"conversion",     "conversion",     "transformation"}),
                // Note: Projection is handled in a special way.

        new TableInfo(OperationMethod.class,
                "[Coordinate_Operation Method]",
                "COORD_OP_METHOD_CODE",
                "COORD_OP_METHOD_NAME"),

        new TableInfo(ParameterDescriptor.class,
                "[Coordinate_Operation Parameter]",
                "PARAMETER_CODE",
                "PARAMETER_NAME"),

        new TableInfo(Unit.class,
                "[Unit of Measure]",
                "UOM_CODE",
                "UNIT_OF_MEAS_NAME")
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
     * Stores information about a specific table.
     */
    private TableInfo(final Class<?> type, final String table,
                      final String codeColumn, final String nameColumn)
    {
        this(type, table, codeColumn, nameColumn, null, null, null);
    }

    /**
     * Stores information about a specific table.
     */
    private TableInfo(final Class<?> type,
                      final String table, final String codeColumn, final String nameColumn,
                      final String typeColumn, final Class<?>[] subTypes, final String[] typeNames)
    {
        this.type       = type;
        this.table      = table;
        this.codeColumn = codeColumn;
        this.nameColumn = nameColumn;
        this.typeColumn = typeColumn;
        this.subTypes   = subTypes;
        this.typeNames  = typeNames;
    }
}
