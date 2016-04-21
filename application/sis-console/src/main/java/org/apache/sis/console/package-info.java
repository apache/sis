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

/**
 * Command line interface for Apache SIS.
 * See {@link org.apache.sis.console.Command} for the list of supported commands.
 *
 * <div class="section">SIS installation on remote machines</div>
 * Some sub-commands can operate on SIS installation on remote machines, provided that Apache SIS
 * has been <a href="http://sis.apache.org/branches.html#trunk">compiled with MBeans enabled</a>
 * and that the remote Java Virtual Machine has been started with the following options:
 *
 * {@preformat shell
 *   java -Dcom.sun.management.jmxremote.port=1099 \
 *        -Dcom.sun.management.jmxremote.authenticate=false \
 *        -Dcom.sun.management.jmxremote.ssl=false \
 *        -Dcom.sun.management.jmxremote.local.only=true \
 *        <other options>
 * }
 *
 * If the port number is different than {@value java.rmi.registry.Registry#REGISTRY_PORT}, then it must be specified
 * to the {@code sis} subcommand after the host name. For example if the port number has been set to 9999, then the
 * {@code about} sub-command shall be invoked as below:
 *
 * {@preformat shell
 *   java org.apache.sis.console.Command about localhost:1099
 * }
 *
 * The {@code com.sun.management.jmxremote.local.only} property is recommended if the remote JVM is an other
 * JVM instance running on the local machine. Otherwise this property can be omitted for debugging purpose.
 * For production environment, see the security settings documented on the
 * <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/management/agent.html">Monitoring
 * and Management Using JMX Technology</a> page.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
package org.apache.sis.console;
