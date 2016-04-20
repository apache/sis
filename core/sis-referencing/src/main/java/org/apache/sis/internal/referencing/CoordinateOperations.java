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
package org.apache.sis.internal.referencing;

import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;


/**
 * The default coordinate operation factory, provided in a separated class for deferring class loading
 * until first needed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class CoordinateOperations extends SystemListener {
    /**
     * The factory.
     */
    private static volatile DefaultCoordinateOperationFactory factory;

    /**
     * For system listener only.
     */
    private CoordinateOperations() {
        super(Modules.REFERENCING);
    }

    /**
     * Discards the factory if the classpath changed.
     */
    static {
        add(new CoordinateOperations());
    }

    /**
     * Invoked when the classpath changed.
     */
    @Override
    protected void classpathChanged() {
        factory = null;
    }

    /**
     * Returns the factory.
     *
     * @return The system-wide factory.
     */
    public static DefaultCoordinateOperationFactory factory() {
        DefaultCoordinateOperationFactory c = factory;
        if (c == null) {
            // DefaultFactories.forBuildin(…) performs the necessary synchronization.
            factory = c = DefaultFactories.forBuildin(CoordinateOperationFactory.class, DefaultCoordinateOperationFactory.class);
        }
        return c;
    }
}
