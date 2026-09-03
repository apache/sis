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
package org.apache.sis.geometries;

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Orientable", specification=ISO_19107) // section 6.4.15
public interface Orientable extends Primitive {

    public static enum Sign {
        POSITIVE,
        NEGATIVE
    }

    /**
     * Returns whether this geometry is traversed in the direction of its underlying primitive
     * ({@link Sign#POSITIVE}) or in the opposite direction ({@link Sign#NEGATIVE}).
     *
     * <p>The default is {@link Sign#POSITIVE}, which is what a geometry defined directly by its own
     * coordinates is: only a geometry that stands for the reverse of another one reports
     * {@link Sign#NEGATIVE}.</p>
     */
    @UML(identifier="orientation", specification=ISO_19107) // section 6.4.15.2
    default Sign getOrientationSign(){
        return Sign.POSITIVE;
    }

    @UML(identifier="proxy", specification=ISO_19107) // section 6.4.15.3
    default Orientable getProxy(){
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="primitive", specification=ISO_19107) // section 6.4.15.3
    default Primitive getPrimitive(){
        //TODO
        throw new UnsupportedOperationException();
    }

    // called opposite on figure 16
    @UML(identifier="reverse", specification=ISO_19107) // section 6.4.15.3
    default Orientable getReverse(){
        //TODO
        throw new UnsupportedOperationException();
    }

}
