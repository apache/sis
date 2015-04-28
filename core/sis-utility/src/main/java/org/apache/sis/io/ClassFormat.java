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
package org.apache.sis.io;

import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import org.apache.sis.util.Classes;


/**
 * Used by {@link CompoundFormat} for formatting the name of objects of type {@link Class}.
 *
 * <div class="section">Thread safety</div>
 * The same {@link #INSTANCE} can be safely used by many threads without synchronization on the part of the caller.
 * Note that this is specific to {@code ClassFormat} and generally not true for arbitrary {@code Format} classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class ClassFormat extends Format {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2321788892790539107L;

    /**
     * The unique instance.
     */
    static final ClassFormat INSTANCE = new ClassFormat();

    /**
     * For the unique {@link #INSTANCE} only.
     */
    private ClassFormat() {
    }

    /**
     * Formats the given class. The given {@code obj} must be an instance of {@link Class},
     * otherwise a {@link ClassCastException} will be thrown.
     */
    @Override
    public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
        return toAppendTo.append(Classes.getShortName((Class<?>) obj));
    }

    /**
     * Can not parse unqualified class name.
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    /**
     * Resolves to the singleton instance on deserialization.
     */
    private Object readResolve() {
        return INSTANCE;
    }
}
