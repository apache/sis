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
package org.apache.sis.pending.geoapi.temporal;

import java.util.Optional;


/**
 * Placeholder for GeoAPI 3.1 interfaces (not yet released).
 * Shall not be visible in public API, as it will be deleted after next GeoAPI release.
 */
@SuppressWarnings("doclint:missing")
public enum TemporalOperatorName {
    AFTER, BEFORE(AFTER), BEGINS, BEGUN_BY(BEGINS), CONTAINS, DURING(CONTAINS),
    EQUALS, OVERLAPS, MEETS, ENDS,
    OVERLAPPED_BY(OVERLAPS), MET_BY(MEETS), ENDED_BY(ENDS), ANY_INTERACTS;

    static {
        EQUALS.reversed = EQUALS;
        ANY_INTERACTS.reversed = ANY_INTERACTS;
    }

    private TemporalOperatorName reversed;

    private TemporalOperatorName() {
    }

    private TemporalOperatorName(TemporalOperatorName r) {
        reversed = r;
        r.reversed = this;
    }

    public String identifier() {
        return name().toLowerCase();
    }

    public Optional<TemporalOperatorName> reversed() {
        return Optional.of(reversed);
    }
}
