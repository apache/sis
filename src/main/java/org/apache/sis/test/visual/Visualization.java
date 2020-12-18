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
package org.apache.sis.test.visual;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;


/**
 * Base class for tests on widgets.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public abstract class Visualization {
    /**
     * The type of the object being tested.
     */
    final Class<?> testing;

    /**
     * Number of invocation of {@link #create(int)} to perform.
     */
    final int numTests;

    /**
     * Creates a new instance of {@code Visualization} which will invoke {@link #create(int)} only once.
     *
     * @param  testing  the type of object to be tested.
     */
    protected Visualization(final Class<?> testing) {
        this(testing, 1);
    }

    /**
     * Creates a new instance of {@code Visualization}.
     *
     * @param  testing   the type of object to be tested.
     * @param  numTests  number of invocation of {@link #create(int)} to perform.
     */
    protected Visualization(final Class<?> testing, final int numTests) {
        this.testing  = testing;
        this.numTests = numTests;
    }

    /**
     * Creates a widget showing the object to test.
     *
     * @param  index  index of test occurrence, from 0 inclusive to the value given at construction time, exclusive.
     * @return a widget showing the object to test.
     */
    protected abstract JComponent create(int index);

    /**
     * Creates and shows a widget visualizing the object to test.
     */
    public final void show() {
        SwingUtilities.invokeLater(() -> DesktopPane.INSTANCE.addAndShow(this));
    }
}
