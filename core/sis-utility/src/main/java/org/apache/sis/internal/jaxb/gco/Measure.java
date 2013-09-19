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
package org.apache.sis.internal.jaxb.gco;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import org.apache.sis.internal.jaxb.gmd.CodeListProxy;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.xml.ValueConverter;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;


/**
 * A measurement value together with its unit of measure.
 * This is used for marshalling an element defined by ISO-19103.
 *
 * <p>This class duplicates {@link org.apache.sis.measure.Measure}, but we have to do that way
 * because that{@code Measure} extends {@link Number} and we are not allowed to use the
 * {@code @XmlValue} annotation on a class that extends an other class.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.measure.Measure
 *
 * @todo We should annotate {@link org.apache.sis.measure.Measure} directly
 *       if we can find some way to use {@code @XmlValue} with that class.
 */
public final class Measure {
    /**
     * The value of the measure.
     */
    @XmlValue
    public double value;

    /**
     * The unit of measure.
     */
    public Unit<?> unit;

    /**
     * Default empty constructor for JAXB. The value is initialized to NaN,
     * but JAXB will overwrite this value if a XML value is presents.
     */
    public Measure() {
        value = Double.NaN;
    }

    /**
     * Constructs a representation of the measure as defined in ISO-19103 standard,
     * with the UOM attribute like {@code "gmxUom.xml#xpointer(//*[@gml:id='m'])"}.
     *
     * @param value The value of the measure.
     * @param unit  The unit of measure to use.
     */
    public Measure(final double value, final Unit<?> unit) {
        this.value = value;
        this.unit  = unit;
    }

    /**
     * Constructs a string representation of the units as defined in the ISO-19103 standard.
     * This method is invoked during XML marshalling. For example in the units are "metre",
     * then this method returns:
     *
     * {@preformat text
     *     http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])
     * }
     *
     * @return The string representation of the unit of measure.
     */
    @XmlAttribute(required = true)
    public String getUOM() {
        final Unit<?> unit = this.unit;
        final String symbol;
        if (unit == null || unit.equals(Unit.ONE)) {
            symbol = "";
        } else if (unit.equals(NonSI.PIXEL)) {
            symbol = "pixel";
        } else {
            symbol = Context.schema(Context.current(), "gmd", CodeListProxy.DEFAULT_SCHEMA)
                    .append("resources/uom/gmxUom.xml#xpointer(//*[@gml:id='").append(unit).append("'])").toString();
        }
        return symbol;
    }

    /**
     * Sets the unit of measure. This method is invoked by JAXB at unmarshalling time,
     * and can be invoked only once.
     *
     * @param uom The unit of measure as a string.
     * @throws URISyntaxException If the {@code uom} looks like a URI, but can not be parsed.
     */
    public void setUOM(String uom) throws URISyntaxException {
        if (uom == null || (uom = CharSequences.trimWhitespaces(uom)).isEmpty()) {
            unit = null;
            return;
        }
        /*
         * Try to guess if the UOM is a URN or URL. We looks for character that are usually
         * part of URI but not part of unit symbols, for example ':'. We can not search for
         * '/' and '.' since they are part of UCUM representation.
         */
        final Context context = Context.current();
        final ValueConverter converter = Context.converter(context);
        if (uom.indexOf(':') >= 0) {
            final URI uri = converter.toURI(context, uom);
            String part = uri.getFragment();
            if (part != null) {
                uom = part;
                int i = uom.lastIndexOf("@gml:id=");
                if (i >= 0) {
                    i += 8; // 8 is the length of "@gml:id="
                    for (final int length=uom.length(); i<length;) {
                        final int c = uom.codePointAt(i);
                        if (!Character.isWhitespace(c)) {
                            if (c == '\'') i++;
                            break;
                        }
                        i += Character.charCount(c);
                    }
                    final int stop = uom.lastIndexOf('\'');
                    uom = CharSequences.trimWhitespaces((stop > i) ? uom.substring(i, stop) : uom.substring(i));
                }
            } else if ((part = uri.getPath()) != null) {
                uom = new File(part).getName();
            }
        }
        unit = converter.toUnit(context, uom);
    }

    /**
     * Sets the unit to the given value, with a warning logged if the user specified an other unit.
     *
     * {@example Some users wrongly assign the "m" unit to <code>Ellipsoid.inverseFlattening</code>.
     *           The SIS adapter force the unit to <code>Unit.ONE</code>, but we want to let the user
     *           know that he probably did something wrong.}
     *
     * @param newUnit The new unit (can not be null).
     */
    public void setUnit(final Unit<?> newUnit) {
        if (unit != null && !unit.equals(newUnit)) {
            final LogRecord record = Errors.getResources(null)
                    .getLogRecord(Level.WARNING, Errors.Keys.IncompatiblePropertyValue_1, unit);
            record.setSourceClassName(getClass().getName());
            record.setSourceMethodName("setUnit");
            Context.warningOccured(Context.current(), this, record);
        }
        unit = newUnit;
    }
}
