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
package org.apache.sis.internal.jaxb.gml;

import java.net.URISyntaxException;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Quantity;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.Schemas;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.Units;


/**
 * A measurement value together with its unit of measure.
 * This is used for marshalling an element defined by ISO-19103.
 *
 * <p>This class duplicates {@link org.apache.sis.measure.Measure}, but we have to do that way
 * because that {@code Measure} extends {@link Number} and we are not allowed to use the
 * {@code @XmlValue} annotation on a class that extends an other class.</p>
 *
 * <div class="section">XML marshalling</div>
 * Measures are used in different ways by the ISO 19115 (Metadata) and GML standards.
 * The former expresses some measurements with an object of XML type {@code gco:Distance}
 * (as a substitution for XML type {@code gco:Measure}):
 *
 * {@preformat xml
 *   <gmd:distance>
 *     <gco:Distance uom=\"http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])\">1000.0</gco:Distance>
 *   </gmd:distance>
 * }
 *
 * while GML will rather use a a syntax like below:
 *
 * {@preformat xml
 *   <gml:semiMajorAxis uom="urn:ogc:def:uom:EPSG::9001">6378137</gml:semiMajorAxis>
 * }
 *
 * Both have a value of type {@code xsd:double} and a {@code uom} attribute (without namespace)
 * of type {@code gml:UomIdentifier}. Those two informations are represented by this class.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.measure.Measure
 * @see org.apache.sis.internal.jaxb.gml.MeasureList
 * @see org.apache.sis.internal.jaxb.gco.UnitAdapter
 */
@XmlType(name = "MeasureType")
public final class Measure {
    /**
     * The value of the measure.
     */
    @XmlValue
    public double value;

    /**
     * The unit of measurement.
     */
    public Unit<?> unit;

    /**
     * {@code true} if the units shall be formatted as {@code xpointer}.
     * If {@code false} (the default), then this class will try to format the units using the GML syntax.
     */
    public boolean asXPointer;

    /**
     * Default empty constructor for JAXB. The value is initialized to NaN,
     * but JAXB will overwrite that value if a XML value is present.
     */
    public Measure() {
        value = Double.NaN;
    }

    /**
     * Constructs a representation of the measure as defined in ISO-19103 standard,
     * with the UOM attribute like {@code "gmxUom.xml#xpointer(//*[@gml:id='m'])"}.
     *
     * @param value The value of the measure.
     * @param unit  The unit of measurement.
     */
    public Measure(final double value, final Unit<?> unit) {
        this.value = value;
        this.unit  = unit;
    }

    /**
     * Constructs a string representation of the units as defined in the ISO-19103 standard.
     * This method is invoked during XML marshalling. For example if the units are "metre",
     * then this method returns one of the following strings, in preference order:
     *
     * {@preformat text
     *     urn:ogc:def:uom:EPSG::9001
     * }
     *
     * or
     *
     * {@preformat text
     *     http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])
     * }
     *
     * @return The string representation of the unit of measure.
     *
     * @todo Strictly speaking, the above URL should be used only for "m", "deg" and "rad" units because they
     *       are the only ones defined in the <code>gmxUom.xml</code> file. What should we do for other units?
     */
    @XmlAttribute(name = "uom", required = true)
    public String getUOM() {
        return getUOM(unit, asXPointer, false);
    }

    /**
     * Implementation of {@link #getUOM()} as a static method for use by classes that define their own
     * {@code uom} attribute, instead of letting the {@code uom} attribute on the measurement value.
     * The main example is {@link org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis}.
     *
     * @param  unit The unit to format.
     * @param  asXPointer {@code true} if the units shall be formatted as {@code xpointer}.
     * @param  inAxis {@code true} for a unit used in Coordinate System Axis definition.
     * @return The string representation of the unit of measure.
     */
    public static String getUOM(final Unit<?> unit, final boolean asXPointer, final boolean inAxis) {
        if (!asXPointer) {
            final Integer code = Units.getEpsgCode(unit, inAxis);
            if (code != null) {
                return DefinitionURI.PREFIX + ":uom:" + Constants.EPSG + "::" + code;
            }
        }
        if (unit == null || unit.equals(Unit.ONE)) {
            return "";
        }
        if (unit.equals(NonSI.PIXEL)) {
            return "pixel"; // TODO: maybe not the most appropriate unit.
        }
        return Context.schema(Context.current(), "gmd", Schemas.METADATA_ROOT).append(Schemas.UOM_PATH)
                .append("#xpointer(//*[@gml:id='").append(unit).append("'])").toString();
    }

    /**
     * Sets the unit of measure. This method is invoked by JAXB at unmarshalling time.
     *
     * @param uom The unit of measure as a string.
     * @throws URISyntaxException If the {@code uom} looks like a URI, but can not be parsed.
     */
    public void setUOM(String uom) throws URISyntaxException {
        final Context context = Context.current();
        unit = Context.converter(context).toUnit(context, uom);
    }

    /**
     * Returns {@link #unit} as a unit compatible with the given quantity.
     *
     * @todo For now, this method does not format useful error message in case of wrong unit type.
     *       We define this method merely as a placeholder for future improvement in error handling.
     *
     * @param  <Q>  Compile-time type of the {@code type} argument.
     * @param  type The quantity for the desired unit.
     * @return A unit compatible with the given type, or {@code null} if none.
     */
    public <Q extends Quantity> Unit<Q> getUnit(final Class<Q> type) {
        return (unit != null) ? unit.asType(type) : null;
    }

    /**
     * Sets the unit to the given value, and returns {@code true} if the current {@link #unit} value was different.
     * A return value of {@code true} means that the caller should log a warning.
     *
     * <div class="note"><b>Example:</b>
     * Some users wrongly assign the "m" unit to {@code Ellipsoid.inverseFlattening}.
     * The SIS adapter forces the unit to {@link Unit#ONE}, but we want to let the user
     * know that he probably did something wrong.</div>
     *
     * @param  newUnit The new unit (can not be null).
     * @return {@code true} if a different unit was defined before this method call.
     */
    public boolean setUnit(final Unit<?> newUnit) {
        final boolean changed = (unit != null && !unit.equals(newUnit));
        unit = newUnit;
        return changed;
    }

    /**
     * Sends a warning for a missing {@code "uom"} attribute.
     *
     * @param caller     The class of the method invoking this method.
     * @param methodName The name of the method invoking this method.
     */
    public static void missingUOM(final Class<?> caller, final String methodName) {
        Context.warningOccured(Context.current(), caller, methodName,
                Errors.class, Errors.Keys.MandatoryAttribute_2, "uom", "Measure");
    }
}
