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
package org.apache.sis.storage.geopose.binding;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Model describing how a pose evolves between two consecutive poses of a sequence.
 *
 * <p>The transition model is carried by the header of a series or of a stream, and applies to
 * every interval of that sequence.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition TransitionModel"
 */
@JsonPropertyOrder({
    TransitionModel.JSON_PROPERTY_AUTHORITY,
    TransitionModel.JSON_PROPERTY_ID,
    TransitionModel.JSON_PROPERTY_PARAMETERS
})
public class TransitionModel extends SpecificationReference {

    public TransitionModel() {
    }

    public TransitionModel(final String authority, final String id, final String parameters) {
        super(authority, id, parameters);
    }
}
