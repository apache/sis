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
package org.apache.sis.internal.geoapi.evolution;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gmd.CodeListUID;


/**
 * An adapter for {@link UnsupportedCodeList}, in order to implement the ISO-19139 standard.
 * See {@link org.apache.sis.internal.jaxb.gmd.CodeListAdapter} for more information.
 *
 * @param <ValueType> The subclass implementing this adapter.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public abstract class UnsupportedCodeListAdapter<ValueType extends UnsupportedCodeListAdapter<ValueType>>
        extends XmlAdapter<ValueType,CodeList<?>>
{
    /**
     * The value of the {@link CodeList}.
     */
    protected CodeListUID identifier;

    /**
     * Empty constructor for subclasses only.
     */
    protected UnsupportedCodeListAdapter() {
    }

    /**
     * Creates a wrapper for a {@link CodeList}, in order to handle the format specified in ISO-19139.
     *
     * @param value The value of the {@link CodeList} to be marshalled.
     */
    protected UnsupportedCodeListAdapter(final CodeListUID value) {
        identifier = value;
    }

    /**
     * Wraps the code into an adapter.
     * Most implementations will be like below:
     *
     * {@preformat java
     *     return new ValueType(value);
     * }
     *
     * @param value The value of {@link CodeList} to be marshalled.
     * @return The wrapper for the code list value.
     */
    protected abstract ValueType wrap(CodeListUID value);

    /**
     * Returns the name of the code list class.
     *
     * @return The code list class name.
     */
    protected abstract String getCodeListName();

    /**
     * Substitutes the adapter value read from an XML stream by the object which will
     * contains the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  adapter The adapter for this metadata value.
     * @return A code list which represents the metadata value.
     */
    @Override
    public final CodeList<?> unmarshal(final ValueType adapter) {
        if (adapter == null) {
            return null;
        }
        return Types.forCodeName(UnsupportedCodeList.class, adapter.identifier.toString(), true);
    }

    /**
     * Substitutes the code list by the adapter to be marshalled into an XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value The code list value.
     * @return The adapter for the given code list.
     */
    @Override
    public final ValueType marshal(final CodeList<?> value) {
        if (value == null) {
            return null;
        }
        final String name = value.name();
        final int length = name.length();
        final StringBuilder buffer = new StringBuilder(length);
        final String codeListValue = toIdentifier(name, buffer, false);
        buffer.setLength(0);
        return wrap(new CodeListUID(Context.current(), getCodeListName(), codeListValue,
                null, toIdentifier(name, buffer, true)));
    }

    /**
     * Converts the given Java constant name to something hopefully close to the UML identifier,
     * or close to the textual value to put in the XML. This method convert the Java constant name
     * to camel case if {@code isValue} is {@code true}, or to lower cases with word separated by
     * spaces if {@code isValue} is {@code true}.
     *
     * @param  name    The Java constant name (e.g. {@code WEB_SERVICES}).
     * @param  buffer  An initially empty buffer to use for creating the identifier.
     * @param  isValue {@code false} for the {@code codeListValue} attribute, or {@code true} for the XML value.
     * @return The identifier (e.g. {@code "webServices"} or {@code "Web services"}).
     */
    protected String toIdentifier(final String name, final StringBuilder buffer, final boolean isValue) {
        final int length = name.length();
        boolean toUpper = isValue;
        for (int i=0; i<length;) {
            int c = name.codePointAt(i);
            i += Character.charCount(c);
            if (c == '_') {
                if (isValue) {
                    c = ' ';
                } else {
                    toUpper = true;
                    continue;
                }
            }
            if (toUpper) {
                c = Character.toUpperCase(c);
                toUpper = false;
            } else {
                c = Character.toLowerCase(c);
            }
            buffer.appendCodePoint(c);
        }
        return buffer.toString();
    }

    /**
     * Invoked by JAXB on marshalling. Subclasses must override this
     * method with the appropriate {@code @XmlElement} annotation.
     *
     * @return The {@code CodeList} value to be marshalled.
     */
    public abstract CodeListUID getElement();

    /*
     * We do not define setter method (even abstract) since it seems to confuse JAXB.
     * It is subclasses responsibility to define the setter method.
     */
}
