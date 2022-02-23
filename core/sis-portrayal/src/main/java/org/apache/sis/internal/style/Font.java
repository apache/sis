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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.Font}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Font implements org.opengis.style.Font {

    private final List<Expression> family = new ArrayList<>();
    private Expression<Feature,String> style;
    private Expression<Feature,String> weight;
    private Expression<Feature,? extends Number> size;

    public Font() {
        this(Collections.EMPTY_LIST,
            StyleFactory.DEFAULT_FONT_STYLE,
            StyleFactory.DEFAULT_FONT_WEIGHT,
            StyleFactory.DEFAULT_FONT_SIZE);
    }

    public Font(List<Expression> family, Expression<Feature, String> style, Expression<Feature, String> weight, Expression<Feature, ? extends Number> size) {
        if (family != null) this.family.addAll(family);
        this.style = style;
        this.weight = weight;
        this.size = size;
    }

    @Override
    public List<Expression> getFamily() {
        return family;
    }

    @Override
    public Expression<Feature, String> getStyle() {
        return style;
    }

    public void setStyle(Expression<Feature, String> style) {
        this.style = style;
    }

    @Override
    public Expression<Feature, String> getWeight() {
        return weight;
    }

    public void setWeight(Expression<Feature, String> weight) {
        this.weight = weight;
    }

    @Override
    public Expression<Feature, ? extends Number> getSize() {
        return size;
    }

    public void setSize(Expression<Feature, ? extends Number> size) {
        this.size = size;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(family, style, weight, size);
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
        final Font other = (Font) obj;
        return Objects.equals(this.family, other.family)
            && Objects.equals(this.style, other.style)
            && Objects.equals(this.weight, other.weight)
            && Objects.equals(this.size, other.size);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Font castOrCopy(org.opengis.style.Font candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Font) {
            return (Font) candidate;
        }
        return new Font(
                candidate.getFamily(),
                candidate.getStyle(),
                candidate.getWeight(),
                candidate.getSize());
    }
}
