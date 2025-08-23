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
package org.apache.sis.referencing.crs;

import java.util.Map;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.GeodeticException;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.privy.ReferencingFactoryContainer;
import org.apache.sis.xml.bind.referencing.CC_Conversion;
import org.apache.sis.metadata.privy.ImplementationHelper;
import org.apache.sis.metadata.privy.Identifiers;
import org.apache.sis.system.Semaphores;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;

// Specific to the geoapi-4.0 branch:
import org.opengis.referencing.crs.DerivedCRS;


/**
 * A coordinate reference system that is defined by its coordinate conversion from another CRS.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@XmlType(name = "AbstractGeneralDerivedCRSType")
@XmlRootElement(name = "AbstractGeneralDerivedCRS")
@XmlSeeAlso({
    DefaultDerivedCRS.class,
    DefaultProjectedCRS.class
})
abstract class AbstractDerivedCRS extends AbstractCRS implements DerivedCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -175151161496419854L;

    /**
     * The conversion from the {@linkplain #getBaseCRS() base CRS} to this CRS.
     * The base CRS of this {@code DerivedCRS} is {@link Conversion#getSourceCRS()}.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setConversionFromBase(Conversion)}</p>
     *
     * @see #getConversionFromBase()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private Conversion conversionFromBase;

    /**
     * Creates a derived CRS from a defining conversion.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractCRS#AbstractCRS(Map, CoordinateSystem) super-class constructor}.
     *
     * @param  properties     the properties to be given to the new derived CRS object.
     * @param  baseCRS        coordinate reference system to base the derived CRS on.
     * @param  baseToDerived  the defining conversion from a normalized base to a normalized derived CRS.
     * @param  derivedCS      the coordinate system for the derived CRS. The number of axes must match
     *                        the target dimension of the {@code baseToDerived} transform.
     * @throws MismatchedDimensionException if the source and target dimensions of {@code baseToDerived}
     *         do not match the dimensions of {@code base} and {@code derivedCS} respectively.
     */
    AbstractDerivedCRS(final Map<String,?>    properties,
                       final SingleCRS        baseCRS,
                       final Conversion       baseToDerived,
                       final CoordinateSystem derivedCS)
            throws MismatchedDimensionException
    {
        super(properties, derivedCS);
        ArgumentChecks.ensureNonNull("baseCRS", baseCRS);
        ArgumentChecks.ensureNonNull("baseToDerived", baseToDerived);
        ArgumentChecks.ensureDimensionsMatch("baseToDerived",
                baseCRS.getCoordinateSystem().getDimension(),
                derivedCS.getDimension(),
                baseToDerived.getMathTransform());
        conversionFromBase = createConversionFromBase(properties, baseCRS, baseToDerived);
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     */
    AbstractDerivedCRS(final AbstractDerivedCRS original, final AbstractCS derivedCS) {
        super(original, null, derivedCS);
        final Conversion conversion = original.conversionFromBase;
        conversionFromBase = createConversionFromBase(null, (SingleCRS) conversion.getSourceCRS(), conversion);
    }

    /**
     * For {@link DefaultDerivedCRS#DefaultDerivedCRS(Map, SingleCRS, CoordinateReferenceSystem, OperationMethod,
     * MathTransform, CoordinateSystem)} constructor only (<strong>not legal for {@code ProjectedCRS}</strong>).
     */
    @SuppressWarnings("unchecked")
    AbstractDerivedCRS(final Map<String,?>             properties,
                       final SingleCRS                 baseCRS,
                       final CoordinateReferenceSystem interpolationCRS,
                       final OperationMethod           method,
                       final MathTransform             baseToDerived,
                       final CoordinateSystem          derivedCS)
            throws MismatchedDimensionException
    {
        super(properties, derivedCS);
        ArgumentChecks.ensureNonNull("baseCRS", baseCRS);
        ArgumentChecks.ensureNonNull("method", method);
        ArgumentChecks.ensureNonNull("baseToDerived", baseToDerived);
        conversionFromBase = new DefaultConversion(ConversionKeys.unprefix(properties),
                baseCRS, this, interpolationCRS, method, baseToDerived);
    }

    /**
     * Constructs a new coordinate reference system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  crs  the coordinate reference system to copy.
     */
    AbstractDerivedCRS(final DerivedCRS crs) {
        super(crs);
        conversionFromBase = createConversionFromBase(null, crs.getBaseCRS(), crs.getConversionFromBase());
    }

    /**
     * Creates the conversion instance to associate with this {@code AbstractDerivedCRS}.
     *
     * <p><b>WARNING:</b> this method is invoked at construction time and will invoke indirectly
     * (through {@link DefaultConversion}) the {@link #getCoordinateSystem()} method on {@code this}.
     * Consequently, this method shall be invoked only after the construction of this {@code AbstractDerivedCRS}
     * instance is advanced enough for allowing the {@code getCoordinateSystem()} method to execute.
     * Subclasses should make their {@code getCoordinateSystem()} method final for better guarantees.</p>
     */
    private Conversion createConversionFromBase(final Map<String,?> properties, final SingleCRS baseCRS, final Conversion conversion) {
        MathTransformFactory factory = null;
        if (properties != null) {
            factory = (MathTransformFactory) properties.get(ReferencingFactoryContainer.MT_FACTORY);
        }
        try {
            return DefaultConversion.castOrCopy(conversion).specialize(baseCRS, this, factory);
        } catch (FactoryException e) {
            throw new IllegalArgumentException(Errors.forProperties(properties).getString(
                    Errors.Keys.IllegalArgumentValue_2, "conversion", conversion.getName()), e);
        }
    }

    /**
     * Returns the conversion from the base CRS to this CRS.
     *
     * @return the conversion to this CRS.
     */
    @Override
    @XmlElement(name = "conversion", required = true)
    public Conversion getConversionFromBase() {
        return conversionFromBase;
    }

    /**
     * Compares this coordinate reference system with the specified object for equality.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (super.equals(object, mode)) {
            final boolean strict = (mode == ComparisonMode.STRICT);
            /*
             * Avoid never-ending recursion: Conversion has a `targetCRS` field (inherited from
             * the AbstractCoordinateOperation super-class) that is set to this AbstractDerivedCRS.
             *
             * Do NOT compare the baseCRS explicitly. This is done implicitely in the comparison of the Conversion
             * objects, since (this.baseCRS == Conversion.sourceCRS) in Apache SIS.  The reason why we delegate the
             * comparison of that CRS to the Conversion object is because we want to ignore the baseCRS axes if the
             * mode said to ignore metadata, but ignoring axis order and units has implication on the MathTransform
             * instances to compare.  The AbstractCoordinateOperation.equals(…) method implementation handles those
             * cases.
             */
            if (Semaphores.queryAndSet(Semaphores.CONVERSION_AND_CRS)) {
                return true;
            } else try {
                return Utilities.deepEquals(
                        strict ? conversionFromBase : getConversionFromBase(),
                        strict ? ((AbstractDerivedCRS) object).conversionFromBase
                               :  ((DerivedCRS) object).getConversionFromBase(), mode);
            } finally {
                Semaphores.clear(Semaphores.CONVERSION_AND_CRS);
            }
        }
        return false;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        /*
         * Do not invoke `conversionFromBase.hashCode()` in order to avoid a never-ending loop.
         * This is because Conversion inherits a `sourceCRS` field from the CoordinateOperation
         * parent type, which is set to this DerivedCRS. Checking the OperationMethod does not
         * work neither for the reason documented inside the AbstractSingleOperation.equals(…)
         * method body. The MathTransform is our best discriminant.
         */
        return super.computeHashCode()
               + 31 * conversionFromBase.getSourceCRS().hashCode()
               + 37 * conversionFromBase.getMathTransform().hashCode();
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    AbstractDerivedCRS() {
    }

    /**
     * Invoked by JAXB at unmarshalling time for setting the <i>defining conversion</i>.
     * At this state, the given conversion has null {@code sourceCRS} and {@code targetCRS}.
     * Those CRS will be set later, in {@link #afterUnmarshal(Unmarshaller, Object)}.
     */
    private void setConversionFromBase(final Conversion conversion) {
        if (conversionFromBase == null) {
            conversionFromBase = conversion;
        } else {
            ImplementationHelper.propertyAlreadySet(AbstractDerivedCRS.class, "setConversionFromBase", "conversion");
        }
    }

    /**
     * Stores a temporary association to the given base CRS.  This method shall be invoked only
     * <strong>after</strong> the call to {@link #setConversionFromBase(Conversion)}, otherwise
     * an exception will be thrown.
     *
     * <p>The given base CRS will not be stored in this {@code AbstractDerivedCRS} instance now,
     * but in another temporary location. The reason is that we need the coordinate system (CS)
     * before we can set the {@code baseCRS} in its final location, but the CS is not yet known
     * when this method is invoked.</p>
     *
     * @param  name  the property name, used only in case of error message to format.
     * @throws IllegalStateException if the base CRS cannot be set.
     */
    @SuppressWarnings("unchecked")
    final void setBaseCRS(final String name, final SingleCRS baseCRS) {
        if (conversionFromBase != null) {
            final SingleCRS previous = CC_Conversion.setBaseCRS(conversionFromBase, baseCRS);
            if (previous != null) {
                CC_Conversion.setBaseCRS(conversionFromBase, previous);   // Temporary location.
                ImplementationHelper.propertyAlreadySet(AbstractDerivedCRS.class, "setBaseCRS", name);
            }
        } else {
            throw new IllegalStateException(Errors.format(Errors.Keys.MissingComponentInElement_2, getInterface(), "conversion"));
        }
    }

    /**
     * Invoked by JAXB after all elements have been unmarshalled. At this point we should have the
     * coordinate system (CS). The CS information is required by {@code createConversionFromBase(…)}
     * in order to create a {@link MathTransform} with correct axis swapping and unit conversions.
     */
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        String property = "conversion";
        if (conversionFromBase != null) {
            final SingleCRS baseCRS = CC_Conversion.setBaseCRS(conversionFromBase, null);  // Clear the temporary value now.
            property = "coordinateSystem";
            if (super.getCoordinateSystem() != null) {
                property = "baseCRS";
                if (baseCRS != null) {
                    conversionFromBase = createConversionFromBase(null, baseCRS, conversionFromBase);
                    return;
                }
            }
        }
        /*
         * If we reach this point, we failed to update the conversion. The `baseCRS` information will be lost
         * and call to `getConversionFromBase()` will throw a ClassCastException if this instance is actually
         * a ProjectedCRS (because of the method overriding with return type covariance).
         */
        throw new GeodeticException(Identifiers.missingValueForProperty(getName(), property));
    }
}
