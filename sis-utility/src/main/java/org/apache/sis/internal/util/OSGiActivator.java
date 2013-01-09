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
package org.apache.sis.internal.util;

import javax.management.JMException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;


/**
 * Bundle activator for OSGi environment.
 * This class should not be used directly.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class OSGiActivator implements BundleActivator, BundleListener {
    /**
     * Creates a new bundle activator.
     */
    public OSGiActivator() {
    }

    /**
     * Invoked when this bundle is started.
     *
     * @param context The execution context of the bundle being started.
     */
    @Override
    public void start(final BundleContext context) {
        context.addBundleListener(this);
    }

    /**
     * Invoked when this bundle is stopped.
     * This method shutdowns the {@code sis-utility} threads.
     *
     * @param  context The execution context of the bundle being stopped.
     * @throws JMException If an error occurred during unregistration of the supervisor MBean.
     */
    @Override
    public void stop(final BundleContext context) throws JMException {
        context.removeBundleListener(this);
        Shutdown.stop(getClass());
    }

    /**
     * Invoked when an other module has been installed or un-installed.
     * This method notifies the Apache SIS library that the classpath may have changed.
     *
     * @param event The event that describe the life-cycle change.
     */
    @Override
    public void bundleChanged(final BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
            case BundleEvent.STOPPED: {
                SystemListener.fireClasspathChanged();
            }
        }
    }
}
