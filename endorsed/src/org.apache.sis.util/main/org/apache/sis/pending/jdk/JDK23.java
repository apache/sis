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

import java.time.Duration;
import java.time.Instant;


/**
 * Place holder for some functionalities defined in a JDK more recent than Java 11.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class JDK23 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK23() {
    }

    /**
     * Placeholder for {@code Instant.until(Instant)}.
     * The method provided in Java 23 is optimized compared to the more generic {@code Duration.between} method.
     * The purpose of this placeholder is only for remembering to do the substitution.
     *
     * @param  start the start time.
     * @param  endExclusive  the end time.
     * @return duration between the two times.
     */
    public static Duration until(Instant start, Instant endExclusive) {
        return Duration.between(start, endExclusive);
    }
}
