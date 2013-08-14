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


/**
 * Merges the binaries produced by <code>JarCollector</code> and compress them using Pack200.
 * This mojo delegates the work to <code>Packer</code>, which can be invoked from the command
 * line without Maven. Maven invocation syntax is:
 *
 * <blockquote><code>mvn org.apache.sis:sis-build-helper:pack --non-recursive</code></blockquote>
 *
 * Do not forget the <code>--non-recursive</code> option, otherwise the Mojo will be executed many time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 *
 * @goal pack
 * @phase install
 */
public class BundleCreator extends AbstractMojo {
    /**
     * The Apache SIS version, without branch name.
     */
    private static final String VERSION = "0.3";

    /**
     * The Apache SIS branch for which this plugin is creating a bundle, prefixed by {@code '-'}.
     * This is declared as a separated constant in order to make easier to update {@link #VERSION}
     * without creating conflicts during branch merges.
     */
    private static final String BRANCH = "";

    /**
     * The root directory (without the "<code>target/binaries</code>" sub-directory) where JARs
     * are to be copied. It should be the directory of the root <code>pom.xml</code>.
     *
     * @parameter property="session.executionRootDirectory"
     * @required
     */
    private String rootDirectory;

    /**
     * Creates the Pack200 files from the JAR files collected in the "<code>target/binaries</code>" directory.
     *
     * @throws MojoExecutionException if the plugin execution failed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        final File targetDirectory = new File(rootDirectory, JarCollector.TARGET_DIRECTORY);
        if (!targetDirectory.isDirectory()) {
            throw new MojoExecutionException("Directory not found: " + targetDirectory);
        }
        try {
            final String fullVersion = VERSION + BRANCH;
            final Packer packer = new Packer(targetDirectory, fullVersion);
            packer.addPack("apache-sis-" + fullVersion + ".jar");
            try {
                packer.createJars();
            } finally {
                packer.close();
            }
            packer.pack();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }
}
