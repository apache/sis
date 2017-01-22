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
package org.apache.sis.internal.feature;

import java.util.Map;
import java.util.HashMap;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.Static;


/**
 * Non-public utility methods for Apache SIS internal usage.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class FeatureUtilities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private FeatureUtilities() {
    }

    /**
     * Creates a parameter descriptor in the Apache SIS namespace. This convenience method shall
     * not be in public API, because users should define operations in their own namespace.
     *
     * @param  name           the parameter group name, typically the same as operation name.
     * @param  parameters     the parameters, or an empty array if none.
     * @return description of the parameters group.
     */
    public static ParameterDescriptorGroup parameters(final String name, final ParameterDescriptor<?>... parameters) {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(ParameterDescriptorGroup.NAME_KEY, name);
        properties.put(Identifier.AUTHORITY_KEY, Citations.SIS);
        return new DefaultParameterDescriptorGroup(properties, 1, 1);
    }
}
