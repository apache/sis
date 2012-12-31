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

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.MBeanServer;
import javax.management.MBeanInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.JMException;
import javax.management.NotCompliantMBeanException;
import java.lang.management.ManagementFactory;

import org.apache.sis.util.About;
import org.apache.sis.util.Localized;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.TreeTable;


/**
 * A central place where to monitor library-wide information through a MBean. For example
 * we register every {@link org.apache.sis.util.collection.WeakHashSet} created as static
 * variables.  The MBean interface should allow administrators to know the cache size and
 * eventually perform some operations like clearing a cache.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class Supervisor extends StandardMBean implements SupervisorMBean, Localized {
    /**
     * Whatever JMX agent is enabled. Setting this variable to {@code false} allows the
     * Java compiler to omit any dependency to this {@code Supervisor} class.
     */
    static final boolean ENABLED = true;

    /**
     * The JMX object name, created when the {@link #register()} is first invoked.
     * {@link ObjectName#WILDCARD} is used as a sentinel value if the registration failed.
     */
    private static ObjectName name;

    /**
     * Registers the {@code Supervisor} instance, if not already done.
     * If the supervisor has already been registered but has not yet been
     * {@linkplain #unregister() unregistered}, then this method does nothing.
     *
     * <p>If the registration fails, then this method logs a message at the warning level
     * and the MBean will not be registered. This method does not propagate the exception
     * because the MBean is not a mandatory part of SIS library.</p>
     */
    static synchronized void register() {
        if (name == null) {
            name = ObjectName.WILDCARD; // In case of failure.
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            try {
                final ObjectName n = new ObjectName("org.apache.sis:type=Supervisor");
                server.registerMBean(new Supervisor(Locale.getDefault()), n);
                name = n; // Store only on success.
            } catch (SecurityException | JMException e) {
                Logging.unexpectedException(Logger.getLogger("org.apache.sis"), Supervisor.class, "register", e);
            }
        }
    }

    /**
     * Unregister the {@code Supervisor} instance. This method does nothing if the supervisor
     * has not been previously successfully {@linkplain #register() registered}, or if it has
     * already been unregistered.
     *
     * @throws JMException If an error occurred during unregistration.
     */
    static synchronized void unregister() throws JMException {
        final ObjectName n = name;
        if (n != null) {
            name = null; // Clear even if the next line fail.
            if (n != ObjectName.WILDCARD) {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(n);
            }
        }
    }

    /**
     * The locale for producing the messages.
     */
    private final Locale locale;

    /**
     * Creates a new {@code Supervisor} which will report messages in the given locale.
     *
     * @param  locale The locale to use for reporting messages.
     * @throws NotCompliantMBeanException Should never happen.
     */
    public Supervisor(final Locale locale) throws NotCompliantMBeanException {
        super(SupervisorMBean.class);
        this.locale = locale;
    }

    /**
     * Returns the supervisor locale.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the operations impact, which is {@code INFO}.
     */
    @Override
    protected int getImpact(final MBeanOperationInfo info) {
        return MBeanOperationInfo.INFO;
    }

    /**
     * Returns the localized description for this MBean.
     */
    @Override
    protected String getDescription(final MBeanInfo info) {
        return getDescription("supervisor");
    }

    /**
     * Returns the localized description for the given constructor, attribute or operation.
     */
    @Override
    protected String getDescription(final MBeanFeatureInfo info) {
        return getDescription(info.getName());
    }

    /**
     * Returns the localized description for the given constructor parameter.
     *
     * @param info     The constructor.
     * @param param    The constructor parameter.
     * @param sequence The parameter number (0 for the first parameter, 1 for the second, etc.)
     */
    @Override
    protected String getDescription(MBeanConstructorInfo info, MBeanParameterInfo param, int sequence) {
        return getDescription(getParameterName(info, param, sequence));
    }

    /**
     * Returns the name of the given constructor parameter.
     *
     * @param info     The constructor.
     * @param param    The constructor parameter.
     * @param sequence The parameter number (0 for the first parameter, 1 for the second, etc.)
     */
    @Override
    protected String getParameterName(MBeanConstructorInfo info, MBeanParameterInfo param, int sequence) {
        return "locale";
    }

    /**
     * Returns the string from the {@code Descriptions} resource bundle for the given key.
     */
    private String getDescription(final String resourceKey) {
        return ResourceBundle.getBundle("org.apache.sis.internal.util.Descriptions",
                locale, Supervisor.class.getClassLoader()).getString(resourceKey);
    }

    // -----------------------------------------------------------------------
    //               Implementation of SupervisorMBean interface
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeTable configuration() {
        return About.configuration(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> warnings() {
        final List<String> warnings = Threads.listDeadThreads();
        if (warnings != null) {
            final Errors resources = Errors.getResources(locale);
            for (int i=warnings.size(); --i>=0;) {
                warnings.set(i, resources.getString(Errors.Keys.DeadThread_1, warnings.get(i)));
            }
        }
        return warnings;
    }
}
