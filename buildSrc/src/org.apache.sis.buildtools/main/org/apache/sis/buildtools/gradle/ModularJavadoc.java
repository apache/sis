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

import java.util.List;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Files;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;


/**
 * Extension to Gradle {@link Javadoc} task.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ModularJavadoc extends Conventions {
    /**
     * The javadoc task.
     */
    private final Javadoc task;

    /**
     * Creates a helper instance.
     *
     * @param  task the {@link Javadoc} task to configure.
     */
    ModularJavadoc(final Javadoc task) {
        super(task.getProject());
        this.task = task;
    }

    /**
     * Invoked when the {@code javadoc} task is executed.
     *
     * @param  context  the extension which is invoking this task.
     */
    final void modularize(final BuildHelper context) {
        final var options = (StandardJavadocDocletOptions) task.getOptions();
        options.encoding     (ENCODING);        // Encoding of Java source file.
        options.docEncoding  (ENCODING);        // Encoding of the generated HTML files.
        options.charSet      (ENCODING);        // Encoding to declare in the HTML META tag.
        options.locale       ("en");            // Locale for navigation bar, help file contents, etc.
        options.breakIterator(true);            // Better boundary detection when determining the end of the first sentence.
        options.noQualifiers ("all");           // Omit qualifying package name before class names in output.
        options.author       (false);           // Excludes the authors text in the generated docs.
        options.version      (false);           // Excludes the version text in the generated docs.
        options.keyWords     (true);            // Adds HTML meta keyword tags to the generated files.
        options.noTimestamp.setValue(false);    // Allow timestamp in generated HTML pages.
        options.tags("category:X:\"Category:\"",
                     "todo:a:\"TODO:\"");
        options.links(
                "https://docs.oracle.com/en/java/javase/11/docs/api",
                "http://www.geoapi.org/3.0/javadoc",
                "https://openjfx.io/javadoc/21/",
                "http://unitsofmeasurement.github.io/unit-api/site/apidocs");
        /*
         * Taglet defined in this `buildSrc` sub-project.
         */
        final File buildSrc = fileRelativeToRoot(BUILD_TOOLS_DIRECTORY);
        final File bClasses = new File(new File(buildSrc, BUILD_DIRECTORY), MAIN_CLASSES_DIRECTORY);
        options.docletpath(bClasses);
        options.tagletPath(bClasses);
        options.setDoclet("org.apache.sis.buildtools.doclet.Doclet");
        options.taglets("org.apache.sis.buildtools.doclet.Include");
        /*
         * Put only one '-' in given options because Gradles add a '-' itself.
         */
        options.addFileOption  ("-add-stylesheet", new File(new File(project.getRootDir(), "parent"), "sis.css"));
        options.addStringOption("-module-source-path", ModularCompilation.getModuleSourcePath(project, false));
        options.addStringOption("-add-modules", String.join(",", context.getModuleNames()));
        /*
         * Same workaround than for the compiler task.
         */
        options.modulePath(options.getClasspath());
        options.setClasspath(List.of());
        /*
         * In current version, Javadoc generation fails because some dependencies are incorrectly
         * put in class-path option instead of module-path option. The above workaround does not work
         * because some class-path entries are added by Gradle only after this configuration phase.
         * The reason for this behavior is unknown and we found no way to resolve this problem yet.
         * For now, we create a patched `javadoc.options` file that user can execute from the command line.
         * The new file is created using the file from the previous run (not this run).
         */
        try {
            patchOptionsFile(task.getOptionsFile().toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        project.getLogger().info(String.format(
                "The `javadoc` task is known to not work in this configuration (tested with Gradle 8.2.1).%n" +
                "Resolving this issue may require changes in the way that Gradle handles the module path.%n" +
                "In the meantime, Javadoc can be generated with the following command line:%n" +
                "%n" +
                "    javadoc @%s%sbuild%<stmp%<sjavadoc%<sjavadoc.options.patched%n" +
                "%n" +
                "It may be necessary to run the `javadoc` task again for generating the patched file.%n" +
                "%n", project.getProjectDir().getName(), File.separator));
    }

    /**
     * Creates a copy of the given file with patched {@code --module-path} option.
     * User can execute with the following command line:
     *
     * {@snippet lang="shell" :
     *   javadoc @build/tmp/javadoc/javadoc.options.patched
     *   }
     *
     * @param  file  the file to patch.
     * @throws IOException if an error occurred while reading or writing the file.
     */
    private static void patchOptionsFile(final Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return;
        }
        final String searchFor = "-classpath ";         // The option to search.
        final String moveTo    = "--module-path ";      // The option where to move the classpath.

        final List<String>  lines      = Files.readAllLines(file);
        final String[]      rewrite    = new String[lines.size()];
        final StringBuilder modulePath = new StringBuilder(16*1024).append(moveTo);
        int     lineOfModulePath = -1;
        int     currentLineIndex =  0;
        boolean foundClassPath   = false;
        boolean foundSomePath    = false;
        for (final String line : lines) {
            if (line.startsWith(searchFor)) {
                appendWithoutQuotes(line, searchFor.length(), foundSomePath, modulePath);
                foundClassPath = true;
                foundSomePath  = true;
            } else {
                if (line.startsWith(moveTo)) {
                    appendWithoutQuotes(line, moveTo.length(), foundSomePath, modulePath);
                    foundSomePath = true;
                    if (lineOfModulePath >= 0) continue;
                    lineOfModulePath = currentLineIndex;
                }
                rewrite[currentLineIndex++] = line;
            }
        }
        if (foundClassPath && lineOfModulePath >= 0) {
            rewrite[lineOfModulePath] = modulePath.toString();
            Files.write(file.getParent().resolve(file.getFileName().toString() + ".patched"),
                        Arrays.asList(rewrite).subList(0, currentLineIndex));
        }
    }

    /**
     * Appends the given value to the string builder without the quotes around the values.
     * This is used for merging many paths (which may be quoted) in a single unquoted path.
     *
     * @param  value      the value to unquote and append to the string builder.
     * @param  start      first character to examine in the given value.
     * @param  separator  whether to prepend a path separator.
     * @param  appendTo   where to append the unquoted value.
     */
    private static void appendWithoutQuotes(final String value, int start, final boolean separator, final StringBuilder appendTo) {
        int end = value.length();
        while (end > 0) {
            final int c = value.codePointBefore(end);
            if (!Character.isWhitespace(c)) break;
            end -= Character.charCount(c);
        }
        while (start < end) {
            final int c = value.codePointAt(start);
            if (!Character.isWhitespace(c)) break;
            start += Character.charCount(c);
        }
        final int last = end - 1;
        if (start < end && value.charAt(start) == '\'' && value.charAt(last) == '\'') {
            start++;
            end = last;
        }
        if (start < end) {
            if (separator) {
                appendTo.append(File.pathSeparatorChar);
            }
            appendTo.append(value, start, end);
        }
    }
}
