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
package org.apache.sis.internal.maven;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.project.MavenProject;

import static org.apache.sis.internal.maven.Filenames.*;


/**
 * Merges the binaries produced by <code>JarCollector</code> and compress them using Pack200.
 * This mojo can be invoked from the command line as below:
 *
 * <blockquote><code>mvn org.apache.sis.core:sis-build-helper:pack --non-recursive</code></blockquote>
 *
 * Do not forget the <code>--non-recursive</code> option, otherwise the Mojo will be executed many time.
 *
 * <p><b>Current limitation:</b>
 * The current implementation uses some hard-coded paths and filenames.
 * See the <cite>Distribution file and Pack200 bundle</cite> section in the <code>src/site/apt/index.apt</code>
 * file for more information.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@Mojo(name = "pack", defaultPhase = LifecyclePhase.INSTALL)
public class BundleCreator extends AbstractMojo {
    /**
     * Project information (name, version, URL).
     */
    @Parameter(property="project", required=true, readonly=true)
    private MavenProject project;

    /**
     * The root directory (without the "<code>target/binaries</code>" sub-directory) where JARs
     * are to be copied. It should be the directory of the root <code>pom.xml</code>.
     */
    @Parameter(property="session.executionRootDirectory", required=true)
    private String rootDirectory;

    /**
     * Creates the Pack200 file from the JAR files collected in the "<code>target/binaries</code>" directory.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        final File targetDirectory = new File(rootDirectory, TARGET_DIRECTORY);
        if (!targetDirectory.isDirectory()) {
            throw new MojoExecutionException("Directory not found: " + targetDirectory);
        }
        final String version = project.getVersion();
        try {
            final Packer packer = new Packer(project.getName(), project.getUrl(), version, targetDirectory);
            packer.preparePack200(FINALNAME_PREFIX + version + ".jar").pack();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }
}
