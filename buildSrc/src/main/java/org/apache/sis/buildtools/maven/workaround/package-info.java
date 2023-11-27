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
 * Generates {@code META-INF/services} files with the content of {@code module-info.class} files.
 * This is used for compatibility when a modularized dependency is declared on the class-path
 * rather than the module-path. This need occurs when dependencies are automatically dispatched
 * between {@code --class-path} and {@code --module-path} options by a tool on which we have no
 * control, such as Maven 3.8.6 or Gradle 8.2.1.
 *
 * <p>Note that this workaround does not fix the real issue,
 * which is that dependencies are loaded as unnamed modules when they should not.
 * The workaround allows libraries and applications to find some service providers despite this problem,
 * sometime not in the way that the providers should be (because of wrappers).
 * But any other features that depend on named modules are still broken.</p>
 *
 * <p>The classes in this package are not used during Apache SIS builds.
 * They are used only if the content of some {@code module-info.java} files changes
 * and the {@code META-INF/services/*} files need to be regenerated.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @see <a href="https://issues.apache.org/jira/browse/MNG-7855">MNG-7855 on JIRA issue tracker</a>
 */
package org.apache.sis.buildtools.maven.workaround;
