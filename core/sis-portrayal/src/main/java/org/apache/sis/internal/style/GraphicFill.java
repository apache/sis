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
import java.util.List;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.GraphicFill}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GraphicFill extends Graphic implements org.opengis.style.GraphicFill {

    public GraphicFill() {
    }

    public GraphicFill(List<GraphicalSymbol> graphicalSymbols,
            Expression<Feature, Number> opacity,
            Expression<Feature, Number> size,
            Expression<Feature, Number> rotation,
            AnchorPoint anchorPoint,
            Displacement displacement) {
        super(graphicalSymbols, opacity, size, rotation, anchorPoint, displacement);
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj)
            && obj instanceof GraphicFill;
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static GraphicFill castOrCopy(org.opengis.style.GraphicFill candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof GraphicFill) {
            return (GraphicFill) candidate;
        }
        final List<org.apache.sis.internal.style.GraphicalSymbol> cs = new ArrayList<>();
        for (org.opengis.style.GraphicalSymbol cr : candidate.graphicalSymbols()) {
            cs.add(org.apache.sis.internal.style.GraphicalSymbol.castOrCopy(cr));
        }
        return new GraphicFill(
                cs,
                candidate.getOpacity(),
                candidate.getSize(),
                candidate.getRotation(),
                AnchorPoint.castOrCopy(candidate.getAnchorPoint()),
                Displacement.castOrCopy(candidate.getDisplacement()));
    }
}
