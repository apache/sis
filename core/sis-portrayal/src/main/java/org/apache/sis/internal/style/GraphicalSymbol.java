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

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public interface GraphicalSymbol extends org.opengis.style.GraphicalSymbol {

    public static GraphicalSymbol castOrCopy(org.opengis.style.GraphicalSymbol candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof org.opengis.style.Mark) {
            return Mark.castOrCopy((org.opengis.style.Mark) candidate);
        } else if (candidate instanceof org.opengis.style.ExternalGraphic) {
            return ExternalGraphic.castOrCopy((org.opengis.style.ExternalGraphic) candidate);
        } else {
            throw new IllegalArgumentException("Unexpected symbol type " + candidate.getClass().getName());
        }
    }

}
