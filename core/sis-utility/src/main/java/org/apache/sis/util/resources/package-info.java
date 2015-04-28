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
 *   <li>the value string is compliant with the {@link java.text.MessageFormat} syntax.</li>
 * </ul>
 *
 * <div class="note"><b>Note:</b>
 * {@link java.util.Formatter java.util.Formatter} is an alternative to {@link java.text.MessageFormat} providing
 * similar functionalities with a C/C++ like syntax. However {@code MessageFormat} has two advantages: it provides
 * a {@code choice} format type (useful for handling plural forms), and localizes properly objects of unspecified type
 * (by contrast, the {@code Formatter} {@code "%s"} type always invoke {@code toString()}). The later advantage is
 * important for messages in which the same argument could receive {@link java.lang.Number} or {@link java.util.Date}
 * instances as well as {@link java.lang.String}. Furthermore, the {@link java.util.logging} framework is designed for
 * use with {@code MessageFormat} (see the {@code Formatter.formatMessage(LogRecord)} method).</div>
 *
 * Apache SIS developers can add resources by editing the {@code *.properties} file in the source code directory,
 * then run the localized resources compiler provided in the {@code sis-build-helper} module.
 * Developers shall <strong>not</strong> apply the {@code MessageFormat} rules for using quotes,
 * since the resources compiler will apply itself the <cite>doubled single quotes</cite> when
 * necessary. This avoid the unfortunate confusion documented in the warning section of
 * {@link java.text.MessageFormat} javadoc.
 *
 * <div class="section">Usage</div>
 * All {@link org.apache.sis.util.resources.IndexedResourceBundle} subclasses provide a
 * {@code getResources(Locale)} static method. It can be used for fetching localized strings
 * as below:
 *
 * {@preformat java
 *     String text = TheBundle.getResources(locale).getString(key, optionalArguments);
 * }
 *
 * For convenience, all {@code IndexedResourceBundle} subclasses provide also various {@code format(int, …)} static
 * methods for fetching localized texts in the {@linkplain java.util.Locale#getDefault() system default locale}:
 *
 * {@preformat java
 *     text = TheBundle.format(key, optionalArguments); // Uses the default locale.
 * }
 *
 * If the locale is not known at method invocation time, {@code formatInternational(int, …)} static methods
 * returns a localizable string which can be localized later:
 *
 * {@preformat java
 *     InternationalString i18n = TheBundle.formatInternational(key, optionalArguments);
 *     String text = i18n.toString(locale); // Localize now.
 * }
 *
 * If optional arguments are present, then the following types are handled in a special way
 * (non exhaustive list):
 *
 * <ul>
 *   <li>{@link java.lang.Number}, {@link java.util.Date}, {@link org.opengis.util.CodeList} and
 *       {@link org.opengis.util.InternationalString} instances are localized using the current
 *       {@code ResourceBundle} locale.</li>
 *   <li>Long {@link java.lang.CharSequence} instances are shortened.</li>
 *   <li>{@link java.lang.Class} and {@link java.lang.Throwable} instances are summarized.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see java.util.ResourceBundle
 * @see java.text.MessageFormat
 * @see org.apache.sis.util.iso.ResourceInternationalString
 */
package org.apache.sis.util.resources;
