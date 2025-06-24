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
package org.apache.sis.console;

import java.util.Date;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.IOException;
import java.rmi.registry.Registry;
import javax.management.JMX;
import javax.management.ObjectName;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import org.apache.sis.setup.About;
import org.apache.sis.util.Version;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.X364;
import org.apache.sis.system.Loggers;
import org.apache.sis.system.Supervisor;
import org.apache.sis.system.SupervisorMBean;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.io.stream.IOUtilities;


/**
 * The "about" subcommand.
 * By default this sub-command prints all information except the {@link About#LIBRARIES} section,
 * because the latter is considered too verbose. Some available options are:
 *
 * <ul>
 *   <li>{@code --brief}:   prints only Apache SIS version number.</li>
 *   <li>{@code --verbose}: prints all information including the libraries.</li>
 * </ul>
 *
 * <h2>About SIS installation on a remote machine</h2>
 * This sub-command can provide information about SIS installation on a remote machine,
 * provided that remote access has been enabled at the Java Virtual Machine startup time.
 * See {@link org.apache.sis.console} package javadoc for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class AboutCommand extends CommandRunner {
    /**
     * Creates the {@code "about"} sub-command.
     *
     * @param  commandIndex  index of the {@code arguments} element containing the {@code "about"} command name, or -1 if none.
     * @param  arguments     the command-line arguments provided by the user.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    AboutCommand(final int commandIndex, final Object[] arguments) throws InvalidOptionException {
        super(commandIndex, arguments, EnumSet.of(Option.LOCALE, Option.TIMEZONE, Option.ENCODING,
                Option.BRIEF, Option.VERBOSE, Option.HELP, Option.DEBUG));
    }

    /**
     * Prints the information to the output stream.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than an uncaught Java exception.
     * @throws Exception if an error occurred while executing the sub-command.
     */
    @Override
    public int run() throws Exception {
        DataDirectory.quiet();
        /*
         * Check the number of arguments, which can be 0 or 1. If present,
         * the argument is the name and port number of a remote machine.
         *
         * In the current implementation, the --brief option is supported only on the local machine.
         */
        final boolean brief = options.containsKey(Option.BRIEF);
        if (hasUnexpectedFileCount(0, brief ? 0 : 1)) {
            return Command.INVALID_ARGUMENT_EXIT_CODE;
        }
        String[] warnings = null;
        final String configuration;
        if (brief && files.isEmpty()) {
            configuration = Vocabulary.forLocale(locale).getString(
                    Vocabulary.Keys.Version_2, "Apache SIS", Version.SIS);
        } else {
            final EnumSet<About> sections = EnumSet.allOf(About.class);
            if (!options.containsKey(Option.VERBOSE)) {
                sections.remove(About.LIBRARIES);
            }
            if (files.isEmpty()) {
                /*
                 * Provide information about the local SIS installation.
                 */
                configuration = About.configuration(sections, locale, getTimeZone()).toString();
            } else {
                /*
                 * Provide information about a remote SIS installation. Those information are accessible
                 * only if explicitly enabled at JVM startup time.
                 *
                 * Tutorial: http://docs.oracle.com/javase/tutorial/jmx/remote/custom.html
                 */
                final String address = IOUtilities.toString(files.get(0));
                if (address == null) {
                    return Command.INVALID_ARGUMENT_EXIT_CODE;
                }
                final String path = toRemoteURL(address);
                final long time = System.nanoTime();
                final TreeTable table;
                try {
                    final JMXServiceURL url = new JMXServiceURL(path);
                    try (JMXConnector jmxc = JMXConnectorFactory.connect(url)) {
                        final MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
                        final SupervisorMBean bean = JMX.newMBeanProxy(mbsc, new ObjectName(Supervisor.NAME), SupervisorMBean.class);
                        table = bean.configuration(sections, locale, getTimeZone());
                        warnings = bean.warnings(locale);
                    }
                } catch (IOException e) {
                    error(Errors.format(Errors.Keys.CanNotConnectTo_1, path), e);
                    return Command.IO_EXCEPTION_EXIT_CODE;
                }
                /*
                 * Logs a message telling how long it took to receive the reply.
                 * Sometimes the delay gives a hint about the server charge.
                 */
                double delay = (System.nanoTime() - time) / (double) Constants.NANOS_PER_SECOND;   // In seconds.
                if (delay >= 0.1) {
                    final double scale = (delay >= 10) ? 1 : (delay >= 1) ? 10 : 100;
                    delay = Math.rint(delay * scale) / scale;
                }
                final LogRecord record = Messages.forLocale(locale).createLogRecord(Level.INFO,
                        Messages.Keys.ConfigurationOf_3, address, new Date(), delay);
                record.setLoggerName(Loggers.APPLICATION);
                Logging.completeAndLog(null, Command.class, "main", record);
                /*
                 * Replace the root node label from "Local configuration" to "Remote configuration"
                 * before to get the string representation of the configuration as a tree-table.
                 */
                table.getRoot().setValue(TableColumn.NAME,
                        Vocabulary.forLocale(locale).getString(Vocabulary.Keys.RemoteConfiguration));
                configuration = table.toString();
            }
        }
        out.println(configuration);
        if (warnings != null) {
            out.println();
            if (colors) {
                color(X364.BACKGROUND_RED);
                color(X364.BOLD);
                out.print(' ');
            }
            Vocabulary.forLocale(locale).appendLabel(Vocabulary.Keys.Warnings, out);
            if (colors) {
                out.print(' ');
                out.println(X364.RESET.sequence());
                color(X364.FOREGROUND_RED);
            } else {
                out.println();
            }
            for (final String warning : warnings) {
                out.println(warning);
            }
            color(X364.FOREGROUND_DEFAULT);
        }
        out.flush();
        return 0;
    }

    /**
     * Creates a {@code "service:jmx:rmi:///jndi/rmi://host:port/jmxrmi"} URL for the given host name.
     * The host name can optionally be followed by a port number.
     */
    static String toRemoteURL(final String host) {
        final StringBuilder buffer = new StringBuilder(60).append("service:jmx:rmi:///jndi/rmi://")
                .append(host, host.regionMatches(true, 0, "localhost", 0, 9) ? 9 : 0, host.length());
        if (host.lastIndexOf(':') < 0) {
            buffer.append(':').append(Registry.REGISTRY_PORT);
        }
        return buffer.append("/jmxrmi").toString();
    }
}
