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

import java.util.Map;
import java.util.Collections;
import java.util.Locale;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.measure.quantity.Angle;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.util.LocalizedParseException;
import org.apache.sis.internal.util.Constants;
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
     * The keywords for {@code ID} or {@code AUTHORITY} elements, as a static array because frequently used.
     */
    static final String[] ID_KEYWORDS = {WKTKeywords.Id, WKTKeywords.Authority};

    /**
     * The keywords of unit elements. Most frequently used keywords should be first.
     */
    private static final String[] UNIT_KEYWORDS = {
        WKTKeywords.Unit,   // Ignored since it does not allow us to know the quantity dimension.
        WKTKeywords.LengthUnit, WKTKeywords.AngleUnit, WKTKeywords.ScaleUnit, WKTKeywords.TimeUnit,
        WKTKeywords.ParametricUnit  // Ignored for the same reason than "Unit".
    };

    /**
     * The base unit associated to the {@link #UNIT_KEYWORDS}, ignoring {@link WKTKeywords#Unit}.
     * For each {@code UNIT_KEYWORDS[i]} element, the associated base unit is {@code BASE_UNIT[i]}.
     */
    private static final Unit<?>[] BASE_UNITS = {
        SI.METRE, SI.RADIAN, Unit.ONE, SI.SECOND
    };

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
        this(Symbols.getDefault(), Collections.<String,Element>emptyMap(), null, null, null, mtFactory, null);
    }

    /**
     * Creates a parser using the specified set of symbols and factory.
     *
     * @param symbols       The set of symbols to use.
     * @param fragments     Reference to the {@link WKTFormat#fragments} map, or an empty map if none.
     * @param numberFormat  The number format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param dateFormat    The date format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param unitFormat    The unit format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param mtFactory     The factory to use to create {@link MathTransform} objects.
     * @param errorLocale   The locale for error messages (not for parsing), or {@code null} for the system default.
     */
    MathTransformParser(final Symbols symbols, final Map<String,Element> fragments,
            final NumberFormat numberFormat, final DateFormat dateFormat, final UnitFormat unitFormat,
            final MathTransformFactory mtFactory, final Locale errorLocale)
    {
        super(symbols, fragments, numberFormat, dateFormat, unitFormat, errorLocale);
        this.mtFactory = mtFactory;
        ensureNonNull("mtFactory", mtFactory);
    }

    /**
     * Returns the name of the class providing the publicly-accessible {@code createFromWKT(String)} method.
     * This information is used for logging purpose only.
     */
    @Override
    String getPublicFacade() {
        return "org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory";
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
        MathTransform tr;
        if ((tr = parseParamMT       (element)) == null &&
            (tr = parseConcatMT      (element)) == null &&
            (tr = parseInverseMT     (element)) == null &&
            (tr = parsePassThroughMT (element)) == null)
        {
            if (mandatory) {
                throw element.missingOrUnknownComponent(WKTKeywords.Param_MT);
            }
        }
        return tr;
    }

    /**
     * Parses the {@code ID["authority", "code"]} element inside a {@code UNIT} element.
     * If such element is found, the authority is {@code "EPSG"} and the code is one of
     * the codes known to the {@link Units#valueOfEPSG(int)}, then that unit is returned.
     * Otherwise this method returns null.
     *
     * <div class="note"><b>Note:</b>
     * this method is a slight departure of ISO 19162, which said <cite>"Should any attributes or values given
     * in the cited identifier be in conflict with attributes or values given explicitly in the WKT description,
     * the WKT values shall prevail."</cite> But some units can hardly be expressed by the {@code UNIT} element,
     * because the later can contain only a conversion factor. For example sexagesimal units (EPSG:9108, 9110
     * and 9111) can hardly be expressed in an other way than by their EPSG code. Thankfully, identifiers in
     * {@code UNIT} elements are rare, so risk of conflicts should be low.</div>
     *
     * @param  parent The parent {@code "UNIT"} element.
     * @return The unit from the identifier code, or {@code null} if none.
     * @throws ParseException if the {@code "ID"} can not be parsed.
     */
    final Unit<?> parseUnitID(final Element parent) throws ParseException {
        final Element element = parent.pullElement(OPTIONAL, ID_KEYWORDS);
        if (element != null) {
            final String codeSpace = element.pullString("codeSpace");
            final Object code      = element.pullObject("code");            // Accepts Integer as well as String.
            element.close(ignoredElements);
            if (Constants.EPSG.equalsIgnoreCase(codeSpace)) try {
                final int n;
                if (Numbers.isInteger(code.getClass())) {
                    n = ((Number) code).intValue();
                } else {
                    n = Integer.parseInt(code.toString());
                }
                return Units.valueOfEPSG(n);
            } catch (NumberFormatException e) {
                warning(parent, element, null, e);
            }
        }
        return null;
    }

    /**
     * Parses an optional {@code "UNIT"} element of unknown dimension.
     * This method tries to infer the quantity dimension from the unit keyword.
     *
     * @param  parent The parent element.
     * @return The {@code "UNIT"} element, or {@code null} if none.
     * @throws ParseException if the {@code "UNIT"} can not be parsed.
     */
    final Unit<?> parseUnit(final Element parent) throws ParseException {
        final Element element = parent.pullElement(OPTIONAL, UNIT_KEYWORDS);
        if (element == null) {
            return null;
        }
        final String  name   = element.pullString("name");
        final double  factor = element.pullDouble("factor");
        final int     index  = element.getKeywordIndex() - 1;
        final Unit<?> unit   = parseUnitID(element);
        element.close(ignoredElements);
        if (unit != null) {
            return unit;
        }
        if (index >= 0 && index < BASE_UNITS.length) {
            return Units.multiply(BASE_UNITS[index], factor);
        }
        // If we can not infer the base type, we have to rely on the name.
        try {
            return parseUnit(name);
        } catch (IllegalArgumentException e) {
            throw (ParseException) new LocalizedParseException(errorLocale,
                    Errors.Keys.UnknownUnit_1, new Object[] {name}, element.offset).initCause(e);
        }
    }

    /**
     * Parses a sequence of {@code "PARAMETER"} elements.
     *
     * @param  element            The parent element containing the parameters to parse.
     * @param  parameters         The group where to store the parameter values.
     * @param  defaultUnit        The default unit (for arbitrary quantity, including angular), or {@code null}.
     * @param  defaultAngularUnit The default angular unit, or {@code null} if none. This is determined by the
     *         context, especially when {@link GeodeticObjectParser} parses a {@code ProjectedCRS} element.
     * @throws ParseException if the {@code "PARAMETER"} element can not be parsed.
     */
    final void parseParameters(final Element element, final ParameterValueGroup parameters,
            final Unit<?> defaultUnit, final Unit<Angle> defaultAngularUnit) throws ParseException
    {
        final Unit<?> defaultSI = (defaultUnit != null) ? defaultUnit.toSI() : null;
        Element param = element;
        try {
            while ((param = element.pullElement(OPTIONAL, WKTKeywords.Parameter)) != null) {
                final String name = param.pullString("name");
                Unit<?> unit = parseUnit(param);
                param.pullElement(OPTIONAL, ID_KEYWORDS);
                /*
                 * DEPARTURE FROM ISO 19162: the specification recommends that we use the identifier instead
                 * than the parameter name. However we do not yet have a "get parameter by ID" in Apache SIS
                 * or in GeoAPI interfaces. This was not considered necessary since SIS is lenient (hopefully
                 * without introducing ambiguity) regarding parameter names, but we may revisit in a future
                 * version if it become no longer the case. See https://issues.apache.org/jira/browse/SIS-210
                 */
                final ParameterValue<?>      parameter  = parameters.parameter(name);
                final ParameterDescriptor<?> descriptor = parameter.getDescriptor();
                final Class<?>               valueClass = descriptor.getValueClass();
                final boolean                isNumeric  = Number.class.isAssignableFrom(valueClass);
                if (isNumeric && unit == null) {
                    unit = descriptor.getUnit();
                    if (unit != null) {
                        final Unit<?> si = unit.toSI();
                        if (si.equals(defaultSI)) {
                            unit = defaultUnit;
                        } else if (si.equals(SI.RADIAN)) {
                            unit = defaultAngularUnit;
                        }
                    }
                }
                if (unit != null) {
                    parameter.setValue(param.pullDouble("doubleValue"), unit);
                } else if (isNumeric) {
                    if (Numbers.isInteger(valueClass)) {
                        parameter.setValue(param.pullInteger("intValue"));
                    } else {
                        parameter.setValue(param.pullDouble("doubleValue"));
                    }
                } else if (valueClass == Boolean.class) {
                    parameter.setValue(param.pullBoolean("booleanValue"));
                } else {
                    parameter.setValue(param.pullString("stringValue"));
                }
                param.close(ignoredElements);
            }
        } catch (ParameterNotFoundException e) {
            throw (ParseException) new LocalizedParseException(errorLocale, Errors.Keys.UnexpectedParameter_1,
                    new String[] {e.getParameterName()}, param.offset).initCause(e);
        } catch (InvalidParameterValueException e) {
            throw (ParseException) new ParseException(e.getLocalizedMessage(), param.offset).initCause(e);
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
    private MathTransform parseParamMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(FIRST, WKTKeywords.Param_MT);
        if (element == null) {
            return null;
        }
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
    private MathTransform parseInverseMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(FIRST, WKTKeywords.Inverse_MT);
        if (element == null) {
            return null;
        }
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
    private MathTransform parsePassThroughMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(FIRST, WKTKeywords.PassThrough_MT);
        if (element == null) {
            return null;
        }
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
     * @param  mode {@link #FIRST}, {@link #OPTIONAL} or {@link #MANDATORY}.
     * @param  parent The parent element.
     * @return The {@code "CONCAT_MT"} element as an {@link MathTransform} object.
     * @throws ParseException if the {@code "CONCAT_MT"} element can not be parsed.
     */
    private MathTransform parseConcatMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(FIRST, WKTKeywords.Concat_MT);
        if (element == null) {
            return null;
        }
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
