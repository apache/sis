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
package org.apache.sis.internal.metadata;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Indicates that the value of a property is computed from values of other properties.
 * This annotation can be applied on getter methods.  All dependent properties must be
 * in the same class than the annotated method. Transitive dependencies do not need to
 * be declared, but the dependency graph shall not contain cycle.
 *
 * <div class="note"><b>Example:</b>
 * {@code ResponsibleParty.individualName} is now deprecated and replaced by the first {@code Individual.name} value
 * found in {@code Responsibility.party} list. Consequently, the {@code DefaultResponsibleParty.getIndividualName()}
 * method is annotated with {@code @Dependencies("getParties")} where {@code getParties()} is a method inherited from
 * the parent class.
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependencies {
    /**
     * Names of other Java method required for computing the annotated property.
     * Should be Java method names rather than UML identifier, in order to avoid ambiguity when a
     * property has both a singular and a plural form (usually with the singular form deprecated).
     *
     * @return other properties in the same class required for computation, not including transitive dependencies.
     */
    String[] value();
}
