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
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;


/**
 * A coordinate reference system that is defined by its coordinate
 * {@linkplain org.apache.sis.referencing.operation.DefaultConversion conversion} from another CRS
 * (not by a {@linkplain org.apache.sis.referencing.datum.AbstractDatum datum}). {@code DerivedCRS}
 * can not be {@linkplain DefaultProjectedCRS projected CRS} themselves, but may be derived from a projected CRS
 * (for example in order to use a {@linkplain org.apache.sis.referencing.cs.DefaultPolarCS polar coordinate system}).
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code GeneralDerivedCRS} instances created
 * using only SIS factories and static constants can be shared by many objects and passed between threads without
 * synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlTransient   // TODO: GML not yet investigated
public class DefaultDerivedCRS extends AbstractDerivedCRS implements DerivedCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8149602276542469876L;

    /**
     * Constructs a new object in which every attributes are set to a default value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultDerivedCRS() {
    }

    /**
     * Creates a derived CRS from a defining conversion.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractCRS#AbstractCRS(Map, CoordinateSystem) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties The properties to be given to the new derived CRS object.
     * @param  baseCRS Coordinate reference system to base the derived CRS on.
     * @param  conversionFromBase The conversion from the base CRS to this derived CRS.
     * @param  derivedCS The coordinate system for the derived CRS. The number of axes
     *         must match the target dimension of the {@code baseToDerived} transform.
     * @throws MismatchedDimensionException if the source and target dimension of {@code baseToDerived}
     *         do not match the dimension of {@code base} and {@code derivedCS} respectively.
     */
    public DefaultDerivedCRS(final Map<String,?>    properties,
                             final SingleCRS        baseCRS,
                             final Conversion       conversionFromBase,
                             final CoordinateSystem derivedCS)
            throws MismatchedDimensionException
    {
        super(properties, baseCRS, conversionFromBase, derivedCS, Conversion.class);
    }

    /**
     * Constructs a new coordinate reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param crs The coordinate reference system to copy.
     *
     * @see #castOrCopy(DerivedCRS)
     */
    protected DefaultDerivedCRS(final DerivedCRS crs) {
        super(crs);
    }

    /**
     * Returns a SIS coordinate reference system implementation with the same values than the given
     * arbitrary implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDerivedCRS castOrCopy(final DerivedCRS object) {
        return (object == null) || (object instanceof DefaultDerivedCRS)
                ? (DefaultDerivedCRS) object : new DefaultDerivedCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code DerivedCRS.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code DerivedCRS}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with
     * their own set of interfaces.</div>
     *
     * @return {@code DerivedCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends DerivedCRS> getInterface() {
        return DerivedCRS.class;
    }

    /**
     * Returns the datum of the {@linkplain #getBaseCRS() base CRS}.
     *
     * @return The datum of the base CRS.
     */
    @Override
    public Datum getDatum() {
        return super.getDatum();
    }

    /**
     * Returns the {@linkplain org.apache.sis.referencing.operation.DefaultConversion#getSourceCRS() source}
     * of the {@linkplain #getConversionFromBase() conversion from base}.
     *
     * @return The base coordinate reference system.
     */
    @Override
    public SingleCRS getBaseCRS() {
        return super.getBaseCRS();
    }

    /**
     * Returns the conversion from the {@linkplain #getBaseCRS() base CRS} to this CRS.
     * In Apache SIS, the conversion source and target CRS are set to the following values:
     *
     * <ul>
     *   <li>The conversion {@linkplain org.apache.sis.referencing.operation.DefaultConversion#getSourceCRS()
     *       source CRS} defines the {@linkplain #getBaseCRS() base CRS} of {@code this} CRS.</li>
     *   <li>The conversion {@linkplain org.apache.sis.referencing.operation.DefaultConversion#getTargetCRS()
     *       target CRS} is {@code this} CRS.
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * This is different than ISO 19111, which allows source and target CRS to be {@code null}.</div>
     *
     * @return The conversion to this CRS.
     */
    @Override
    public Conversion getConversionFromBase() {
        return super.getConversionFromBase();
    }

    /**
     * Formats the inner part of the <cite>Well Known Text</cite> (WKT) representation of this CRS.
     *
     * @return {@code "Fitted_CS"} (WKT 1) or a type-dependent keyword (WKT 2).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendName(this, formatter, null);
        final boolean isWKT1 = (formatter.getConvention().majorVersion() == 1);
        final Conversion conversionFromBase = getConversionFromBase();  // Gives to users a chance to override.
        /*
         * Both WKT 1 and WKT 2 format the base CRS. But WKT 1 formats the MathTransform before the base CRS,
         * while WKT 2 formats the conversion method and parameter values after the base CRS.
         */
        if (isWKT1) {
            MathTransform inverse = conversionFromBase.getMathTransform();
            try {
                inverse = inverse.inverse();
            } catch (NoninvertibleTransformException exception) {
                formatter.setInvalidWKT(this, exception);
                inverse = null;
            }
            formatter.newLine();
            formatter.append(inverse);
        }
        formatter.newLine();
        formatter.append(WKTUtilities.toFormattable(getBaseCRS()));
        if (isWKT1) {
            return "Fitted_CS";
        } else {
            formatter.append(new Parameters(this));    // Format inside a "DefiningConversion" element.
            if (!isBaseCRS(formatter)) {
                formatCS(formatter, getCoordinateSystem(), isWKT1);
            }
            return "EngineeringCRS"; // TODO: may be GeodeticCRS, VerticalCRS, etc.
        }
    }

    /**
     * Temporary object for formatting the conversion method and parameters inside a
     * a {@code DerivingConversion} element. This is used in WKT 2 formatting only.
     */
    private static final class Parameters extends FormattableObject {
        /** The conversion which specify the operation method and parameters. */
        private final Conversion conversion;

        /** Creates a new temporary {@code DerivingConversion} elements for the parameters of the given CRS. */
        Parameters(final AbstractDerivedCRS crs) {
            conversion = crs.getConversionFromBase();
        }

        /** Formats this {@code Conversion} element. */
        @Override protected String formatTo(final Formatter formatter) {
            WKTUtilities.appendName(conversion, formatter, null);
            formatter.newLine();
            formatter.append(DefaultOperationMethod.castOrCopy(conversion.getMethod()));
            formatter.newLine();
            for (final GeneralParameterValue param : conversion.getParameterValues().values()) {
                WKTUtilities.append(param, formatter);
            }
            return "DerivingConversion";
        }
    }
}
