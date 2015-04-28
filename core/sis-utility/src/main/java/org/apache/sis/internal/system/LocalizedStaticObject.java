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

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotates a static object using the {@linkplain java.util.Locale#getDefault() default locale}
 * and {@linkplain java.util.TimeZone#getDefault() default timezone} values which existed at the
 * object creation time.
 *
 * If JDK provided listeners allowing SIS to be notified about locale and timezone changes, we would
 * reset the annotated object to {@code null}. However since those listeners do not exist as of JDK7,
 * we use this annotation in the meantime for identifying the code which would need to be revisited
 * if we want to take in account default locale/timezone changes in a future SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface LocalizedStaticObject {
}
