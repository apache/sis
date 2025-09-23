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
import org.apache.sis.util.ArraysExt;


/**
 * Constant strings for the keywords used in Well Known Text (WKT) formatting.
 * All constant values and identical to the names of the constants.
 *
 * <p><strong>Those constants should be used for WKT only.</strong>
 *
 * Do not use those constants in contexts unrelated to WKT (e.g. in {@code @XmlRootElement} annotations),
 * even if the string has the same value. It should be possible to change a keyword used in WKT formatting
 * without affecting GML for instance.</p>
 *
 * <h2>Implementation note</h2>
 * All constants in this class are static and final. The Java compiler should replace those constants
 * by their literal values at compile time, which avoid the loading of this class at run-time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class WKTKeywords {
    /**
     * Do not allow instantiation of this class.
     */
    private WKTKeywords() {
    }

    /**
     * Keywords defined by <abbr>WKT</abbr> 2. Some of those keywords are inherited from <abbr>WKT</abbr> 1
     * but have been kept in <abbr>WKT</abbr> 2 for compatibility reasons.
     */
    public static final String
            Anchor                = "Anchor",
            AnchorEpoch           = "AnchorEpoch",
            AngleUnit             = "AngleUnit",
            Area                  = "Area",
            Authority             = "Authority",
            Axis                  = "Axis",
            AxisMaxValue          = "AxisMaxValue",
            AxisMinValue          = "AxisMinValue",
            BaseEngCRS            = "BaseEngCRS",
            BaseGeodCRS           = "BaseGeodCRS",
            BaseGeogCRS           = "BaseGeogCRS",
            BaseParamCRS          = "BaseParamCRS",
            BaseProjCRS           = "BaseProjCRS",
            BaseTimeCRS           = "BaseTimeCRS",
            BaseVertCRS           = "BaseVertCRS",
            BBox                  = "BBox",
            BoundCRS              = "BoundCRS",
            Citation              = "Citation",
            Compd_CS              = "Compd_CS",
            CompoundCRS           = "CompoundCRS",
            ConcatenatedOperation = "ConcatenatedOperation",
            Conversion            = "Conversion",
            CoordinateMetadata    = "CoordinateMetadata",
            CoordinateOperation   = "CoordinateOperation",
            CS                    = "CS",
            Datum                 = "Datum",
            DerivingConversion    = "DerivingConversion",
            Dynamic               = "Dynamic",
            EDatum                = "EDatum",
            Ellipsoid             = "Ellipsoid",
            EngCRS                = "EngCRS",
            EngineeringCRS        = "EngineeringCRS",
            EngineeringDatum      = "EngineeringDatum",
            EnsembleAccuracy      = "EnsembleAccuracy",
            Ensemble              = "Ensemble",
            Epoch                 = "Epoch",
            Formula               = "Formula",
            FrameEpoch            = "FrameEpoch",
            GeocCS                = "GeocCS",
            GeodCRS               = "GeodCRS",
            GeodeticCRS           = "GeodeticCRS",
            GeodeticDatum         = "GeodeticDatum",
            GeogCRS               = "GeogCRS",
            GeogCS                = "GeogCS",
            GeographicCRS         = "GeographicCRS",
            IDatum                = "IDatum",
            Id                    = "Id",
            ImageCRS              = "ImageCRS",
            ImageDatum            = "ImageDatum",
            InterpolationCRS      = "InterpolationCRS",
            LengthUnit            = "LengthUnit",
            Local_CS              = "Local_CS",
            Local_Datum           = "Local_Datum",
            Member                = "Member",
            Meridian              = "Meridian",
            Method                = "Method",
            OperationAccuracy     = "OperationAccuracy",
            Order                 = "Order",
            ParameterFile         = "ParameterFile",
            ParameterGroup        = "ParameterGroup",
            Parameter             = "Parameter",
            ParametricCRS         = "ParametricCRS",
            ParametricDatum       = "ParametricDatum",
            ParametricUnit        = "ParametricUnit",
            PDatum                = "PDatum",
            Point                 = "Point",
            PrimeMeridian         = "PrimeMeridian",
            PrimeM                = "PrimeM",
            ProjCRS               = "ProjCRS",
            ProjCS                = "ProjCS",
            ProjectedCRS          = "ProjectedCRS",
            Projection            = "Projection",
            RangeMeaning          = "RangeMeaning",
            Remark                = "Remark",
            ScaleUnit             = "ScaleUnit",
            Scope                 = "Scope",
            SourceCRS             = "SourceCRS",
            Spheroid              = "Spheroid",
            Step                  = "Step",
            TargetCRS             = "TargetCRS",
            TDatum                = "TDatum",
            TemporalQuantity      = "TemporalQuantity",
            TimeCRS               = "TimeCRS",
            TimeDatum             = "TimeDatum",
            TimeExtent            = "TimeExtent",
            TimeOrigin            = "TimeOrigin",
            TimeUnit              = "TimeUnit",
            ToWGS84               = "ToWGS84",
            TRF                   = "TRF",
            Unit                  = "Unit",
            URI                   = "URI",
            Usage                 = "Usage",
            VDatum                = "VDatum",
            Version               = "Version",
            VertCRS               = "VertCRS",
            Vert_CS               = "Vert_CS",
            Vert_Datum            = "Vert_Datum",
            VerticalCRS           = "VerticalCRS",
            VerticalDatum         = "VerticalDatum",
            VerticalExtent        = "VerticalExtent",
            VRF                   = "VRF";

    /**
     * The following keywords are specific to <abbr>WKT</abbr> 1 or <abbr>ESRI</abbr>.
     * Those keywords have not been kept in <abbr>WKT</abbr> 2.
     */
    public static final String
            GeogTran       = "GeogTran",               // ESRI-specific.
            Param_MT       = "Param_MT",
            Inverse_MT     = "Inverse_MT",
            Concat_MT      = "Concat_MT",
            PassThrough_MT = "PassThrough_MT",
            Fitted_CS      = "Fitted_CS";

    /**
     * Coordinate system types. They are not WKT keywords, but are legal values for the "type"
     * element in a {@code "CS[…]"} element.
     */
    public static final String
            affine      = "affine",
            Cartesian   = "Cartesian",      // Upper case 'C' is intentional.
            cylindrical = "cylindrical",
            ellipsoidal = "ellipsoidal",
            linear      = "linear",
            parametric  = "parametric",
            polar       = "polar",
            spherical   = "spherical",
            temporal    = "temporal",
            vertical    = "vertical";

    /**
     * Mapping between types of object and WKT keywords. Each GeoAPI interfaces is associated to one
     * or many WKT keywords: new keywords defined by version 2 (sometimes with synonymous) and legacy
     * keywords defined by WKT 1.
     *
     * @see #forType(Class)
     */
    private static final Map<Class<?>,String[]> TYPES = new HashMap<>(30);
    static {
        /*
         * `SingleCRS` subtypes. The `GeographicCRS` and `GeocentricCRS` types are handled separately
         * because they are implied by the `GeodeticCRS` type in the `subtypes` array, so they do not
         * need to be repeated there.
         */
        addType(org.opengis.referencing.crs.GeographicCRS.class,  GeodeticCRS, GeodCRS, GeogCS);
        String[][] subtypes;
        subtypes = new String[][] {
            addType(org.opengis.referencing.crs.GeodeticCRS.class,    GeodeticCRS,    GeodCRS, GeocCS, GeogCS),
            addType(org.opengis.referencing.crs.ProjectedCRS.class,   ProjectedCRS,   ProjCRS, ProjCS),
            addType(org.opengis.referencing.crs.VerticalCRS.class,    VerticalCRS,    VertCRS, Vert_CS),
            addType(org.opengis.referencing.crs.EngineeringCRS.class, EngineeringCRS, EngCRS,  Local_CS),
            addType(org.opengis.referencing.crs.DerivedCRS.class,     /* TODO: check ISO. */   Fitted_CS),
            addType(org.opengis.referencing.crs.TemporalCRS.class,    TimeCRS)
        };
        /*
         * `CoordinateReferenceSystem` subtypes: all `SingleCRS` + the `CompoundCRS`.
         */
        subtypes = new String[][] {
            addType(org.opengis.referencing.crs.SingleCRS.class, ArraysExt.concatenate(subtypes)),
            addType(org.opengis.referencing.crs.CompoundCRS.class, CompoundCRS, Compd_CS),
        };
        addType(org.opengis.referencing.crs.CoordinateReferenceSystem.class, ArraysExt.concatenate(subtypes));
        /*
         * `Datum` subtypes.
         */
        subtypes = new String[][] {
            addType(org.opengis.referencing.datum.GeodeticDatum.class,    GeodeticDatum,    Datum, TRF),
            addType(org.opengis.referencing.datum.TemporalDatum.class,    TimeDatum,        TDatum),
            addType(org.opengis.referencing.datum.VerticalDatum.class,    VerticalDatum,    VDatum, Vert_Datum, VRF),
            addType(org.opengis.referencing.datum.EngineeringDatum.class, EngineeringDatum, EDatum, Local_Datum)
        };
        addType(org.opengis.referencing.datum.Datum.class, ArraysExt.concatenate(subtypes));
        /*
         * Other types having no common parent (ignoring `IdentifiedObject`).
         */
        addType(org.opengis.referencing.datum.Ellipsoid.class,              Ellipsoid, Spheroid);
        addType(org.opengis.referencing.datum.PrimeMeridian.class,          PrimeMeridian, PrimeM);
        addType(org.opengis.referencing.cs.CoordinateSystemAxis.class,      Axis);
        addType(org.apache.sis.referencing.datum.BursaWolfParameters.class, ToWGS84);
        addType(org.opengis.referencing.operation.MathTransform.class,      Param_MT, Concat_MT, Inverse_MT, PassThrough_MT);
        addType(org.opengis.coordinate.CoordinateMetadata.class,            CoordinateMetadata);
        addType(org.opengis.geometry.DirectPosition.class,                  Point);
    }

    /**
     * Adds WKT keywords for the specified type.
     */
    private static String[] addType(final Class<?> type, final String... keywords) {
        if (TYPES.put(type, keywords) != null) {
            throw new AssertionError(type);
        }
        return keywords;
    }

    /**
     * Returns the WKT keywords for an object of the specified type.
     * This method returns a direct reference to internal array;
     * <strong>do not modify</strong>.
     *
     * @param  type  the GeoAPI interface of the element.
     * @return the WKT keywords, or {@code null} if none or unknown.
     */
    public static String[] forType(final Class<?> type) {
        return TYPES.get(type);
    }
}
