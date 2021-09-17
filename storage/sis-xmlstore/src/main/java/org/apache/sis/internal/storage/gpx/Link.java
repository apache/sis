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
package org.apache.sis.internal.storage.gpx;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.util.iso.Types;


/**
 * A link to an external resource (Web page, digital photo, video clip, <i>etc</i>) with additional information.
 * This element provides 3 properties:
 *
 * <ul>
 *   <li>The {@linkplain #uri}, which is the only mandatory property.</li>
 *   <li>The {@linkplain #text} to show for the link.</li>
 *   <li>The MIME {@linkplain #type}.</li>
 * </ul>
 *
 * Those properties can be read or modified directly. All methods defined in this class are bridges to
 * the ISO 19115 metadata model and can be ignored if the user only wants to manipulate the GPX model.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class Link implements OnlineResource {
    /**
     * URL of hyper-link.
     *
     * @see #getLinkage()
     */
    @XmlAttribute(name = Attributes.HREF, required = true)
    public URI uri;

    /**
     * Text of hyper-link.
     *
     * @see #getName()
     */
    @XmlElement(name = Tags.TEXT)
    public String text;

    /**
     * MIME type of content (for example "image/jpeg").
     */
    @XmlElement(name = Tags.TYPE)
    public String type;

    /**
     * Creates an initially empty instance.
     * Callers should set at least the {@link #uri} field after construction.
     */
    public Link() {
    }

    /**
     * Creates a new instance initialized to the given URI.
     *
     * @param  uri  the URI.
     */
    public Link(final URI uri) {
        this.uri = uri;
    }

    /**
     * Creates a new instance initialized to the given URI.
     *
     * @param  uri  the URI, or {@code null}.
     * @return the link, or {@code null} if the given URI was null.
     */
    static Link valueOf(final URI uri) {
        return (uri != null) ? new Link(uri) : null;
    }

    /**
     * Invoked by JAXB after unmarshalling. If the {@linkplain #uri} is not set but the {@link #text} looks
     * like a URI, uses that text. The intent is to handle link that should have been defined like below:
     *
     * {@preformat xml
     *   <link href="http://some.site.org">
     *   </link>
     * }
     *
     * but instead has erroneously been defined like below:
     *
     * {@preformat xml
     *   <link>
     *     <text>http://some.site.org</text>
     *   </link>
     * }
     *
     * If we fail to convert the text to a URI, we will leave the object state as-is.
     */
    final void afterUnmarshal(Unmarshaller um, Object parent) {
        if (uri == null && text != null) {
            final Context context = Context.current();
            try {
                Context.converter(context).toURI(context, text);
            } catch (URISyntaxException e) {
                Context.warningOccured(context, Link.class, "afterUnmarshal", e, true);
            }
        }
    }

    /**
     * Copies properties from the given ISO 19115 metadata.
     */
    private Link(final OnlineResource r, final Locale locale) {
        uri  = r.getLinkage();
        text = Types.toString(r.getName(), locale);
    }

    /**
     * Returns the given ISO 19115 metadata as a {@code Link} instance.
     * This method copies the data only if needed.
     *
     * @param  r       the ISO 19115 metadata, or {@code null}.
     * @param  locale  the locale to use for localized strings.
     * @return the GPX metadata, or {@code null}.
     */
    public static Link castOrCopy(final OnlineResource r, final Locale locale) {
        return (r == null || r instanceof Link) ? (Link) r : new Link(r, locale);
    }

    /**
     * ISO 19115 metadata property determined by the {@link #uri} field.
     *
     * @return location for on-line access using a URL address or similar scheme.
     */
    @Override
    public URI getLinkage() {
        return uri;
    }

    /**
     * ISO 19115 metadata property determined by the {@link #text} field.
     *
     * @return name of the online resource.
     */
    @Override
    public InternationalString getName() {
        return (text != null) ? new SimpleInternationalString(text) : null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return text description of what the online resource is/does.
     */
    @Override
    public InternationalString getDescription() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return function performed by the online resource.
     */
    @Override
    public OnLineFunction getFunction() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return request used to access the resource, or {@code null}.
     */
    @Override
    public String getProtocolRequest() {
        return null;
    }

    /**
     * Compares this {@code Link} with the given object for equality.
     *
     * @param  obj  the object to compare with this {@code Link}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Link) {
            final Link that = (Link) obj;
            return Objects.equals(this.uri,  that.uri)  &&
                   Objects.equals(this.text, that.text) &&
                   Objects.equals(this.type, that.type);
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code Link}.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(uri, text, type);
    }

    /**
     * Returns the string representation of {@link #uri}, or {@code null} if {@code uri} is null.
     * This method is intended to allow direct usage of {@code Link} in code that format arbitrary {@link Object}.
     *
     * @return the URI, or {@code null}.
     */
    @Override
    public String toString() {
        return (uri != null) ? uri.toString() : null;
    }
}
