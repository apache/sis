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
package org.opengis.annotation;


/**
 * Workaround for a bug in the JDK6 Javadoc tools.
 * Attempt to generate Javadoc on the Apache SIS trunk with JDK6 crashes with the following exception:
 *
 * <pre>java.lang.ClassCastException: com.sun.tools.javadoc.MethodDocImpl cannot be cast to com.sun.tools.javadoc.AnnotationTypeElementDocImpl</pre>
 *
 * The problem is caused by existence of the <code>@UML</code> annotation, defined in GeoAPI.
 * Adding dependency to the GeoAPI JAR file does not help. The workaround applied here is to
 * redefine the UML annotation without the <code>@Documented</code> meta-annotation.
 * The {@code UML} annotation defined here should have precedence over the GeoAPI annotation
 * at Javadoc generation time only (not at compile-time).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public @interface UML {
    /**
     * The UML identifier for the annotated interface, method or code list element.
     *
     * @return The UML identifier used in the standard.
     */
    String identifier();

    /**
     * The obligation declared in the UML.
     *
     * @return The obligation declared in the standard.
     */
    Obligation obligation() default Obligation.MANDATORY;

    /**
     * The specification where this UML come from.
     *
     * @return The originating specification.
     */
    Specification specification();
}
