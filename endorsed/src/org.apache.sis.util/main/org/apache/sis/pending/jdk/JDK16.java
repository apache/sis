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
package org.apache.sis.pending.jdk;

import java.util.List;
import java.util.stream.Stream;
import java.util.logging.LogRecord;
import org.apache.sis.util.internal.UnmodifiableArrayList;


/**
 * Place holder for some functionalities defined in a JDK more recent than Java 11.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class JDK16 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK16() {
    }

    /**
     * Place holder for {@link Stream#toList()} method added in JDK16.
     *
     * @param  <T>  type of elements in the stream.
     * @param  s    the stream to convert to a list.
     * @return the stream content as a list.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(final Stream<T> s) {
        return (List<T>) UnmodifiableArrayList.wrap(s.toArray());
    }

    /**
     * Place holder for {@code LogRecord.getLongThreadID()}.
     *
     * @param  record  the record for which to get the thread identifier.
     * @return thread identifier of the given record.
     */
    @SuppressWarnings("deprecation")
    public static long getLongThreadID(final LogRecord record) {
        return record.getThreadID();
    }

    /**
     * Returns {@code true} if the thread identifier of the given record is probably equal to the given identifier.
     * This method reproduces the hashing algorithm used in {@code LogRecord.shortThreadID(long)} private method.
     *
     * @param  record    the record for which to compare the thread identifier.
     * @param  threadId  the thread identifier to compare.
     * @return whether the thread identifier of the given record is probably equal to the given value.
     */
    @SuppressWarnings("deprecation")
    public static boolean isSameThread(final LogRecord record, final long threadId) {
        int hash = Long.hashCode(threadId);
        if (threadId < 0 || threadId > Integer.MAX_VALUE) {
            if (hash >= 0) hash = ~hash;        // Makes it negative.
        }
        return record.getThreadID() == hash;
    }
}
