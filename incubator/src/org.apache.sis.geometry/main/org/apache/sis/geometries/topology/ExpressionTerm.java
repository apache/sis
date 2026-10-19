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
package org.apache.sis.geometries.topology;

import java.util.Collection;
import java.util.List;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A term of an {@link Expression}: an oriented primitive with an integer coefficient, such as
 * 2<var>e₁</var>. A term is a monomial, so it is also an expression of a single term.
 * Arithmetic on terms is the usual vector arithmetic.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.1.9
 */
@UML(identifier="ExpressionTerm", specification=ISO_19107)
public interface ExpressionTerm extends Expression {

    /**
     * The integer multiplying the {@linkplain #getVariable() variable}. The default is 1.
     *
     * @return coefficient of this term.
     *
     * @see ISO 19107:2019 - 10.1.9
     */
    @UML(identifier="coefficient", specification=ISO_19107)
    default int getCoefficient() {
        return 1;
    }

    /**
     * The oriented primitive of this term.
     *
     * @return variable of this term.
     *
     * @see ISO 19107:2019 - 10.1.9
     */
    @UML(identifier="variable", specification=ISO_19107)
    DirectedTopo getVariable();

    /**
     * Returns this term alone, or nothing if its {@linkplain #getCoefficient() coefficient} is zero.
     *
     * @return this term, or an empty list for the zero expression.
     */
    @Override
    default Collection<ExpressionTerm> getTerms() {
        return (getCoefficient() == 0) ? List.of() : List.of(this);
    }

}
