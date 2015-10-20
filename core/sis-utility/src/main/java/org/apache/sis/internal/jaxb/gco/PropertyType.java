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
package org.apache.sis.internal.jaxb.gco;

import java.util.UUID;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.xml.XLink;
import org.apache.sis.xml.NilObject;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.xml.ReferenceResolver;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.PrimitiveTypeProperties;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.resources.Errors;


/**
 * Base class for adapters from GeoAPI interfaces to their SIS implementations.
 * Implementation subclasses are actually both JAXB adapters and wrappers around
 * the value to be marshalled. Wrappers exist because ISO 19139 have the strange
 * habit to wrap every properties in an extra level, for example:
 *
 * {@preformat xml
 *   <CI_ResponsibleParty>
 *     <contactInfo>
 *       <CI_Contact>
 *         ...
 *       </CI_Contact>
 *     </contactInfo>
 *   </CI_ResponsibleParty>
 * }
 *
 * The {@code <CI_Contact>} level is not really necessary, and JAXB is not designed for inserting
 * such level since it is not the usual way to write XML. In order to get this output with JAXB,
 * we have to wrap metadata object in an additional object. So each {@code PropertyType} subclass
 * is both a JAXB adapter and a wrapper. We have merged those functionalities in order to avoid
 * doubling the amount of classes, which is already large.
 *
 * <p>In ISO 19139 terminology:</p>
 * <ul>
 *   <li>the public classes defined in the {@code org.apache.sis.metadata.iso} packages are defined
 *       as {@code Foo_Type} in ISO 19139, where <var>Foo</var> is the ISO name of a class.</li>
 *   <li>the {@code PropertyType} subclasses are defined as {@code Foo_PropertyType} in
 *       ISO 19139 schemas.</li>
 * </ul>
 *
 * <div class="section">Guidlines for subclasses</div>
 * Subclasses shall provide a method returning the SIS implementation class for the metadata value.
 * This method will be systematically called at marshalling time by JAXB. Typical implementation
 * ({@code BoundType} and {@code ValueType} need to be replaced by the concrete class):
 *
 * {@preformat java
 *   &#64;XmlElementRef
 *   public BoundType getElement() {
 *       if (skip()) return null;
 *       final ValueType metadata = this.metadata;
 *       return (metadata instanceof BoundType) ? (BoundType) metadata : new BoundType(metadata);
 *   }
 *
 *   public void getElement(final BoundType metadata) {
 *       this.metadata = metadata;
 *   }
 * }
 *
 * The actual implementation may be slightly more complicated than the above if there is
 * various subclasses to check.
 *
 * <div class="note"><b>Note:</b>
 * A previous version provided an abstract {@code getElement()} method in this class
 * for enforcing its definition in subclasses. But this has been removed for two reasons:
 * <ul>
 *   <li>While the return value is usually {@code BoundType}, in some situations it is
 *       rather an other type like {@code String}. For this raison the return type must
 *       be declared as {@code Object}, and subclasses have to restrict it to a more
 *       specific type.</li>
 *   <li>The parameterized return type forces the compiler to generate bridge methods under
 *       the hood. In the particular case of typical {@code PropertyType} subclasses,
 *       this increases the size of {@code .class} files by approximatively 4.5%.
 *       While quite small, this is a useless overhead since we never need to invoke the
 *       abstract {@code getElement()} from this class.</li>
 * </ul></div>
 *
 * @param <ValueType> The adapter subclass.
 * @param <BoundType> The interface being adapted.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see XmlAdapter
 */
public abstract class PropertyType<ValueType extends PropertyType<ValueType,BoundType>, BoundType>
        extends XmlAdapter<ValueType,BoundType>
{
    /**
     * {@code true} if marshalling an XML based on ISO 19115:2003 model. A value of {@code false}
     * (ISO 19115:2014 model) is not yet supported, so we currently use this variable only as a way
     * to identify the code to revisit when we will want to support the new model.
     */
    public static final boolean LEGACY_XML = true;

    /**
     * The wrapped GeoAPI metadata instance, or {@code null} if the metadata shall not be marshalled.
     * Metadata are not marshalled when replaced by {@code xlink:href} or {@code uuidref} attributes.
     */
    protected BoundType metadata;

    /**
     * Either {@code null}, an {@link ObjectReference} or a {@link String}.
     *
     * <ul>
     *   <li>{@link ObjectReference} defines the {@code uuidref}, {@code xlink:href}, {@code xlink:role},
     *       {@code xlink:arcrole}, {@code xlink:title}, {@code xlink:show} and {@code xlink:actuate} attributes.</li>
     *   <li>{@link String} defines the {@code nilReason} attribute.</li>
     * </ul>
     *
     * Those two properties are exclusive (if the user defines an object reference,
     * then the attribute is not nil).
     */
    private Object reference;

    /**
     * Empty constructor for subclasses only.
     */
    protected PropertyType() {
    }

    /**
     * Builds a {@code PropertyType} wrapper for the given primitive type wrapper.
     * This constructor checks for nil reasons only if {@code check} is {@code true}.
     *
     * @param value The primitive type wrapper.
     * @param mayBeNil {@code true} if we should check for nil reasons.
     */
    PropertyType(final BoundType value, final boolean mayBeNil) {
        metadata = value;
        if (mayBeNil) {
            final Object property = PrimitiveTypeProperties.property(value);
            if (property instanceof NilReason) {
                reference = property.toString();
                metadata  = null;
            }
        }
    }

    /**
     * Builds a wrapper for the given GeoAPI interface. This constructor checks if the given metadata
     * implements the {@link NilObject} or {@link IdentifiedObject} interface. If the object implements
     * both of them (should not happen, but we never know), then the identifiers will have precedence.
     *
     * @param value The interface to wrap.
     */
    protected PropertyType(final BoundType value) {
        /*
         * Do not invoke NilReason.forObject(metadata) in order to avoid unnecessary synchronization.
         * Subclasses will use the PropertyType(BoundType, boolean) constructor instead when a check
         * for primitive type is required.
         */
        if (value instanceof NilObject) {
            final NilReason reason = ((NilObject) value).getNilReason();
            if (reason != null) {
                reference = reason.toString();
                return;
            }
        }
        /*
         * Verifies if the object to marshall can be replaced by a xlink or uuidref.
         * First, check if we can use a xlink:href="#foo" reference to a gml:id="foo".
         * Only if no gml:id was found, check for user-defined xlink or uuidref.
         */
        @SuppressWarnings("OverridableMethodCallDuringObjectConstruction")
        final Class<BoundType>  type     = getBoundType();
        final Context           context  = Context.current();
        final ReferenceResolver resolver = Context.resolver(context);
        final String id = Context.getObjectID(context, value);
        if (id != null && resolver.canSubstituteByReference(context, type, value, id)) try {
            final XLink link = new XLink();
            link.setHRef(new URI(null, null, id));
            reference = new ObjectReference(null, link);
            return;
        } catch (URISyntaxException e) {
            Context.warningOccured(context, getClass(), "<init>", e, true);
        }
        metadata = value;   // Non-null only after we verified that not a NilObject or xlink:href="#foo".
        if (value instanceof IdentifiedObject) {
            /*
             * Get the identifiers as full UUID or XLink objects. We do not use the more permissive methods
             * working with arbitrary strings -- e.g. map.get(IdentifierSpace.HREF) -- because we are going
             * to use those values for marshalling REFERENCES to an externally-defined metadata object, not
             * for declaring the attributes to marshal together with the metadata. Since references REPLACE
             * the original metadata object, we are better to ensure that they are well formed - in case of
             * doubt, we are better to marshal the full object. We are not loosing information since in the
             * later case, the identifiers will be marshalled as Strings by ISOMetadata. Example:
             *
             *   <gmd:CI_Citation>
             *     <gmd:series uuidref="f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf">      ☚ marshalled by this
             *       <gmd:CI_Series uuid="f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf">    ☚ marshalled by ISOMetadata
             *         ...
             *       </gmd:CI_Series>
             *     </gmd:series>
             *   </gmd:CI_Citation>
             *
             * We do not try to parse UUID or XLink objects from String because it should be the job of
             * org.apache.sis.internal.jaxb.ModifiableIdentifierMap.put(Citation, String).
             */
            final IdentifierMap map = ((IdentifiedObject) value).getIdentifierMap();
            XLink link = map.getSpecialized(IdentifierSpace.XLINK);
            UUID  uuid = map.getSpecialized(IdentifierSpace.UUID);
            if (uuid != null || link != null) {
                /*
                 * Check if the user gives us the permission to use reference to those identifiers.
                 * If not, forget them in order to avoid marshalling the identifiers twice (see the
                 * example in the above comment).
                 */
                if (uuid != null) {
                    if (resolver.canSubstituteByReference(context, type, value, uuid)) {
                        metadata = null;
                    } else {
                        uuid = null;
                    }
                }
                /*
                 * There is no risk of duplication for 'xlink' because there is no such attribute in ISOMetadata.
                 * So if the user does not allow us to omit the metadata object, we will still keep the xlink for
                 * informative purpose.
                 */
                if (link != null && resolver.canSubstituteByReference(context, type, value, link)) {
                    metadata = null;
                }
                if (uuid != null || link != null) {
                    reference = new ObjectReference(uuid, link);
                }
            }
        }
    }

    /**
     * Returns the object reference, or {@code null} if none and the {@code create} argument is {@code false}.
     * If the {@code create} argument is {@code true}, then this method will create the object reference when
     * first needed. In the later case, any previous {@code gco:nilReason} will be overwritten since
     * the object is not nil.
     */
    private ObjectReference reference(final boolean create) {
        final Object ref = reference;
        if (ref instanceof ObjectReference) {
            return (ObjectReference) ref;
        } else if (create) {
            final ObjectReference newRef = new ObjectReference();
            reference = newRef;
            return newRef;
        } else {
            return null;
        }
    }

    /**
     * Returns the {@code xlink}, or {@code null} if none and {@code create} is {@code false}.
     * If the {@code create} argument is {@code true}, then this method will create the XLink
     * when first needed. In the later case, any previous {@code gco:nilReason} will be
     * overwritten since the object is not nil.
     */
    private XLink xlink(final boolean create) {
        final ObjectReference ref = reference(create);
        if (ref == null) {
            return null;
        }
        XLink xlink = ref.xlink;
        if (create && xlink == null) {
            ref.xlink = xlink = new XLink();
            xlink.setType(XLink.Type.SIMPLE); // The "simple" type is fixed in the "gco" schema.
        }
        return xlink;
    }

    /**
     * The reason why a mandatory attribute if left unspecified.
     *
     * @return the current value, or {@code null} if none.
     * @category gco:PropertyType
     */
    @XmlAttribute(name = "nilReason", namespace = Namespaces.GCO)
    public final String getNilReason() {
        final Object ref = reference;
        return (ref instanceof String) ? (String) ref : null;
    }

    /**
     * Sets the {@code nilReason} attribute value. This method does nothing if a
     * non-null {@linkplaih #reference} exists, since in such case the object can
     * not be nil.
     *
     * @param nilReason The new attribute value.
     * @category gco:PropertyType
     */
    public final void setNilReason(final String nilReason) {
        if (!(reference instanceof ObjectReference)) {
            reference = nilReason;
        }
    }

    /**
     * A URN to an external resources, or to an other part of a XML document, or an identifier.
     * The {@code uuidref} attribute is used to refer to an XML element that has a corresponding
     * {@code uuid} attribute.
     *
     * @return the current value, or {@code null} if none.
     * @category gco:ObjectReference
     */
    @XmlAttribute(name = "uuidref")  // Defined in "gco" as unqualified attribute.
    public final String getUUIDREF() {
        final ObjectReference ref = reference(false);
        return (ref != null) ? toString(ref.uuid) : null;
    }

    /**
     * Sets the {@code uuidref} attribute value.
     *
     * @param  uuid The new attribute value.
     * @throws IllegalArgumentException If the given UUID can not be parsed.
     * @category gco:ObjectReference
     */
    public final void setUUIDREF(final String uuid) throws IllegalArgumentException {
        final Context context = Context.current();
        reference(true).uuid = Context.converter(context).toUUID(context, uuid);
    }

    /**
     * Returns the given URI as a string, or returns {@code null} if the given argument is null.
     */
    private static String toString(final Object text) {
        return (text != null) ? text.toString() : null;
    }

    /**
     * Parses the given URI, or returns {@code null} if the given argument is null or empty.
     */
    private static URI toURI(final String uri) throws URISyntaxException {
        final Context context = Context.current();
        return Context.converter(context).toURI(context, uri);
    }

    /**
     * A URN to an external resources, or to an other part of a XML document, or an identifier.
     * The {@code xlink:href} attribute allows an XML element to refer to another XML element
     * that has a corresponding {@code id} attribute.
     *
     * @return the current value, or {@code null} if none.
     * @category xlink
     */
    @XmlSchemaType(name = "anyURI")
    @XmlAttribute(name = "href", namespace = Namespaces.XLINK)
    public final String getHRef() {
        final XLink link = xlink(false);
        return (link != null) ? toString(link.getHRef()) : null;
    }

    /**
     * Sets the {@code href} attribute value.
     *
     * @param href The new attribute value.
     * @throws URISyntaxException If th given string can not be parsed as a URI.
     * @category xlink
     */
    public final void setHRef(final String href) throws URISyntaxException {
        xlink(true).setHRef(toURI(href));
    }

    /**
     * A URI reference for some description of the arc role.
     *
     * @return the current value, or {@code null} if none.
     * @category xlink
     */
    @XmlSchemaType(name = "anyURI")
    @XmlAttribute(name = "role", namespace = Namespaces.XLINK)
    public final String getRole() {
        final XLink link = xlink(false);
        return (link != null) ? toString(link.getRole()) : null;
    }

    /**
     * Sets the {@code role} attribute value.
     *
     * @param role The new attribute value.
     * @throws URISyntaxException If th given string can not be parsed as a URI.
     * @category xlink
     */
    public final void setRole(final String role) throws URISyntaxException {
        xlink(true).setRole(toURI(role));
    }

    /**
     * A URI reference for some description of the arc role.
     *
     * @return the current value, or {@code null} if none.
     * @category xlink
     */
    @XmlSchemaType(name = "anyURI")
    @XmlAttribute(name = "arcrole", namespace = Namespaces.XLINK)
    public final String getArcRole() {
        final XLink link = xlink(false);
        return (link != null) ? toString(link.getArcRole()) : null;
    }

    /**
     * Sets the {@code arcrole} attribute value.
     *
     * @param arcrole The new attribute value.
     * @throws URISyntaxException If th given string can not be parsed as a URI.
     * @category xlink
     */
    public final void setArcRole(final String arcrole) throws URISyntaxException {
        xlink(true).setArcRole(toURI(arcrole));
    }

    /**
     * Just as with resources, this is simply a human-readable string with a short description
     * for the arc.
     *
     * @return the current value, or {@code null} if none.
     * @category xlink
     */
    @XmlAttribute(name = "title", namespace = Namespaces.XLINK)
    public final String getTitle() {
        final XLink link = xlink(false);
        return (link != null) ? StringAdapter.toString(link.getTitle()) : null;
    }

    /**
     * Sets the {@code title} attribute value.
     *
     * @param title The new attribute value.
     * @category xlink
     */
    public final void setTitle(String title) {
        if (title != null && !(title = title.trim()).isEmpty()) {
            xlink(true).setTitle(new SimpleInternationalString(title));
        }
    }

    /**
     * Communicates the desired presentation of the ending resource on traversal
     * from the starting resource. It's value should be treated as follows:
     *
     * <ul>
     *   <li>new: load ending resource in a new window, frame, pane, or other presentation context</li>
     *   <li>replace: load the resource in the same window, frame, pane, or other presentation context</li>
     *   <li>embed: load ending resource in place of the presentation of the starting resource</li>
     *   <li>other: behavior is unconstrained; examine other markup in the link for hints</li>
     *   <li>none: behavior is unconstrained</li>
     * </ul>
     *
     * @return the current value, or {@code null} if none.
     * @category xlink
     */
    @XmlAttribute(name = "show", namespace = Namespaces.XLINK)
    public final XLink.Show getShow() {
        final XLink link = xlink(false);
        return (link != null) ? link.getShow() : null;
    }

    /**
     * Sets the {@code show} attribute value.
     *
     * @param show The new attribute value.
     * @category xlink
     */
    public final void setShow(final XLink.Show show) {
        xlink(true).setShow(show);
    }

    /**
     * Communicates the desired timing of traversal from the starting resource to the ending resource.
     * It's value should be treated as follows:
     *
     * <ul>
     *   <li>onLoad: traverse to the ending resource immediately on loading the starting resource</li>
     *   <li>onRequest: traverse from the starting resource to the ending resource only on a post-loading event triggered for this purpose</li>
     *   <li>other: behavior is unconstrained; examine other markup in link for hints</li>
     *   <li>none: behavior is unconstrained</li>
     * </ul>
     *
     * @return the current value, or {@code null} if none.
     * @category xlink
     */
    @XmlAttribute(name = "actuate", namespace = Namespaces.XLINK)
    public final XLink.Actuate getActuate() {
        final XLink link = xlink(false);
        return (link != null) ? link.getActuate() : null;
    }

    /**
     * Sets the {@code actuate} attribute value.
     *
     * @param actuate The new attribute value.
     * @category xlink
     */
    public final void setActuate(final XLink.Actuate actuate) {
        xlink(true).setActuate(actuate);
    }

    // Do NOT declare attributes xlink:label, xlink:from and xlink:to,
    // because they are not part of the xlink:simpleLink group.


    // ======== XmlAdapter methods ===============================================================


    /**
     * Returns the bound type, which is typically the GeoAPI interface.
     * Subclasses need to return a hard-coded value. They shall not compute
     * a value from the object fields, because this method is invoked from
     * the constructor.
     *
     * @return The bound type, which is typically the GeoAPI interface.
     */
    protected abstract Class<BoundType> getBoundType();

    /**
     * Creates a new instance of this class wrapping the given metadata.
     * This method is invoked by {@link #marshal} after making sure that
     * {@code value} is not null.
     *
     * @param value The GeoAPI interface to wrap.
     * @return The adapter.
     */
    protected abstract ValueType wrap(final BoundType value);

    /**
     * Converts a GeoAPI interface to the appropriate adapter for the way it will be
     * marshalled into an XML file or stream. JAXB calls automatically this method at
     * marshalling time.
     *
     * @param value The bound type value, here the interface.
     * @return The adapter for the given value.
     */
    @Override
    public final ValueType marshal(final BoundType value) {
        if (value == null) {
            return null;
        }
        return wrap(value);
    }

    /**
     * Converts an adapter read from an XML stream to the GeoAPI interface which will
     * contains this value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value The adapter for a metadata value.
     * @return An instance of the GeoAPI interface which represents the metadata value.
     * @throws URISyntaxException If a URI can not be parsed.
     */
    @Override
    public final BoundType unmarshal(final ValueType value) throws URISyntaxException {
        return (value != null) ? value.resolve(Context.current()) : null;
    }

    /**
     * If the {@linkplain #metadata} is still null, tries to resolve it using UUID, XLink
     * or NilReason information. This method is invoked at unmarshalling time.
     *
     * @throws URISyntaxException If a nil reason is present and can not be parsed.
     */
    final BoundType resolve(final Context context) throws URISyntaxException {
        final ObjectReference ref = reference(false);
        if (ref != null) {
            metadata = ref.resolve(context, getBoundType(), metadata);
        }
        if (metadata == null) {
            final String value = getNilReason();
            if (value != null) {
                final NilReason nilReason = Context.converter(context).toNilReason(context, value);
                if (nilReason != null) {
                    metadata = nilReason.createNilObject(getBoundType());
                }
            }
        }
        return metadata;
    }

    /**
     * Invoked by subclasses when the unmarshalled object is missing a component.
     * This method is invoked when the missing component is essential to SIS working.
     * This method is not invoked if the missing component is flagged as mandatory by GML,
     * but is not mandatory for SIS working.
     *
     * @param  missing The name of the missing XML component.
     * @throws IllegalArgumentException Always thrown.
     *
     * @since 0.7
     */
    protected final void incomplete(final String missing) throws IllegalArgumentException {
        throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingComponentInElement_2, getBoundType(), missing));
    }

    /*
     * Do not provide the following method here:
     *
     *     public abstract BountType getElement();
     *
     * as it adds a small but unnecessary overhead.
     * See class Javadoc for more information.
     */
}
