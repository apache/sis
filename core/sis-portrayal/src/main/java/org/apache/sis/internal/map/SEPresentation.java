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
package org.apache.sis.internal.map;

import java.util.Objects;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.storage.Resource;
import org.opengis.feature.Feature;
import org.opengis.style.Symbolizer;

/**
 * A presentation build with a standard Symbology Encoding Symbolizer.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final class SEPresentation extends Presentation {

    private Symbolizer symbolizer;

    public SEPresentation() {
    }

    public SEPresentation(MapLayer layer, Resource resource, Feature candidate, Symbolizer symbolizer) {
        super(layer, resource, candidate);
        this.symbolizer = symbolizer;
    }

    /**
     * @return Symbogy Encoding symbolizer
     */
    public Symbolizer getSymbolizer() {
        return symbolizer;
    }

    public void setSymbolizer(Symbolizer symbolizer) {
        this.symbolizer = symbolizer;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.symbolizer);
        return hash;
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
        final SEPresentation other = (SEPresentation) obj;
        if (!Objects.equals(this.symbolizer, other.symbolizer)) {
            return false;
        }
        return super.equals(obj);
    }

}
