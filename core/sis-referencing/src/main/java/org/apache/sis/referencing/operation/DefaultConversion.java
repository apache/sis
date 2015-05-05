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
package org.apache.sis.referencing.operation;

import java.util.Map;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;


/**
 * A parameterized mathematical operation that converts coordinates to another CRS without any change of
 * {@linkplain org.apache.sis.referencing.datum.AbstractDatum datum}.
 * The best-known example of a coordinate conversion is a map projection.
 * The parameters describing coordinate conversions are defined rather than empirically derived.
 *
 * <p>This coordinate operation contains an {@linkplain DefaultOperationMethod operation method}, usually
 * with associated parameter values. In the SIS default implementation, the parameter values are inferred from the
 * {@linkplain #getMathTransform() math transform}. Subclasses may have to override the {@link #getParameterValues()}
 * method if they need to provide a different set of parameters.</p>
 *
 * <div class="section">Defining conversions</div>
 * {@code OperationMethod} instances are generally created for a pair of existing {@linkplain #getSourceCRS() source}
 * and {@linkplain #getTargetCRS() target CRS}. But {@code Conversion} instances without those information may exist
 * temporarily while creating a {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS derived} or
 * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}.
 * Those <cite>defining conversions</cite> have no source and target CRS since those elements are provided by the
 * derived or projected CRS themselves. This class provides a {@linkplain #DefaultConversion(Map, OperationMethod,
 * MathTransform) constructor} for such defining conversions.
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code CoordinateOperation} instances created
 * using only SIS factories and static constants can be shared by many objects and passed between threads without
 * synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see DefaultTransformation
 */
@XmlType(name = "ConversionType")
@XmlRootElement(name = "Conversion")
public class DefaultConversion extends AbstractSingleOperation implements Conversion {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2148164324805562793L;

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultConversion() {
    }

    /**
     * Creates a coordinate conversion from the given properties.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractCoordinateOperation#AbstractCoordinateOperation(Map, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransform) super-class constructor}.
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
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the identified object.
     * @param sourceCRS  The source CRS.
     * @param targetCRS  The target CRS.
     * @param interpolationCRS The CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param method     The coordinate operation method (mandatory in all cases).
     * @param transform  Transform from positions in the source CRS to positions in the target CRS.
     */
    public DefaultConversion(final Map<String,?>             properties,
                             final CoordinateReferenceSystem sourceCRS,
                             final CoordinateReferenceSystem targetCRS,
                             final CoordinateReferenceSystem interpolationCRS,
                             final OperationMethod           method,
                             final MathTransform             transform)
    {
        super(properties, sourceCRS, targetCRS, interpolationCRS, method, transform);
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
    }

    /**
     * Creates a defining conversion from the given transform.
     * This conversion has no source and target CRS since those elements will be provided by the
     * {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS derived} or
     * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}.
     *
     * <p>The properties given in argument follow the same rules than for the
     * {@linkplain #DefaultConversion(Map, CoordinateReferenceSystem, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, OperationMethod, MathTransform) above constructor}.</p>
     *
     * @param properties The properties to be given to the identified object.
     * @param method     The operation method.
     * @param transform  Transform from positions in the source CRS to positions in the target CRS.
     */
    public DefaultConversion(final Map<String,?>   properties,
                             final OperationMethod method,
                             final MathTransform   transform)
    {
        super(properties, null, null, null, method, transform);
    }

    /**
     * Constructs a new conversion with the same values than the specified one, together with the
     * specified source and target CRS. While the source conversion can be an arbitrary one, it is
     * typically a defining conversion.
     *
     * @param definition The defining conversion.
     * @param sourceCRS  The source CRS.
     * @param targetCRS  The target CRS.
     */
    DefaultConversion(final Conversion                definition,
                      final CoordinateReferenceSystem sourceCRS,
                      final CoordinateReferenceSystem targetCRS)
    {
        super(definition, sourceCRS, targetCRS);
    }

    /**
     * Creates a new coordinate operation with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param operation The coordinate operation to copy.
     *
     * @see #castOrCopy(Conversion)
     */
    protected DefaultConversion(final Conversion operation) {
        super(operation);
    }

    /**
     * Returns a SIS coordinate operation implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of
     *       {@link org.opengis.referencing.operation.Conversion},
     *       {@link org.opengis.referencing.operation.Projection},
     *       {@link org.opengis.referencing.operation.CylindricalProjection},
     *       {@link org.opengis.referencing.operation.ConicProjection} or
     *       {@link org.opengis.referencing.operation.PlanarProjection},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultConversion}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultConversion} instance is created using the
     *       {@linkplain #DefaultConversion(Conversion) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultConversion castOrCopy(final Conversion object) {
        return SubTypes.forConversion(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code Conversion.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return The conversion interface implemented by this class.
     */
    @Override
    public Class<? extends Conversion> getInterface() {
        return Conversion.class;
    }

    /**
     * Returns a specialization of this conversion with a more specific type, source and target CRS.
     * This {@code specialize(…)} method is typically invoked on {@linkplain #DefaultConversion(Map,
     * OperationMethod, MathTransform) defining conversion} instances, when more information become
     * available about the conversion to create.
     *
     * <p>The given {@code baseType} argument can be one of the following values:</p>
     * <ul>
     *   <li><code>{@linkplain org.opengis.referencing.operation.Conversion}.class</code></li>
     *   <li><code>{@linkplain org.opengis.referencing.operation.Projection}.class</code></li>
     *   <li><code>{@linkplain org.opengis.referencing.operation.CylindricalProjection}.class</code></li>
     *   <li><code>{@linkplain org.opengis.referencing.operation.ConicProjection}.class</code></li>
     *   <li><code>{@linkplain org.opengis.referencing.operation.PlanarProjection}.class</code></li>
     * </ul>
     *
     * This {@code specialize(…)} method returns a conversion which implement at least the given {@code baseType}
     * interface, but may also implement a more specific GeoAPI interface if {@code specialize(…)} has been able
     * to infer the type from this operation {@linkplain #getMethod() method}.
     *
     * @param  <T>        Compile-time type of the {@code baseType} argument.
     * @param  baseType   The base GeoAPI interface to be implemented by the conversion to return.
     * @param  sourceCRS  The source CRS.
     * @param  targetCRS  The target CRS.
     * @return The conversion of the given type between the given CRS.
     * @throws ClassCastException if a contradiction is found between the given {@code baseType},
     *         the defining {@linkplain DefaultConversion#getInterface() conversion type} and
     *         the {@linkplain DefaultOperationMethod#getOperationType() method operation type}.
     */
    public <T extends Conversion> T specialize(final Class<T> baseType,
            final CoordinateReferenceSystem sourceCRS, final CoordinateReferenceSystem targetCRS)
    {
        ArgumentChecks.ensureNonNull("baseType",  baseType);
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        return SubTypes.create(baseType, this, sourceCRS, targetCRS);
    }
}
