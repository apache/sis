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
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.Static;

// Branch-dependent imports
import org.apache.sis.feature.AbstractIdentifiedType;


/**
 * Non-public utility methods for Apache SIS internal usage.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class FeatureUtilities extends Static {
    /**
     * The parameter descriptor for the "Link" operation, which does not take any parameter.
     * We use those parameters as a way to identify the link operation without making the
     * {@code LinkOperation} class public.
     */
    public static final ParameterDescriptorGroup LINK_PARAMS = parameters("Link");

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

    /**
     * If the given property is a link, returns the name of the referenced property.
     * Otherwise returns {@code null}.
     *
     * @param  property  the property to test, or {@code null} if none.
     * @return the referenced property name, or {@code null} if none.
     */
    static String linkOf(final AbstractIdentifiedType property) {
        if (property instanceof AbstractOperation) {
            final AbstractOperation op = (AbstractOperation) property;
            if (op.getParameters() == LINK_PARAMS) {
                /*
                 * The dependencies collection contains exactly one element on Apache SIS implementation.
                 * However the user could define his own operation with the same parameter descriptor name.
                 * This is unlikely since it would probably be a bug, but we are paranoiac.
                 */
                return CollectionsExt.first(op.getDependencies());
            }
        }
        return null;
    }
}
