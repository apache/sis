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
package org.apache.sis.style.se1;

import javax.swing.Icon;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.metadata.citation.OnlineResource;


/**
 * Common superclass for the types of markers that can appear as children of a graphic object.
 * Each {@link Graphic} instance contains an arbitrary amount of graphical symbols, when can be
 * either well-known shapes ({@link Mark}) or references to image files ({@link ExternalGraphic}).
 * Graphic content should be static.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Chris Dillard (SYS Technologies)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @see Graphic#graphicalSymbols()
 *
 * @since 1.5
 */
@XmlTransient
public abstract class GraphicalSymbol<R> extends StyleElement<R> {
    /**
     * URL to the image or mark, or {@code null} if none.
     *
     * @see #getOnlineResource()
     * @see #setOnlineResource(OnlineResource)
     */
//  @XmlElement(name = "OnlineResource")
    protected OnlineResource onlineResource;

    /**
     * Handler to the locally loaded image, or {@code null} if none.
     * In Java this is represented by an {@link Icon}.
     * But the XML representation shall be either XML or Base-64-encoded binary.
     *
     * @see #getInlineContent()
     * @see #setInlineContent(Icon)
     */
//  @XmlElement(name = "InlineContent")
    protected Icon inlineContent;

    /**
     * The expected document MIME type, or {@code null} if unspecified.
     *
     * @see #getFormat()
     * @see #setFormat(String)
     */
    @XmlElement(name = "Format")       // Required in ExternalGraphic but not in Mark.
    protected String format;

    /**
     * For JAXB unmarshalling only.
     */
    GraphicalSymbol() {
        // Thread-local factory will be used.
    }

    /**
     * Creates a new graphical symbol.
     * Intentionally restricted to this package because {@link #properties()} is package-private.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    GraphicalSymbol(final StyleFactory<R> factory) {
        super(factory);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    GraphicalSymbol(final GraphicalSymbol<R> source) {
        super(source);
        onlineResource = source.onlineResource;
        inlineContent  = source.inlineContent;
        format         = source.format;
    }

    /**
     * Returns an online or local resource to a file that contains an image.
     * The URL should not reference external graphics that may change at arbitrary times,
     * because the system may cache that resource. The returned value may be empty.
     * if the image is already loaded locally and provided by {@link #getInlineContent()}.
     *
     * @return URL to the image, or empty if the image or mark is provided by other means.
     *
     * @see #getFormat()
     */
    public Optional<OnlineResource> getOnlineResource() {
        return Optional.ofNullable(onlineResource);
    }

    /**
     * Sets an online or local resource to a file that contains an image.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new URL to the image, or {@code null} if none.
     */
    public void setOnlineResource(final OnlineResource value) {
        onlineResource = value;
    }

    /**
     * Returns a handler to the locally loaded image.
     * If present, this property overrides {@link #getOnlineResource()}.
     *
     * @return handler to the locally loaded image.
     */
    public Optional<Icon> getInlineContent() {
        return Optional.ofNullable(inlineContent);
    }

    /**
     * Sets a handler to the locally loaded image.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new handler to the locally loaded image, or {@code null} if none.
     */
    public void setInlineContent(final Icon value) {
        inlineContent = value;
    }

    /**
     * Returns the mime type of the online resource or inline content.
     * This information allows the styler to select the best-supported
     * format from the list of URLs with equivalent content.
     *
     * @return mime type.
     *
     * @see #getOnlineResource()
     */
    public Optional<String> getFormat() {
        return Optional.ofNullable(format);
    }

    /**
     * Sets the mime type of the online resource or inline content.
     *
     * @param  value  new mime type, or {@code null} if unspecified.
     */
    public void setFormat(final String value) {
        format = value;
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public GraphicalSymbol<R> clone() {
        return (GraphicalSymbol<R>) super.clone();
    }
}
