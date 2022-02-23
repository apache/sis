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
package org.apache.sis.internal.style;

import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.ContrastMethod;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.ContrastEnhancement}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ContrastEnhancement implements org.opengis.style.ContrastEnhancement {

    private ContrastMethod method;
    private Expression<Feature, ? extends Number> gammaValue;

    public ContrastEnhancement() {
        this(ContrastMethod.NONE, StyleFactory.LITERAL_ONE);
    }

    public ContrastEnhancement(ContrastMethod method, Expression<Feature, ? extends Number> gammaValue) {
        ArgumentChecks.ensureNonNull("method", method);
        ArgumentChecks.ensureNonNull("gammaValue", gammaValue);
        this.method = method;
        this.gammaValue = gammaValue;
    }

    @Override
    public ContrastMethod getMethod() {
        return method;
    }

    public void setMethod(ContrastMethod method) {
        ArgumentChecks.ensureNonNull("method", method);
        this.method = method;
    }

    @Override
    public Expression<Feature, ? extends Number> getGammaValue() {
        return gammaValue;
    }

    public void setGammaValue(Expression<Feature,? extends Number> gammaValue) {
        ArgumentChecks.ensureNonNull("gammaValue", gammaValue);
        this.gammaValue = gammaValue;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, gammaValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ContrastEnhancement other = (ContrastEnhancement) obj;
        return Objects.equals(this.method, other.method)
            && Objects.equals(this.gammaValue, other.gammaValue);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static ContrastEnhancement castOrCopy(org.opengis.style.ContrastEnhancement candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof ContrastEnhancement) {
            return (ContrastEnhancement) candidate;
        }
        return new ContrastEnhancement(candidate.getMethod(), candidate.getGammaValue());
    }
}
