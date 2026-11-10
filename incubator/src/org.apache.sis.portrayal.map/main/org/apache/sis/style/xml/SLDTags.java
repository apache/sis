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
package org.apache.sis.style.xml;

import org.apache.sis.xml.Namespaces;


/**
 * XML tags of Styled Layer Descriptor 1.0 and 1.1, in the {@value #NAMESPACE} namespace.
 * Does not duplicate the symbology tags of {@link SETags}, which are also used in SLD 1.0
 * (in the SLD namespace), except for the tags specific to SLD 1.0.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class SLDTags {
    /**
     * Namespace of Styled Layer Descriptor 1.0 and 1.1.
     */
    static final String NAMESPACE = Namespaces.SLD;

    /**
     * Usual prefix for the {@link #NAMESPACE} namespace.
     */
    static final String PREFIX = "sld";

    // Layers
    static final String STYLED_LAYER_DESCRIPTOR = "StyledLayerDescriptor";
    static final String USE_SLD_LIBRARY         = "UseSLDLibrary";
    static final String NAMED_LAYER             = "NamedLayer";
    static final String USER_LAYER              = "UserLayer";
    static final String NAMED_STYLE             = "NamedStyle";
    static final String USER_STYLE              = "UserStyle";
    static final String IS_DEFAULT              = "IsDefault";

    // Sources of data
    static final String REMOTE_OWS     = "RemoteOWS";
    static final String SERVICE        = "Service";
    static final String INLINE_FEATURE = "InlineFeature";

    // Constraints
    static final String LAYER_FEATURE_CONSTRAINTS  = "LayerFeatureConstraints";
    static final String FEATURE_TYPE_CONSTRAINT    = "FeatureTypeConstraint";
    static final String EXTENT                     = "Extent";
    static final String VALUE                      = "Value";
    static final String LAYER_COVERAGE_CONSTRAINTS = "LayerCoverageConstraints";
    static final String COVERAGE_CONSTRAINT        = "CoverageConstraint";
    static final String COVERAGE_EXTENT            = "CoverageExtent";
    static final String RANGE_AXIS                 = "RangeAxis";
    static final String TIME_PERIOD                = "TimePeriod";

    // Symbology elements specific to SLD 1.0
    static final String CSS_PARAMETER   = "CssParameter";
    static final String COLOR_MAP_ENTRY = "ColorMapEntry";
    static final String COLOR           = "color";
    static final String OPACITY         = "opacity";
    static final String QUANTITY        = "quantity";
    static final String LABEL           = "label";
    static final String TYPE            = "type";
    static final String RAMP            = "ramp";
    static final String INTERVALS       = "intervals";
    static final String VALUES          = "values";

    /**
     * Do not allow instantiation of this class.
     */
    private SLDTags() {
    }
}
