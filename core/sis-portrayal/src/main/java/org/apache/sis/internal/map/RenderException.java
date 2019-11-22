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
package org.apache.sis.internal.map;

import org.apache.sis.util.ArgumentChecks;

/**
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public class RenderException extends Exception {

    public RenderException(final String message) {
        super(message);
        ArgumentChecks.ensureNonEmpty("message", message);
    }

    public RenderException(final Throwable throwable) {
        this(((throwable.getMessage() == null) ? "No message" : throwable.getMessage()), throwable);
    }

    public RenderException(final String message, final Throwable throwable) {
        super(message, throwable);
        ArgumentChecks.ensureNonEmpty("message", message);
    }
}
