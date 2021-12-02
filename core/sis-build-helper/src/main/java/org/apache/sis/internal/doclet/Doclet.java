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
package org.apache.sis.internal.doclet;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import javax.tools.Diagnostic;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.Doclet.Option;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.StandardDoclet;


/**
 * A doclet which delegates the work to the standard doclet, then performs additional actions.
 * The post-javadoc actions are:
 *
 * <ul>
 *   <li>Copy additional resources.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.5
 * @module
 */
public final class Doclet extends StandardDoclet {
    /**
     * The name of the SIS-specific stylesheet file.
     */
    private static final String STYLESHEET = "sis.css";

    /**
     * The directory where HTML pages will be written.
     */
    private String outputDirectory;

    /**
     * Invoked by the Javadoc tools for instantiating the custom doclet.
     */
    public Doclet() {
    }

    /**
     * Returns the options supported by the standard doclet.
     *
     * @return all the supported options.
     */
    @Override
    public Set<Option> getSupportedOptions() {
        final Set<Option> options = new LinkedHashSet<>();
        for (final Option op : super.getSupportedOptions()) {
            if (op.getNames().contains("-d")) {
                options.add(new Option() {
                    @Override public int          getArgumentCount() {return op.getArgumentCount();}
                    @Override public String       getDescription()   {return op.getDescription();}
                    @Override public Option.Kind  getKind()          {return op.getKind();}
                    @Override public List<String> getNames()         {return op.getNames();}
                    @Override public String       getParameters()    {return op.getParameters();}
                    @Override public boolean process(final String option, final List<String> arguments) {
                        outputDirectory = arguments.get(0);
                        return op.process(option, arguments);
                    }
                });
            } else {
                options.add(op);
            }
        }
        return options;
    }

    /**
     * Returns a name identifying this doclet.
     *
     * @return "ApacheSIS".
     */
    @Override
    public String getName() {
        return "ApacheSIS";
    }

    /**
     * Invoked by Javadoc for starting the doclet.
     *
     * @param  environment  the Javadoc environment.
     * @return {@code true} on success, or {@code false} on failure.
     */
    @Override
    public boolean run(final DocletEnvironment environment) {
        final boolean success = super.run(environment);
        if (success && outputDirectory != null) try {
            final Path output = Paths.get(outputDirectory);
            final Path resources = resources(output);
            copyResources(resources, output);
            final Rewriter r = new Rewriter();
            for (final File file : output.toFile().listFiles()) {
                if (file.isDirectory()) {       // Do not process files in the root directory, only in sub-directories.
                    r.processDirectory(file);
                }
            }
        } catch (IOException e) {
            error(e);
            return false;
        } catch (UncheckedIOException e) {
            error(e.getCause());
            return false;
        }
        return success;
    }

    /**
     * Returns the {@code src/main/javadoc/} directory relative to the root of the Maven project.
     * This method scans parents of the given directory until we find the root of the Maven project.
     */
    private static Path resources(Path directory) throws FileNotFoundException {
        boolean isModuleDirectory = false;
        while ((directory = directory.getParent()) != null && Files.isDirectory(directory)) {
            if (Files.isRegularFile(directory.resolve("pom.xml"))) {
                isModuleDirectory = true;
                final Path candidate = directory.resolve("src").resolve("main").resolve("javadoc");
                if (Files.isRegularFile(candidate.resolve(STYLESHEET))) {
                    return candidate;
                }
            } else if (isModuleDirectory) {
                // If we were in a Maven module and the parent directory does not
                // have a pom.xml file, then we are no longer in the Maven project.
                break;
            }
        }
        throw new FileNotFoundException("Can not locate \"src/main/javadoc\" from the root of this Maven project.");
    }

    /**
     * Creates links to Javadoc resources in the top-level directory (not from "{@code doc-files}" subdirectories).
     * While the Maven documentation said that the "{@code src/main/javadoc}" directory is copied by default, or a
     * directory can be specified with {@code <javadocResourcesDirectory>}, I have been unable to make it work even
     * with absolute paths.
     *
     * @param  inputFile        the directory containing resources.
     * @param  outputDirectory  the directory where to copy the resource files.
     * @throws IOException      if an error occurred while reading or writing.
     */
    private static void copyResources(final Path inputDirectory, final Path outputDirectory) throws IOException {
        Files.walk(inputDirectory).forEach((input) -> {
            if (Files.isRegularFile(input)) {
                final Path file = input.getFileName();
                switch (file.toString()) {
                    case "overview.html":
                    case STYLESHEET: break;
                    default: {
                        final Path output = outputDirectory.resolve(input.getFileName());
                        if (!Files.exists(output)) try {
                            try {
                                Files.createLink(output, input);
                            } catch (UnsupportedOperationException e) {
                                Files.copy(input, output);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        break;
                    }
                }
            }
        });
    }

    /**
     * Reports an I/O errors.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private void error(final IOException e) {
        final Reporter reporter = getReporter();
        if (reporter != null) {
            final StringWriter buffer = new StringWriter();
            final PrintWriter p = new PrintWriter(buffer);
            e.printStackTrace(p);
            reporter.print(Diagnostic.Kind.ERROR, buffer.toString());
        } else {
            e.printStackTrace();            // This fallback should not be needed, but we are paranoiac.
        }
    }
}
