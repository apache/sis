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
package org.apache.sis.web.jakarta;

import jakarta.ejb.Startup;
import jakarta.ejb.Singleton;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.sis.system.Shutdown;


/**
 * Listener for Enterprise JavaBeans, used as a shutdown hook when the <abbr>EJB</abbr> is undeployed.
 * This class should not be used directly.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Startup
@Singleton
public final class JavaBeanHook {
    /**
     * Invoked by reflection.
     */
    public JavaBeanHook() {
    }

    /**
     * Notifies Apache <abbr>SIS</abbr> that the application is initializing.
     */
    @PostConstruct
    public void initialize() {
        Shutdown.setContainer("EJB (Jakarta)");
    }

    /**
     * Receives notification that the application is about to be shutdown.
     */
    @PreDestroy
    public void shutdown() {
        try {
            Shutdown.stop(getClass());
        } catch (Exception e) {
            Shutdown.log(getClass(), e);
        }
    }
}
