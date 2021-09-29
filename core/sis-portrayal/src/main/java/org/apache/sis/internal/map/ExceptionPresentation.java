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

import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.feature.Feature;


/**
 * Produced by the portrayal engines when an exception occurred.
 * Exception presentations are placed in the Stream of presentation leaving
 * the user the choice to log, ignore or stop rendering as needed.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public class ExceptionPresentation extends Presentation {

    private final Exception exception;

    /**
     * @param exception not null.
     */
    public ExceptionPresentation(Exception exception) {
        ArgumentChecks.ensureNonNull("exception", exception);
        this.exception = exception;
    }

    /**
     * @param exception not null.
     */
    public ExceptionPresentation(MapLayer layer, Resource resource, Feature candidate, Exception exception) {
        super(layer,resource, candidate);
        ArgumentChecks.ensureNonNull("exception", exception);
        this.exception = exception;
    }

    /**
     * @return exception, never null
     */
    public Exception getException() {
        return exception;
    }
}
