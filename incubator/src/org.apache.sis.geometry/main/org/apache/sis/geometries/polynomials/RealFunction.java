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
package org.apache.sis.geometries.polynomials;

import java.net.URI;
import java.util.List;
import org.apache.sis.measure.Range;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="RealFunction", specification=ISO_19107) // section 7.7.2
public interface RealFunction {

    @UML(identifier="name", specification=ISO_19107) // section 7.7.2.2
    GenericName getName();

    @UML(identifier="domain", specification=ISO_19107) // section 7.7.2.3
    Range getDomain();

    @UML(identifier="metadata", specification=ISO_19107) // section 7.7.2.4
    List<URI> getMetadata();

    @UML(identifier="value", specification=ISO_19107) // section 7.7.2.5
    double value(double r);
}
