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
package org.apache.sis.referencing.operation.transform;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;


/**
 * An object capable to create {@code MathTransform} instances from given parameter values.
 * This interface is the Apache SIS mechanism by which
 * {@linkplain org.apache.sis.referencing.operation.DefaultFormula formula} are concretized as Java code.
 * A math transform provider ignores the source and target <abbr>CRS</abbr> and works with coordinates in
 * predefined axis order and units — typically (east, north, up) in degrees or meters — although some
 * variations are allowed in the number of dimensions (typically the "up" dimension being optional).
 * Adjustments for <abbr>CRS</abbr> axis order, units and exact number of dimensions are caller's responsibility.
 *
 * <p>This interface is generally not used directly. The recommended way to get a {@link MathTransform}
 * is to {@linkplain org.apache.sis.referencing.CRS#findOperation find the coordinate operation}
 * (generally from a pair of <var>source</var> and <var>target</var> CRS), then to invoke
 * {@link org.opengis.referencing.operation.CoordinateOperation#getMathTransform()}.
 * Alternatively, one can also use a {@linkplain DefaultMathTransformFactory math transform factory}.</p>
 *
 * <p>Implementations of this interface usually extend {@link org.apache.sis.referencing.operation.DefaultOperationMethod},
 * but this is not mandatory. This interface can also be used alone since {@link MathTransform} instances can be created
 * for other purpose than coordinate operations.</p>
 *
 *
 * <h2>How to add custom coordinate operations to Apache SIS</h2>
 * {@link DefaultMathTransformFactory} can discover automatically new coordinate operations
 * (including map projections) by scanning the module path. To define a custom coordinate operation,
 * one needs to define a <strong>thread-safe</strong> class implementing <strong>both</strong> this
 * {@code MathTransformProvider} interface and the {@link org.opengis.referencing.operation.OperationMethod} one.
 * While not mandatory, we suggest to extend {@link org.apache.sis.referencing.operation.DefaultOperationMethod}.
 * Example:
 *
 * {@snippet lang="java" :
 * public class MyOperationProvider extends DefaultOperationMethod implements MathTransformProvider {
 *     private static final ParameterDescriptor<Foo> FOO;
 *     private static final ParameterDescriptor<Bar> BAR;
 *     private static final ParameterDescriptorGroup PARAMETERS;
 *     static {
 *         final var builder = new ParameterBuilder();
 *         FOO = builder.addName("Foo").create(Foo.class, null);
 *         BAR = builder.addName("Bar").create(Bar.class, null);
 *         PARAMETERS = builder.addName("My operation").createGroup(FOO, BAR);
 *     }
 *
 *     public MyOperationProvider() {
 *         super(Map.of(NAME_KEY, PARAMETERS.getName()), PARAMETERS);
 *     }
 *
 *     @Override
 *     public MathTransform createMathTransform(Context context) {
 *         var pg  = Parameters.castOrWrap(context.getCompletedParameters();
 *         Foo foo = pg.getMandatoryValue(FOO);
 *         Bar bar = pg.getMandatoryValue(BAR);
 *         return new MyOperation(foo, bar);
 *     }
 * }
 * }
 *
 * In the common case where the provider needs numerical parameter values in a specific units of measurement,
 * the following pattern can be used:
 *
 * {@snippet lang="java" :
 *     double semiMajor = values.parameter("semi_major").doubleValue(Units.METRE);
 *     double semiMinor = values.parameter("semi_minor").doubleValue(Units.METRE);
 * }
 *
 * Then the class name of that implementation shall be declared in {@code module-info.java}
 * as a provider of the {@code org.opengis.referencing.operation.OperationMethod} service.
 *
 * @author  Martin Desruisseaux (Geomatys, IRD)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.operation.DefaultOperationMethod
 * @see DefaultMathTransformFactory
 * @see AbstractMathTransform
 *
 * @since 0.6
 */
@FunctionalInterface
public interface MathTransformProvider {
    /**
     * Creates a math transform from the specified group of parameter values.
     * Invoking this method is equivalent to invoking {@link #createMathTransform(Context)}
     * with the given factory and parameters wrapped in an instance of {@code Context}.
     *
     * @param  factory     the factory to use if this constructor needs to create other math transforms.
     * @param  parameters  the parameter values that define the transform to create.
     * @return the math transform created from the given parameters.
     * @throws InvalidParameterNameException if the given parameter group contains an unknown parameter.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws InvalidParameterValueException if a parameter has an invalid value.
     * @throws FactoryException if the math transform cannot be created for some other reason
     *         (for example a required file was not found).
     */
    default MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup parameters)
            throws InvalidParameterNameException, ParameterNotFoundException,
                   InvalidParameterValueException, FactoryException
    {
        return createMathTransform(new MathTransformProvider.Context() {
            @Override public MathTransformFactory getFactory() {
                return (factory != null) ? factory : Context.super.getFactory();
            }
            @Override public ParameterValueGroup getCompletedParameters() {
                return parameters;
            }
        });
    }

    /**
     * The parameter values that define the transform to create, together with its context.
     * The context includes the desired number of source and target dimensions,
     * and the factory to use if the provider needs to create math transform steps.
     * The context intentionally (by design) excludes the following information:
     *
     * <ul>
     *   <li>Details about coordinate systems (only their types is provided),
     *       because axis order and unit conversions are handled separately.</li>
     *   <li>Datum, because inferring datum shifts is the task of other classes,
     *       generally requiring a connection to a geodetic registry.</li>
     * </ul>
     *
     * <h2>Purpose of the dimension properties</h2>
     * If the operation method accepts different number of dimensions (for example, operations
     * that can optionally use or compute an ellipsoidal height as the third dimension),
     * then the provider can use the desired number of dimensions for selecting which variant
     * (2D versus 3D) of the operation method to use.
     *
     * <h2>Purpose of the factory property</h2>
     * Some math transforms may actually be implemented as a chain of operation steps, for example a
     * {@linkplain DefaultMathTransformFactory#createConcatenatedTransform(MathTransform, MathTransform)
     * concatenation} of {@linkplain DefaultMathTransformFactory#createAffineTransform affine transforms}
     * with other kinds of transforms. In such cases, providers can use the given factory for creating
     * and concatenating the affine steps.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.6
     * @since   1.5
     */
    public interface Context {
        /**
         * The factory to use if the provider needs to create other math transforms as operation steps.
         * This is often the factory which is invoking the {@link #createMathTransform(Context)} method,
         * but not necessarily.
         *
         * <p>The default implementation returns the {@link DefaultMathTransformFactory}.</p>
         *
         * @return the factory to use if the provider needs to create other math transforms.
         */
        default MathTransformFactory getFactory() {
            return DefaultMathTransformFactory.provider();
        }

        /**
         * Returns the desired number of source dimensions.
         * This value can be the determined from the source coordinate reference system, but not necessarily.
         * It can also be the number of target dimensions of the previous step in a concatenated transform.
         *
         * <p>The number of source dimensions may be unknown (absent).
         * In such case, the default number of dimensions is method-specific.
         * Some operation methods may search for a {@link org.opengis.parameter.ParameterValue} named {@code "dim"}.
         * Other operation methods will fallback on a hard-coded number of dimensions, typically 2 or 3.</p>
         *
         * @return desired number of source dimensions.
         *
         * @see MathTransform#getSourceDimensions()
         */
        default OptionalInt getSourceDimensions() {
            return OptionalInt.empty();
        }

        /**
         * Returns the desired number of target dimensions.
         * This value can be the determined from the target coordinate reference system, but not necessarily.
         * It can also be the number of source dimensions of the next step in a concatenated transform.
         *
         * <p>The number of target dimensions may be unknown (absent).
         * In such case, the default number of dimensions is method-specific.
         * Some operation methods may search for a {@link org.opengis.parameter.ParameterValue} named {@code "dim"}.
         * Other operation methods will fallback on a hard-coded number of dimensions, typically 2 or 3.</p>
         *
         * @return desired number of target dimensions.
         *
         * @see MathTransform#getTargetDimensions()
         */
        default OptionalInt getTargetDimensions() {
            return OptionalInt.empty();
        }

        /**
         * Returns the type of the source coordinate system.
         * The returned value may be an interface or an implementation class.
         * If the coordinate system type is unknown, then this method returns {@code CoordinateSystem.class}.
         *
         * @return the type of the source coordinate system, or {@code CoordinateSystem.class} if unknown.
         */
        default Class<? extends CoordinateSystem> getSourceCSType() {
            return CoordinateSystem.class;
        }

        /**
         * Returns the type of the target coordinate system.
         * The returned value may be an interface or an implementation class.
         * If the coordinate system type is unknown, then this method returns {@code CoordinateSystem.class}.
         *
         * @return the type of the target coordinate system, or {@code CoordinateSystem.class} if unknown.
         */
        default Class<? extends CoordinateSystem> getTargetCSType() {
            return CoordinateSystem.class;
        }

        /**
         * Returns the ellipsoid which is used together with the source coordinate system.
         *
         * @return the ellipsoid which is used together with the source coordinate system, or empty if none.
         *
         * @since 1.6
         */
        default Optional<Ellipsoid> getSourceEllipsoid() {
            return Optional.empty();
        }

        /**
         * Returns the ellipsoid which is used together with the target coordinate system.
         *
         * @return the ellipsoid which is used together with the target coordinate system, or empty if none.
         *
         * @since 1.6
         */
        default Optional<Ellipsoid> getTargetEllipsoid() {
            return Optional.empty();
        }

        /**
         * Returns the names of parameters that have been inferred from the context.
         * The set of keys can contain any of {@code "dim"},
         * {@code     "semi_major"}, {@code     "semi_minor"},
         * {@code "src_semi_major"}, {@code "src_semi_minor"},
         * {@code "tgt_semi_major"}, {@code "tgt_semi_minor"} and/or
         * {@code "inverse_flattening"}, depending on the operation method used.
         * The parameters named in that set are included in the parameters
         * returned by {@link #getCompletedParameters()}.
         *
         * <h4>Associated Boolean values</h4>
         * The associated Boolean in the map tells whether the named parameter value is really contextual.
         * The Boolean is {@code TRUE} if the value was inferred from the context, or was explicitly set
         * by the user to the same value as what would have been inferred from the context.
         * OTherwise (i.e., if there is a mismatch between inferred and user-defined value),
         * the Boolean is {@code FALSE}, a warning should be logged by the implementation,
         * and the user-supplied value should have precedence in the parameters returned by
         * {@link #getCompletedParameters()}.
         *
         * @return names of parameters inferred from context.
         */
        default Map<String,Boolean> getContextualParameters() {
            return Map.of();
        }

        /**
         * Returns the parameter values that fully define the transform to create.
         * The parameters returned by this method shall include parameters that are usually inferred from
         * the context (source and target <abbr>CRS</abbr>) rather that explicitly provided by the users.
         * Examples of inferred parameters are {@code "dim"},
         * {@code     "semi_major"}, {@code     "semi_minor"},
         * {@code "src_semi_major"}, {@code "src_semi_minor"},
         * {@code "tgt_semi_major"}, {@code "tgt_semi_minor"} and/or
         * {@code "inverse_flattening"}, depending on the operation method.
         *
         * <p>An exception to above rule is the source and target number of dimensions,
         * which can be specified either by the {@code "dim"} parameter or by the
         * {@link #getSourceDimensions()} and {@link #getTargetDimensions()} methods.
         * The reason for this departure is that the number of dimensions is often not a formal
         * parameter of an operation method, but can nevertheless be used for inferring variants.
         * For example the <q>Geographic3D offsets</q> (EPSG:9660) method does not have a "dimension" argument
         * because, as its name implies, that operation method is intended for the three-dimensional case only.
         * However, if the number of dimensions is nevertheless 2, the provider may be able to opportunistically
         * redirect to the <q>Geographic2D offsets</q> (EPSG:9619) operation method, because those two methods
         * are actually implemented by the same code (an affine transform) in Apache SIS.</p>
         *
         * @return the parameter values that fully define the transform to create.
         */
        ParameterValueGroup getCompletedParameters();
    }

    /**
     * Creates a math transform from a group of parameter values and its context.
     * The context includes the factory to use and the desired number of source and target dimensions.
     * The given number of dimensions is only a hint. Providers can use different numbers of dimensions
     * (often hard-coded in the formulas) than the ones specified in the {@code context}.
     * Callers should check the actual number of dimensions of the returned transform.
     *
     * @param  context  the parameter values that define the transform to create, together with its context.
     * @return the math transform created from the given parameters.
     * @throws InvalidParameterNameException if the parameter group contains an unknown parameter.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws InvalidParameterValueException if a parameter has an invalid value.
     * @throws FactoryException if the math transform cannot be created for some other reason
     *         (for example a required file was not found).
     *
     * @since 1.5
     */
    MathTransform createMathTransform(Context context)
            throws InvalidParameterNameException, ParameterNotFoundException,
                   InvalidParameterValueException, FactoryException;
}
