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
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.measure.Units;
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
 * @version 0.8
 * @since   0.4
 * @module
 */
@XmlRootElement(name = "SecondDefiningParameter", namespace = Namespaces.GML)
public final class SecondDefiningParameter {
    /**
     * Nested parameter, for JAXB purpose.
     * This is used for marshalling XML like below:
     *
     * {@preformat xml
     *   <gml:secondDefiningParameter>
     *     <gml:SecondDefiningParameter>
     *       <gml:semiMinorAxis uom="urn:ogc:def:uom:EPSG::9001">6371007</gml:semiMinorAxis>
     *     </gml:SecondDefiningParameter>
     *   </gml:secondDefiningParameter>
     * }
     */
    @XmlElement(name = "SecondDefiningParameter")
    public SecondDefiningParameter secondDefiningParameter;

    /**
     * Whether the ellipsoid is a sphere, or {@code null} if unspecified.
     * If this value is {@code true}, then the XML shall be marshalled like below:
     *
     * {@preformat xml
     *   <gml:secondDefiningParameter>
     *     <gml:SecondDefiningParameter>
     *       <gml:isSphere>true</gml:isSphere>
     *     </gml:SecondDefiningParameter>
     *   </gml:secondDefiningParameter>
     * }
     *
     * @since 0.8
     */
    @XmlElement
    public Boolean isSphere;

    /**
     * The measure, which is either the polar radius or the inverse of the flattening value.
     * We distinguish those two cases by the unit: if the measure is the inverse flattening,
     * then the unit must be {@link Units#UNITY}.
     *
     * <p>This value should be {@code null} if {@link #isSphere} is {@code true}.</p>
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
     * @param  ellipsoid  the ellipsoid from which to get the semi-minor or inverse flattening value.
     * @param  nested     {@code true} if the element should be nested in an other XML type.
     */
    public SecondDefiningParameter(final Ellipsoid ellipsoid, final boolean nested) {
        if (nested) {
            secondDefiningParameter = new SecondDefiningParameter(ellipsoid, false);
        } else {
            if (ellipsoid.isSphere()) {
                isSphere = Boolean.TRUE;
            } else if (ellipsoid.isIvfDefinitive()) {
                measure = new Measure(ellipsoid.getInverseFlattening(), Units.UNITY);
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
        return (measure != null) && Units.UNITY.equals(measure.unit);
    }

    /**
     * Returns the semi-minor axis value as a measurement.
     *
     * @return the measure of the semi-minor axis.
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
     * @param  measure  the semi-minor axis value.
     */
    public void setSemiMinorAxis(final Measure measure) {
        this.measure = measure;
    }

    /**
     * Returns the inverse of the flattening value as a measurement.
     * Note: The unit of this measurement is dimensionless.
     *
     * @return the inverse of the flattening value as a measurement.
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
     * @param  measure  the inverse flattening value.
     */
    public void setInverseFlattening(final Measure measure) {
        if (measure.setUnit(Units.UNITY)) {
            Context.warningOccured(Context.current(), SecondDefiningParameter.class, "setInverseFlattening",
                    Errors.class, Errors.Keys.IncompatiblePropertyValue_1, "uom");
        }
        this.measure = measure;
    }
}
