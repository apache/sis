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
 * Generates Maven POM from a template file. We do not use Gradle's API for this task
 * (e.g. the setter methods in {@link org.gradle.api.publish.maven.MavenPom}) because
 * as of Gradle 8.2.1, they do not provide sufficient control over the file content.
 * In particular we didn't found the way to specify the parent POM,
 * neither how to control the list of dependencies.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
package org.apache.sis.buildtools.maven;
