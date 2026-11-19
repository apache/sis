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
package org.apache.sis.process;

import java.util.EventListener;


/**
 * Receives general indications of {@linkplain Process process} progress and events indicating
 * start, completion or failure. For any {@linkplain Process#addListener registered listeners},
 * the {@link Process#call()} method shall invoke at least the {@link #started started} and
 * {@link #completed completed} or {@link #failed failed} methods (mandatory). Calls to the
 * {@link #progressing progressing} method are optional but recommended.
 *
 * @author Johann Sorel (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public interface ProcessListener extends EventListener {
    /**
     * Reports that a process is beginning. {@link Process} implementations
     * are required to call this method exactly once.
     *
     * @param event The progress event.
     */
    void started(ProcessEvent event);

    /**
     * Invoked at any time during process progression. {@link Process} implementations
     * may invoke this method an arbitrary number of time. In addition to the source,
     * task name and progress indicator, the event argument may contain the following
     * optional information:
     * <ul>
     *   <li>{@link ProcessEvent#getOutput()}, if non-null, indicates an intermediate calculation value.</li>
     *   <li>{@link ProcessEvent#getException()}, if non-null, indicates a non-fatal warning.</li>
     * </ul>
     *
     * @param event The progress event.
     */
    void progressing(ProcessEvent event);

    /**
     * Reports that a process has been dismissed.
     *
     * @param event The progress event.
     */
    void dismissed(ProcessEvent event);

    /**
     * Reports that a process has been paused. In addition to the
     * source and task name, the event argument may contain the following optional information:
     * <ul>
     *   <li>{@link ProcessEvent#getOutput()}, if non-null, indicates the calculation state before
     *       the pause.</li>
     * </ul>
     *
     * @param event The progress event.
     */
    void paused(ProcessEvent event);

    /**
     * Reports that a process has been resumed after a pause.
     *
     * @param event The progress event.
     */
    void resumed(ProcessEvent event);

    /**
     * Reports that a process has completed successfully. {@link Process} implementations
     * are required to call either this method or {@link #failed(ProcessEvent)} exactly once.
     * In addition to the source and task name, the event argument shall contain the following
     * information:
     * <ul>
     *   <li>{@link ProcessEvent#getOutput()} is the final result returned by {@link Process#call()}.</li>
     * </ul>
     *
     * @param event The progress event.
     */
    void completed(ProcessEvent event);

    /**
     * Reports that a process has failed. {@link Process} implementations are required to call
     * either this method or {@link #completed(ProcessEvent)} exactly once. In addition to the
     * source and task name, the event argument may contain the following optional information:
     * <ul>
     *   <li>{@link ProcessEvent#getException()}, if non-null, indicates the failure cause.</li>
     *   <li>{@link ProcessEvent#getOutput()}, if non-null, indicates the calculation state before
     *       the failure.</li>
     * </ul>
     *
     * @param event The progress event.
     */
    void failed(ProcessEvent event);
}
