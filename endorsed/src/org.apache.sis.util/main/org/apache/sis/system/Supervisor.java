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
package org.apache.sis.system;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
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
import javax.management.InstanceAlreadyExistsException;
import java.lang.management.ManagementFactory;
import org.apache.sis.setup.About;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.collection.TreeTable;


/**
 * A central place where to monitor library-wide information through a MBean. For example
 * we register every {@link org.apache.sis.util.collection.WeakHashSet} created as static
 * variables.  The MBean interface should allow administrators to know the cache size and
 * eventually perform some operations like clearing a cache.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Supervisor extends StandardMBean implements SupervisorMBean {
    /**
     * Whatever JMX agent is enabled. Setting this variable to {@code false} allows the
     * Java compiler to omit any dependency to this {@code Supervisor} class.
     */
    @Configuration
    static final boolean ENABLED = false;

    /**
     * The JMX object name for the {@code Supervisor} service.
     */
    public static final String NAME = "org.apache.sis:type=Supervisor";

    /**
     * The JMX object name, created when the {@link #register()} is first invoked.
     * {@link ObjectName#WILDCARD} is used as a sentinel value if the registration failed.
     */
    private static volatile ObjectName name;

    /**
     * Registers the {@code Supervisor} instance, if not already done.
     * If the supervisor has already been registered but has not yet been
     * {@linkplain #unregister() unregistered}, then this method does nothing.
     *
     * <p>If the registration fails, then this method logs a message at the warning level
     * and the MBean will not be registered. This method does not propagate the exception
     * because the MBean is not a mandatory part of SIS library.</p>
     */
    @Configuration(writeAccess = Configuration.Access.INTERNAL)
    public static synchronized void register() {
        if (name == null) {
            name = ObjectName.WILDCARD;                         // In case of failure.
            final LogRecord record;
            try {
                final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                final ObjectName n = new ObjectName(NAME);
                server.registerMBean(new Supervisor(), n);
                name = n;                                    // Store only on success.
                return;
            } catch (InstanceAlreadyExistsException e) {
                record = Messages.forLocale(null).createLogRecord(Level.CONFIG, Messages.Keys.AlreadyRegistered_2, "MBean", NAME);
            } catch (JMException e) {
                record = new LogRecord(Level.WARNING, e.toString());
                record.setThrown(e);
            } catch (SecurityException e) {
                record = new LogRecord(Level.CONFIG, e.toString());
            }
            Logging.completeAndLog(SystemListener.LOGGER, Supervisor.class, "register", record);
        }
    }

    /**
     * Unregister the {@code Supervisor} instance. This method does nothing if the supervisor
     * has not been previously successfully {@linkplain #register() registered}, or if it has
     * already been unregistered.
     *
     * @throws JMException if an error occurred during unregistration.
     */
    @Configuration(writeAccess = Configuration.Access.INTERNAL)
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
     * Creates a new {@code Supervisor}.
     *
     * @throws NotCompliantMBeanException should never happen.
     */
    public Supervisor() throws NotCompliantMBeanException {
        super(SupervisorMBean.class);
    }

    /**
     * Returns the operations impact, which is {@code INFO}.
     *
     * @return {@code INFO}.
     */
    @Override
    protected int getImpact(final MBeanOperationInfo info) {
        return MBeanOperationInfo.INFO;
    }

    /**
     * Returns the localized description for this MBean.
     *
     * @return a localized description.
     */
    @Override
    protected String getDescription(final MBeanInfo info) {
        return getDescription("supervisor");
    }

    /**
     * Returns the localized description for the given constructor, attribute or operation.
     *
     * @return a localized description for the given attribute or operation.
     */
    @Override
    protected String getDescription(final MBeanFeatureInfo info) {
        return getDescription(info.getName());
    }

    /**
     * Returns the localized description for the given constructor parameter.
     *
     * @param  info      the constructor.
     * @param  param     the constructor parameter.
     * @param  sequence  the parameter number (0 for the first parameter, 1 for the second, etc.)
     * @return a localized description for the specified constructor parameter.
     */
    @Override
    protected String getDescription(MBeanConstructorInfo info, MBeanParameterInfo param, int sequence) {
        return getDescription(getParameterName(info, param, sequence));
    }

    /**
     * Returns the name of the given constructor parameter.
     *
     * @param  info      the constructor.
     * @param  param     the constructor parameter.
     * @param  sequence  the parameter number (0 for the first parameter, 1 for the second, etc.)
     * @return the name of the specified constructor parameter.
     */
    @Override
    protected String getParameterName(MBeanConstructorInfo info, MBeanParameterInfo param, int sequence) {
        return "locale";
    }

    /**
     * Returns the string from the {@code Descriptions} resource bundle for the given key.
     */
    private String getDescription(final String resourceKey) {
        return ResourceBundle.getBundle("org.apache.sis.system.Descriptions",
                Locale.getDefault(Locale.Category.DISPLAY),
                Supervisor.class.getClassLoader()).getString(resourceKey);
    }

    // -----------------------------------------------------------------------
    //               Implementation of SupervisorMBean interface
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeTable configuration(final EnumSet<About> sections, final Locale locale, final TimeZone timezone) {
        return About.configuration(sections, locale, timezone);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] warnings(final Locale locale) {
        final DaemonThread lastCreatedDaemon;
        synchronized (Threads.class) {
            lastCreatedDaemon = Threads.lastCreatedDaemon;
        }
        final List<Thread> threads = DaemonThread.listStalledThreads(lastCreatedDaemon);
        if (threads == null) {
            return null;
        }
        final String[] warnings = new String[threads.size()];
        final Errors resources = Errors.forLocale(locale);
        for (int i=0; i<warnings.length; i++) {
            final Thread thread = threads.get(i);
            warnings[i] = resources.getString(thread.isAlive() ?
                    Errors.Keys.StalledThread_1 : Errors.Keys.DeadThread_1, thread.getName());
        }
        return warnings;
    }
}
