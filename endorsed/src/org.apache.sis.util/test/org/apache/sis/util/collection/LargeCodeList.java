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
package org.apache.sis.util.collection;

import org.opengis.util.CodeList;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * A code list containing more than 64 elements. This implementation can be used by tests
 * that requires a large number of code list elements.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")
public final class LargeCodeList extends CodeList<LargeCodeList> {
    /**
     * Creates 100 code list elements.
     * We need to construct values with {@code valueOf(String)} instead of the constructor
     * because this package is not exported to GeoAPI. See {@link CodeList} class javadoc.
     */
    static {
        for (int i=0; i<80; i++) {
            assertEquals(i, valueOf("LC#" + i).ordinal());
        }
    }

    /**
     * Constructs an element.
     */
    private LargeCodeList(String name) {
        super(name);
    }

    /**
     * Returns the list of {@code LargeCodeList}s.
     *
     * @return the list of codes declared in the current JVM.
     */
    public static LargeCodeList[] values() {
        return values(LargeCodeList.class);
    }

    /**
     * Returns the list of codes of the same kind as this code list element.
     *
     * @return list of codes of {@code LargeCodeList} kind.
     */
    @Override
    public LargeCodeList[] family() {
        return values();
    }

    /**
     * Returns the code that matches the given string, or returns a new one if none match it.
     *
     * @param  code  the name of the code list element to fetch or to create.
     * @return a code list element matching the given name.
     */
    public static LargeCodeList valueOf(final String code) {
        return valueOf(LargeCodeList.class, code, LargeCodeList::new).get();
    }
}
