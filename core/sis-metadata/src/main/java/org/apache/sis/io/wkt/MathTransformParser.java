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
package org.apache.sis.io.wkt;

import java.util.Locale;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.util.LocalizedParseException;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Well Known Text (WKT) parser for {@linkplain MathTransform math transform}s.
 * Note that while this base class is restricted to math transforms, subclasses may parse a wider range of objects.
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.geoapi.org/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html">Well Know Text specification</a>
 */
class MathTransformParser extends AbstractParser {
    /**
     * The factory to use for creating math transforms.
     */
    final MathTransformFactory mtFactory;

    /**
     * The classification of the last math transform or projection parsed, or {@code null} if none.
     */
    private transient String classification;

    /**
     * The method for the last math transform passed, or {@code null} if none.
     *
     * @see #getOperationMethod()
     */
    private transient OperationMethod lastMethod;

    /**
     * Creates a parser using the default set of symbols and factory.
     */
    public MathTransformParser() {
        this(DefaultFactories.forBuildin(MathTransformFactory.class));
    }

    /**
     * Creates a parser for the given factory.
     *
     * <p><b>Maintenance note:</b> this constructor is invoked through reflection by
     * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createFromWKT(String)}.
     * Do not change the method signature even if it doesn't break the compilation, unless the reflection code
     * is also updated.</p>
     *
     * @param mtFactory The factory to use to create {@link MathTransform} objects.
     */
    public MathTransformParser(final MathTransformFactory mtFactory) {
        this(Symbols.getDefault(), null, null, null, mtFactory, null);
    }

    /**
     * Creates a parser using the specified set of symbols and factory.
     *
     * @param symbols       The set of symbols to use.
     * @param numberFormat  The number format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param dateFormat    The date format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param unitFormat    The unit format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param mtFactory     The factory to use to create {@link MathTransform} objects.
     * @param errorLocale   The locale for error messages (not for parsing), or {@code null} for the system default.
     */
    MathTransformParser(final Symbols symbols, final NumberFormat numberFormat, final DateFormat dateFormat,
            final UnitFormat unitFormat, final MathTransformFactory mtFactory, final Locale errorLocale)
    {
        super(symbols, numberFormat, dateFormat, unitFormat, errorLocale);
        this.mtFactory = mtFactory;
        ensureNonNull("mtFactory", mtFactory);
    }

    /**
     * Parses the next element in the specified <cite>Well Know Text</cite> (WKT) tree.
     *
     * @param  element The element to be parsed.
     * @return The parsed object, or {@code null} if the element is not recognized.
     * @throws ParseException if the element can not be parsed.
     */
    @Override
    Object parseObject(final Element element) throws ParseException {
        return parseMathTransform(element, true);
    }

    /**
     * Parses the next {@code MathTransform} in the specified <cite>Well Know Text</cite> (WKT) tree.
     *
     * @param  element The parent element.
     * @param  mandatory {@code true} if a math transform must be present, or {@code false} if optional.
     * @return The next element as a {@code MathTransform} object, or {@code null}.
     * @throws ParseException if the next element can not be parsed.
     */
    final MathTransform parseMathTransform(final Element element, final boolean mandatory) throws ParseException {
        lastMethod = null;
        classification = null;
        String keyword = null;
        final Object child = element.peek();
        if (child instanceof Element) {
            keyword = ((Element) child).keyword;
            if (keyword != null) {
                if (keyword.equalsIgnoreCase(WKTKeywords.Param_MT))       return parseParamMT      (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Concat_MT))      return parseConcatMT     (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Inverse_MT))     return parseInverseMT    (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.PassThrough_MT)) return parsePassThroughMT(element);
            }
        }
        if (mandatory) {
            throw element.keywordNotFound(keyword);
        }
        return null;
    }

    /**
     * Parses a sequence of {@code "PARAMETER"} elements.
     *
     * @param  element     The parent element containing the parameters to parse.
     * @param  parameters  The group where to store the parameter values.
     * @param  linearUnit  The default linear unit, or {@code null}.
     * @param  angularUnit The default angular unit, or {@code null}.
     * @throws ParseException if the {@code "PARAMETER"} element can not be parsed.
     */
    final void parseParameters(final Element element, final ParameterValueGroup parameters,
            final Unit<Length> linearUnit, final Unit<Angle> angularUnit) throws ParseException
    {
        Element param = element;
        try {
            while ((param = element.pullOptionalElement(WKTKeywords.Parameter)) != null) {
                final String                 name       = param.pullString("name");
                final ParameterValue<?>      parameter  = parameters.parameter(name);
                final ParameterDescriptor<?> descriptor = parameter.getDescriptor();
                final Class<?>               valueClass = descriptor.getValueClass();
                if (Number.class.isAssignableFrom(valueClass)) {
                    Unit<?> unit = descriptor.getUnit();
                    if (Units.isLinear(unit)) {
                        unit = linearUnit;
                    } else if (Units.isAngular(unit)) {
                        unit = angularUnit;
                    }
                    if (unit != null) {
                        parameter.setValue(param.pullDouble("value"), unit);
                    } else if (Numbers.isInteger(valueClass)) {
                        parameter.setValue(param.pullInteger("value"));
                    } else {
                        parameter.setValue(param.pullDouble("value"));
                    }
                } else if (valueClass == Boolean.class) {
                    parameter.setValue(param.pullBoolean("value"));
                } else {
                    parameter.setValue(param.pullString("value"));
                }
                param.close(ignoredElements);
            }
        } catch (ParameterNotFoundException exception) {
            throw (ParseException) new LocalizedParseException(errorLocale, Errors.Keys.UnexpectedParameter_1,
                    new String[] {exception.getParameterName()}, param.offset).initCause(exception);
        }
    }

    /**
     * Parses a {@code "PARAM_MT"} element. This element has the following pattern:
     *
     * {@preformat text
     *     PARAM_MT["<classification-name>" {,<parameter>}* ]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "PARAM_MT"} element as an {@link MathTransform} object.
     * @throws ParseException if the {@code "PARAM_MT"} element can not be parsed.
     */
    final MathTransform parseParamMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(WKTKeywords.Param_MT);
        classification = element.pullString("classification");
        final ParameterValueGroup parameters;
        try {
            parameters = mtFactory.getDefaultParameters(classification);
        } catch (NoSuchIdentifierException exception) {
            throw element.parseFailed(exception);
        }
        /*
         * Scan over all PARAMETER["name", value] elements and
         * set the corresponding parameter in the parameter group.
         */
        parseParameters(element, parameters, null, null);
        element.close(ignoredElements);
        /*
         * We now have all informations for constructing the math transform.
         */
        final MathTransform transform;
        try {
            transform = mtFactory.createParameterizedTransform(parameters);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        lastMethod = mtFactory.getLastMethodUsed();
        return transform;
    }

    /**
     * Parses an {@code "INVERSE_MT"} element. This element has the following pattern:
     *
     * {@preformat text
     *     INVERSE_MT[<math transform>]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "INVERSE_MT"} element as an {@link MathTransform} object.
     * @throws ParseException if the {@code "INVERSE_MT"} element can not be parsed.
     */
    final MathTransform parseInverseMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(WKTKeywords.Inverse_MT);
        MathTransform transform = parseMathTransform(element, true);
        try {
            transform = transform.inverse();
        } catch (NoninvertibleTransformException exception) {
            throw element.parseFailed(exception);
        }
        element.close(ignoredElements);
        return transform;
    }

    /**
     * Parses a {@code "PASSTHROUGH_MT"} element. This element has the following pattern:
     *
     * {@preformat text
     *     PASSTHROUGH_MT[<integer>, <math transform>]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "PASSTHROUGH_MT"} element as an {@link MathTransform} object.
     * @throws ParseException if the {@code "PASSTHROUGH_MT"} element can not be parsed.
     */
    final MathTransform parsePassThroughMT(final Element parent) throws ParseException {
        final Element element           = parent.pullElement(WKTKeywords.PassThrough_MT);
        final int firstAffectedOrdinate = parent.pullInteger("firstAffectedOrdinate");
        final MathTransform transform   = parseMathTransform(element, true);
        element.close(ignoredElements);
        try {
            return mtFactory.createPassThroughTransform(firstAffectedOrdinate, transform, 0);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "CONCAT_MT"} element. This element has the following pattern:
     *
     * {@preformat text
     *     CONCAT_MT[<math transform> {,<math transform>}*]
     * }
     *
     * @param  parent The parent element.
     * @return The {@code "CONCAT_MT"} element as an {@link MathTransform} object.
     * @throws ParseException if the {@code "CONCAT_MT"} element can not be parsed.
     */
    final MathTransform parseConcatMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(WKTKeywords.Concat_MT);
        MathTransform transform = parseMathTransform(element, true);
        MathTransform optionalTransform;
        while ((optionalTransform = parseMathTransform(element, false)) != null) {
            try {
                transform = mtFactory.createConcatenatedTransform(transform, optionalTransform);
            } catch (FactoryException exception) {
                throw element.parseFailed(exception);
            }
        }
        element.close(ignoredElements);
        return transform;
    }

    /**
     * Returns the operation method for the last math transform parsed. This is used by
     * {@link GeodeticObjectParser} in order to built {@link org.opengis.referencing.crs.DerivedCRS}.
     */
    final OperationMethod getOperationMethod() {
        if (lastMethod == null) {
            /*
             * Safety in case some MathTransformFactory implementations do not support
             * getLastMethod(). Performs a slower and less robust check as a fallback.
             */
            if (classification != null) {
                lastMethod = ReferencingServices.getInstance().getOperationMethod(
                        mtFactory.getAvailableMethods(SingleOperation.class), classification);
            }
        }
        return lastMethod;
    }
}
