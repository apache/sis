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
package org.apache.sis.util;

import org.opengis.referencing.IdentifiedObject;


/**
 * May be thrown on attempt to use an object which has been corrupted by a previous operation.
 * Apache SIS throws this exception only on a <em>best effort</em> basis, when it detected an
 * object in an inconsistent state <em>after</em> the original problem.
 *
 * <div class="note"><b>Analogy:</b>
 * this exception has a similar goal than {@link java.util.ConcurrentModificationException}: to reduce the risk of
 * non-deterministic behavior at an undetermined time in the future after an event has compromised the data integrity.
 * Like {@code ConcurrentModificationException}, this {@code CorruptedObjectException} should be used only to detect
 * bugs; it would be wrong to write a program that depends on this exception for its correctness.
 *
 * <p>This exception is different than {@link AssertionError} in that {@code CorruptedObjectException} is not
 * necessarily caused by a bug in the library. An object may become corrupted because of external factors, as
 * illustrated in the use cases below.</p></div>
 *
 * Some use cases for this exception are:
 * <ul class="verbose">
 *   <li><b>Attempt to use an aborted calculation:</b><br>
 *   if an operation failed in the middle of a structural modification, some specific exception (<strong>not</strong>
 *   this {@code CorruptedObjectException}) should be thrown and the object discarded. But if the user does not discard
 *   the object and try to use it again, unpredictable behavior may happen. Some implementations are robust enough for
 *   detecting such unsafe usage: their methods may throw this {@code CorruptedObjectException} on attempt to use the
 *   object after the original failure.</li>
 *
 *   <li><b>Change in an “immutable” object:</b><br>
 *   some objects are expected to be immutable. For example the same Coordinate Reference System (CRS) instance is
 *   typically shared by thousands of objects. However {@link org.opengis.referencing.crs.CoordinateReferenceSystem}
 *   is an interface, Therefore, nothing prevent users from providing a mutable instance. For example if the value
 *   returned by {@link org.opengis.referencing.cs.CoordinateSystem#getDimension()} changes between two invocations,
 *   many objects that use that coordinate system will fall in an inconsistent state. If an operation detects such
 *   inconsistency, it may throw this {@code CorruptedObjectException}.</li>
 * </ul>
 *
 * <div class="section">Exception cause</div>
 * Since this exception may be thrown an undetermined amount of time after the data corruption, the root cause is
 * often unknown at this point. Sometime a more descriptive exception has been thrown earlier, but may have been
 * ignored by the user.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public class CorruptedObjectException extends RuntimeException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7595678373605419502L;

    /**
     * Constructs a new exception with no message.
     */
    public CorruptedObjectException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The detail message, or {@code null} if none.
     */
    public CorruptedObjectException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the name of the given object.
     *
     * @param object The corrupted object, or {@code null} if unknown.
     *
     * @since 0.6
     */
    public CorruptedObjectException(final IdentifiedObject object) {
        super(object != null ? String.valueOf(object.getName()) : null);
    }
}
