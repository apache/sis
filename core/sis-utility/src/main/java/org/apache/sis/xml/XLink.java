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
package org.apache.sis.xml;

import java.net.URI;
import java.util.Locale;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlSchemaType;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Classes;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Loggers;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * The XML attributes defined by OGC in the
 * <a href="http://schemas.opengis.net/xlink/1.0.0/xlinks.xsd">xlink</a> schema.
 *
 * The allowed combinations of any one attribute depend on the value of the special
 * {@link #getType() type} attribute. Following is a summary of the element types
 * (columns) on which the global attributes (rows) are allowed, with an indication
 * of whether a value is required (R) or optional (O)
 * (Source: <a href="http://www.w3.org/TR/xlink/">W3C</a>):
 *
 * <table class="sis">
 * <caption>XLink attribute usage patterns</caption>
 * <tr>
 *   <th> </th>
 *   <th style="width: 14%">{@link XLink.Type#SIMPLE simple}</th>
 *   <th style="width: 14%">{@link XLink.Type#EXTENDED extended}</th>
 *   <th style="width: 14%">{@link XLink.Type#LOCATOR locator}</th>
 *   <th style="width: 14%">{@link XLink.Type#ARC arc}</th>
 *   <th style="width: 14%">{@link XLink.Type#RESOURCE resource}</th>
 *   <th style="width: 14%">{@link XLink.Type#TITLE title}</th>
 * </tr>
 *   <tr align="center"><td align="left"><b>{@link #getType() type}</b></td>       <td>R</td><td>R</td><td>R</td><td>R</td><td>R</td><td>R</td></tr>
 *   <tr align="center"><td align="left"><b>{@link #getHRef() href}</b></td>       <td>O</td><td> </td><td>R</td><td> </td><td> </td><td> </td></tr>
 *   <tr align="center"><td align="left"><b>{@link #getRole() role}</b></td>       <td>O</td><td>O</td><td>O</td><td> </td><td>O</td><td> </td></tr>
 *   <tr align="center"><td align="left"><b>{@link #getArcRole() arcrole}</b></td> <td>O</td><td> </td><td> </td><td>O</td><td> </td><td> </td></tr>
 *   <tr align="center"><td align="left"><b>{@link #getTitle() title}</b></td>     <td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td> </td></tr>
 *   <tr align="center"><td align="left"><b>{@link #getShow() show}</b></td>       <td>O</td><td> </td><td> </td><td>O</td><td> </td><td> </td></tr>
 *   <tr align="center"><td align="left"><b>{@link #getActuate() actuate}</b></td> <td>O</td><td> </td><td> </td><td>O</td><td> </td><td> </td></tr>
 *   <tr align="center"><td align="left"><b>{@link #getLabel() label}</b></td>     <td> </td><td> </td><td>O</td><td> </td><td>O</td><td> </td></tr>
 *   <tr align="center"><td align="left"><b>{@link #getFrom() from}</b></td>       <td> </td><td> </td><td> </td><td>O</td><td> </td><td> </td></tr>
 *   <tr align="center"><td align="left"><b>{@link #getTo() to}</b></td>           <td> </td><td> </td><td> </td><td>O</td><td> </td><td> </td></tr>
 * </table>
 *
 * When {@code xlink} attributes are found at unmarshalling time instead of an object definition,
 * those attributes are given to the {@link ReferenceResolver#resolve(MarshalContext, Class, XLink)}
 * method. Users can override that method in order to fetch an instance in some catalog for the given
 * {@code xlink} values.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see <a href="http://www.w3.org/TR/xlink/">XML Linking Language</a>
 * @see <a href="http://schemas.opengis.net/xlink/1.0.0/xlinks.xsd">OGC schema</a>
 */
@XmlTransient
public class XLink implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4046720871882443681L;

    /**
     * The type of link. If {@code null}, then the type will be inferred by {@link #getType()}.
     *
     * @see #getType()
     */
    private Type type;

    /**
     * A URN to an external resources, or to an other part of a XML document, or an identifier.
     *
     * @see #getHRef()
     * @category locator
     */
    private URI href;

    /**
     * A URI reference for some description of the arc role.
     *
     * @see #getRole()
     * @category semantic
     */
    private URI role;

    /**
     * A URI reference for some description of the arc role.
     *
     * @see #getArcRole()
     * @category semantic
     */
    private URI arcrole;

    /**
     * Just as with resources, this is simply a human-readable string with a short description
     * for the arc.
     *
     * @see #getTitle()
     * @category semantic
     */
    private InternationalString title;

    /**
     * Communicates the desired presentation of the ending resource on traversal
     * from the starting resource.
     *
     * @see #getShow()
     * @category behavior
     */
    private Show show;

    /**
     * Communicates the desired timing of traversal from the starting resource to the ending resource.
     *
     * @see #getActuate()
     * @category behavior
     */
    private Actuate actuate;

    /**
     * Identifies the target of a {@code from} or {@code to} attribute.
     *
     * @see #getLabel()
     * @category traversal
     */
    private String label;

    /**
     * The starting resource. The value must correspond to the same value for some
     * {@code label} attribute.
     *
     * @see #getFrom()
     * @category traversal
     */
    private String from;

    /**
     * The ending resource. The value must correspond to the same value for some
     * {@code label} attribute.
     *
     * @see #getTo()
     * @category traversal
     */
    private String to;

    /**
     * The cached hash code value, computed only if this {@code XLink} is unmodifiable. Otherwise,
     * this field is left to zero. This field is computed when the {@link #freeze()} method has
     * been invoked.
     */
    private int hashCode;

    /**
     * Creates a new link. The initial value of all attributes is {@code null}.
     */
    public XLink() {
    }

    /**
     * Creates a new link as a copy of the given link.
     *
     * @param link The link to copy, or {@code null} if none.
     */
    public XLink(final XLink link) {
        if (link != null) {
            type    = link.type;
            href    = link.href;
            role    = link.role;
            arcrole = link.arcrole;
            title   = link.title;
            show    = link.show;
            actuate = link.actuate;
            label   = link.label;
            from    = link.from;
            to      = link.to;
        }
    }

    /**
     * The type of a {@code xlink}. This type can be determined from the set of non-null
     * attribute values in a {@link XLink} instance.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     *
     * @see XLink#getType()
     */
    @XmlEnum
    public static enum Type {
        /**
         * A simple link. Allows the {@link XLink#getHRef() href}, {@link XLink#getRole() role},
         * {@link XLink#getArcRole() arcrole}, {@link #getTitle() title}, {@link XLink#getShow()
         * show} and {@link XLink#getActuate() actuate} attributes, all of them being optional.
         */
        @XmlEnumValue("simple")
        SIMPLE(0x1 | 0x2 | 0x4 | 0x8 | 0x10 | 0x20 | 0x40, 0x1),

        /**
         * An extended, possibly multi-resource, link. Allows the {@link XLink#getRole() role}
         * and {@link #getTitle() title} attributes, all of them being optional.
         */
        @XmlEnumValue("extended")
        EXTENDED(0x1 | 0x4 | 0x10, 0x1),

        /**
         * A pointer to an external resource. Allows the {@link XLink#getHRef() href},
         * {@link XLink#getRole() role}, {@link #getTitle() title} and {@link XLink#getLabel()
         * label} attributes, where {@code href} is mandatory and all other are optional.
         */
        @XmlEnumValue("locator")
        LOCATOR(0x1 | 0x2 | 0x4 | 0x10 | 0x80, 0x1 | 0x2),

        /**
         * An internal resource. Allows the {@link XLink#getRole() role},  {@link #getTitle() title}
         * and {@link #getLabel() label} attributes, all of them being optional.
         */
        @XmlEnumValue("resource")
        RESOURCE(0x1 | 0x4 | 0x10 | 0x80, 0x1),

        /**
         * A traversal rule between resources. Allows the {@link XLink#getArcRole() arcrole},
         * {@link #getTitle() title}, {@link XLink#getShow() show}, {@link XLink#getActuate()
         * actuate} {@link #getFrom() from} and {@link #getTo() to} attributes, all of them
         * being optional.
         */
        @XmlEnumValue("arc")
        ARC(0x1 | 0x8 | 0x10 | 0x20 | 0x40 | 0x100 | 0x200, 0x1),

        /**
         * A descriptive title for another linking element.
         */
        @XmlEnumValue("title")
        TITLE(0x1, 0x1),

        /**
         * A special value for computing the type automatically from the {@link XLink} attributes.
         * After a call to {@code XLink.setType(AUTO)}, any call to {@code XLink.getType()} will
         * infer the type from the non-null attributes as according the table documented in the
         * {@link XLink} javadoc.
         */
        AUTO(-1, 0);

        /**
         * A bitmask which specified the non-null fields expected for a given type.
         * The bit values are:
         *
         * <ul>
         *   <li>{@code type}:     0x1</li>
         *   <li>{@code href}:     0x2</li>
         *   <li>{@code role}:     0x4</li>
         *   <li>{@code arcrole}:  0x8</li>
         *   <li>{@code title}:   0x10</li>
         *   <li>{@code show}:    0x20</li>
         *   <li>{@code actuate}: 0x40</li>
         *   <li>{@code label}:   0x80</li>
         *   <li>{@code from}:   0x100</li>
         *   <li>{@code to}:     0x200</li>
         * </ul>
         */
        final int fieldMask, mandatory;

        /**
         * Creates a new type which allows the fields specified by the given mask.
         */
        private Type(final int mask, final int mandatory) {
            this.fieldMask = mask;
            this.mandatory = mandatory;
        }

        /**
         * Returns the attribute name for this type.
         */
        final String identifier() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Returns a mask of fields for which a non-null value has been defined.
     * The bit values are defined in the {@link XLink.Type#fieldMask} javadoc.
     */
    private int fieldMask() {
        int mask = 0;
        if (type    != null) mask |= 0x1;
        if (href    != null) mask |= 0x2;
        if (role    != null) mask |= 0x4;
        if (arcrole != null) mask |= 0x8;
        if (title   != null) mask |= 0x10;
        if (show    != null) mask |= 0x20;
        if (actuate != null) mask |= 0x40;
        if (label   != null) mask |= 0x80;
        if (from    != null) mask |= 0x100;
        if (to      != null) mask |= 0x200;
        return mask;
    }

    /**
     * Returns the type of link. May have one of the following values:
     *
     * <ul>
     *   <li><b>simple:</b>   a simple link</li>
     *   <li><b>extended:</b> an extended, possibly multi-resource, link</li>
     *   <li><b>locator:</b>  a pointer to an external resource</li>
     *   <li><b>resource:</b> an internal resource</li>
     *   <li><b>arc:</b>      a traversal rule between resources</li>
     *   <li><b>title:</b>    a descriptive title for another linking element</li>
     * </ul>
     *
     * The default value is {@code null}. If the {@link #setType(XLink.Type)} method has been
     * invoked with the {@link org.apache.sis.xml.XLink.Type#AUTO AUTO} enum, then this method
     * will infer a type from the attributes having a non-null value.
     *
     * @return The type of link, or {@code null}.
     */
    @XmlAttribute(name = "type", namespace = Namespaces.XLINK, required = true)
    public Type getType() {
        if (type != Type.AUTO) {
            return type;
        }
        Type best = null;
        int min = Integer.SIZE;
        final int defined = fieldMask();
        final int undefined = ~(defined | 0x1);
        for (final Type candidate : Type.values()) {
            final int forbidden = ~candidate.fieldMask;
            if (forbidden == 0) {
                continue; // Skip the AUTO enum.
            }
            // Test if this XLink instance defines only values allowed by the candidate type.
            if ((defined & forbidden) != 0) {
                continue;
            }
            // Test if this XLink instance defines all mandatory fields.
            if ((undefined & candidate.mandatory) != 0) {
                continue;
            }
            // Select the type requerying the smallest amount of fields.
            final int n = Integer.bitCount(undefined & candidate.fieldMask);
            if (n < min) {
                min = n;
                best = candidate;
            }
        }
        return best; // May still null.
    }

    /**
     * Sets the type of link. Any value different than {@link org.apache.sis.xml.XLink.Type#AUTO
     * Type.AUTO} (including {@code null}) will overwrite the value inferred automatically by
     * {@link #getType()}. A {@code AUTO} value will enable automatic type detection.
     *
     * @param type The new type of link, or {@code null} if none.
     */
    public void setType(final Type type) {
        canWrite(0x1, "type", "type"); // We want a non-null value in all cases.
        if (type != null && (fieldMask() & ~type.fieldMask) != 0) {
            throw new IllegalStateException(Errors.format(Errors.Keys.InconsistentAttribute_2, "type", type.identifier()));
        }
        this.type = type;
    }

    /**
     * Checks if the given attribute can be set.
     *
     * @param  field The attribute code, as documented in {@link XLink.Type#fieldMask}.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the given field can not be set for this kind of {@code xlink}.
     */
    private void canWrite(final int field, final String name, final Object value) throws IllegalStateException {
        if (hashCode != 0) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "XLink"));
        }
        final Type type = this.type;
        if (type != null) {
            if (value != null) {
                if ((type.fieldMask & field) == 0) {
                    throw new IllegalStateException(Errors.format(
                            Errors.Keys.ForbiddenAttribute_2, name, type.identifier()));
                }
            } else {
                if ((type.mandatory & field) != 0) {
                    throw new IllegalStateException(Errors.format(
                            Errors.Keys.MandatoryAttribute_2, name, type.identifier()));
                }
            }
        }
    }

    /**
     * Returns a URN to an external resources, or to an other part of a XML document, or an identifier.
     *
     * @return A URN to a resources, or {@code null} if none.
     *
     * @category locator
     */
    @XmlSchemaType(name = "anyURI")
    @XmlAttribute(name = "href", namespace = Namespaces.XLINK)
    public URI getHRef() {
        return href;
    }

    /**
     * Sets the URN to a resources.
     *
     * @param  href A URN to a resources, or {@code null} if none.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the link type {@linkplain #setType has been explicitely set}.
     *         and that type does not allow the {@code "href"} attribute.
     *
     * @category locator
     */
    public void setHRef(final URI href) throws IllegalStateException {
        canWrite(0x2, "href", href);
        this.href = href;
    }

    /**
     * Returns a URI reference for some description of the arc role.
     *
     * @return A URI reference for some description of the arc role, or {@code null} if none.
     *
     * @category semantic
     */
    @XmlSchemaType(name = "anyURI")
    @XmlAttribute(name = "role", namespace = Namespaces.XLINK)
    public URI getRole() {
        return role;
    }

    /**
     * Sets the URI reference for some description of the arc role.
     *
     * @param  role A URI reference for some description of the arc role, or {@code null} if none.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the link type {@linkplain #setType has been explicitely set}.
     *         and that type does not allow the {@code "role"} attribute.
     *
     * @category semantic
     */
    public void setRole(final URI role) throws IllegalStateException {
        canWrite(0x4, "role", role);
        this.role = role;
    }

    /**
     * Returns a URI reference for some description of the arc role.
     *
     * @return A URI reference for some description of the arc role, or {@code null} if none.
     *
     * @category semantic
     */
    @XmlSchemaType(name = "anyURI")
    @XmlAttribute(name = "arcrole", namespace = Namespaces.XLINK)
    public URI getArcRole() {
        return arcrole;
    }

    /**
     * Sets a URI reference for some description of the arc role.
     *
     * @param  arcrole A URI reference for some description of the arc role, or {@code null} if none.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the link type {@linkplain #setType has been explicitely set}.
     *         and that type does not allow the {@code "arcrole"} attribute.
     *
     * @category semantic
     */
    public void setArcRole(final URI arcrole) throws IllegalStateException {
        canWrite(0x8, "arcrole", arcrole);
        this.arcrole = arcrole;
    }

    /**
     * Returns a human-readable string with a short description for the arc.
     *
     * @return A human-readable string with a short description for the arc, or {@code null} if none.
     *
     * @category semantic
     */
    @XmlAttribute(name = "title", namespace = Namespaces.XLINK)
    public InternationalString getTitle() {
        return title;
    }

    /**
     * Sets a human-readable string with a short description for the arc.
     *
     * @param  title A human-readable string with a short description for the arc,
     *         or {@code null} if none.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the link type {@linkplain #setType has been explicitely set}.
     *         and that type does not allow the {@code "title"} attribute.
     *
     * @category semantic
     */
    public void setTitle(final InternationalString title) throws IllegalStateException {
        canWrite(0x10, "title", title);
        this.title = title;
    }

    /**
     * Communicates the desired presentation of the ending resource on traversal
     * from the starting resource.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     *
     * @see XLink#getShow()
     */
    @XmlEnum
    public static enum Show {
        /**
         * Load ending resource in a new window, frame, pane, or other presentation context.
         */
        @XmlEnumValue("new") NEW,

        /**
         * Load the resource in the same window, frame, pane, or other presentation context.
         */
        @XmlEnumValue("replace") REPLACE,

        /**
         * Load ending resource in place of the presentation of the starting resource.
         */
        @XmlEnumValue("embed") EMBED,

        /**
         * Behavior is unconstrained; examine other markup in the link for hints.
         */
        @XmlEnumValue("other") OTHER,

        /**
         * Behavior is unconstrained.
         */
        @XmlEnumValue("none") NONE
    }

    /**
     * Returns the desired presentation of the ending resource on traversal
     * from the starting resource. It's value should be treated as follows:
     *
     * <ul>
     *   <li><b>new:</b>     load ending resource in a new window, frame, pane, or other presentation context</li>
     *   <li><b>replace:</b> load the resource in the same window, frame, pane, or other presentation context</li>
     *   <li><b>embed:</b>   load ending resource in place of the presentation of the starting resource</li>
     *   <li><b>other:</b>   behavior is unconstrained; examine other markup in the link for hints</li>
     *   <li><b>none:</b>    behavior is unconstrained</li>
     * </ul>
     *
     * @return The desired presentation of the ending resource, or {@code null} if unspecified.
     *
     * @category behavior
     */
    @XmlAttribute(name = "show", namespace = Namespaces.XLINK)
    public Show getShow() {
        return show;
    }

    /**
     * Sets the desired presentation of the ending resource on traversal from the starting resource.
     *
     * @param  show The desired presentation of the ending resource, or {@code null} if unspecified.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the link type {@linkplain #setType has been explicitely set}.
     *         and that type does not allow the {@code "show"} attribute.
     *
     * @category behavior
     */
    public void setShow(final Show show) throws IllegalStateException {
        canWrite(0x20, "show", show);
        this.show = show;
    }

    /**
     * Communicates the desired timing of traversal from the starting resource to the ending
     * resource.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     *
     * @see XLink#getActuate()
     */
    @XmlEnum
    public static enum Actuate {
        /**
         * Traverse to the ending resource immediately on loading the starting resource.
         */
        @XmlEnumValue("onLoad") ON_LOAD,

        /**
         * Traverse from the starting resource to the ending resource only on a post-loading event
         * triggered for this purpose.
         */
        @XmlEnumValue("onRequest") ON_REQUEST,

        /**
         * Behavior is unconstrained; examine other markup in link for hints.
         */
        @XmlEnumValue("other") OTHER,

        /**
         * Behavior is unconstrained.
         */
        @XmlEnumValue("none") NONE
    }

    /**
     * Returns the desired timing of traversal from the starting resource to the ending
     * resource. It's value should be treated as follows:
     *
     * <ul>
     *   <li><b>onLoad:</b>    traverse to the ending resource immediately on loading the starting resource</li>
     *   <li><b>onRequest:</b> traverse from the starting resource to the ending resource only on a post-loading event triggered for this purpose</li>
     *   <li><b>other:</b>     behavior is unconstrained; examine other markup in link for hints</li>
     *   <li><b>none:</b>      behavior is unconstrained</li>
     * </ul>
     *
     * @return The desired timing of traversal from the starting resource to the ending resource,
     *         or {@code null} if unspecified.
     *
     * @category behavior
     */
    @XmlAttribute(name = "actuate", namespace = Namespaces.XLINK)
    public Actuate getActuate() {
        return actuate;
    }

    /**
     * Sets the desired timing of traversal from the starting resource to the ending resource.
     *
     * @param  actuate The desired timing of traversal from the starting resource to the ending
     *         resource, or {@code null} if unspecified.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the link type {@linkplain #setType has been explicitely set}.
     *         and that type does not allow the {@code "actuate"} attribute.
     *
     * @category behavior
     */
    public void setActuate(final Actuate actuate) throws IllegalStateException {
        canWrite(0x40, "actuate", actuate);
        this.actuate = actuate;
    }

    /**
     * Returns an identification of the target of a {@code from} or {@code to} attribute.
     *
     * @return An identification of the target of a {@code from} or {@code to} attribute, or {@code null}.
     *
     * @category traversal
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets an identification of the target of a {@code from} or {@code to} attribute.
     *
     * @param  label An identification of the target of a {@code from} or {@code to} attribute, or {@code null}.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the link type {@linkplain #setType has been explicitely set}.
     *         and that type does not allow the {@code "label"} attribute.
     *
     * @category traversal
     */
    public void setLabel(final String label) throws IllegalStateException {
        canWrite(0x80, "label", label);
        this.label = label;
    }

    /**
     * Returns the starting resource. The value must correspond to the same value for some
     * {@code label} attribute.
     *
     * @return The starting resource, or {@code null}.
     *
     * @category traversal
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the starting resource. The value must correspond to the same value for some
     * {@code label} attribute.
     *
     * @param  from The starting resource, or {@code null}.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the link type {@linkplain #setType has been explicitely set}.
     *         and that type does not allow the {@code "from"} attribute.
     *
     * @category traversal
     */
    public void setFrom(final String from) throws IllegalStateException {
        canWrite(0x100, "from", from);
        this.from = from;
    }

    /**
     * Returns the ending resource. The value must correspond to the same value for some
     * {@code label} attribute.
     *
     * @return The ending resource, or {@code null}.
     *
     * @category traversal
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets the ending resource. The value must correspond to the same value for some
     * {@code label} attribute.
     *
     * @param  to The ending resource, or {@code null}.
     * @throws UnsupportedOperationException If this {@code xlink} is unmodifiable.
     * @throws IllegalStateException If the link type {@linkplain #setType has been explicitely set}.
     *         and that type does not allow the {@code "to"} attribute.
     *
     * @category traversal
     */
    public void setTo(final String to) throws IllegalStateException {
        canWrite(0x200, "to", to);
        this.to = to;
    }

    /**
     * Marks this {@code xlink} as unmodifiable. After this method call, any call to a setter
     * method will throw an {@link UnsupportedOperationException}.
     *
     * <p>After the first call to this method, any subsequent calls have no effect.</p>
     */
    public void freeze() {
        if (hashCode == 0) {
            hashCode = hash();
        }
    }

    /**
     * Compares this {@code XLink} with the given object for equality.
     *
     * @param object The object to compare with this XLink.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final XLink that = (XLink) object;
            final int h0 = hashCode;
            if (h0 != 0) {
                final int h1 = that.hashCode;
                if (h1 != 0 && h0 != h1) {
                    return false; // Slight optimization using the pre-computed hash code values.
                }
            }
            return Objects.equals(this.type,    that.type)    &&
                   Objects.equals(this.href,    that.href)    &&
                   Objects.equals(this.role,    that.role)    &&
                   Objects.equals(this.arcrole, that.arcrole) &&
                   Objects.equals(this.title,   that.title)   &&
                   Objects.equals(this.show,    that.show)    &&
                   Objects.equals(this.actuate, that.actuate) &&
                   Objects.equals(this.label,   that.label)   &&
                   Objects.equals(this.from,    that.from)    &&
                   Objects.equals(this.to,      that.to);
        }
        return false;
    }

    /**
     * Returns a hash code value for this XLink.
     */
    @Override
    public int hashCode() {
        int hash = hashCode;
        if (hash == 0) {
            hash = hash();
            // Do not save the hash code value, since it may change.
        }
        return hash;
    }

    /**
     * Computes the hash code now. This method is guaranteed to return a value different
     * than zero, in order to allow us to use 0 as a sentinel value for modifiable xlink.
     */
    private int hash() {
        int hash = Objects.hash(href, role, arcrole, title, show, actuate, label, from, to, type) ^ (int) serialVersionUID;
        if (hash == 0) {
            hash = -1;
        }
        return hash;
    }

    /**
     * Returns a string representation of this object. The default implementation returns the
     * simple class name followed by non-null attributes, as in the example below:
     *
     * {@preformat text
     *     XLink[type="locator", href="urn:ogc:def:method:EPSG::4326"]
     * }
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(64);
        buffer.append(Classes.getShortClassName(this)).append('[');
        append(buffer, "type",    getType());
        append(buffer, "href",    getHRef());
        append(buffer, "role",    getRole());
        append(buffer, "arcrole", getArcRole());
        append(buffer, "title",   getTitle());
        append(buffer, "show",    getShow());
        append(buffer, "actuate", getActuate());
        append(buffer, "label",   getLabel());
        append(buffer, "from",    getFrom());
        append(buffer, "to",      getTo());
        return buffer.append(']').toString();
    }

    /**
     * Appends the given attribute in the given buffer if the attribute value is not null.
     * If the given value is an attribute, the XML name will be used rather than the Java
     * field name.
     */
    private static void append(final StringBuilder buffer, final String label, Object value) {
        if (value != null) {
            if (buffer.charAt(buffer.length() - 1) != '[') {
                buffer.append(", ");
            }
            if (value instanceof Enum<?>) try {
                final XmlEnumValue xml = value.getClass().getField(((Enum<?>) value).name()).getAnnotation(XmlEnumValue.class);
                if (xml != null) {
                    value = xml.value();
                }
            } catch (NoSuchFieldException e) {
                // Should never happen with Enums. But if it
                // happen anyway, this is not a fatal error.
                Logging.unexpectedException(Logging.getLogger(Loggers.XML), XLink.class, "toString", e);
            }
            buffer.append(label).append("=\"").append(value).append('"');
        }
    }
}
