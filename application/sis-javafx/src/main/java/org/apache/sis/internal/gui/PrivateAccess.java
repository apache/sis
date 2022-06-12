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
package org.apache.sis.internal.gui;

import java.util.function.Consumer;
import java.util.function.BiFunction;
import org.apache.sis.gui.coverage.CoverageExplorer;
import org.apache.sis.gui.dataset.WindowHandler;


/**
 * Accessor for fields that we want to keep private for now.
 * This is a way to simulate the behavior of {@code friend} keyword in C++.
 * Each field shall be set only once in a static block initializer and shall
 * not be modified after initialization.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final class PrivateAccess {
    /**
     * Do not allow instantiation of this class.
     */
    private PrivateAccess() {
    }

    /**
     * Accessor for {@link org.apache.sis.gui.dataset.WindowHandler.ForCoverage} constructor.
     * Used for assigning {@link CoverageExplorer#window} when duplicating an existing window.
     * Shall be invoked in JavaFX thread.
     */
    public static volatile BiFunction<WindowHandler, CoverageExplorer, WindowHandler> newWindowHandler;

    /**
     * Accessor for {@link WindowHandler#finish()} method. Shall be invoked in JavaFX thread.
     */
    public static volatile Consumer<WindowHandler> finishWindowHandler;
}
