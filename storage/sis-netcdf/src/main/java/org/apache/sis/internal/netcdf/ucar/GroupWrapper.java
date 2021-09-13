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
package org.apache.sis.internal.netcdf.ucar;

import java.util.Collection;
import ucar.nc2.Group;
import org.apache.sis.internal.netcdf.Node;
import org.apache.sis.internal.netcdf.Decoder;


/**
 * Wrapper for a netCDF group.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
final class GroupWrapper extends Node {
    /**
     * The netCDF group.
     */
    private final Group group;

    /**
     * Creates a new node wrapping the given netCDF group.
     */
    GroupWrapper(final Decoder decoder, final Group node) {
        super(decoder);
        group = node;
    }

    /**
     * Returns the name of this group.
     */
    @Override
    public String getName() {
        return group.getShortName();
    }

    /**
     * Returns the names of all attributes associated to this node.
     */
    @Override
    public Collection<String> getAttributeNames() {
        return VariableWrapper.toNames(group.attributes());
    }

    /**
     * Returns the type of the attribute of the given name, or {@code null}.
     */
    @Override
    public Class<?> getAttributeType(final String attributeName) {
        return VariableWrapper.getAttributeType(group.attributes().findAttributeIgnoreCase(attributeName));
    }

    /**
     * Returns the single value or vector of values for the given attribute, or {@code null} if none.
     * The returned value can be an instance of {@link String}, {@link Number},
     * {@link org.apache.sis.math.Vector} or {@code String[]}.
     */
    @Override
    protected Object getAttributeValue(final String attributeName) {
        return VariableWrapper.getAttributeValue(group.attributes().findAttributeIgnoreCase(attributeName));
    }
}
