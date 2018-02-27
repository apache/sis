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

import javax.xml.bind.annotation.XmlElement;
import org.opengis.metadata.identification.TopicCategory;
import org.apache.sis.internal.jaxb.cat.EnumAdapter;
import org.apache.sis.xml.Namespaces;


/**
 * JAXB adapter for {@link TopicCategory}
 * in order to wrap the value in an XML element as specified by ISO 19115-3 standard.
 * See package documentation for more information about the handling of {@code CodeList} in ISO 19115-3.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guihem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final class MD_TopicCategoryCode extends EnumAdapter<MD_TopicCategoryCode, TopicCategory> {
    /**
     * The enumeration value.
     */
    @XmlElement(name = "MD_TopicCategoryCode", namespace = Namespaces.MRI)
    private String value;

    /**
     * Empty constructor for JAXB only.
     */
    public MD_TopicCategoryCode() {
    }

    /**
     * Returns the wrapped value.
     *
     * @param  wrapper  the wrapper.
     * @return the wrapped value.
     */
    @Override
    public final TopicCategory unmarshal(final MD_TopicCategoryCode wrapper) {
        return TopicCategory.valueOf(name(wrapper.value));
    }

    /**
     * Wraps the given value.
     *
     * @param  e  the value to wrap.
     * @return the wrapped value.
     */
    @Override
    public final MD_TopicCategoryCode marshal(final TopicCategory e) {
        if (e == null) {
            return null;
        }
        final MD_TopicCategoryCode wrapper = new MD_TopicCategoryCode();
        wrapper.value = value(e);
        return wrapper;
    }
}
