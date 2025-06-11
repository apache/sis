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
import java.util.HashMap;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.measure.IncommensurableException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.internal.ParameterizedTransformBuilder;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;

// Specific to the main branch:
import static org.apache.sis.pending.geoapi.referencing.MissingMethods.getDatumEnsemble;


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
 * or explicitly provided at construction time in a <dfn>defining conversion</dfn> (see below).</p>
 *
 * <h2>Defining conversions</h2>
 * {@code OperationMethod} instances are generally created for a pair of existing {@linkplain #getSourceCRS() source}
 * and {@linkplain #getTargetCRS() target CRS}. But {@code Conversion} instances without those information may exist
 * temporarily while creating a {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS derived} or
 * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}.
 * Those <i>defining conversions</i> have no source and target CRS since those elements are provided by the
 * derived or projected CRS themselves. This class provides a {@linkplain #DefaultConversion(Map, OperationMethod,
 * MathTransform, ParameterValueGroup) constructor} for such defining conversions.
 *
 * <p>After the source and target CRS become known, we can invoke the {@link #specialize specialize(…)} method for
 * {@linkplain DefaultMathTransformFactory#createParameterizedTransform creating a math transform from the parameters}
 * and assign the source and target CRS to it.</p>
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. This means that unless otherwise noted in the javadoc,
 * {@code Conversion} instances created using only SIS factories and static constants can be shared
 * by many objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see DefaultTransformation
 *
 * @since 0.6
 */
@XmlType(name = "ConversionType")
@XmlRootElement(name = "Conversion")
public class DefaultConversion extends AbstractSingleOperation implements Conversion {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2148164324805562793L;

    /**
     * Creates a coordinate conversion from the given properties.
     * The properties given in argument follow the same rules as for the
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
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity()}</td>
     *   </tr>
     * </table>
     *
     * <h4>Relationship between datum</h4>
     * By definition, coordinate <b>conversions</b> do not change the datum. Consequently, the given {@code sourceCRS}
     * and {@code targetCRS} should use the same datum. If the datum is not the same, then the coordinate operation
     * should probably be a {@linkplain DefaultTransformation transformation} instead.
     * However, Apache SIS does not enforce that condition, but we encourage users to follow it.
     * The reason why SIS is tolerant is because some gray areas may exist about whether an operation
     * should be considered as a conversion or a transformation.
     *
     * <p>Note that while Apache SIS accepts to construct {@code DefaultConversion} instances
     * with different source and target datum, it does not accept to use such instances for
     * {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS derived CRS} construction.</p>
     *
     * <h4>Example</h4>
     * Converting time instants from a {@linkplain org.apache.sis.referencing.crs.DefaultTemporalCRS temporal CRS}
     * using the <i>January 1st, 1950</i> epoch to another temporal CRS using the <i>January 1st, 1970</i> epoch is
     * a datum change, since the epoch is part of {@linkplain org.apache.sis.referencing.datum.DefaultTemporalDatum
     * temporal datum} definition. However, such operation does not have all the accuracy issues of transformations
     * between geodetic reference frames (empirically determined, over-determined systems, stochastic nature of the parameters).
     * Consequently, some users may consider sufficient to represent temporal epoch changes as conversions instead
     * than transformations.
     *
     * @param properties        the properties to be given to the identified object.
     * @param sourceCRS         the source CRS.
     * @param targetCRS         the target CRS, which shall use a datum {@linkplain Utilities#equalsIgnoreMetadata
     *                          equals (ignoring metadata)} to the source CRS datum.
     * @param interpolationCRS  the CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param method            the coordinate operation method (mandatory in all cases).
     * @param transform         transform from positions in the source CRS to positions in the target CRS.
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
     * at <i>defining conversion</i> construction time.
     * The source and target CRS will become known later, at the
     * {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS Derived CRS} or
     * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected CRS}
     * construction time.
     *
     * <p>The {@code properties} map given in argument follows the same rules as for the
     * {@linkplain #DefaultConversion(Map, CoordinateReferenceSystem, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, OperationMethod, MathTransform) above constructor}.</p>
     *
     * <h4>Transform and parameters arguments</h4>
     * At least one of the {@code transform} or {@code parameters} argument must be non-null.
     * If the caller supplies a {@code transform} argument, then it shall be a transform expecting
     * {@linkplain org.apache.sis.referencing.cs.AxesConvention#NORMALIZED normalized} input coordinates
     * and producing normalized output coordinates. See {@link org.apache.sis.referencing.cs.AxesConvention}
     * for more information about what Apache SIS means by "normalized".
     *
     * <p>If the caller cannot yet supply a {@code MathTransform}, then (s)he shall supply the parameter values needed
     * for creating that transform, with the possible omission of {@code "semi_major"} and {@code "semi_minor"} values.
     * The semi-major and semi-minor parameter values will be set automatically when the
     * {@link #specialize specialize(…)} method will be invoked.</p>
     *
     * <p>If both the {@code transform} and {@code parameters} arguments are non-null, then the latter should describe
     * the parameters used for creating the transform. Those parameters will be stored for information purpose and can
     * be given back by the {@link #getParameterValues()} method.</p>
     *
     * @param properties  the properties to be given to the identified object.
     * @param method      the operation method.
     * @param transform   transform from positions in the source CRS to positions in the target CRS, or {@code null}.
     * @param parameters  the {@code transform} parameter values, or {@code null}.
     *
     * @see DefaultMathTransformFactory#swapAndScaleAxes(MathTransform, MathTransformProvider.Context)
     */
    @SuppressWarnings("this-escape")    // False positive.
    public DefaultConversion(final Map<String,?>       properties,
                             final OperationMethod     method,
                             final MathTransform       transform,
                             final ParameterValueGroup parameters)
    {
        super(properties, method);
        this.transform = transform;
        if (transform == null && parameters == null) {
            throw new IllegalArgumentException(Resources.forProperties(properties)
                    .getString(Resources.Keys.UnspecifiedParameterValues));
        }
        setParameterValues(parameters, null);
        checkDimensions(properties);
    }

    /**
     * Constructs a new conversion with the same values as the specified one, together with the
     * specified source and target CRS. While the source conversion can be an arbitrary one,
     * it is typically a defining conversion.
     *
     * @param definition  the defining conversion.
     * @param source      the new source CRS.
     * @param target      the new target CRS.
     * @param factory     the factory to use for creating a transform from the parameters or for performing axis changes.
     */
    DefaultConversion(final Conversion definition,
                      final CoordinateReferenceSystem source,
                      final CoordinateReferenceSystem target,
                      final MathTransformFactory factory) throws FactoryException
    {
        super(definition);
        int interpDim = ReferencingUtilities.getDimension(super.getInterpolationCRS().orElse(null));
        if (transform == null) {
            /*
             * If the user did not specify explicitly a MathTransform, we will need to create it from the parameters.
             * This case happens when creating a ProjectedCRS because the length of semi-major and semi-minor axes are
             * often missing at defining conversion creation time. Since this constructor knows those semi-axis lengths
             * thanks to the `sourceCRS` argument, we can complete the parameters.
             */
            if (parameters == null) {
                throw new InvalidGeodeticParameterException(Resources.format(Resources.Keys.UnspecifiedParameterValues));
            }
            /*
             * Note that setTargetAxes(…) intentionally uses only the CoordinateSystem instead of the full
             * CoordinateReferenceSystem because the targetCRS is typically under construction when this
             * method in invoked, and attempts to use it can cause NullPointerException.
             */
            final boolean isDerived = (target instanceof GeneralDerivedCRS);
            final var builder = new ParameterizedTransformBuilder(factory, null);
            builder.setParameters(parameters, true);
            builder.setSourceAxes(source);
            builder.setTargetAxes(target.getCoordinateSystem(), isDerived ? null : ReferencingUtilities.getEllipsoid(target));
            transform = builder.create();
            if (builder instanceof MathTransformProvider.Context) {
                final var context = (MathTransformProvider.Context) builder;
                setParameterValues(context.getCompletedParameters(), new HashMap<>(context.getContextualParameters()));
            }
        } else {
            /*
             * If the user specified explicitly a MathTransform, we may still need to swap or scale axes.
             * If this conversion is a defining conversion (which is usually the case when creating a new
             * ProjectedCRS), then there is a method for this job.
             */
            if (sourceCRS == null && targetCRS == null) {
                var builder = new ParameterizedTransformBuilder(factory, null);
                builder.setSourceAxes(source.getCoordinateSystem(), null);
                builder.setTargetAxes(target.getCoordinateSystem(), null);    // See comment on the other setTargetAxes(…) call.
                transform = builder.swapAndScaleAxes(transform);
            } else {
                /*
                 * If this conversion is not a defining conversion (i.e. if this is the conversion of an existing
                 * `ProjectedCRS`, in which case the math transform may not be normalized), then we fallback on a
                 * simpler `swapAndScaleAxes(…)` method defined in this class.
                 * This is needed for `AbstractCRS.forConvention(AxisConvention)`.
                 */
                transform = swapAndScaleAxes(transform, source, sourceCRS, interpDim, true,  factory);
                transform = swapAndScaleAxes(transform, targetCRS, target, interpDim, false, factory);
                interpDim = 0;  // Skip createPassThroughTransform(…) since it was handled by swapAndScaleAxes(…).
            }
        }
        if (interpDim != 0) {
            transform = factory.createPassThroughTransform(interpDim, transform, 0);
        }
        sourceCRS = source;
        targetCRS = target;
    }

    /**
     * Creates a new coordinate operation with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  operation  the coordinate operation to copy.
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
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultConversion}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultConversion} instance is created using the
     *       {@linkplain #DefaultConversion(Conversion) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultConversion castOrCopy(final Conversion object) {
        if (object == null || object instanceof DefaultConversion) {
            return (DefaultConversion) object;
        }
        if (object instanceof Projection) {
            return new DefaultProjection((Projection) object);
        } else {
            return new DefaultConversion(object);
        }
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code Conversion.class}.
     *
     * @return the conversion interface implemented by this class.
     */
    @Override
    public Class<? extends Conversion> getInterface() {
        return Conversion.class;
    }

    /**
     * Returns a specialization of this conversion with non-null <abbr>CRS</abbr>s.
     * This {@code specialize(…)} method is typically invoked on {@linkplain #DefaultConversion(Map,
     * OperationMethod, MathTransform, ParameterValueGroup) defining conversion} instances,
     * when more information become available about the conversion to create.
     *
     * @param  sourceCRS  the source CRS.
     * @param  targetCRS  the target CRS.
     * @param  factory    the factory to use for creating a transform from the parameters
     *         or for performing axis changes, or {@code null} for the default factory.
     * @return conversion which declares the given <abbr>CRS</abbr>s as the source and target.
     * @throws MismatchedDatumException if the given CRS do not use the same datum as the source and target CRS
     *         of this conversion.
     * @throws FactoryException if the creation of a {@link MathTransform} from the {@linkplain #getParameterValues()
     *         parameter values}, or a {@linkplain CoordinateSystems#swapAndScaleAxes change of axis order or units}
     *         failed.
     *
     * @since 1.5
     */
    public Conversion specialize(final CoordinateReferenceSystem sourceCRS,
                                 final CoordinateReferenceSystem targetCRS,
                                 MathTransformFactory factory) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        /*
         * Conceptual consistency check: verify that the new CRSs use the same datum as the previous ones,
         * since the purpose of this method is not to apply datum changes. Datum changes are the purpose of
         * a dedicated kind of operations, namely Transformation.
         */
        ensureCompatibleDatum("sourceCRS", super.getSourceCRS(), sourceCRS);
        if (!(targetCRS instanceof GeneralDerivedCRS)) {
            ensureCompatibleDatum("targetCRS", super.getTargetCRS(), targetCRS);
        } else {
            /*
             * Special case for derived and projected CRS: we cannot check directly the datum of the target CRS
             * of a derived CRS, because this method is invoked indirectly by SIS AbstractDerivedCRS constructor
             * before its `conversionFromBase` field is set. Since the Apache SIS implementations of derived CRS
             * map the datum to getConversionFromBase().getSourceCRS().getDatum(), invoking targetCRS.getDatum()
             * below may result in a NullPointerException. Instead, we verify that `this` conversion use the same
             * datum for source and target CRS, since DerivedCRS and ProjectedCRS are expected to have the same
             * datum than their source CRS.
             */
            ensureCompatibleDatum("targetCRS", sourceCRS, super.getTargetCRS());
        }
        final boolean isProjection = (targetCRS instanceof ProjectedCRS) && (sourceCRS instanceof GeographicCRS);
        if (super.getSourceCRS() == sourceCRS &&
            super.getTargetCRS() == targetCRS &&
            super.getMathTransform() != null &&
            isProjection == (this instanceof Projection))
        {
            return this;
        }
        if (factory == null) {
            factory = DefaultMathTransformFactory.provider();
        }
        if (isProjection) {
            return new DefaultProjection(this, sourceCRS, targetCRS, factory);
        }
        return new DefaultConversion(this, sourceCRS, targetCRS, factory);
    }

    /**
     * Ensures that the {@code actual} CRS uses a datum which is equal, ignoring metadata,
     * to the datum of the {@code expected} CRS.
     *
     * @param  param     the parameter name, used only in case of error.
     * @param  expected  the CRS containing the expected datum, or {@code null}.
     * @param  actual    the CRS for which to check the datum, or {@code null}.
     * @throws MismatchedDatumException if the two CRS use different datum.
     */
    private static void ensureCompatibleDatum(final String param,
            final CoordinateReferenceSystem expected,
            final CoordinateReferenceSystem actual)
    {
        if ((expected instanceof SingleCRS) && (actual instanceof SingleCRS)) {
            final var crs1 = (SingleCRS) expected;
            final var crs2 = (SingleCRS) actual;
            IdentifiedObject datum = crs1.getDatum();
            IdentifiedObject other;
            if (datum != null) {
                other = crs2.getDatum();
            } else {
                datum = getDatumEnsemble(crs1);
                other = getDatumEnsemble(crs2);
            }
            if (datum != null && other != null && !Utilities.equalsIgnoreMetadata(datum, other)) {
                throw new MismatchedDatumException(Resources.format(
                        Resources.Keys.IncompatibleDatum_2, datum.getName(), param));
            }
        }
    }

    /**
     * Concatenates to the given transform the operation needed for swapping and scaling the axes.
     * The two coordinate systems must implement the same GeoAPI coordinate system interface.
     * For example if {@code sourceCRS} uses a {@code CartesianCS}, then {@code targetCRS} must use
     * a {@code CartesianCS} too.
     *
     * @param  transform  the transform to which to concatenate axis changes.
     * @param  sourceCRS  the first CRS of the pair for which to check for axes changes.
     * @param  targetCRS  the second CRS of the pair for which to check for axes changes.
     * @param  interpDim  the number of dimensions of the interpolation CRS, or 0 if none.
     * @param  isSource   {@code true} for pre-concatenating the changes, or {@code false} for post-concatenating.
     * @param  factory    the factory to use for performing axis changes.
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
        } catch (IncommensurableException e) {
            throw new InvalidGeodeticParameterException(Errors.format(Errors.Keys.IllegalArgumentValue_2,
                    (isSource ? "sourceCRS" : "targetCRS"),
                    (isSource ?  sourceCRS  :  targetCRS).getName()), e);
        }
        return transform;
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
    private DefaultConversion() {
    }
}
