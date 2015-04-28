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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.metadata.extent.VerticalExtent;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.gco.GO_Real;
import org.apache.sis.internal.metadata.ReferencingServices;


/**
 * Vertical domain of dataset.
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link #setBounds(Envelope)} for setting the extent from the given envelope.</li>
 * </ul>
 *
 * <div class="section">Limitations</div>
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
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "EX_VerticalExtent_Type", propOrder = {
    "minimumValue",
    "maximumValue",
    "verticalCRS"
})
@XmlRootElement(name = "EX_VerticalExtent")
public class DefaultVerticalExtent extends ISOMetadata implements VerticalExtent {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1963873471175296153L;

    /**
     * The lowest vertical extent contained in the dataset.
     */
    private Double minimumValue;

    /**
     * The highest vertical extent contained in the dataset.
     */
    private Double maximumValue;

    /**
     * Provides information about the vertical coordinate reference system to
     * which the maximum and minimum elevation values are measured. The CRS
     * identification includes unit of measure.
     */
    private VerticalCRS verticalCRS;

    /**
     * Constructs an initially empty vertical extent.
     */
    public DefaultVerticalExtent() {
    }

    /**
     * Creates a vertical extent initialized to the specified values.
     *
     * @param minimumValue The lowest vertical extent contained in the dataset, or {@link Double#NaN} if none.
     * @param maximumValue The highest vertical extent contained in the dataset, or {@link Double#NaN} if none.
     * @param verticalCRS  The information about the vertical coordinate reference system, or {@code null}.
     */
    public DefaultVerticalExtent(final double minimumValue,
                                 final double maximumValue,
                                 final VerticalCRS verticalCRS)
    {
        if (!Double.isNaN(minimumValue)) this.minimumValue = minimumValue;
        if (!Double.isNaN(maximumValue)) this.maximumValue = maximumValue;
        this.verticalCRS  = verticalCRS;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(VerticalExtent)
     */
    public DefaultVerticalExtent(final VerticalExtent object) {
        super(object);
        if (object != null) {
            minimumValue = object.getMinimumValue();
            maximumValue = object.getMaximumValue();
            verticalCRS  = object.getVerticalCRS();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultVerticalExtent}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultVerticalExtent} instance is created using the
     *       {@linkplain #DefaultVerticalExtent(VerticalExtent) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultVerticalExtent castOrCopy(final VerticalExtent object) {
        if (object == null || object instanceof DefaultVerticalExtent) {
            return (DefaultVerticalExtent) object;
        }
        return new DefaultVerticalExtent(object);
    }

    /**
     * Returns the lowest vertical extent contained in the dataset.
     *
     * @return The lowest vertical extent, or {@code null}.
     */
    @Override
    @XmlElement(name = "minimumValue", required = true)
    @XmlJavaTypeAdapter(GO_Real.class)
    public Double getMinimumValue() {
        return minimumValue;
    }

    /**
     * Sets the lowest vertical extent contained in the dataset.
     *
     * @param newValue The new minimum value.
     */
    public void setMinimumValue(final Double newValue) {
        checkWritePermission();
        minimumValue = newValue;
    }

    /**
     * Returns the highest vertical extent contained in the dataset.
     *
     * @return The highest vertical extent, or {@code null}.
     */
    @Override
    @XmlElement(name = "maximumValue", required = true)
    @XmlJavaTypeAdapter(GO_Real.class)
    public Double getMaximumValue() {
        return maximumValue;
    }

    /**
     * Sets the highest vertical extent contained in the dataset.
     *
     * @param newValue The new maximum value.
     */
    public void setMaximumValue(final Double newValue) {
        checkWritePermission();
        maximumValue = newValue;
    }

    /**
     * Provides information about the vertical coordinate reference system to
     * which the maximum and minimum elevation values are measured.
     * The CRS identification includes unit of measure.
     *
     * @return The vertical CRS, or {@code null}.
     */
    @Override
    @XmlElement(name = "verticalCRS", required = true)
    public VerticalCRS getVerticalCRS() {
        return verticalCRS;
    }

    /**
     * Sets the information about the vertical coordinate reference system to
     * which the maximum and minimum elevation values are measured.
     *
     * @param newValue The new vertical CRS.
     */
    public void setVerticalCRS(final VerticalCRS newValue) {
        checkWritePermission();
        verticalCRS = newValue;
    }

    /**
     * Sets this vertical extent to values inferred from the specified envelope. The envelope can
     * be multi-dimensional, in which case the {@linkplain Envelope#getCoordinateReferenceSystem()
     * envelope CRS} must have a vertical component.
     *
     * <p><b>Note:</b> this method is available only if the referencing module is on the classpath.</p>
     *
     * @param  envelope The envelope to use for setting this vertical extent.
     * @throws UnsupportedOperationException if the referencing module is not on the classpath.
     * @throws TransformException if the envelope can not be transformed to a vertical extent.
     *
     * @see DefaultExtent#addElements(Envelope)
     * @see DefaultGeographicBoundingBox#setBounds(Envelope)
     * @see DefaultTemporalExtent#setBounds(Envelope)
     */
    public void setBounds(final Envelope envelope) throws TransformException {
        checkWritePermission();
        ReferencingServices.getInstance().setBounds(envelope, this);
    }
}
