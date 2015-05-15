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

import org.apache.sis.util.Static;


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
 * <div class="note"><b>Note:</b>
 * this class should not contain any method or non-final constant, in order to avoid class loading.
 * This class is intended to be used only at compile-time and could be omitted from the JAR file.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html">WKT 2 specification</a>
 * @see <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">Legacy WKT 1</a>
 */
public final class WKTKeywords extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private WKTKeywords() {
    }

    /**
     * Related to {@link org.apache.sis.referencing.AbstractIdentifiedObject}
     * (including {@link org.apache.sis.metadata.iso.ImmutableIdentifier}).
     */
    public static final String
            Id        = "Id",
            URI       = "URI",
            Citation  = "Citation",
            Authority = "Authority",
            Anchor    = "Anchor",
            Scope     = "Scope",
            Area      = "Area",
            Remarks   = "Remarks";

    /**
     * Related to {@link org.apache.sis.referencing.cs.AbstractCS}
     * and {@link org.apache.sis.referencing.datum.AbstractDatum}.
     */
    public static final String
            CS            = "CS",
            Axis          = "Axis",
            Order         = "Order",
            Meridian      = "Meridian",
            PrimeMeridian = "PrimeMeridian",
            PrimeM        = "PrimeM",
            Ellipsoid     = "Ellipsoid",
            Spheroid      = "Spheroid",
            ToWGS84       = "ToWGS84";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultGeocentricCRS}
     * and {@link org.apache.sis.referencing.crs.DefaultGeographicCRS}.
     */
    public static final String
            BBox        = "BBox",
            Datum       = "Datum",
            GeodeticCRS = "GeodeticCRS",
            BaseGeodCRS = "BaseGeodCRS",
            GeogCS      = "GeogCS",
            GeocCS      = "GeocCS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultVerticalCRS}.
     */
    public static final String
            VerticalExtent = "VerticalExtent",
            VerticalDatum  = "VerticalDatum",
            VerticalCRS    = "VerticalCRS",
            BaseVertCRS    = "BaseVertCRS",
            Vert_Datum     = "Vert_Datum",
            Vert_CS        = "Vert_CS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultTemporalCRS}.
     */
    public static final String
            TimeExtent  = "TimeExtent",
            TimeOrigin  = "TimeOrigin",
            TimeDatum   = "TimeDatum",
            TimeCRS     = "TimeCRS",
            BaseTimeCRS = "BaseTimeCRS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultImageCRS}
     * and {@link org.apache.sis.referencing.crs.DefaultEngineeringCRS}.
     * Former can be seen as a special case of the later.
     */
    public static final String
            ImageDatum       = "ImageDatum",
            ImageCRS         = "ImageCRS",
            EngineeringDatum = "EngineeringDatum",
            EngineeringCRS   = "EngineeringCRS",
            BaseEngCRS       = "BaseEngCRS",
            Local_Datum      = "Local_Datum",
            Local_CS         = "Local_CS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultCompoundCRS}.
     */
    public static final String
            CompoundCRS = "CompoundCRS",
            Compd_CS    = "Compd_CS";

    /**
     * Related to {@link org.apache.sis.referencing.crs.DefaultProjectedCRS}.
     */
    public static final String
            ProjectedCRS = "ProjectedCRS",
            BaseProjCRS  = "BaseProjCRS",
            ProjCS       = "ProjCS";

    /**
     * Related to {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation}.
     */
    public static final String
            Method              = "Method",
            Formula             = "Formula",
            Projection          = "Projection",
            Conversion          = "Conversion",
            DerivingConversion  = "DerivingConversion",
            CoordinateOperation = "CoordinateOperation",
            OperationAccuracy   = "OperationAccuracy",
            SourceCRS           = "SourceCRS",
            TargetCRS           = "TargetCRS",
            InterpolationCRS    = "InterpolationCRS",
            Parameter           = "Parameter",
            ParameterFile       = "ParameterFile",
            ParameterGroup      = "ParameterGroup";

    /**
     * Related to {@link org.apache.sis.referencing.operation.transform.AbstractMathTransform}.
     */
    public static final String
            Param_MT       = "Param_MT",
            Inverse_MT     = "Inverse_MT",
            Concat_MT      = "Concat_MT",
            PassThrough_MT = "PassThrough_MT",
            Fitted_CS      = "Fitted_CS";
}
