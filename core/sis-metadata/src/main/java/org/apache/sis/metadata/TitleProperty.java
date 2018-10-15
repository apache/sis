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
package org.apache.sis.metadata;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Inherited;
import java.lang.annotation.Documented;


/**
 * Identifies the name of a property to use for summarizing in one line the content of a metadata object.
 * For example in a {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation citation} instance,
 * the {@code "title"} property is often the only information that a user needs for a first look.
 * This annotation is used in {@linkplain MetadataStandard#asTreeTable metadata tree views} for producing briefer trees,
 * especially when there is redundant node names.
 *
 * <div class="note"><b>Example:</b>
 * the {@code Citation} type contains a {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation#getDates() date}
 * property which itself contains another {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitationDate#getDate()
 * date} property. They form a tree like below:
 *
 * {@preformat text
 *   Citation
 *    ├─Title……………………… My document
 *    └─Date
 *       ├─Date………………… 2012/01/01
 *       └─Date type…… Creation
 * }
 *
 * With <code>&#64;TitleProperty(name="title")</code> on {@code DefaultCitation} implementation class and
 * <code>&#64;TitleProperty(name="date")</code> on {@code DefaultCitationDate} class,
 * Apache SIS can produce a more compact tree table view should be as below:
 *
 * {@preformat text
 *   Citation……………………… My document
 *    └─Date………………………… 2012/01/01
 *       └─Date type…… Creation
 * }
 * </div>
 *
 * The property referenced by this annotation should be the main property if possible, but not necessarily
 * since it may be only a label. However the property shall be a singleton ([0…1] or [1…1] multiplicity)
 * and can not be another metadata object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 *
 * @see ValueExistencePolicy#COMPACT
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TitleProperty {
    /**
     * Returns the name of the property to use as a title for a metadata object.
     * An empty value means that the metadata has no title property
     * (may be used for overriding a value inherited from the parent type).
     *
     * @return property name of the value to use as a title or summary sentence, or an empty value if none.
     */
    String name();
}
