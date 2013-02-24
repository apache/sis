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
package com.sun.xml.internal.bind.marshaller;


/**
 * A placeholder for an internal class from Sun/Oracle JDK 6. This class is provided only for
 * compilation of {@link org.apache.sis.xml.OGCNamespacePrefixMapper}, and its {@code .class}
 * file is deleted after compilation by the {@code maven-antrun-plugin} task in the
 * {@code sis-utility/pom.xml} file.
 *
 * <p>The {@code NamespacePrefixMapper} class is part of Sun/Oracle JDK since version 6,
 * but is considered internal API. For that reason, the JDK class is intentionally hidden
 * by {@code javac} unless the non-standard {@code -XDignore.symbol.file} option is given
 * to the compiler. We could declare a system dependency to the {@code rt.jar} file in the
 * {@code pom.xml}, but this is specific to the Sun/Oracle JDK and would not work with JDK 8.
 * Providing a skeleton class here, and deleting the {@code NamespacePrefixMapper.class} file
 * after compilation, is the most portable approach we have found.</p>
 *
 * <p>Note that we do not declare any method in this class. This is not needed if we make sure
 * that the method signatures in {@code OGCNamespacePrefixMapper} match the ones in the JDK.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-3.0)
 * @version 0.3
 * @module
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-74">SIS-74</a>
 */
public abstract class NamespacePrefixMapper {
}
