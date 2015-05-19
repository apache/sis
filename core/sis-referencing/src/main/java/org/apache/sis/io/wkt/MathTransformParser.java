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
import java.text.ParseException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.referencing.OperationMethods;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.util.Numbers;

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
class MathTransformParser extends Parser {
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
        this(Symbols.getDefault(), DefaultFactories.forBuildin(MathTransformFactory.class), null);
    }

    /**
     * Creates a parser using the specified set of symbols and factory.
     *
     * @param symbols The set of symbols to use.
     * @param mtFactory The factory to use to create {@link MathTransform} objects.
     * @param displayLocale The locale for error messages (not for number parsing),
     *        or {@code null} for the system default.
     */
    public MathTransformParser(final Symbols symbols, final MathTransformFactory mtFactory, final Locale locale) {
        super(symbols, locale);
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
     * @param  required {@code true} if a math transform is required to be present.
     * @return The next element as a {@code MathTransform} object, or {@code null}.
     * @throws ParseException if the next element can not be parsed.
     */
    private MathTransform parseMathTransform(final Element element, final boolean required) throws ParseException {
        lastMethod = null;
        classification = null;
        final Object key = element.peek();
        String keyword = WKTKeywords.Param_MT;
        if (key instanceof Element) {
            keyword = ((Element) key).keyword;
            if (keyword != null) {
                if (keyword.equalsIgnoreCase(WKTKeywords.Param_MT))       return parseParamMT      (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Concat_MT))      return parseConcatMT     (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.Inverse_MT))     return parseInverseMT    (element);
                if (keyword.equalsIgnoreCase(WKTKeywords.PassThrough_MT)) return parsePassThroughMT(element);
            }
        }
        if (required) {
            throw element.keywordNotFound(keyword, keyword == WKTKeywords.Param_MT);
        }
        return null;
    }

    /**
     * Parses a "PARAM_MT" element. This element has the following pattern:
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
        Element param;
        while ((param = element.pullOptionalElement(WKTKeywords.Parameter)) != null) {
            final String name = param.pullString("name");
            final ParameterValue<?> parameter = parameters.parameter(name);
            final Class<?> type = parameter.getDescriptor().getValueClass();
            if (Number.class.isAssignableFrom(type)) {
                if (Numbers.isInteger(type)) {
                    parameter.setValue(param.pullInteger("value"));
                } else {
                    parameter.setValue(param.pullDouble("value"));
                }
            } else if (type == Boolean.class) {
                parameter.setValue(param.pullBoolean("value"));
            } else {
                parameter.setValue(param.pullString("value"));
            }
            param.close(ignoredElements);
        }
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
     * {@link Parser} in order to built {@link org.opengis.referencing.crs.DerivedCRS}.
     */
    final OperationMethod getOperationMethod() {
        if (lastMethod == null) {
            /*
             * Safety in case come MathTransformFactory implementation do not support
             * getLastMethod(). Performs a slower and less robust check as a fallback.
             */
            if (classification != null) {
                lastMethod = OperationMethods.getOperationMethod(
                        mtFactory.getAvailableMethods(SingleOperation.class), classification);
            }
        }
        return lastMethod;
    }
}
