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
package org.apache.sis.internal.jaxb.gmd;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;
import org.apache.sis.internal.jaxb.Context;


/**
 * An adapter for {@link CodeList}, in order to implement the ISO-19139 standard. This object
 * wraps a {@link CodeListUID}, which contain {@link CodeListUID#codeList codeList} and
 * {@link CodeListUID#codeListValue codeListValue} attributes. The result looks like below:
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
 * @since   0.3
 * @version 0.7
 * @module
 */
public abstract class CodeListAdapter<ValueType extends CodeListAdapter<ValueType,BoundType>,
        BoundType extends CodeList<BoundType>> extends XmlAdapter<ValueType,BoundType>
{
    /**
     * The value of the {@link CodeList}.
     */
    protected CodeListUID identifier;

    /**
     * Empty constructor for subclasses only.
     */
    protected CodeListAdapter() {
    }

    /**
     * Creates a wrapper for a {@link CodeList}, in order to handle the format specified in ISO-19139.
     *
     * @param value The value of {@link CodeList} to be marshalled.
     */
    protected CodeListAdapter(final CodeListUID value) {
        identifier = value;
    }

    /**
     * Wraps the given value.
     * Most implementations will be like below:
     *
     * {@preformat java
     *     return new ValueType(value);
     * }
     *
     * However is some cases, the {@code value} argument may be inspected.
     * For example {@link org.apache.sis.internal.jaxb.code.MD_RestrictionCode}
     * replaces {@code "licence"} by {@code "license"} for ISO 19115:2003 compatibility.
     *
     * @param value The value of {@link CodeList}, to be marshalled.
     * @return The wrapper for the code list value.
     */
    protected abstract ValueType wrap(CodeListUID value);

    /**
     * Returns the class of code list wrapped by this adapter.
     *
     * @return The code list class.
     */
    protected abstract Class<BoundType> getCodeListClass();

    /**
     * Substitutes the adapter value read from an XML stream by the object which will
     * contain the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  adapter The adapter for this metadata value.
     * @return A code list which represents the metadata value.
     */
    @Override
    public final BoundType unmarshal(final ValueType adapter) {
        return (adapter != null) ? Types.forCodeName(getCodeListClass(), adapter.identifier.toString(), true) : null;
    }

    /**
     * Substitutes the code list by the adapter to be marshalled into an XML file
     * or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param  code The code list value.
     * @return The adapter for the given code list.
     */
    @Override
    public final ValueType marshal(final BoundType code) {
        if (code == null) {
            return null;
        }
        final CodeListUID p;
        if (isEnum()) {
            // To be removed after GEO-199 resolution.
            p = new CodeListUID();
            p.value = Types.getCodeName(code);
        } else {
            p = new CodeListUID(Context.current(), code);
        }
        return wrap(p);
    }

    /**
     * Returns {@code true} if this code list is actually an enum. The default implementation
     * returns {@code false} in every cases, since there is very few enums in ISO 19115.
     *
     * @return {@code true} if this code list is actually an enum.
     *
     * @todo Remove this method after we refactored enum wrappers as {@link EnumAdapter} subclasses
     *       instead of {@code CodeListAdapter}. This requires the resolution of GEO-199 first.
     *
     * @see <a href="http://jira.codehaus.org/browse/GEO-199">GEO-199</a>
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
    public abstract CodeListUID getElement();

    /*
     * We do not define setter method (even abstract) since it seems to confuse JAXB.
     * It is subclasses responsibility to define the setter method. The existence of
     * this setter will be tested by MetadataAnnotationsTest.
     */
}
