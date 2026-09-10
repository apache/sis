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
 * A well-defined mapping from an interval of real numbers onto the real numbers.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.7.2
 */
@UML(identifier="RealFunction", specification=ISO_19107)
public interface RealFunction {

    /**
     * Locally defined identifier of this function.
     *
     * @return name of this function.
     *
     * @see ISO 19107:2019 - 7.7.2.2
     */
    @UML(identifier="name", specification=ISO_19107)
    GenericName getName();

    /**
     * Interval over which this function is defined.
     *
     * @return domain of this function.
     *
     * @see ISO 19107:2019 - 7.7.2.3
     */
    @UML(identifier="domain", specification=ISO_19107)
    Range getDomain();

    /**
     * References to the documentation describing this function.
     *
     * @return metadata of this function, possibly empty.
     *
     * @see ISO 19107:2019 - 7.7.2.4
     */
    @UML(identifier="metadata", specification=ISO_19107)
    List<URI> getMetadata();

    /**
     * Returns the value of this function for the given real number.
     *
     * @param  r  value in the {@linkplain #getDomain() domain} of this function.
     * @return value of this function at the given number.
     *
     * @see ISO 19107:2019 - 7.7.2.5
     */
    @UML(identifier="value", specification=ISO_19107)
    double value(double r);
}
