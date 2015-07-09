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
package org.apache.sis.metadata.iso.identification;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.annotation.UML;
import org.opengis.util.InternationalString;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.opengis.metadata.identification.Resolution;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.GO_Distance;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.util.resources.Messages;

import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;

// Branch-specific imports
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Level of detail expressed as a scale factor or a ground distance.
 *
 * <div class="section">Relationship between properties</div>
 * ISO 19115 defines {@code Resolution} as an <cite>union</cite> (in the C/C++ sense):
 * only one of the properties in this class can be set to a non-empty value.
 * Setting any property to a non-empty value discard all the other ones.
 * See the {@linkplain #DefaultResolution(Resolution) constructor javadoc}
 * for information about which property has precedence on copy operations.
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
 * @version 0.6
 * @module
 *
 * @see AbstractIdentification#getSpatialResolutions()
 */
@XmlType(name = "MD_Resolution_Type") // No need for propOrder since this structure is a union (see javadoc).
@XmlRootElement(name = "MD_Resolution")
public class DefaultResolution extends ISOMetadata implements Resolution {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 4333582736458380544L;

    /**
     * Enumeration of possible values for {@link #property}.
     */
    private static final byte SCALE=1, DISTANCE=2, VERTICAL=3, ANGULAR=4, TEXT=5;

    /**
     * The names of the mutually exclusive properties.
     * The index of each name shall be the value of the above {@code byte} constants minus one.
     */
    private static final String[] NAMES = {
        "equivalentScale",
        "distance",
        "vertical",
        "angularDistance",
        "levelOfDetail"
    };

    /**
     * The names of the setter methods, for logging purpose only.
     */
    private static final String[] SETTERS = {
        "setEquivalentScale",
        "setDistance",
        "setVertical",
        "setAngularDistance",
        "setLevelOfDetail"
    };

    /**
     * Specifies which property is set, or 0 if none.
     */
    private byte property;

    /**
     * Either the scale as a {@link RepresentativeFraction} instance, the distance, the angle,
     * or the level of details as an {@link InternationalString} instance.
     */
    private Object value;

    /**
     * Constructs an initially empty resolution.
     */
    public DefaultResolution() {
    }

    /**
     * Creates a new resolution initialized to the given scale.
     *
     * @param scale The scale, or {@code null} if none.
     *
     * @since 0.4
     */
    public DefaultResolution(final RepresentativeFraction scale) {
        if (scale != null) {
            value = scale;
            property = SCALE;
        }
    }

    // Note: there is not yet DefaultResolution(double) method because
    //       we need to update the Unit Of Measurement package first.

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * <p>If more than one of the {@linkplain #getEquivalentScale() equivalent scale},
     * {@linkplain #getDistance() distance}, {@linkplain #getVertical() vertical},
     * {@linkplain #getAngularDistance() angular distance} and {@linkplain #getLevelOfDetail() level of detail}
     * are specified, then the first of those values is taken and the other values are silently discarded.</p>
     *
     * <div class="note"><b>Note on properties validation:</b>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     * </div>
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Resolution)
     */
    public DefaultResolution(final Resolution object) {
        super(object);
        if (object != null) {
            for (byte p=SCALE; p<=TEXT; p++) {
                Object c = null;
                switch (p) {
                    case SCALE:    c = object.getEquivalentScale(); break;
                    case DISTANCE: c = object.getDistance(); break;
                    case VERTICAL: if (c instanceof DefaultResolution) c = ((DefaultResolution) object).getVertical(); break;
                    case ANGULAR:  if (c instanceof DefaultResolution) c = ((DefaultResolution) object).getAngularDistance(); break;
                    case TEXT:     if (c instanceof DefaultResolution) c = ((DefaultResolution) object).getLevelOfDetail(); break;
                    default:       throw new AssertionError(p);
                }
                if (c != null) {
                    property = p;
                    value = c;
                    break;
                }
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultResolution}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultResolution} instance is created using the
     *       {@linkplain #DefaultResolution(Resolution) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultResolution castOrCopy(final Resolution object) {
        if (object == null || object instanceof DefaultResolution) {
            return (DefaultResolution) object;
        }
        return new DefaultResolution(object);
    }

    /**
     * Sets the properties identified by the {@code code} argument, if non-null.
     * This discards any other properties.
     *
     * @param code     The property which is going to be set.
     * @param newValue The new value.
     */
    private void setProperty(final byte code, final Object newValue) {
        checkWritePermission();
        if (value != null && property != code) {
            if (newValue == null) {
                return; // Do not erase the other property.
            }
            Context.warningOccured(Context.current(), DefaultResolution.class, SETTERS[code-1],
                    Messages.class, Messages.Keys.DiscardedExclusiveProperty_2, NAMES[property-1], NAMES[code-1]);
        }
        value = newValue;
        property = code;
    }

    /**
     * Returns the level of detail expressed as the scale of a comparable hardcopy map or chart.
     *
     * @return Level of detail expressed as the scale of a comparable hardcopy, or {@code null}.
     */
    @Override
    @XmlElement(name = "equivalentScale")
    public RepresentativeFraction getEquivalentScale()  {
        return (property == SCALE) ? (RepresentativeFraction) value : null;
    }

    /**
     * Sets the level of detail expressed as the scale of a comparable hardcopy map or chart.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards all other properties.
     *
     * @param newValue The new equivalent scale.
     */
    public void setEquivalentScale(final RepresentativeFraction newValue) {
        setProperty(SCALE, newValue);
    }

    /**
     * Returns the ground sample distance.
     *
     * @return The ground sample distance, or {@code null}.
     */
    @Override
    @XmlElement(name = "distance")
    @XmlJavaTypeAdapter(GO_Distance.class)
    @ValueRange(minimum=0, isMinIncluded=false)
    public Double getDistance() {
        return (property == DISTANCE) ? (Double) value : null;
    }

    /**
     * Sets the ground sample distance.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards all other properties.
     *
     * @param newValue The new distance, or {@code null}.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     */
    public void setDistance(final Double newValue) {
        if (ensurePositive(DefaultResolution.class, "distance", true, newValue)) {
            setProperty(DISTANCE, newValue);
        }
    }

    /**
     * Returns the vertical sampling distance.
     *
     * @return The vertical sampling distance, or {@code null}.
     *
     * @since 0.5
     */
    @UML(identifier="vertical", obligation=CONDITIONAL, specification=ISO_19115)
    @ValueRange(minimum=0, isMinIncluded=false)
    public Double getVertical() {
        return (property == VERTICAL) ? (Double) value : null;
    }

    /**
     * Sets the vertical sampling distance.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards all other properties.
     *
     * @param newValue The new distance, or {@code null}.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     *
     * @since 0.5
     */
    public void setVertical(final Double newValue) {
        if (ensurePositive(DefaultResolution.class, "vertical", true, newValue)) {
            setProperty(VERTICAL, newValue);
        }
    }

    /**
     * Returns the angular sampling measure.
     *
     * @return The angular sampling measure, or {@code null}.
     *
     * @since 0.5
     */
    @UML(identifier="angularDistance", obligation=CONDITIONAL, specification=ISO_19115)
    @ValueRange(minimum=0, isMinIncluded=false)
    public Double getAngularDistance() {
        return (property == ANGULAR) ? (Double) value : null;
    }

    /**
     * Sets the angular sampling measure.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards all other properties.
     *
     * @param newValue The new distance, or {@code null}.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     *
     * @since 0.5
     */
    public void setAngularDistance(final Double newValue) {
        if (ensurePositive(DefaultResolution.class, "angular", true, newValue)) {
            setProperty(ANGULAR, newValue);
        }
    }

    /**
     * Returns a brief textual description of the spatial resolution of the resource.
     *
     * @return Textual description of the spatial resolution, or {@code null}.
     *
     * @since 0.5
     */
    @UML(identifier="levelOfDetail", obligation=CONDITIONAL, specification=ISO_19115)
    public InternationalString getLevelOfDetail() {
        return (property == TEXT) ? (InternationalString) value : null;
    }

    /**
     * Sets the textual description of the spatial resolution of the resource.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards all other properties.
     *
     * @param newValue The new distance.
     *
     * @since 0.5
     */
    public void setLevelOfDetail(final InternationalString newValue) {
        setProperty(TEXT, newValue);
    }
}
