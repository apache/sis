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
import javax.measure.converter.ConversionException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Datum;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.Utilities;


/**
 * A parameterized mathematical operation that converts coordinates to another CRS without any change of
 * {@linkplain org.apache.sis.referencing.datum.AbstractDatum datum}.
 * The best-known example of a coordinate conversion is a map projection.
 * The parameters describing coordinate conversions are defined rather than empirically derived.
 *
 * <p>This coordinate operation contains an {@linkplain DefaultOperationMethod operation method}, usually
 * with associated {@linkplain org.apache.sis.parameter.DefaultParameterValueGroup parameter values}.
 * In the SIS implementation, the parameter values can be either inferred from the
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform math transform}
 * or explicitely provided at construction time in a <cite>defining conversion</cite> (see below).</p>
 *
 * <div class="section">Defining conversions</div>
 * {@code OperationMethod} instances are generally created for a pair of existing {@linkplain #getSourceCRS() source}
 * and {@linkplain #getTargetCRS() target CRS}. But {@code Conversion} instances without those information may exist
 * temporarily while creating a {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS derived} or
 * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}.
 * Those <cite>defining conversions</cite> have no source and target CRS since those elements are provided by the
 * derived or projected CRS themselves. This class provides a {@linkplain #DefaultConversion(Map, OperationMethod,
 * MathTransform, ParameterValueGroup) constructor} for such defining conversions.
 *
 * <p>After the source and target CRS become known, we can invoke the {@link #specialize specialize(…)} method for
 * {@linkplain DefaultMathTransformFactory#createBaseToDerived(CoordinateReferenceSystem, ParameterValueGroup,
 * CoordinateSystem) creating a math transform from the parameters}, instantiate a new {@code Conversion} of a
 * more specific type
 * ({@link org.opengis.referencing.operation.ConicProjection},
 *  {@link org.opengis.referencing.operation.CylindricalProjection} or
 *  {@link org.opengis.referencing.operation.PlanarProjection}) if possible,
 * and assign the source and target CRS to it.</p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. This means that unless otherwise noted in the javadoc,
 * {@code Conversion} instances created using only SIS factories and static constants can be shared
 * by many objects and passed between threads without synchronization.
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
     * Creates a defining conversion from the given transform and/or parameters.
     * This conversion has no source and target CRS since those elements are usually unknown
     * at <cite>defining conversion</cite> construction time. The source and target CRS will
     * become known later, at the
     * {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS Derived CRS} or
     * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected CRS}
     * construction time.
     *
     * <p>The {@code properties} map given in argument follows the same rules than for the
     * {@linkplain #DefaultConversion(Map, CoordinateReferenceSystem, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, OperationMethod, MathTransform) above constructor}.</p>
     *
     * <div class="section">Transform and parameters arguments</div>
     * At least one of the {@code transform} or {@code parameters} argument must be non-null.
     * If the caller supplies a {@code transform} argument, then it shall be a transform expecting
     * {@linkplain org.apache.sis.referencing.cs.AxesConvention#NORMALIZED normalized} input coordinates
     * and producing normalized output coordinates. See {@link org.apache.sis.referencing.cs.AxesConvention}
     * for more information about what Apache SIS means by "normalized".
     *
     * <p>If the caller can not yet supply a {@code MathTransform}, then (s)he shall supply the parameter values needed
     * for creating that transform, with the possible omission of {@code "semi_major"} and {@code "semi_minor"} values.
     * The semi-major and semi-minor parameter values will be set automatically when the
     * {@link #specialize specialize(…)} method will be invoked.</p>
     *
     * <p>If both the {@code transform} and {@code parameters} arguments are non-null, then the later should describes
     * the parameters used for creating the transform. Those parameters will be stored for information purpose and can
     * be given back by the {@link #getParameterValues()} method.</p>
     *
     * @param properties The properties to be given to the identified object.
     * @param method     The operation method.
     * @param transform  Transform from positions in the source CRS to positions in the target CRS, or {@code null}.
     * @param parameters The {@code transform} parameter values, or {@code null}.
     *
     * @see DefaultMathTransformFactory#createBaseToDerived(CoordinateSystem, MathTransform, CoordinateSystem)
     */
    public DefaultConversion(final Map<String,?>       properties,
                             final OperationMethod     method,
                             final MathTransform       transform,
                             final ParameterValueGroup parameters)
    {
        super(properties, method, transform, parameters);
    }

    /**
     * Constructs a new conversion with the same values than the specified one, together with the
     * specified source and target CRS. While the source conversion can be an arbitrary one, it is
     * typically a defining conversion.
     *
     * @param definition The defining conversion.
     * @param sourceCRS  The source CRS.
     * @param targetCRS  The target CRS.
     * @param factory    The factory to use for creating a transform from the parameters or for performing axis changes.
     */
    DefaultConversion(final Conversion definition,
                      final CoordinateReferenceSystem sourceCRS,
                      final CoordinateReferenceSystem targetCRS,
                      final MathTransformFactory factory) throws FactoryException
    {
        super(definition, sourceCRS, targetCRS, createMathTransform(definition, sourceCRS, targetCRS, factory));
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
     * @param  factory    The factory to use for creating a transform from the parameters or for performing axis changes.
     * @return The conversion of the given type between the given CRS.
     * @throws ClassCastException if a contradiction is found between the given {@code baseType},
     *         the defining {@linkplain DefaultConversion#getInterface() conversion type} and
     *         the {@linkplain DefaultOperationMethod#getOperationType() method operation type}.
     * @throws FactoryException if the creation of a {@link MathTransform} from the {@linkplain #getParameterValues()
     *         parameter values}, or a {@linkplain CoordinateSystems#swapAndScaleAxes change of axis order or units}
     *         failed.
     *
     * @see DefaultMathTransformFactory#createBaseToDerived(CoordinateReferenceSystem, ParameterValueGroup, CoordinateSystem)
     */
    public <T extends Conversion> T specialize(final Class<T> baseType,
            final CoordinateReferenceSystem sourceCRS, final CoordinateReferenceSystem targetCRS,
            final MathTransformFactory factory) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("baseType",  baseType);
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        ArgumentChecks.ensureNonNull("factory",   factory);
        ensureCompatibleDatum("sourceCRS", super.getSourceCRS(), sourceCRS);
        ensureCompatibleDatum("targetCRS", super.getTargetCRS(), targetCRS);
        return SubTypes.create(baseType, this, sourceCRS, targetCRS, factory);
    }

    /**
     * Ensures that the {@code actual} CRS uses a datum which is equals, ignoring metadata,
     * to the datum of the {@code expected} CRS.
     *
     * @param param     The parameter name, used only in case of error.
     * @param expected  The CRS containing the expected datum, or {@code null}.
     * @param actual    The CRS for which to check the datam, or {@code null}.
     */
    private static void ensureCompatibleDatum(final String param,
            final CoordinateReferenceSystem expected,
            final CoordinateReferenceSystem actual)
    {
        if ((expected instanceof SingleCRS) && (actual instanceof SingleCRS)) {
            final Datum datum = ((SingleCRS) expected).getDatum();
             if (datum != null && !Utilities.equalsIgnoreMetadata(datum, ((SingleCRS) actual).getDatum())) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatibleDatum_2, datum.getName(), param));
             }
        }
    }

    /**
     * Creates the math transform to be given to the sub-class constructor.
     * This method is a workaround for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static MathTransform createMathTransform(
            final Conversion definition,
            final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS,
            final MathTransformFactory factory) throws FactoryException
    {
        final int interpDim = ReferencingUtilities.getDimension(getInterpolationCRS(definition));
        MathTransform mt = definition.getMathTransform();
        if (mt == null) {
            /*
             * If the user did not specified explicitely a MathTransform, we will need to create it
             * from the parameters. This case happen often when creating a ProjectedCRS, because the
             * user often did not have all needed information when he created the defining conversion:
             * the length of semi-major and semi-minor axes were often missing. But now we know those
             * lengths thanks to the 'sourceCRS' argument given to this method. So we can complete the
             * parameters. This is the job of MathTransformFactory.createBaseToDerived(…).
             */
            final ParameterValueGroup parameters = definition.getParameterValues();
            if (parameters == null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnspecifiedParameterValues));
            }
            mt = factory.createBaseToDerived(sourceCRS, parameters, targetCRS.getCoordinateSystem());
        } else {
            /*
             * If the user specified explicitely a MathTransform, we may still need to swap or scale axes.
             * If this conversion is a defining conversion (which is usually the case when creating a new
             * ProjectedCRS), then DefaultMathTransformFactory has a specialized createBaseToDerived(…)
             * method for this job.
             */
            final CoordinateReferenceSystem mtSource = definition.getSourceCRS();
            final CoordinateReferenceSystem mtTarget = definition.getTargetCRS();
            if (mtSource == null && mtTarget == null && factory instanceof DefaultMathTransformFactory) {
                mt = ((DefaultMathTransformFactory) factory).createBaseToDerived(
                        sourceCRS.getCoordinateSystem(), mt,
                        targetCRS.getCoordinateSystem());
            } else {
                /*
                 * If we can not use our SIS factory implementation, or if this conversion is not a defining
                 * conversion (i.e. if this is the conversion of an existing ProjectedCRS, in which case the
                 * math transform may not be normalized), then we fallback on a simpler swapAndScaleAxes(…)
                 * method defined in this class. This is needed for AbstractCRS.forConvention(AxisConvention).
                 */
                mt = swapAndScaleAxes(mt, sourceCRS, mtSource, interpDim, true,  factory);
                mt = swapAndScaleAxes(mt, mtTarget, targetCRS, interpDim, false, factory);
                return mt;  // Skip createPassThroughTransform(…) since it was handled by swapAndScaleAxes(…).
            }
        }
        if (interpDim != 0) {
            mt = factory.createPassThroughTransform(interpDim, mt, 0);
        }
        return mt;
    }

    /**
     * Concatenates to the given transform the operation needed for swapping and scaling the axes.
     * The two coordinate systems must implement the same GeoAPI coordinate system interface.
     * For example if {@code sourceCRS} uses a {@code CartesianCS}, then {@code targetCRS} must use
     * a {@code CartesianCS} too.
     *
     * @param transform The transform to which to concatenate axis changes.
     * @param sourceCRS The first CRS of the pair for which to check for axes changes.
     * @param targetCRS The second CRS of the pair for which to check for axes changes.
     * @param interpDim The number of dimensions of the interpolation CRS, or 0 if none.
     * @param isSource  {@code true} for pre-concatenating the changes, or {@code false} for post-concatenating.
     * @param factory   The factory to use for performing axis changes.
     */
    private static MathTransform swapAndScaleAxes(MathTransform transform,
            final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS,
            final int interpDim, final boolean isSource,
            final MathTransformFactory factory) throws FactoryException
    {
        if (sourceCRS != null && targetCRS != null && sourceCRS != targetCRS) try {
            Matrix m = CoordinateSystems.swapAndScaleAxes(sourceCRS.getCoordinateSystem(),
                                                          targetCRS.getCoordinateSystem());
            if (!m.isIdentity()) {
                if (interpDim != 0) {
                    m = Matrices.createPassThrough(interpDim, m, 0);
                }
                final MathTransform s = factory.createAffineTransform(m);
                transform = factory.createConcatenatedTransform(isSource ? s : transform,
                                                                isSource ? transform : s);
            }
        } catch (ConversionException e) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2,
                    (isSource ? "sourceCRS" : "targetCRS"),
                    (isSource ?  sourceCRS  :  targetCRS).getName()), e);
        }
        return transform;
    }
}
