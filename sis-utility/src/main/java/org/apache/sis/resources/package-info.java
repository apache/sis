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
 * Localized resources for SIS. While anyone could use the resources provided in this package,
 * those resources are primarily for internal SIS usage and may change in any future version.
 *
 * <p>Apache SIS resources are provided in binary files having the "{@code .utf}" extension.
 * The resource keys are numeric constants declared in the {@code Keys} static inner classes.
 * Values are strings which may optionally have slots for one or more parameters, identified
 * by the "<code>{</code><var>n</var><code>}</code>" characters sequences where <var>n</var>
 * is the parameter number (first parameter is "<code>{0}</code>").
 * If, and only if, a string value has slots for at least one parameter, then:</p>
 *
 * <ul>
 *   <li>the key name ends with the {@code '_'} character followed by the expected number of parameters;</li>
 *   <li>the value string will be compliant with the {@link java.text.MessageFormat} syntax.</li>
 * </ul>
 *
 * <p>Apache SIS developers can add resources by editing the {@code *.properties} file
 * in the source code directory, then run the localized resources compiler provided in the
 * <code><a href="{@website}/sis-build-helper/index.html">sis-build-helper</a></code> module.
 * Developers shall <strong>not</strong> apply the {@code MessageFormat} rules for using quotes,
 * since the resources compiler will apply itself the <cite>doubled single quotes</cite> when
 * necessary. This avoid the unfortunate confusion documented in the warning section of
 * {@link java.text.MessageFormat} javadoc.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-1.2)
 * @version 0.3
 * @module
 *
 * @see java.util.ResourceBundle
 * @see java.text.MessageFormat
 */
package org.apache.sis.resources;
