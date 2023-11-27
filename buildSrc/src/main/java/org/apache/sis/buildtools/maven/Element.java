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
package org.apache.sis.buildtools.maven;


/**
 * An XML element to read from the original {@code pom.xml} file content.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum Element {
    /**
     * The group of the artifact to publish.
     */
    GROUB_ID("<groupId>", "${sis.groupId}"),

    /**
     * The identifier of the artifact to publish.
     */
    ARTIFACT_ID("<artifactId>", "${sis.artifactId}"),

    /**
     * The version of the artifact to publish.
     */
    VERSION("<version>", "${sis.version}"),

    /**
     * Human-readable name of the artifact to publish.
     */
    NAME("<name>", "${sis.name}"),

    /**
     * Human-readable description of the artifact to publish.
     */
    DESCRIPTION("<description>", "${sis.description}");

    /**
     * The XML tag where to read or write the element value.
     */
    final String tag;

    /**
     * The placeholder for the value to write.
     */
    final String property;

    /**
     * Creates a new enumeration value.
     *
     * @param tag       XML tag where to read or write the element value.
     * @param property  placeholder for the value to write.
     */
    private Element(final String tag, final String property) {
        this.tag      = tag;
        this.property = property;
    }
}
