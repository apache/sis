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
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.ColorMap}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ColorMap implements org.opengis.style.ColorMap {

    private Expression<Feature,?> function;

    public ColorMap() {
    }

    public ColorMap(Expression<Feature, ?> function) {
        this.function = function;
    }

    @Override
    public Expression<Feature,?> getFunction() {
        return function;
    }

    public void setFunction(Expression<Feature, ?> function) {
        this.function = function;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(function);
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
        final ColorMap other = (ColorMap) obj;
        return Objects.equals(this.function, other.function);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static ColorMap castOrCopy(org.opengis.style.ColorMap candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof ColorMap) {
            return (ColorMap) candidate;
        }
        return new ColorMap(candidate.getFunction());
    }
}
