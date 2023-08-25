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
package org.apache.sis.buildtools.gradle;

import java.util.Map;
import java.util.HashMap;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.apache.sis.buildtools.maven.Generator;


/**
 * Extension to Gradle Maven publishing tasks.
 * The publication name must be the JPMS module name without the {@code org.apache.sis} prefix.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
final class ModularPublishing {
    /**
     * The project to publish.
     */
    private final Project project;

    /**
     * The publications to handle as dependencies for other publications.
     * Keys are JPMS module names.
     *
     * @todo It should not be a static field. But I didn't found another way
     *       to get the compilation results of other sub-projects.
     */
    private static final Map<String,Dependency> publications = new HashMap<>();

    /**
     * Creates a helper instance.
     *
     * @param  project  the project to publish.
     */
    ModularPublishing(final Project project) {
        this.project = project;
    }

    /**
     * Gets the list of publications and register code for rewriting the {@code pom.xml} files.
     */
    final void configure() {
        final PublishingExtension ext = project.getExtensions().findByType(PublishingExtension.class);
        if (ext != null) {
            ext.getPublications().configureEach((pub) -> {
                if (pub instanceof MavenPublication) {
                    final var mp  = (MavenPublication) pub;
                    final var dep = new Dependency(mp);
                    final MavenPom pom = mp.getPom();
                    synchronized (publications) {
                        publications.put(dep.module, dep);
                        pom.withXml((xml) -> Generator.rewrite(project, publications.values(), mp, xml));
                    }
                }
            });
        }
    }
}
