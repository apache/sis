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
package org.apache.sis.internal.jaxb.code;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.CodeList;
import org.apache.sis.util.type.CodeLists;
import org.apache.sis.internal.jaxb.MarshalContext;


/**
 * An adapter for {@link CodeList}, in order to implement the ISO-19139 standard. This object
 * wraps a {@link CodeListProxy}, which contain {@link CodeListProxy#codeList codeList} and
 * {@link CodeListProxy#codeListValue codeListValue} attributes. The result looks like below:
 *
 * {@preformat xml
 *   <dateType>
 *     <CI_DateTypeCode codeList="../Codelist/ML_gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision" codeSpace="fra">
 *       révision
 *     </CI_DateTypeCode>
 *   </dateType>
 * }
 *
 * A subclass must exist for each code list, with a {@link #getElement()} method having a
 * {@code @XmlElement} annotation.
 *
 * @param <ValueType> The subclass implementing this adapter.
 * @param <BoundType> The code list being adapted.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 *
 * @see CodeListLocaleAdapter
 */
public abstract class CodeListAdapter<ValueType extends CodeListAdapter<ValueType,BoundType>,
        BoundType extends CodeList<BoundType>> extends XmlAdapter<ValueType,BoundType>
{
    /**
     * A proxy form of the {@link CodeList}.
     */
    protected CodeListProxy proxy;

    /**
     * Empty constructor for subclasses only.
     */
    protected CodeListAdapter() {
    }

    /**
     * Creates a wrapper for a {@link CodeList}, in order to handle the format specified
     * in ISO-19139.
     *
     * @param proxy The proxy version of {@link CodeList} to be marshalled.
     */
    protected CodeListAdapter(final CodeListProxy proxy) {
        this.proxy = proxy;
    }

    /**
     * Forces the initialization of the given code list class, since some
     * calls to {@link CodeList#valueOf(Class, String)} are done whereas
     * the constructor has not already been called.
     *
     * @param <T>  The code list type.
     * @param type The code list class to initialize.
     */
    protected static <T extends CodeList<T>> void ensureClassLoaded(final Class<T> type) {
        final String name = type.getName();
        try {
            Class.forName(name, true, type.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new TypeNotPresentException(name, e); // Should never happen.
        }
    }

    /**
     * Wraps the proxy value into an adapter.
     *
     * @param proxy The proxy version of {@link CodeList}, to be marshalled.
     * @return The adapter that wraps the proxy value.
     */
    protected abstract ValueType wrap(final CodeListProxy proxy);

    /**
     * Returns the class of code list wrapped by this adapter.
     *
     * @return The code list class.
     */
    protected abstract Class<BoundType> getCodeListClass();

    /**
     * Substitutes the adapter value read from an XML stream by the object which will
     * contains the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  adapter The adapter for this metadata value.
     * @return A code list which represents the metadata value.
     */
    @Override
    public final BoundType unmarshal(final ValueType adapter) {
        if (adapter == null) {
            return null;
        }
        return CodeLists.valueOf(getCodeListClass(), adapter.proxy.identifier());
    }

    /**
     * Substitutes the code list by the adapter to be marshalled into an XML file
     * or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param  value The code list value.
     * @return The adapter for the given code list.
     */
    @Override
    public final ValueType marshal(final BoundType value) {
        if (value == null) {
            return null;
        }
        return wrap(isEnum() ? new CodeListProxy(CodeLists.getCodeName(value))
                             : new CodeListProxy(MarshalContext.current(), value));
    }

    /**
     * Returns {@code true} if this code list is actually an enum. The default implementation
     * returns {@code false} in every cases, since there is very few enums in ISO 19115.
     *
     * @return {@code true} if this code list is actually an enum.
     */
    protected boolean isEnum() {
        return false;
    }

    /**
     * Invoked by JAXB on marshalling. Subclasses must override this
     * method with the appropriate {@code @XmlElement} annotation.
     *
     * @return The {@code CodeList} value to be marshalled.
     */
    public abstract CodeListProxy getElement();

    /*
     * We do not define setter method (even abstract) since it seems to confuse JAXB.
     * It is subclasses responsibility to define the setter method. The existence of
     * this setter will be tested by MetadataAnnotationsTest.
     */
}
