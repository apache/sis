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

import java.net.URI;
import java.util.Map;
import java.util.Arrays;
import java.util.Locale;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.format.MeasurementParseException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.privy.ReferencingFactoryContainer;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.measure.Units;

// Specific to the main branch:
import org.apache.sis.referencing.privy.CoordinateOperations;
import org.apache.sis.referencing.operation.transform.MathTransformBuilder;


/**
 * Well Known Text (WKT) parser for {@linkplain MathTransform math transform}s.
 * Note that while this base class is restricted to math transforms, subclasses may parse a wider range of objects.
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
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
        WKTKeywords.TemporalQuantity,   // Alternative keyword for "TimeUnit".
        WKTKeywords.ParametricUnit      // Ignored for the same reason as "Unit".
    };

    /**
     * The base units associated to the {@link #UNIT_KEYWORDS}, ignoring {@link WKTKeywords#Unit}.
     * For each {@code UNIT_KEYWORDS[i]} element, the associated base unit is {@code BASE_UNIT[i-1]}.
     */
    private static final Unit<?>[] BASE_UNITS = {
        Units.METRE, Units.RADIAN, Units.UNITY, Units.SECOND, Units.SECOND
    };

    /**
     * Some conversion factors applied to {@link #UNIT_KEYWORDS} for which rounding errors are found in practice.
     * Some Well Known Texts define factors with low accuracy, as in {@code ANGLEUNIT["degree", 0.01745329252]}.
     * This causes the parser to fail to recognize that the unit is degree and to convert angles with that factor.
     * This may result in surprising behavior like <a href="https://issues.apache.org/jira/browse/SIS-377">SIS-377</a>.
     * This array is a workaround for that problem, adding the missing accuracy to factors.
     * This workaround should be removed in a future version if we fix
     * <a href="https://issues.apache.org/jira/browse/SIS-433">SIS-433</a>.
     *
     * <p>Values in each array <strong>must</strong> be sorted in ascending order.</p>
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-377">SIS-377</a>
     * @see <a href="https://issues.apache.org/jira/browse/SIS-433">SIS-433</a>
     */
    private static final double[][] CONVERSION_FACTORS = {
        {0.3048,                    // foot, declared for avoiding that unit to be confused with US survey foot.
         0.30480060960121924,       // US survey foot
         1609.3472186944375},       // US survey mile
        {Math.PI/(180*60*60),       // Arc-second:  4.84813681109536E-6
         Math.PI/(180*60),          // Arc-minute:  2.908882086657216E-4
         Math.PI/(200),             // Grad:        1.5707963267948967E-2
         Math.PI/(180)}             // Degree:      1.7453292519943295E-2
    };

    /**
     * The factories to use for creating math transforms and geodetic objects.
     */
    final ReferencingFactoryContainer factories;

    /**
     * The classification of the last math transform or projection parsed, or {@code null} if none.
     */
    private transient String classification;

    /**
     * The method for the last math transform passed, or {@code null} if none.
     *
     * @see #getOperationMethod(Element)
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
     * @param  mtFactory  the factory to use for creating {@link MathTransform} objects.
     */
    public MathTransformParser(final MathTransformFactory mtFactory) {
        this(null, Map.of(), Symbols.getDefault(), null, null, null,
                new ReferencingFactoryContainer(null, null, null, null, null, mtFactory), null);
    }

    /**
     * Creates a parser using the specified set of symbols and factories.
     *
     * @param  sourceFile    URI to declare as the source of the WKT definitions, or {@code null} if unknown.
     * @param  fragments     reference to the {@link WKTFormat#fragments} map, or an empty map if none.
     * @param  symbols       the set of symbols to use. Cannot be null.
     * @param  numberFormat  the number format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param  dateFormat    the date format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param  unitFormat    the unit format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param  factories     the factories to use for creating math transforms and geodetic objects.
     * @param  errorLocale   the locale for error messages (not for parsing), or {@code null} for the system default.
     */
    MathTransformParser(final URI sourceFile, final Map<String,StoredTree> fragments, final Symbols symbols,
            final NumberFormat numberFormat, final DateFormat dateFormat, final UnitFormat unitFormat,
            final ReferencingFactoryContainer factories, final Locale errorLocale)
    {
        super(sourceFile, fragments, symbols, numberFormat, dateFormat, unitFormat, errorLocale);
        this.factories = factories;
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
     * Parses the next element in the specified <i>Well Know Text</i> (WKT) tree.
     *
     * @param  element  the element to be parsed.
     * @return the parsed object, or {@code null} if the element is not recognized.
     * @throws ParseException if the element cannot be parsed.
     */
    @Override
    Object buildFromTree(final Element element) throws ParseException {
        return parseMathTransform(element, true);
    }

    /**
     * Parses the next {@code MathTransform} in the specified <i>Well Know Text</i> (WKT) tree.
     *
     * @param  element    the parent element.
     * @param  mandatory  {@code true} if a math transform must be present, or {@code false} if optional.
     * @return the next element as a {@code MathTransform} object, or {@code null}.
     * @throws ParseException if the next element cannot be parsed.
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
     * <h4>Standard compliance note</h4>
     * This method is a slight departure of ISO 19162, which said <q>Should any attributes or values given
     * in the cited identifier be in conflict with attributes or values given explicitly in the WKT description,
     * the WKT values shall prevail.</q> But some units can hardly be expressed by the {@code UNIT} element,
     * because the latter can contain only a conversion factor. For example, sexagesimal units (EPSG:9108, 9110
     * and 9111) can hardly be expressed in another way than by their EPSG code. Thankfully, identifiers in
     * {@code UNIT} elements are rare, so risk of conflicts should be low.
     *
     * @param  parent  the parent {@code "UNIT"} element.
     * @param  unitID  where to store the authority code for caller's information, or {@code null} if none.
     * @return the unit from the identifier code, or {@code null} if none.
     * @throws ParseException if the {@code "ID"} cannot be parsed.
     */
    final Unit<?> parseUnitID(final Element parent, final Object[] unitID) throws ParseException {
        final Element element = parent.pullElement(OPTIONAL, ID_KEYWORDS);
        if (element != null) {
            final String codeSpace = element.pullString("codeSpace");
            final Object code      = element.pullObject("code");            // Accepts Integer as well as String.
            element.close(ignoredElements);
            if (unitID != null) unitID[0] = code;
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
     * @param  parent  the parent element.
     * @return the {@code "UNIT"} element, or {@code null} if none.
     * @throws ParseException if the {@code "UNIT"} cannot be parsed.
     *
     * @see GeodeticObjectParser#parseScaledUnit(Element, String, Unit)
     */
    final Unit<?> parseUnit(final Element parent) throws ParseException {
        final Element element = parent.pullElement(OPTIONAL, UNIT_KEYWORDS);
        if (element == null) {
            return null;
        }
        final int     index  = element.getKeywordIndex() - 1;
        final String  name   = element.pullString("name");
        final Unit<?> unit   = parseUnitID(element, null);
        final Unit<?> base   = (index >= 0 && index < BASE_UNITS.length) ? BASE_UNITS[index] : null;
        /*
         * The conversion factor form base unit is mandatory, except for temporal units
         * because the conversion may not be exact (because of variable duration of day,
         * month or year). Note however that Apache SIS 1.5 will fallback on constant
         * factors anyway, therefore the problem described by ISO is not really solved.
         */
        double factor;
        if (base == Units.SECOND && element.peekValue() == null) {
            factor = Double.NaN;
        } else {
            factor = element.pullDouble("factor");
        }
        element.close(ignoredElements);
        if (unit != null) {
            return unit;
        }
        /*
         * Conversion factor can be applied only if the base dimension (angle, linear, scale, etc.) is known.
         * However, before to apply that factor, we may need to fix rounding errors found in some WKT strings.
         * In particular, the conversion factor for degrees is sometimes written as 0.01745329252 instead of
         * 0.017453292519943295.
         */
        if (base != null && !Double.isNaN(factor)) {
            if (index < CONVERSION_FACTORS.length) {
                factor = completeUnitFactor(CONVERSION_FACTORS[index], factor);
            }
            return base.multiply(factor);
        }
        // If we cannot infer the base type, we have to rely on the name.
        try {
            return parseUnit(name);
        } catch (MeasurementParseException e) {
            throw new UnparsableObjectException(errorLocale, Errors.Keys.UnknownUnit_1,
                    new Object[] {name}, element.offset).initCause(e);
        }
    }

    /**
     * If the unit conversion factor specified in the Well Known Text is missing some fraction digits,
     * tries to complete them. The main use case is to replace 0.01745329252 by 0.017453292519943295
     * in degree units.
     *
     * @param  predefined  some known conversion factors, in ascending order.
     * @param  factor      the conversion factor specified in the Well Known Text element.
     * @return the conversion factor to use.
     */
    private static double completeUnitFactor(final double[] predefined, final double factor) {
        int i = Arrays.binarySearch(predefined, factor);
        if (i < 0) {
            i = Math.max(~i, 1);
            double accurate = predefined[i-1];
            if (i < predefined.length) {
                double next = predefined[i];
                if (next - factor < factor - accurate) {
                    accurate = next;
                }
            }
            if (DecimalFunctions.equalsIgnoreMissingFractionDigits(accurate, factor)) {
                return accurate;
            }
        }
        return factor;
    }

    /**
     * If the unit conversion factor specified in the Well Known Text is missing some fraction digits,
     * tries to complete them. The main use case is to replace 0.01745329252 by 0.017453292519943295
     * in degree units.
     *
     * @param  baseUnit  the base unit for which to complete the conversion factor.
     * @param  factor    the conversion factor specified in the Well Known Text element.
     * @return the conversion factor to use.
     */
    static double completeUnitFactor(final Unit<?> baseUnit, final double factor) {
        for (int i=CONVERSION_FACTORS.length; --i>=0;) {
            if (BASE_UNITS[i] == baseUnit) {
                return completeUnitFactor(CONVERSION_FACTORS[i], factor);
            }
        }
        return factor;
    }

    /**
     * Parses a sequence of {@code "PARAMETER"} elements.
     *
     * @param  element             the parent element containing the parameters to parse.
     * @param  parameters          the group where to store the parameter values.
     * @param  defaultUnit         the default unit (for arbitrary quantity, including angular), or {@code null}.
     * @param  defaultAngularUnit  the default angular unit, or {@code null} if none. This is determined by the context,
     *                             especially when {@link GeodeticObjectParser} parses a {@code ProjectedCRS} element.
     * @throws ParseException if the {@code "PARAMETER"} element cannot be parsed.
     */
    final void parseParameters(final Element element, final ParameterValueGroup parameters,
            final Unit<?> defaultUnit, final Unit<Angle> defaultAngularUnit) throws ParseException
    {
        final Unit<?> defaultSI = (defaultUnit != null) ? defaultUnit.getSystemUnit() : null;
        Element param = element;
        try {
            while ((param = element.pullElement(OPTIONAL, WKTKeywords.Parameter, WKTKeywords.ParameterFile)) != null) {
                final String name = param.pullString("name");
                Unit<?> unit = parseUnit(param);
                param.pullElement(OPTIONAL, ID_KEYWORDS);
                /*
                 * DEPARTURE FROM ISO 19162: the specification recommends that we use the identifier instead
                 * than the parameter name. However, we do not yet have a "get parameter by ID" in Apache SIS
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
                        final Unit<?> si = unit.getSystemUnit();
                        if (si.equals(defaultSI)) {
                            unit = defaultUnit;
                        } else if (si.equals(Units.RADIAN)) {
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
                if (sourceFile != null && parameter instanceof DefaultParameterValue<?>) {
                    ((DefaultParameterValue<?>) parameter).setSourceFile(sourceFile);
                }
                param.close(ignoredElements);
            }
        } catch (ParameterNotFoundException e) {
            throw new UnparsableObjectException(errorLocale, Errors.Keys.UnexpectedParameter_1,
                    new String[] {e.getParameterName()}, param.offset).initCause(e);
        } catch (InvalidParameterValueException e) {
            throw (ParseException) new ParseException(e.getLocalizedMessage(), param.offset).initCause(e);
        }
    }

    /**
     * Parses a {@code "PARAM_MT"} element. This element has the following pattern:
     *
     * {@snippet lang="wkt" :
     *     PARAM_MT["<classification-name>" {,<parameter>}* ]
     *     }
     *
     * @param  parent  the parent element.
     * @return the {@code "PARAM_MT"} element as an {@link MathTransform} object.
     * @throws ParseException if the {@code "PARAM_MT"} element cannot be parsed.
     */
    private MathTransform parseParamMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(FIRST, WKTKeywords.Param_MT);
        if (element == null) {
            return null;
        }
        classification = element.pullString("classification");
        final MathTransformBuilder builder;
        try {
            builder = CoordinateOperations.builder(factories.getMathTransformFactory(), classification);
        } catch (NoSuchIdentifierException exception) {
            throw element.parseFailed(exception);
        }
        /*
         * Scan over all PARAMETER["name", value] elements and
         * set the corresponding parameter in the parameter group.
         */
        parseParameters(element, builder.parameters(), null, null);
        element.close(ignoredElements);
        /*
         * We now have all information for constructing the math transform.
         */
        final MathTransform transform;
        try {
            transform = builder.create();
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
        lastMethod = builder.getMethod().orElse(null);
        return transform;
    }

    /**
     * Parses an {@code "INVERSE_MT"} element. This element has the following pattern:
     *
     * {@snippet lang="wkt" :
     *     INVERSE_MT[<math transform>]
     *     }
     *
     * @param  parent  the parent element.
     * @return the {@code "INVERSE_MT"} element as an {@link MathTransform} object.
     * @throws ParseException if the {@code "INVERSE_MT"} element cannot be parsed.
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
     * {@snippet lang="wkt" :
     *     PASSTHROUGH_MT[<integer>, <math transform>]
     *     }
     *
     * @param  parent  the parent element.
     * @return the {@code "PASSTHROUGH_MT"} element as an {@link MathTransform} object.
     * @throws ParseException if the {@code "PASSTHROUGH_MT"} element cannot be parsed.
     */
    private MathTransform parsePassThroughMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(FIRST, WKTKeywords.PassThrough_MT);
        if (element == null) {
            return null;
        }
        final int firstAffectedCoordinate = parent.pullInteger("firstAffectedCoordinate");
        final MathTransform transform = parseMathTransform(element, true);
        final MathTransformFactory mtFactory = factories.getMathTransformFactory();
        element.close(ignoredElements);
        try {
            return mtFactory.createPassThroughTransform(firstAffectedCoordinate, transform, 0);
        } catch (FactoryException exception) {
            throw element.parseFailed(exception);
        }
    }

    /**
     * Parses a {@code "CONCAT_MT"} element. This element has the following pattern:
     *
     * {@snippet lang="wkt" :
     *     CONCAT_MT[<math transform> {,<math transform>}*]
     *     }
     *
     * @param  parent  the parent element.
     * @return the {@code "CONCAT_MT"} element as an {@link MathTransform} object.
     * @throws ParseException if the {@code "CONCAT_MT"} element cannot be parsed.
     */
    private MathTransform parseConcatMT(final Element parent) throws ParseException {
        final Element element = parent.pullElement(FIRST, WKTKeywords.Concat_MT);
        if (element == null) {
            return null;
        }
        MathTransform transform = parseMathTransform(element, true);
        MathTransform optionalTransform;
        final MathTransformFactory mtFactory = factories.getMathTransformFactory();
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
     * Returns the operation method for the last math transform parsed, or {@code null} if unspecified.
     * This is used by {@link GeodeticObjectParser} in order to build {@link org.opengis.referencing.crs.DerivedCRS}.
     *
     * @param  element  the element being parsed. Used in case an exception must be thrown.
     * @return last operation method used.
     * @throws ParseException if the last method cannot be obtained.
     */
    final OperationMethod getOperationMethod(final Element element) throws ParseException {
        if (lastMethod == null) {
            /*
             * Safety in case some MathTransformFactory implementations do not support
             * getLastMethod(). Performs a slower and less robust check as a fallback.
             */
            if (classification != null) try {
                lastMethod = factories.findOperationMethod(classification);
            } catch (NoSuchIdentifierException e) {
                throw element.parseFailed(e);
            }
        }
        return lastMethod;
    }
}
