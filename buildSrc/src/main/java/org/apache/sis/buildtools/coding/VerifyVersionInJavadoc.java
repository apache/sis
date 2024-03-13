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
package org.apache.sis.buildtools.coding;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Verifies the usage of {@code @since} and {@code @version} Javadoc tags in source code.
 * If one of those tags is missing in a public exported class, an error is reported.
 * If those tags are present in a package-private or non-exported class, they are removed.
 *
 * <h2>How to use</h2>
 * Run the following command from the project root directory, where "." is the current directory
 * (can be replaced by a path if desired):
 *
 * {@snippet lang="shell" :
 *   java --class-path buildSrc/build/classes/java/main org.apache.sis.buildtools.coding.VerifyVersionInJavadoc .
 *   }
 *
 * <h2>Rational</h2>
 * This tool has been created because in all Apache SIS versions from 0.3 to 1.3, the {@code @since} and
 * {@code @version} Javadoc tags were put on all classes, public or not. It was a little bit misleading
 * because non-public classes can be moved, split, merged, <i>etc.</i>, making the meaning of "since"
 * confusing. The rule is that {@code @since} should tell when a class was first available in public API,
 * which is not necessarily when it was first created. Finally, with the use of JPMS exporting only some
 * chosen packages, the presence/absence of those tags is a useful way to remind whether or not a class
 * in process of being modified is part of public API.
 *
 * <h2>Limitations</h2>
 * This class does only a gross analysis. It may sometime be wrong.
 * Developers should check with {@code git diff} before to commit.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public final class VerifyVersionInJavadoc {
    /**
     * Verifies the Javadoc tags in the Java source classes.
     *
     * @param  args  the root directory of all branches.
     * @throws IOException if an error occurred while reading or writing a file.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Expected: root directory of project to filter.");
            return;
        }
        final File root = new File(args[0]);
        var p = new VerifyVersionInJavadoc();
        p.listExports(root);
        p.scan(root);
    }

    /**
     * Name of {@value} source files.
     */
    private static final String MODULE_INFO = "module-info.java";

    /**
     * Java keyword to search for.
     */
    private static final String EXPORTS = "exports ", PACKAGE = "package ",
            CLASS = "class ", INTERFACE = "interface ", ENUM = "enum ",
            PUBLIC = "public ", PROTECTED = "protected ", PRIVATE = "private ";

    /**
     * All packages that are exported to everyone.
     */
    private final Set<String> exportedPackages;

    /**
     * Whether the file currently being processed is a public or protected class, interface or enumeration.
     */
    private boolean isPublicType;

    /**
     * Whether the file currently being processed is an interface or an enumeration.
     * Methods in interfaces are implicitly public, and fields in enumeration too.
     */
    private boolean isImplicitlyPublic;

    /**
     * Whether the current file is a test class.
     */
    private boolean isTest;

    /**
     * Creates a new processor.
     */
    private VerifyVersionInJavadoc() {
        exportedPackages = new HashSet<>(64);
    }

    /**
     * Finds all exported packages. This method only collect information without modifying any file.
     * It should be invoked before the actual processing done by {@link #scan(File)}.
     *
     * @param  directory  the directory to scan.
     * @throws IOException if an error occurred while reading a source file.
     */
    private void listExports(final File directory) throws IOException {
        for (File file : directory.listFiles()) {
            if (file.getName().equals(MODULE_INFO)) {
                for (String line : Files.readAllLines(file.toPath())) {
                    line = getPackageName(EXPORTS, line.trim());
                    if (line != null && !line.contains(" to ")) {
                        exportedPackages.add(line);
                    }
                }
                return;     // Do not scan sub-directories.
            }
            if (file.isDirectory()) {
                listExports(file);
            }
        }
    }

    /**
     * Returns the package name after an {@code exports} or {@code package} keyword.
     *
     * @param  keyword  {@link #EXPORTS} or {@link #PACKAGE}.
     * @param  line     the source code line.
     * @return the package name if found, or {@code null} otherwise.
     */
    private static String getPackageName(final String keyword, final String line) {
        if (line.startsWith(keyword) && line.endsWith(";")) {
            return line.substring(keyword.length(), line.length() - 1).trim();
        }
        return null;
    }

    /**
     * Returns {@code true} if the source code contains a {@code package} statement with the name
     * of an exported package.
     *
     * @param  lines  lines of code to scan.
     * @return whether the code is for a class or a {@code package-info} in an exported package.
     */
    private boolean isExportedPackage(final List<String> lines) {
        return lines.stream().map((line) -> getPackageName(PACKAGE, line.trim()))
                    .filter(Objects::nonNull).findFirst()
                    .map(exportedPackages::contains).orElse(Boolean.FALSE);
    }

    /**
     * Searches for {@code @since} and {@code @version} Javadoc tags in all source files.
     * If the class is not exported or is not public, those tags will be removed.
     * Tags in private methods may also be removed.
     *
     * @param  directory  the directory to scan.
     * @throws IOException if an error occurred while reading or writing a source file.
     */
    private void scan(final File directory) throws IOException {
        for (File file : directory.listFiles()) {
            final String name = file.getName();
            if (name.endsWith(".java")) {
                if (!name.equals(MODULE_INFO)) {
                    process(file.toPath());
                }
            } else if (file.isDirectory() && !name.equals("geoapi")) {
                final boolean old = isTest;
                isTest |= name.equals("test");
                scan(file);
                isTest = old;
            }
        }
    }

    /**
     * Searches for {@code @since} and {@code @version} Javadoc tags in a single source file.
     * If the class is not exported or not public, those tags will be removed.
     * Tags in private methods may also be removed.
     *
     * @param  file  the file to process.
     * @throws IOException if an error occurred while reading or writing the source file.
     */
    private void process(final Path file) throws IOException {
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        final boolean isPackageInfo = file.getFileName().toString().endsWith("-info.java");
        final boolean isExportedPackage = isExportedPackage(lines);
        isPublicType         = false;
        isImplicitlyPublic   = false;
        boolean modified     = false;
        boolean foundSince   = false;
        boolean foundVersion = false;
        for (int i=0; i<lines.size(); i++) {
            final String  line      = lines.get(i).trim();
            final boolean isSince   = line.startsWith("* @since ");
            final boolean isVersion = line.startsWith("* @version ");
            foundSince   |= isSince;
            foundVersion |= isVersion;
            if (isSince | isVersion) {
                if (isTest || !(isExportedPackage && (isPackageInfo || isPublic(lines, i)))) {
                    lines.remove(i--);
                    modified = true;
                }
            } else if (line.equals("*/") && lines.get(i-1).trim().equals("*")) {
                lines.remove(--i);      // Remove trailing empty comment lines.
                modified = true;
            }
        }
        if (isPublicType & !isTest & !(foundSince & foundVersion)) {
            System.err.println("Missing @since or @version: " + file);
        }
        if (modified) {
            Files.write(file, lines, StandardCharsets.UTF_8);
        }
    }

    /**
     * Determines whether the class, interface or method after the specified line is public or protected.
     *
     * @param  lines  all lines of the source file to process.
     * @param  i      index of current line.
     */
    private boolean isPublic(final List<String> lines, int i) {
        boolean isSkippingComments = true;
        int innerArguments = 0;
        while (++i < lines.size()) {
            String line = lines.get(i).trim();
            if (isSkippingComments) {
                if (!line.startsWith("*/")) {
                    continue;                           // Skip more comment lines.
                }
                isSkippingComments = false;
                line = line.substring(2).trim();        // Maybe there is code on the same line.
            }
            if (line.startsWith("/*") && !line.contains("*/")) {
                isSkippingComments = true;
                continue;                               // Comment block followed by another comment block.
            }
            if (!line.isEmpty() && !line.startsWith("//")) {
                if (line.contains(PRIVATE)) {
                    return false;
                }
                if (line.contains(PUBLIC) || line.contains(PROTECTED)) {
                    final boolean implicit = line.contains(INTERFACE) || line.contains(ENUM);
                    if (implicit || line.contains(CLASS)) {
                        isImplicitlyPublic = implicit;
                        isPublicType = true;
                    }
                    return isPublicType;
                }
                final boolean wasInner = (innerArguments != 0);
                innerArguments += count(line, '(') - count(line, ')');
                if (innerArguments != 0 || wasInner || line.charAt(0) == '@') {         // Skip annotation lines.
                    continue;
                }
                // Not a class or interface, assumes a field or a method.
                return isPublicType & isImplicitlyPublic;
            }
        }
        return false;       // Unexpected end of file.
    }

    /**
     * Returns the number of occurrences of the given character.
     *
     * @param  line  the line where to search for the character.
     * @param  c     the character to count.
     * @return number of occurrences of the given character.
     */
    private static int count(final String line, final char c) {
        int i=0, n=0;
        while ((i = line.indexOf(c, i)) >= 0) {
            i++;
            n++;
        }
        return n;
    }
}
