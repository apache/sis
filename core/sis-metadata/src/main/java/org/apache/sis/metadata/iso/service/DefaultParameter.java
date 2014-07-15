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
package org.apache.sis.metadata.iso.service;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.MemberName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.service.Parameter;
import org.opengis.metadata.service.ParameterDirection;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Parameter information.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 *
 * @todo Merge with {@link org.apache.sis.parameter}?
 */
@XmlType(name = "SV_Parameter_Type", propOrder = {
    "name",
    "direction",
    "description",
    "optionality",
    "repeatability"
})
@XmlRootElement(name = "SV_Parameter")
public class DefaultParameter extends ISOMetadata implements Parameter {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -5335736212313243889L;

    /**
     * The name, as used by the service for this parameter.
     */
    private MemberName name;

    /**
     * Indication if the parameter is an input to the service, an output or both.
     */
    private ParameterDirection direction;

    /**
     * A narrative explanation of the role of the parameter.
     */
    private InternationalString description;

    /**
     * Indication if the parameter is required.
     */
    private InternationalString optionality;

    /**
     * Indication if more than one value of the parameter may be provided.
     */
    private boolean repeatability;

    /**
     * Constructs an initially empty parameter.
     */
    public DefaultParameter() {
    }

    /**
     * Constructs a new parameter initialized to the specified values.
     *
     * @param name          The name, as used by the service for this parameter.
     * @param optionality   Indication if the parameter is required.
     * @param repeatability Indication if more than one value of the parameter may be provided.
     */
    public DefaultParameter(final MemberName name,
                            final InternationalString optionality,
                            final boolean repeatability)
    {
        this.name          = name;
        this.optionality   = optionality;
        this.repeatability = repeatability;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Parameter)
     */
    public DefaultParameter(final Parameter object) {
        super(object);
        if (object != null) {
            this.name          = object.getName();
            this.direction     = object.getDirection();
            this.description   = object.getDescription();
            this.optionality   = object.getOptionality();
            this.repeatability = object.getRepeatability();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultParameter}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultParameter} instance is created using the
     *       {@linkplain #DefaultParameter(Parameter) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultParameter castOrCopy(final Parameter object) {
        if (object == null || object instanceof DefaultParameter) {
            return (DefaultParameter) object;
        }
        return new DefaultParameter(object);
    }

    /**
     * Returns the name, as used by the service for this parameter.
     *
     * @return The name, as used by the service for this parameter.
     */
    @Override
    @XmlElement(name = "name", required = true)
    public MemberName getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param newValue The new name, as used by the service for this parameter.
     */
    public void setName(final MemberName newValue) {
        checkWritePermission();
        this.name = newValue;
    }

    /**
     * Returns whether the parameter is an input to the service, an output or both.
     *
     * @return Indication if the parameter is an input to the service, an output or both, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "direction")
    public ParameterDirection getDirection() {
        return direction;
    }

    /**
     * Sets whether the parameter is an input to the service, an output or both.
     *
     * @param newValue Whether the parameter is an input to the service, an output or both.
     */
    public void setDirection(final ParameterDirection newValue) {
        checkWritePermission();
        this.direction = newValue;
    }

    /**
     * Returns a narrative explanation of the role of the parameter.
     *
     * @return A narrative explanation of the role of the parameter, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "description")
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the narrative explanation of the role of the parameter.
     *
     * @param newValue The new narrative explanation of the role of the parameter.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        this.description = newValue;
    }

    /**
     * Returns whether the parameter is required.
     *
     * @return Whether the parameter is required.
     */
    @Override
    @XmlElement(name = "optionality", required = true)
    public InternationalString getOptionality() {
        return optionality;
    }

    /**
     * Sets whether the parameter is required.
     *
     * @param newValue Whether the parameter is required.
     */
    public void setOptionality(final InternationalString newValue) {
        checkWritePermission();
        this.optionality = newValue;
    }

    /**
     * Returns whether more than one value of the parameter may be provided.
     *
     * @return Whether more than one value of the parameter may be provided.
     */
    @Override
    @XmlElement(name = "repeatability", required = true)
    public boolean getRepeatability() {
        return repeatability;
    }

    /**
     * Sets whether more than one value of the parameter may be provided.
     *
     * @param newValue Whether more than one value of the parameter may be provided.
     */
    public void setRepeatability(final boolean newValue) {
        checkWritePermission();
        this.repeatability = newValue;
    }
}
