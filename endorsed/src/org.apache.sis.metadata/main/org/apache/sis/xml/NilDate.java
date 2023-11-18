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
package org.apache.sis.xml;

import java.util.Date;
import java.io.ObjectStreamException;


/**
 * An empty {@code Date} which is nil for the given reason.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class NilDate extends Date implements NilObject {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4374532826187673813L;

    /**
     * The reason why the object is nil.
     */
    private final NilReason reason;

    /**
     * Creates a new international string which is nil for the given reason.
     */
    NilDate(final NilReason reason) {
        super(0);
        this.reason = reason;
    }

    /**
     * Returns the reason why this object is nil.
     */
    @Override
    public NilReason getNilReason() {
        return reason;
    }

    /**
     * Unconditionally returns en empty string.
     */
    @Override
    public String toString() {
        return "";
    }

    /**
     * Invoked on deserialization for replacing the deserialized instance by the unique instance.
     */
    private Object readResolve() throws ObjectStreamException {
        return reason.createNilObject(Date.class);
    }
}
