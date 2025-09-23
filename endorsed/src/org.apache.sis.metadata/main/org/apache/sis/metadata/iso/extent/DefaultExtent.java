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
package org.apache.sis.metadata.iso.extent;

import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BinaryOperator;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.pending.jdk.JDK19;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import org.apache.sis.xml.NilObject;
import org.apache.sis.xml.NilReason;


/**
 * Information about spatial, vertical, and temporal extent.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code EX_Extent}
 * {@code   ├─description……………………} The spatial and temporal extent for the referring object.
 * {@code   ├─geographicElement……} Geographic component of the extent of the referring object.
 * {@code   ├─temporalElement…………} Temporal component of the extent of the referring object.
 * {@code   │   └─extent………………………} The date and time for the content of the dataset.
 * {@code   └─verticalElement…………} Vertical component of the extent of the referring object.
 * {@code       ├─minimumValue………} The lowest vertical extent contained in the dataset.
 * {@code       ├─maximumValue………} The highest vertical extent contained in the dataset.
 * {@code       └─verticalCRS…………} Information about the vertical coordinate reference system.</div>
 *
 * This type has four conditional properties:
 * {@linkplain #getGeographicElements() geographic elements},
 * {@linkplain #getTemporalElements() temporal elements},
 * {@linkplain #getVerticalElements() vertical elements} and
 * {@linkplain #getDescription() description}.
 * At least one of the four shall be used.
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link #addElements(Envelope)} for adding extents inferred from the given envelope.</li>
 *   <li>{@link Extents#getGeographicBoundingBox(Extent)} for extracting a global geographic bounding box.</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @version 1.4
 *
 * @see Extents#getGeographicBoundingBox(Extent)
 * @see org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity()
 *
 * @since 0.3
 */
@TitleProperty(name = "description")
@XmlType(name = "EX_Extent_Type", propOrder = {
    "description",
    "geographicElements",
    "temporalElements",
    "verticalElements"
})
@XmlRootElement(name = "EX_Extent")
public class DefaultExtent extends ISOMetadata implements Extent {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2979058128422252800L;

    /**
     * The spatial and temporal extent for the referring object.
     */
    @SuppressWarnings("serial")
    private InternationalString description;

    /**
     * Provides geographic component of the extent of the referring object.
     */
    @SuppressWarnings("serial")
    private Collection<GeographicExtent> geographicElements;

    /**
     * Provides vertical component of the extent of the referring object.
     */
    @SuppressWarnings("serial")
    private Collection<VerticalExtent> verticalElements;

    /**
     * Provides temporal component of the extent of the referring object.
     */
    @SuppressWarnings("serial")
    private Collection<TemporalExtent> temporalElements;

    /**
     * Constructs an initially empty extent.
     */
    public DefaultExtent() {
    }

    /**
     * Constructs an extent initialized to the given description or components.
     * Any argument given to this constructor can be {@code null}.
     * While a valid {@code Extent} requires at least one component to be non-null,
     * this constructor does not perform such verification.
     *
     * @param  description         a description, or {@code null} if none.
     * @param  geographicElements  a geographic component, or {@code null} if none.
     * @param  verticalElements    a vertical component, or {@code null} if none.
     * @param  temporalElements    a temporal component, or {@code null} if none.
     */
    public DefaultExtent(final CharSequence     description,
                         final GeographicExtent geographicElements,
                         final VerticalExtent   verticalElements,
                         final TemporalExtent   temporalElements)
    {
        this.description        = Types.toInternationalString(description);
        this.geographicElements = singleton(geographicElements, GeographicExtent.class);
        this.verticalElements   = singleton(verticalElements,   VerticalExtent.class);
        this.temporalElements   = singleton(temporalElements,   TemporalExtent.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Extent)
     */
    public DefaultExtent(final Extent object) {
        super(object);
        if (object != null) {
            description        = object.getDescription();
            geographicElements = copyCollection(object.getGeographicElements(), GeographicExtent.class);
            temporalElements   = copyCollection(object.getTemporalElements(),   TemporalExtent.class);
            verticalElements   = copyCollection(object.getVerticalElements(),   VerticalExtent.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultExtent}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultExtent} instance is created using the
     *       {@linkplain #DefaultExtent(Extent) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultExtent castOrCopy(final Extent object) {
        if (object == null || object instanceof DefaultExtent) {
            return (DefaultExtent) object;
        }
        return new DefaultExtent(object);
    }

    /**
     * Returns the spatial and temporal extent for the referring object.
     *
     * @return the spatial and temporal extent, or {@code null} in none.
     */
    @Override
    @XmlElement(name = "description")
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the spatial and temporal extent for the referring object.
     *
     * @param  newValue  the new description.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission(description);
        description = newValue;
    }

    /**
     * Provides geographic component of the extent of the referring object
     *
     * @return the geographic extent, or an empty set if none.
     */
    @Override
    @XmlElement(name = "geographicElement")
    public Collection<GeographicExtent> getGeographicElements() {
        return geographicElements = nonNullCollection(geographicElements, GeographicExtent.class);
    }

    /**
     * Sets geographic component of the extent of the referring object.
     *
     * @param  newValues  the new geographic elements.
     */
    public void setGeographicElements(final Collection<? extends GeographicExtent> newValues) {
        geographicElements = writeCollection(newValues, geographicElements, GeographicExtent.class);
    }

    /**
     * Provides vertical component of the extent of the referring object.
     *
     * @return the vertical extent, or an empty set if none.
     */
    @Override
    @XmlElement(name = "verticalElement")
    public Collection<VerticalExtent> getVerticalElements() {
        return verticalElements = nonNullCollection(verticalElements, VerticalExtent.class);
    }

    /**
     * Sets vertical component of the extent of the referring object.
     *
     * @param  newValues  the new vertical elements.
     */
    public void setVerticalElements(final Collection<? extends VerticalExtent> newValues) {
        verticalElements = writeCollection(newValues, verticalElements, VerticalExtent.class);
    }

    /**
     * Provides temporal component of the extent of the referring object.
     *
     * @return the temporal extent, or an empty set if none.
     */
    @Override
    @XmlElement(name = "temporalElement")
    public Collection<TemporalExtent> getTemporalElements() {
        return temporalElements = nonNullCollection(temporalElements, TemporalExtent.class);
    }

    /**
     * Sets temporal component of the extent of the referring object.
     *
     * @param  newValues  the new temporal elements.
     */
    public void setTemporalElements(final Collection<? extends TemporalExtent> newValues) {
        temporalElements = writeCollection(newValues, temporalElements, TemporalExtent.class);
    }

    /**
     * Returns a non-null value if this extent is non-empty.
     * This implementation does not test if the elements are themselves empty.
     */
    private Boolean isNonEmpty() {
        return (geographicElements != null) || (verticalElements != null) || (temporalElements != null)
                || (description != null) ? Boolean.TRUE : null;
    }

    /**
     * Adds geographic, vertical or temporal extents inferred from the given envelope.
     * This method inspects the {@linkplain Envelope#getCoordinateReferenceSystem() envelope CRS}
     * and creates a {@link GeographicBoundingBox}, {@link VerticalExtent} or {@link TemporalExtent}
     * elements as needed.
     *
     * <p><b>Note:</b> this method is available only if the referencing module is on the module path.</p>
     *
     * @param  envelope  the envelope to use for inferring the additional extents.
     * @throws UnsupportedOperationException if the referencing module is not on the module path.
     * @throws TransformException if a coordinate transformation was required and failed.
     *
     * @see DefaultGeographicBoundingBox#setBounds(Envelope)
     * @see DefaultVerticalExtent#setBounds(Envelope)
     * @see DefaultTemporalExtent#setBounds(Envelope)
     */
    public void addElements(final Envelope envelope) throws TransformException {
        checkWritePermission(isNonEmpty());
        if (!ReferencingServices.getInstance().addElements(Objects.requireNonNull(envelope), this)) {
            throw new NotSpatioTemporalException(3, envelope);
        }
    }

    /**
     * Sets this extent to the intersection of this extent with the specified one.
     * This method computes the intersections of all geographic, vertical and temporal elements in this extent
     * with all geographic, vertical and temporal elements in the other extent, ignoring duplicated results.
     *
     * @param  other  the extent to intersect with this extent.
     * @throws IllegalArgumentException if two elements to intersect are not compatible (e.g. mismatched
     *         {@linkplain DefaultGeographicBoundingBox#getInclusion() bounding box inclusion status} or
     *         mismatched {@linkplain DefaultVerticalExtent#getVerticalCRS() vertical datum}).
     *
     * @see Extents#intersection(Extent, Extent)
     * @see org.apache.sis.geometry.GeneralEnvelope#intersect(Envelope)
     *
     * @since 0.8
     */
    public void intersect(final Extent other) {
        checkWritePermission(isNonEmpty());
        final InternationalString od = other.getDescription();
        if (od != null && !(description instanceof NilObject)) {
            if (description == null || (od instanceof NilObject)) {
                description = od;
            } else if (!description.equals(od)) {
                description = NilReason.MISSING.createNilObject(InternationalString.class);
            }
        }
        geographicElements = intersect(GeographicExtent.class, geographicElements, other.getGeographicElements(), Extents::intersection);
        verticalElements   = intersect(VerticalExtent.class,   verticalElements,   other.getVerticalElements(),   Extents::intersection);
        temporalElements   = intersect(TemporalExtent.class,   temporalElements,   other.getTemporalElements(),   Extents::intersection);
    }

    /**
     * Computes the intersections of all elements in the given {@code sources} collection will all elements
     * in the given {@code targets} collection. If one of those collections is null or empty, this method
     * returns all elements of the other collection (may be {@code targets} itself).
     *
     * @param  <T>        compile-time value of {@code type} argument.
     * @param  type       the type of elements in the collections.
     * @param  targets    the elements in this {@code DefaultExtent}. Also the collection where results will be stored.
     * @param  sources    the elements from the other {@code Extent} to intersect with this extent.
     * @param  intersect  the function computing intersections.
     * @return the intersection results. May be the same instance as {@code targets} with elements replaced.
     */
    private <T> Collection<T> intersect(final Class<T> type, Collection<T> targets, Collection<? extends T> sources, final BinaryOperator<T> intersect) {
        if (!Containers.isNullOrEmpty(sources)) {
            if (!Containers.isNullOrEmpty(targets)) {
                final LinkedHashSet<T> results = JDK19.newLinkedHashSet(targets.size());
                T empty = null;
                for (final T target : targets) {
                    for (final T source : sources) {
                        final T e = intersect.apply(target, source);
                        results.add(e);
                        /*
                         * If the two elements do not intersect, remember the value created by the intersection method
                         * for meaning "no intersection".  We remember only the first value since we always create the
                         * same value for meaning "no intersection".
                         */
                        if (empty == null && e != source && e != target && (e instanceof Emptiable) && ((Emptiable) e).isEmpty()) {
                            empty = e;
                        }
                    }
                }
                /*
                 * Remove the "no intersection" value, unless this is the only result.
                 */
                results.remove(null);
                if (results.size() > 1) {
                    results.remove(empty);
                }
                sources = results;
            }
            targets = writeCollection(sources, targets, type);
        }
        return targets;
    }
}
