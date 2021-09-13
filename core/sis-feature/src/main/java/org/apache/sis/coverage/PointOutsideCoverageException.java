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
package org.apache.sis.coverage;


/**
 * Thrown when an evaluate method is invoked for a location outside the domain of the coverage.
 *
 * <div class="note"><b>Upcoming API change:</b>
 * this class may move to GeoAPI in a future version. If that move happens,
 * the {@code org.apache.sis.coverage} package name would become {@code org.opengis.coverage}.</div>
 */
public class PointOutsideCoverageException extends CannotEvaluateException {
    /**
     * Creates an exception with no message.
     */
    public PointOutsideCoverageException() {
        super();
    }

    /**
     * Creates an exception with the specified message.
     *
     * @param  message  the detail message. The detail message is saved for
     *         later retrieval by the {@link #getMessage()} method.
     */
    public PointOutsideCoverageException(final String message) {
        super(message);
    }
}
