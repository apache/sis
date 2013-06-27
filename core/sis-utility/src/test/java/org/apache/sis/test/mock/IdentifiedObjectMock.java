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
package org.apache.sis.test.mock;

import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.sis.internal.jaxb.gco.GO_GenericName;


/**
 * A dummy implementation of {@link IdentifiedObject} with minimal XML (un)marshalling capability.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "IdentifiedObjectType")
@XmlRootElement(name = "IO_IdentifiedObject")
public final strictfp class IdentifiedObjectMock implements IdentifiedObject {
    /**
     * The alias to (un)marshal to XML
     */
    @XmlElement
    @XmlJavaTypeAdapter(GO_GenericName.class)
    public GenericName alias;

    /**
     * Creates an initially empty identified object.
     * This constructor is required by JAXB.
     */
    public IdentifiedObjectMock() {
    }

    /**
     * Creates an initially empty identified object of the given alias.
     * Callers are free to assign new value to the {@link #alias} field directly.
     *
     * @param alias The initial {@link #alias} value (can be {@code null}).
     */
    public IdentifiedObjectMock(final GenericName alias) {
        this.alias = alias;
    }

    /**
     * Returns the name (currently null).
     *
     * @return The name of this object.
     */
    @Override
    public ReferenceIdentifier getName() {
        return null;
    }

    /**
     * Returns {@link #alias} in an unmodifiable collection.
     *
     * @return {@link #alias} singleton.
     */
    @Override
    public Collection<GenericName> getAlias() {
        return (alias != null) ? Collections.singleton(alias) : Collections.<GenericName>emptySet();
    }

    /**
     * Returns the identifiers (currently null).
     *
     * @return The identifiers of this object.
     */
    @Override
    public Set<ReferenceIdentifier> getIdentifiers() {
        return null;
    }

    /**
     * Returns the remarks (currently null).
     *
     * @return The remarks associated to this object.
     */
    @Override
    public InternationalString getRemarks() {
        return null;
    }

    /**
     * Returns the WKT representation (currently none).
     *
     * @return The WLK representation of this object.
     * @throws UnsupportedOperationException If there is no WKT representation.
     */
    @Override
    public String toWKT() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
