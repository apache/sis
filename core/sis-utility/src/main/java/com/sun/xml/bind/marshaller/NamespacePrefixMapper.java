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
package com.sun.xml.bind.marshaller;


/**
 * A copy of {@code com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper} in the package
 * used by the JAXB version <strong>not</strong> bundled with the JDK. This class is identical
 * to the one bundled with JDK 6 except that the package name does not contains the
 * "{@code internal}" part. Some servers like Glassfish uses this JAXB implementation instead
 * than the one bundled in JDK 6, so we must be able to support both.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-3.0)
 * @version 0.3
 * @module
 *
 * @see com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper
 */
public abstract class NamespacePrefixMapper {
}
