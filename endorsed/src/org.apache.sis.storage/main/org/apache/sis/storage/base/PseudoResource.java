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
package org.apache.sis.storage.base;

import java.util.Locale;
import org.opengis.metadata.Metadata;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.Localized;


/**
 * A pseudo-resource used as a way to specify listeners to the {@link AbstractResource} constructor.
 * Instances of this class are short-lived: no reference should be stored.
 * This pseudo-resource should never be visible to users.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PseudoResource extends AbstractResource implements Localized {
    /**
     * Creates a new instance wrapping the given listeners.
     *
     * @param  listeners  the listeners that {@link AbstractResource} should take, or {@code null}.
     */
    public PseudoResource(final StoreListeners listeners) {
        super(listeners, true);
    }

    /**
     * Returns the locale for formatting messages, or {@code null} if unspecified.
     *
     * @return the locale for messages (typically specified by the data store), or {@code null} if unknown.
     */
    @Override
    public Locale getLocale() {
        return listeners.getLocale();
    }

    /**
     * Ignored.
     *
     * @return {@code null}.
     */
    @Override
    protected Metadata createMetadata() {
        return null;
    }
}
