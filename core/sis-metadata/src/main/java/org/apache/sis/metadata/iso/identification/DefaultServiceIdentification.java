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
package org.apache.sis.metadata.iso.identification;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.identification.ServiceIdentification;
import org.apache.sis.xml.Namespaces;


/**
 * Identification of capabilities which a service provider makes available to a service user
 * through a set of interfaces that define a behaviour.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_ServiceIdentification_Type")
@XmlRootElement(name = "SV_ServiceIdentification", namespace = Namespaces.SRV) // Prefix is really SV_ according ISO 19115 corrigendum.
public class DefaultServiceIdentification extends AbstractIdentification implements ServiceIdentification {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 4988429302218128178L;

    /**
     * Constructs an initially empty service identification.
     */
    public DefaultServiceIdentification() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ServiceIdentification)
     */
    public DefaultServiceIdentification(final ServiceIdentification object) {
        super(object);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultServiceIdentification}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultServiceIdentification} instance is created using the
     *       {@linkplain #DefaultServiceIdentification(ServiceIdentification) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultServiceIdentification castOrCopy(final ServiceIdentification object) {
        if (object == null || object instanceof DefaultServiceIdentification) {
            return (DefaultServiceIdentification) object;
        }
        return new DefaultServiceIdentification(object);
    }
}
