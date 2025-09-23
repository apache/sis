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
package org.apache.sis.xml.bind.gml;

import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.internal.shared.CodeLists;
import org.apache.sis.xml.bind.Context;


/**
 * JAXB adapter for GML code lists, in order to integrate the value in an element
 * complying with GML standard. A subclass shall exist for each code list.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <BoundType>  the code list being wrapped.
 */
public abstract class CodeListAdapter<BoundType extends CodeList<BoundType>> extends XmlAdapter<CodeListAdapter.Value, BoundType> {
    /**
     * Wraps the {@link CodeList} value in a GML document.
     */
    public static final class Value {
        /** The code space of the {@link #value} as a URI, or {@code null}. */
        @XmlAttribute
        public String codeSpace;

        /** The code list identifier. */
        @XmlValue
        public String value;

        /** Empty constructor for JAXB only. */
        public Value() {
        }

        /** Creates a new wrapper for the given value. */
        Value(final String codeSpace, final CodeList<?> code) {
           this.codeSpace = codeSpace;
           value = Types.getCodeName(code);
        }
    }

    /**
     * Empty constructor for subclasses only.
     */
    protected CodeListAdapter() {
    }

    /**
     * Returns the class of code list wrapped by this adapter.
     *
     * @return the code list class.
     */
    protected abstract Class<BoundType> getCodeListClass();

    /**
     * Returns the default code space for the wrapped code list.
     * The default implementation returns {@code null}.
     *
     * @return the default code space, or {@code null}.
     */
    protected String getCodeSpace() {
        return null;
    }

    /**
     * Substitutes the adapter value read from an XML stream by the object which will
     * contain the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  identifier  the code space and identifier.
     * @return a code list which represents the GML value, or {@code null}.
     */
    @Override
    public final BoundType unmarshal(final Value identifier) {
        if (identifier != null) try {
            return CodeLists.getOrCreate(getCodeListClass(), identifier.value);
        } catch (RuntimeException e) {
            Context.warningOccured(Context.current(), CodeListAdapter.class, "unmarshal", e, true);
        }
        return null;
    }

    /**
     * Substitutes the code list by the proxy to be marshalled into an XML file
     * or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param  code  the code list value.
     * @return the proxy for the given code list.
     */
    @Override
    public final Value marshal(final BoundType code) {
        return (code != null) ? new Value(getCodeSpace(), code) : null;
    }
}
