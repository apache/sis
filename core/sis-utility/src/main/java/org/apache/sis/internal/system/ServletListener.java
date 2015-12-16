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
package org.apache.sis.internal.system;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;


/**
 * Listener for servlet context, used as a shutdown hook when the application is undeployed.
 * This class should not be used directly.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see OSGiActivator
 */
@WebListener("Apache SIS shutdown hook")
public final class ServletListener implements ServletContextListener {
    /**
     * Receives notification that the web application initialization process is starting.
     *
     * @param event The context of the servlet being initialized.
     */
    @Override
    public void contextInitialized(final ServletContextEvent event) {
        final String env = event.getServletContext().getServerInfo();
        Shutdown.setContainer(env != null ? env : "Servlet");
    }

    /**
     * Receives notification that the application is about to be shutdown.
     *
     * @param event The context of the servlet being shutdown.
     */
    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        try {
            Shutdown.stop(getClass());
        } catch (Exception e) {
            event.getServletContext().log(e.toString(), e);
        }
    }
}
