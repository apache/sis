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

import java.util.logging.Level;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.filter.internal.shared.WarningEvent;


/**
 * Forwards to resource listeners the warnings that occurred during the execution of expressions and filters.
 * This is a bridge between the private filter/expression warning system to public resource warning system.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WarningAdapter implements Consumer<WarningEvent> {
    /**
     * The listeners where to send the warnings.
     */
    private final StoreListeners listeners;

    /**
     * Creates a new adapter from filter warnings to resource warnings.
     *
     * @param  listeners  the listeners where to send the warnings.
     */
    public WarningAdapter(final StoreListeners listeners) {
        this.listeners = listeners;
    }

    /**
     * Invoked when a warning occurred during the execution of filters or expressions.
     *
     * @param  event  the warning event.
     */
    @Override
    public void accept(final WarningEvent event) {
        listeners.warning(event.recoverable ? Level.FINE : Level.WARNING, null, event.exception);
    }

    /**
     * Executes the given action with a redirection of all warnings to the given listeners.
     *
     * @todo Replace by {@code ScopedValue.call(…)} when allowed to use JDK25.
     *
     * @param  <V>        the return value type of the given action.
     * @param  action     the action to execute.
     * @param  listeners  the listeners where to send the warnings.
     * @return the return value of the given action.
     */
    public static <V> V execute(final Supplier<V> action, final StoreListeners listeners) {
        final ThreadLocal<Consumer<WarningEvent>> context = WarningEvent.LISTENER;
        final Consumer<WarningEvent> old = context.get();
        try {
            context.set(new WarningAdapter(listeners));
            return action.get();
        } finally {
            context.set(old);
        }
    }
}
