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
package org.apache.sis.internal.processing.isoline;

import java.awt.Shape;
import java.util.AbstractList;
import java.util.NavigableMap;
import java.util.concurrent.Future;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.apache.sis.util.resources.Errors;


/**
 * Deferred isoline result, created when computation is continuing in background.
 * The {@link Future} result is requested the first time that {@link #get(int)} is invoked.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class Result extends AbstractList<NavigableMap<Double,Shape>> {
    /**
     * The task computing isolines result. Reset to {@code null} when no longer needed.
     */
    private Future<Isolines[]> task;

    /**
     * The result of {@link Future#get()} fetched when first needed.
     */
    private NavigableMap<Double,Shape>[] isolines;

    /**
     * Creates a new list for the given future isolines.
     */
    Result(final Future<Isolines[]> task) {
        this.task = task;
    }

    /**
     * Fetches the isolines from the {@link Future} if not already done.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private NavigableMap<Double,Shape>[] isolines() {
        if (isolines == null) {
            if (task == null) {
                throw new CompletionException(Errors.format(Errors.Keys.BackgroundComputationFailed), null);
            }
            try {
                isolines = Isolines.toArray(task.get());
                task = null;
            } catch (InterruptedException e) {
                // Do not clear `task`: the result may become available later.
                throw new CompletionException(Errors.format(Errors.Keys.InterruptedWhileWaitingResult), e);
            } catch (ExecutionException e) {
                task = null;
                throw new CompletionException(Errors.format(Errors.Keys.BackgroundComputationFailed), e.getCause());
            }
        }
        return isolines;
    }

    /**
     * Returns the list length, which is the number of bands.
     */
    @Override public int size() {
        return isolines().length;
    }

    /**
     * Returns the isolines in the given band.
     */
    @Override public NavigableMap<Double,Shape> get(final int band) {
        return isolines()[band];
    }

    /**
     * Returns the list content as an array.
     */
    @Override public Object[] toArray() {
        return isolines().clone();
    }
}
