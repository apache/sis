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
package org.apache.sis.internal.style;

import java.util.Optional;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;


/**
 * Informative description of a style object being defined.
 * Description values are mostly used in User Interfaces (lists, trees, …).
 *
 * <p>Note that most style object also have a name.
 * But the name is not part of the description because a name
 * has a functional use that is more than just descriptive.</p>
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlType(name = "DescriptionType", propOrder = {
    "title",
    "summary"
})
@XmlRootElement(name = "Description")
public class Description extends StyleElement {
    /**
     * Human readable title of the style, or {@code null} if none.
     *
     * @see #getTitle()
     * @see #setTitle(InternationalString)
     */
    @XmlElement(name = "Title")
    protected InternationalString title;

    /**
     * Human readable, prose description of this style, or {@code null} if none.
     * This field should be named "abstract", but is named otherwise because "abstract" is a Java keyword.
     *
     * @see #getAbstract()
     * @see #setAbstract(InternationalString)
     */
    @XmlElement(name = "Abstract")
    protected InternationalString summary;

    /**
     * Creates an initially empty description.
     */
    public Description() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Description(final Description source) {
        super(source);
        title   = source.title;
        summary = source.summary;
    }

    /**
     * Returns the human readable title of the style.
     * This can be any string, but should be fairly short as it is intended to
     * be used in list boxes or drop down menus or other selection interfaces.
     *
     * @return the human readable title of the style.
     */
    public Optional<InternationalString> getTitle() {
        return Optional.ofNullable(title);
    }

    /**
     * Sets a human readable title of the style.
     * If this method is never invoked, then the default value is {@code null}.
     *
     * @param  value  new human readable title of the style, or {@code null} if none.
     */
    public void setTitle(final InternationalString value) {
        title = value;
    }

    /**
     * Returns a human readable, prose description of this style.
     * This can be any string and can consist of any amount of text.
     *
     * @return a human readable, prose description of this style.
     */
    public Optional<InternationalString> getAbstract() {
        return Optional.ofNullable(summary);
    }

    /**
     * Sets a human readable, prose description of this style.
     * If this method is never invoked, then the default value is {@code null}.
     *
     * @param  value  new human readable, prose description of this style, or {@code null} if none.
     */
    public void setAbstract(final InternationalString value) {
        summary = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {title, summary};
    }

    /**
     * Returns a clone of this object.
     * The {@link InternationalString} members are assumed immutable and not cloned.
     *
     * @return a clone of this object.
     */
    @Override
    public Description clone() {
        final var clone = (Description) super.clone();
        return clone;
    }
}
