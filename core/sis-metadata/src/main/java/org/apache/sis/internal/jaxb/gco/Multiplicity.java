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
package org.apache.sis.internal.jaxb.gco;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.sis.measure.NumberRange;


/**
 * The possible cardinality of a relation. Represented by a set of simple multiplicity ranges.
 * Example:
 *
 * {@preformat xml
 *   <gco:Multiplicity>
 *     <gco:range>
 *       <gco:MultiplicityRange>
 *         <gco:lower>
 *           <gco:Integer>1</gco:Integer>
 *         </gco:lower>
 *         <gco:upper>
 *           <gco:UnlimitedInteger isInfinite="false">15</gco:UnlimitedInteger>
 *         </gco:upper>
 *       </gco:MultiplicityRange>
 *     </gco:range>
 *   </gco:Multiplicity>
 * }
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@XmlType(name = "Multiplicity_Type")
public final class Multiplicity {
    /**
     * The multiplicity, defined as ranges of integers.
     */
    @XmlElementWrapper(name = "Multiplicity", required = true)
    @XmlElement(name = "range", required = true)
    @XmlJavaTypeAdapter(GO_MultiplicityRange.class)
    private List<NumberRange<Integer>> range;

    /**
     * Creates a new multiplicity initialized with no ranges.
     */
    public Multiplicity() {
    }

    /**
     * Creates a new multiplicity initialized with the given ranges.
     *
     * @param  ranges  the ranges.
     */
    @SafeVarargs
    public Multiplicity(final NumberRange<Integer>... ranges) {
        this.range = Arrays.asList(ranges);
    }

    /**
     * Returns the multiplicity as a live list (change to this list are reflected to this object).
     *
     * @return the multiplicity ranges as a live list.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<NumberRange<Integer>> range() {
        if (range == null) {
            range = new ArrayList<>();
        }
        return range;
    }
}
