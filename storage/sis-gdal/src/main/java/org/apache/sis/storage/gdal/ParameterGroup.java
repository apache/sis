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
package org.apache.sis.storage.gdal;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.MissingResourceException;

import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.ReferenceIdentifier;

import org.apache.sis.referencing.NamedIdentifier;


/**
 * A relatively "simple" implementation of a parameter group.
 * In order to keep the model simpler, this parameter group is also its own descriptor.
 * This is not quite a recommended practice (such descriptors are less suitable for use in
 * {@link java.util.HashMap}), but allow us to keep the amount of classes smaller and closely related
 * interfaces together.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class ParameterGroup extends PJObject implements ParameterValueGroup, ParameterDescriptorGroup, Cloneable {
    /**
     * The list of parameters included in this group. This simple group implementation supports only
     * {@link Parameter} instances, which are used both as {@linkplain ParameterDescriptor parameter descriptor}
     * and {@linkplain ParameterValue parameter values} for the {@code double} value type.
     *
     * <p>This list is <cite>live</cite>: changes to this list will be reflected immediately in the
     * {@link #descriptors()} and {@link #values()} views.</p>
     */
    private final List<Parameter> parameters;

    /**
     * An unmodifiable view over the {@linkplain #parameters} list. This view is returned by the {@link #descriptors()}
     * and {@link #values()} methods. We have to make it unmodifiable for type safety reason.
     */
    private final List<Parameter> unmodifiable;

    /**
     * Creates a new parameter group for the given identifier.
     */
    ParameterGroup(final ReferenceIdentifier identifier, final Collection<GenericName> aliases) {
        super(identifier, aliases);
        parameters = new ArrayList<>();
        unmodifiable = Collections.unmodifiableList(parameters);
    }

    /**
     * Creates a new parameter group for the given identifier and parameters.
     */
    ParameterGroup(final ReferenceIdentifier identifier, final Collection<GenericName> aliases, final Parameter... param) {
        super(identifier, aliases);
        parameters = new ArrayList<>(Arrays.asList(param));
        unmodifiable = Collections.unmodifiableList(parameters);
    }

    /**
     * Creates a new parameter group as a copy of the given one.
     *
     * @throws ClassCastException if the given parameter contains subgroups,
     *         which are not supported by this simple implementation.
     */
    ParameterGroup(final ParameterValueGroup param) throws ClassCastException {
        super(param.getDescriptor());
        final List<GeneralParameterValue> values = param.values();
        parameters = new ArrayList<>(values.size());
        for (final GeneralParameterValue value : values) {
            parameters.add(new Parameter((ParameterValue) value));
        }
        unmodifiable = Collections.unmodifiableList(parameters);
    }

    /**
     * Returns the descriptor of the parameter group, which is {@code this}.
     */
    @Override
    public ParameterDescriptorGroup getDescriptor() {
        return this;
    }

    /**
     * Returns the minimum number of times that values for this group are required.
     * This method returns 1, meaning that this group shall alway be supplied at least once.
     */
    @Override
    public int getMinimumOccurs() {
        return 1;
    }

    /**
     * Returns the maximum number of times that values for this group can be included.
     * This method returns 1, meaning that values for this group shall alway be supplied
     * exactly once.
     */
    @Override
    public int getMaximumOccurs() {
        return 1;
    }

    /**
     * Returns the parameter descriptors in this group.
     * The list returned by this method is unmodifiable.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<GeneralParameterDescriptor> descriptors() {
        return (List) unmodifiable;                 // Cast is safe only for unmodifiable list.
    }

    /**
     * Returns the parameter values in this group.
     * The list returned by this method is unmodifiable.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<GeneralParameterValue> values() {
        return (List) unmodifiable;                 // Cast is safe only for unmodifiable list.
    }

    /**
     * Delegates to {@link #parameter(String)}, since our simple implementation does not
     * distinguish parameter descriptors and parameter values.
     */
    @Override
    public GeneralParameterDescriptor descriptor(final String name) throws ParameterNotFoundException {
        return parameter(name);
    }

    /**
     * Returns the value in this group for the specified {@linkplain Identifier#getCode identifier code}.
     * If the value is not found, create a new one. This is not quite the expected behavior for this
     * method, but Proj.4 does not include a list of expected parameter values for each projection,
     * so we don't know in advance what are the allowed parameters.
     */
    @Override
    public Parameter parameter(final String name) {
        for (final Parameter candidate : parameters) {
            if (name.equalsIgnoreCase(candidate.getName().getCode())) {
                return candidate;
            }
            // If the name is not recognized, try the alias (if any).
            for (final GenericName alias : candidate.getAlias()) {
                if (name.equalsIgnoreCase(alias.tip().toString())) {
                    return candidate;
                }
            }
        }
        final NamedIdentifier identifier;
        if (this.name instanceof NamedIdentifier) {
            identifier = new NamedIdentifier(((NamedIdentifier) this.name).getAuthority(), name);
        } else {
            identifier = new NamedIdentifier(null, this.name.getCodeSpace(), name, null, null);
        }
        final Parameter param;
        try {
            param = new Parameter(identifier, ResourcesLoader.getAliases(name, true));
        } catch (FactoryException e) { // Should never happen, unless an I/O error occurred.
            throw new MissingResourceException(e.getLocalizedMessage(), ResourcesLoader.PARAMETERS_FILE, name);
        }
        parameters.add(param);
        return param;
    }

    /**
     * Always throws an exception, since this simple parameter group does not support subgroups.
     */
    @Override
    public List<ParameterValueGroup> groups(final String name) throws ParameterNotFoundException {
        throw new ParameterNotFoundException("No such parameter group: " + name, name);
    }

    /**
     * Always throws an exception, since this simple parameter group does not support subgroups.
     */
    @Override
    public ParameterValueGroup addGroup(String name) throws ParameterNotFoundException {
        throw new ParameterNotFoundException("No such parameter group: " + name, name);
    }

    /**
     * Returns a new group with the same {@linkplain #name name} and {@linkplain #parameters}
     * than this group. The {@link Parameter#value value} of each parameter is left to
     * their default value.
     */
    @Override
    public ParameterGroup createValue() {
        final Parameter[] param = new Parameter[parameters.size()];
        for (int i=0; i<param.length; i++) {
            param[i] = parameters.get(i).createValue();
        }
        return new ParameterGroup(name, aliases, param);
    }

    /**
     * Returns a copy of this parameter group.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")          // Okay since this class is final.
    public ParameterGroup clone() {
        return new ParameterGroup(this);
    }

    /**
     * Returns a string representation of all parameters in this group.
     */
    @Override
    public String toString() {
        return super.toString() + " = " + parameters;
    }
}
