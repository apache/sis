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
package org.apache.sis.storage.isobmff.base;

import org.apache.sis.storage.isobmff.Box;


/**
 * A box with a content that can be ignored.
 *
 * <h4>Container</h4>
 * The container can be the file or another box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class FreeSpace extends Box {
    /**
     * Numerical representation of the {@code "free"} box type.
     */
    public static final int BOXTYPE = ((((('f' << 8) | 'r') << 8) | 'e') << 8) | 'e';

    /**
     * Numerical representation of the {@code "skip"} box type.
     * This is an alternative type for this box.
     */
    public static final int SKIP = ((((('s' << 8) | 'k') << 8) | 'i') << 8) | 'p';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * Creates a new box with no data.
     */
    public FreeSpace() {
    }
}
