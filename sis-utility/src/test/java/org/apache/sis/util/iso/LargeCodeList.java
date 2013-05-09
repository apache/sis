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
package org.apache.sis.util.iso;

import java.util.List;
import java.util.ArrayList;
import org.opengis.util.CodeList;

import static org.junit.Assert.*;


/**
 * A code list containing more than 64 elements. This implementation can be used by tests
 * that requires a large amount of code list elements.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@SuppressWarnings("serial")
public final strictfp class LargeCodeList  extends CodeList<LargeCodeList> {
    /**
     * List of all enumerations of this type.
     */
    private static final List<LargeCodeList> VALUES = new ArrayList<LargeCodeList>(100);

    /**
     * Creates 100 code list elements.
     */
    static {
        for (int i=0; i<100; i++) {
            assertEquals(i, new LargeCodeList(i).ordinal());
        }
    }

    /**
     * Constructs an element. The new element is automatically
     * added to the list to be returned by {@link #values}.
     */
    private LargeCodeList(final int i) {
        super("LC#" + i, VALUES);
    }

    /**
     * Returns the list of {@code LargeCodeList}s.
     *
     * @return The list of codes declared in the current JVM.
     */
    public static LargeCodeList[] values() {
        synchronized (VALUES) {
            return VALUES.toArray(new LargeCodeList[VALUES.size()]);
        }
    }

    /**
     * Returns the list of codes of the same kind than this code list element.
     */
    @Override
    public LargeCodeList[] family() {
        return values();
    }

    /**
     * Returns the axis code that matches the given string,
     * or returns a new one if none match it.
     *
     * @param code The name of the code list element to fetch or to create.
     * @return A code list element matching the given name.
     */
    public static LargeCodeList valueOf(final String code) {
        return valueOf(LargeCodeList.class, code);
    }
}
