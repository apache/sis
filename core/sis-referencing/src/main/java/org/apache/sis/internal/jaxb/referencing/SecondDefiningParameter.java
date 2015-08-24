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
package org.apache.sis.internal.jaxb.referencing;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.measure.unit.Unit;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gml.Measure;
import org.apache.sis.util.resources.Errors;


/**
 * Stores the second defining parameter of an {@link Ellipsoid}.
 * The purpose of this class is to allow JAXB to handle a second defining parameter,
 * according to the kind of ellipsoid we are facing to.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@XmlRootElement(name = "SecondDefiningParameter", namespace = Namespaces.GML)
public final class SecondDefiningParameter {
    /**
     * Nested parameter, for JAXB purpose.
     */
    @XmlElement(name = "SecondDefiningParameter")
    public SecondDefiningParameter secondDefiningParameter;

    /**
     * The measure, which is either the polar radius or the inverse of the flattening value.
     * We distinguish those two cases by the unit: if the measure is the inverse flattening,
     * then the unit must be {@link Unit#ONE}.
     *
     * @see Ellipsoid#getSemiMinorAxis()
     * @see Ellipsoid#getInverseFlattening()
     */
    public Measure measure;

    /**
     * JAXB mandatory empty constructor.
     */
    public SecondDefiningParameter() {
    }

    /**
     * Stores the semi-minor axis or the inverse of the flattening value.
     *
     * @param ellipsoid The ellipsoid from which to get the semi-minor or inverse flattening value.
     * @param nested {@code true} if the element should be nested in an other XML type.
     */
    public SecondDefiningParameter(final Ellipsoid ellipsoid, final boolean nested) {
        if (nested) {
            secondDefiningParameter = new SecondDefiningParameter(ellipsoid, false);
        } else {
            if (ellipsoid.isIvfDefinitive()) {
                measure = new Measure(ellipsoid.getInverseFlattening(), Unit.ONE);
            } else {
                measure = new Measure(ellipsoid.getSemiMinorAxis(), ellipsoid.getAxisUnit());
            }
        }
    }

    /**
     * Returns {@code true} if the measure is the inverse of the flattening value.
     *
     * @return {@code true} if the measure is the inverse of the flattening value.
     */
    public boolean isIvfDefinitive() {
        return (measure != null) && Unit.ONE.equals(measure.unit);
    }

    /**
     * Returns the semi-minor axis value as a measurement.
     *
     * @return The measure of the semi-minor axis.
     */
    @XmlElement(name = "semiMinorAxis")
    public Measure getSemiMinorAxis() {
        return isIvfDefinitive() ? null : measure;
    }

    /**
     * Sets the semi-minor axis value. This is invoked by JAXB for unmarshalling.
     * The unit of measurement (if any) shall be linear, but we do not verify that now.
     * This will be verified by {@code DefaultEllipsoid.setSecondDefiningParameter(…)}.
     *
     * @param measure The semi-minor axis value.
     */
    public void setSemiMinorAxis(final Measure measure) {
        this.measure = measure;
    }

    /**
     * Returns the inverse of the flattening value as a measurement.
     * Note: The unit of this measurement is dimensionless.
     *
     * @return The inverse of the flattening value as a measurement.
     */
    @XmlElement(name = "inverseFlattening")
    public Measure getInverseFlattening() {
        return isIvfDefinitive() ? measure : null;
    }

    /**
     * Sets the inverse of the flattening value. This is invoked by JAXB for unmarshalling.
     *
     * <p>Note that some GML wrongly assign the "m" unit to this measure, which is wrong.
     * This method overwrite the unit with a dimensionless one. This is required anyway
     * in order to distinguish between the two cases.</p>
     *
     * @param measure The inverse flattening value.
     */
    public void setInverseFlattening(final Measure measure) {
        if (measure.setUnit(Unit.ONE)) {
            Context.warningOccured(Context.current(), SecondDefiningParameter.class, "setInverseFlattening",
                    Errors.class, Errors.Keys.IncompatiblePropertyValue_1, "uom");
        }
        this.measure = measure;
    }
}
