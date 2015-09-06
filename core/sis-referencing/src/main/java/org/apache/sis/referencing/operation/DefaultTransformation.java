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
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;


/**
 * A parameterized mathematical operation that transforms coordinates to another CRS with a change of
 * {@linkplain org.apache.sis.referencing.datum.AbstractDatum datum}.
 * The parameters of a coordinate transformation are empirically derived from data containing the coordinates of a
 * series of points in both coordinate reference systems. This computational process is usually "over-determined",
 * allowing derivation of error (or {@linkplain #getCoordinateOperationAccuracy() accuracy}) estimates for the
 * transformation. Also, the stochastic nature of the parameters may result in multiple (different)
 * {@linkplain #getOperationVersion() versions} of the same coordinate transformation.
 *
 * <p>This coordinate operation contains an {@linkplain DefaultOperationMethod operation method}, usually
 * with associated parameter values. In the SIS default implementation, the parameter values are inferred from the
 * {@linkplain #getMathTransform() math transform}. Subclasses may have to override the {@link #getParameterValues()}
 * method if they need to provide a different set of parameters.</p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. This means that unless otherwise noted in the javadoc,
 * {@code Transformation} instances created using only SIS factories and static constants can be shared
 * by many objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see DefaultConversion
 */
@XmlType(name = "TransformationType")
@XmlRootElement(name = "Transformation")
public class DefaultTransformation extends AbstractSingleOperation implements Transformation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7486704846151648971L;

    /**
     * Creates a coordinate transformation from the given properties.
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
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#COORDINATE_OPERATION_ACCURACY_KEY}</td>
     *     <td>{@link org.opengis.metadata.quality.PositionalAccuracy} (optionally as array)</td>
     *     <td>{@link #getCoordinateOperationAccuracy()}</td>
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
    public DefaultTransformation(final Map<String,?>             properties,
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
     * Creates a new coordinate operation with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param operation The coordinate operation to copy.
     *
     * @see #castOrCopy(Transformation)
     */
    protected DefaultTransformation(final Transformation operation) {
        super(operation);
    }

    /**
     * Returns a SIS coordinate operation implementation with the values of the given arbitrary implementation.
     * If the given object is already an instance of {@code DefaultTransformation}, then it is returned unchanged.
     * Otherwise a new {@code DefaultTransformation} instance is created using the
     * {@linkplain #DefaultTransformation(Transformation) copy constructor} and returned.
     * Note that this is a <cite>shallow</cite> copy operation, since the other properties contained in the given
     * object are not recursively copied.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultTransformation castOrCopy(final Transformation object) {
        return (object == null) || (object instanceof DefaultTransformation)
                ? (DefaultTransformation) object : new DefaultTransformation(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code Transformation.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code Transformation}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code Transformation.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends Transformation> getInterface() {
        return Transformation.class;
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultTransformation() {
    }
}
