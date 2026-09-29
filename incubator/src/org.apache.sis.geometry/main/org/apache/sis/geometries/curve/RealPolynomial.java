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
package org.apache.sis.geometries.curve;

import java.net.URI;
import java.util.List;
import org.apache.sis.measure.Range;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.util.GenericName;


/**
 * A real function defined by a polynomial,
 * <var>p</var>(<var>t</var>) = <var>c</var><sub>0</sub> + <var>c</var><sub>1</sub><var>t</var> + … +
 * <var>c</var><sub><var>n</var></sub><var>t</var><sup><var>n</var></sup>.
 *
 * <p>Difference with ISO 19107, which declares this data type as implementing {@code RealFunction}:
 * this interface does not extend {@link RealFunction} in Java, but declares the same members.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.7.6
 */
@UML(identifier="RealPolynomial", specification=ISO_19107)
public interface RealPolynomial {

    /**
     * Locally defined identifier of this polynomial.
     *
     * @return name of this polynomial.
     *
     * @see ISO 19107:2019 - 7.7.6.2
     */
    @UML(identifier="name", specification=ISO_19107)
    GenericName getName();

    /**
     * Interval over which this polynomial is defined.
     *
     * @return domain of this polynomial.
     *
     * @see ISO 19107:2019 - 7.7.6.2
     */
    @UML(identifier="domain", specification=ISO_19107)
    Range getDomain();

    /**
     * References to the documentation describing this polynomial.
     *
     * @return metadata of this polynomial, possibly empty.
     *
     * @see ISO 19107:2019 - 7.7.6.2
     */
    @UML(identifier="metadata", specification=ISO_19107)
    List<URI> getMetadata();

    /**
     * Degree of this polynomial, i.e. the highest power of the variable used.
     *
     * @return degree of this polynomial.
     *
     * @see ISO 19107:2019 - 7.7.6.3
     */
    @UML(identifier="degree", specification=ISO_19107)
    int getDegree();

    /**
     * Coefficients of this polynomial, from the constant term to the term of highest degree.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The array length is {@link #getDegree()} + 1.</li>
     * </ul>
     *
     * @return coefficients of this polynomial.
     *
     * @see ISO 19107:2019 - 7.7.6.4
     */
    @UML(identifier="c", specification=ISO_19107)
    double[] getC();

    /**
     * Returns the value of this polynomial for the given real number.
     *
     * @param  r  value in the {@linkplain #getDomain() domain} of this polynomial.
     * @return value of this polynomial at the given number.
     *
     * @see ISO 19107:2019 - 7.7.6.5
     */
    @UML(identifier="value", specification=ISO_19107)
    double value(double r);
}
