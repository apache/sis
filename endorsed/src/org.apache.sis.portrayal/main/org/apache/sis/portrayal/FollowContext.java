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
package org.apache.sis.portrayal;

import java.util.Map;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;


/**
 * Contextual information about an "objective to display" transform change which is in progress.
 * We use one instance per thread on the assumption that events are processed in the same thread,
 * at least between {@link PlanarCanvas} instances that are connected in the same graph.
 * This condition is documented in the {@link CanvasFollower} class Javadoc.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see CanvasFollower#CONTEXT
 */
final class FollowContext {
    /**
     * Whether to add {@link CanvasFollower} instances to the {@link #deferred} queue
     * instead of executing their {@code propagate(…)} method.
     *
     * @see CanvasFollower#propagate(TransformChangeEvent)
     */
    private boolean propagateLater;

    /**
     * Listeners for which the call to {@code propagate(…)} has been deferred to a later time.
     * This is needed when we have two or more {@link CanvasFollower} instances registered on
     * the same source canvas but for different target canvases.
     *
     * <h4>Use case</h4>
     * Consider a case where a change in canvas <var>A</var> is propagated to canvas <var>B</var>
     * which in turn propagates its own change to canvas <var>C</var>. If canvas <var>A</var> has
     * another {@code CanvasFollower} propagating the same change directly to <var>C</var>, it is
     * better to give precedence to the latter because it is more direct (it avoids to propagate
     * a transformation of another transformation). But because of the order in which a tree of
     * listeners is executed, we need a mechanism if which the execution of a branch is deferred.
     *
     * @see CanvasFollower#propagate(TransformChangeEvent)
     */
    private final Queue<CanvasFollower> deferred;

    /**
     * Canvases in which a change of "objective to display" transform has already been propagated.
     * This is used for avoiding never-ending loops if two or more instances of {@link CanvasFollower}
     * result in a cyclic graph of {@code PlanarCanvas}.
     */
    private final Map<PlanarCanvas, Boolean> propagated;

    /**
     * Creates an empty context.
     */
    FollowContext() {
        deferred   = new ArrayDeque<>(4);       // There is usually not many instances.
        propagated = new IdentityHashMap<>(4);
    }

    /**
     * Resets this {@code FollowContext} to the same state as after construction.
     */
    final void clear() {
        propagateLater = false;
        deferred.clear();
        propagated.clear();
    }

    /**
     * Returns whether a {@code TransformChangeEvent} is already in process of being propagated.
     * A return value of {@code true} means that the caller is handling events that were fired as
     * a consequence of the original event, or are listeners notified after the first listener.
     */
    final boolean isPropagating(final CanvasFollower follower) {
        if (propagated.isEmpty()) {
            propagated.put(follower.source, Boolean.TRUE);
            return false;
        }
        return true;
    }

    /**
     * Executes {@code follower.propagate(…)} immediately or adds it to a queue of methods to be invoked later.
     * The purpose is to execute {@code propagate(…)} in a different order than the usual tree traversal order.
     * We want all siblings to be executed before to traverse the children.
     *
     * <p>This method does nothing if the target canvas has already been notified.</p>
     *
     * @param  follower  the follower for which to execute or defer the call to {@code propagate(…)}.
     * @param  event     the event to propagate if it needs to be done immediately.
     */
    final void propagateOrDefer(final CanvasFollower follower, final TransformChangeEvent event) {
        if (propagated.put(follower.target, Boolean.FALSE) == null) {
            if (propagateLater) {
                deferred.add(follower);
            } else {
                propagateLater = true;
                follower.propagate(event);
                propagateLater = false;
            }
        }
    }

    /**
     * Executes all {@code follower.propagate(…)} calls that were deferred.
     * This method should be invoked after all siblings have been processed.
     * Target canvases that have already been processed are ignored.
     *
     * @param  event  the event to propagate.
     */
    final void executeDeferred(final TransformChangeEvent event) {
        CanvasFollower follower;
        while ((follower = deferred.poll()) != null) {
            if (propagated.put(follower.target, Boolean.FALSE) == null) {
                follower.propagate(event);
            }
        }
    }
}
