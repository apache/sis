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
package org.apache.sis.web.javaee;

import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebListener;
import org.apache.sis.system.Shutdown;


/**
 * Copy of the {@linkplain org.apache.sis.web.jakarta.ServletListener listener for Jakarta}
 * using the JaveEE package name instead of Jakarta.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@WebListener("Apache SIS shutdown hook")
public final class ServletListener implements ServletContainerInitializer, ServletContextListener {
    /**
     * Invoked by reflection by the service loader.
     */
    public ServletListener() {
    }

    /**
     * Notifies Apache <abbr>SIS</abbr> that the application is initializing.
     *
     * @param handled  classes declared by {@link HandlesTypes}, or {@code null} if none.
     * @param context  context of the web application in which the classes were discovered.
     */
    @Override
    public void onStartup(final Set<Class<?>> handled, final ServletContext context) {
        context.addListener(this);
        final String env = context.getServerInfo();
        Shutdown.setContainer(env != null ? env : "Servlet");
    }

    /**
     * Receives notification that the application is about to be shutdown.
     *
     * @param  event  the context of the servlet being shutdown.
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
