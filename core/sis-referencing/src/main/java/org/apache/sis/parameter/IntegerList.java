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
package org.apache.sis.parameter;

import java.lang.reflect.Array;
import javax.xml.bind.annotation.XmlValue;
import org.apache.sis.util.CharSequences;


/**
 * XML representation of a sequence of integer values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see org.apache.sis.internal.jaxb.gml.MeasureList
 */
final class IntegerList {
    /**
     * The integer values.
     */
    @XmlValue
    public String value;

    /**
     * Default empty constructor for JAXB. The value is initialized to null,
     * but JAXB will overwrite that value if a XML value is present.
     */
    public IntegerList() {
    }

    /**
     * Creates a list of integers backed by the given array.
     *
     * @param array The integer values as a Java array.
     */
    public IntegerList(final Object array) {
        final StringBuilder builder = new StringBuilder();
        final int length = Array.getLength(array);
        for (int i=0; i<length; i++) {
            if (i != 0) builder.append(' ');
            builder.append(Array.get(array, i));
        }
        value = builder.toString();
    }

    /**
     * Returns the values as an array.
     */
    public int[] toArray() {
        return CharSequences.parseInts(value, ' ', 10);
    }
}
