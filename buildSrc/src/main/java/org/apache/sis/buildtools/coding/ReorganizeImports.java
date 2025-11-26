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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Reorganizes the import statements with a different sections for imports that are specific to a branch.
 * The goal is to separate in different groups the imports that are different between the branches.
 * Each group is identified by a label such as "// Specific to geoapi-3.1 and geoapi-4.0 branches:".
 * This separation makes easier to resolve conflicts during branch merges.
 *
 * <p>This program also opportunistically brings together the imports of the same packages.
 * Except for the above, import order is not modified: no alphabetical order is enforced.
 * Other opportunistic cleanups are the removal of zero-width spaces and trailing spaces.</p>
 *
 * <h2>How to use</h2>
 * A directory must contain a checkout of the three Apache SIS branches in directories of
 * the same name as the branches: {@code main}, {@code geoapi-3.1} and {@code geoapi-4.0}.
 * The commit on all branches shall be right after a fresh merge of development branches.
 * Run the following command in that directory where "." is the current directory
 * (can be replaced by a path if desired):
 *
 * {@snippet lang="shell" :
 *   java --class-path main/buildSrc/build/classes/java/main org.apache.sis.buildtools.coding.ReorganizeImports .
 *   }
 *
 * Above command will modify all above-cited branches.
 * First test and commit {@code geoapi-4.0}:
 *
 * {@snippet lang="shell" :
 *   cd geoapi-4.0
 *   git diff
 *   git add --update
 *   gradle test
 *   git diff
 *   git add --update
 *   git commit --message "Post-merge automatic reorganization of imports order."
 *   }
 *
 * Then temporarily stash the changes in {@code geoapi-3.1}, merge, pop the stashed changes, test and commit:
 *
 * {@snippet lang="shell" :
 *   cd ../geoapi-3.1
 *   git diff
 *   git add --update
 *   gradle test
 *   git stash
 *   git merge geoapi-4.0 -s ours --no-commit
 *   git stash pop
 *   git add --update
 *   gradle test
 *   git diff
 *   git commit --message "Merge of automatic reorganization of imports order."
 *   }
 *
 * Finally apply the same pattern on the {@code main} branch.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class ReorganizeImports extends SimpleFileVisitor<Path> {
    /**
     * Reorganize imports in the three Apache SIS development branches of a root directory.
     *
     * @param  args  the root directory of all branches.
     * @throws IOException if an error occurred while reading or writing a file.
     */
    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Expected: root directory of main, geoapi-3.1 and geoapi-4.0 branches.");
            return;
        }
        final Path root = Path.of(args[0]);
        final var geoapi4 = new ReorganizeImports(root.resolve("geoapi-4.0"), null);
        final var geoapi3 = new ReorganizeImports(root.resolve("geoapi-3.1"), geoapi4);
        final var main    = new ReorganizeImports(root.resolve("main"), geoapi3);
        final String[] branchNames = compareUsages(geoapi4, geoapi3, main);
        geoapi4.rewrite(branchNames);
        geoapi3.rewrite(branchNames);
        main   .rewrite(branchNames);
    }

    /**
     * Directories to exclude.
     */
    private static final Set<String> EXCLUDES = Set.of(
        "snapshot");    // geoapi/snapshot

    /**
     * Java keyword to search for, in order and with a trailing space.
     */
    private static final String PACKAGE = "package ", IMPORT = "import ", STATIC = "static ";

    /**
     * Order of import statements.
     *
     * @see Source#order(String)
     */
    private static final String[] IMPORT_ORDER = {
        "java",                     // Include also "javax"
        "jakarta",
        "org.locationtech.jts",     // Optional dependency
        "com.esri",                 // Optional dependency
        null,                       // All other packages.
        "javax.measure",
        "org.junit",
        "org.opengis",
        "org.apache.sis"
    };

    /**
     * Classes or packages of test dependencies. This list needs to contain only the cases
     * that are not covered by the heuristic rules encoded in {@code isTestElement(String)}.
     * The first time that one of those elements is found,
     * a "// Test dependencies" header comment will be added.
     *
     * @see Source#isTestElement(String)
     */
    private static final String[] TEST_ELEMENTS = {
        "org.junit",
        "org.opentest4j",
        "org.opengis.test",
        "org.apache.sis.metadata.TreeTableViewTest",
        "org.apache.sis.referencing.cs.HardCodedAxes",
        "org.apache.sis.referencing.cs.HardCodedCS",
        "org.apache.sis.referencing.crs.HardCodedCRS",
        "org.apache.sis.referencing.datum.HardCodedDatum",
        "org.apache.sis.referencing.operation.HardCodedConversions",
        "org.apache.sis.metadata.iso.citation.HardCodedCitations"
    };

    /**
     * Whether to sort classes inside a group of classes (a package).
     * Packages are not sorted because the order used in source code
     * is usually intentional (Java classes first, then GeoAPI, <i>etc.</i>).
     */
    private static final boolean SORT_LEXICOGRAPHY = false;

    /**
     * Root directory of the project for which to reorganize imports.
     */
    private final Path root;

    /**
     * A mask with a single bit set for identifying the branch of the sources.
     */
    private final int bitmask;

    /**
     * List of import statements. Keys are relative paths to the Java source file,
     * and values are the import statements for that class.
     */
    private final Map<Path,Source> sources;

    /**
     * Masks telling in which branches a file is present.
     * This map is shared by all branches.
     */
    private final Map<Path,Integer> branchesOfFiles;

    /**
     * Creates a new import reorganizer.
     *
     * @param  root      root directory of the project for which to reorganize imports.
     * @param  upstream  the branch which is merged in this branch.
     * @throws IOException if an error occurred while reading a file.
     */
    private ReorganizeImports(final Path root, final ReorganizeImports upstream) throws IOException {
        this.root = root;
        if (upstream == null) {
            bitmask = 1;
            branchesOfFiles = new HashMap<>();
        } else {
            bitmask = upstream.bitmask << 1;
            branchesOfFiles = upstream.branchesOfFiles;
        }
        sources = new LinkedHashMap<>();
        Files.walkFileTree(root, this);
    }

    /**
     * Checks whether the specified directory should be visited.
     *
     * @param  directory   the directory to potentially visit.
     * @param  attributes  ignored.
     * @return whether to walk in the directory or skip it.
     */
    @Override
    public FileVisitResult preVisitDirectory(final Path directory, final BasicFileAttributes attributes) {
        if (EXCLUDES.contains(directory.getFileName().toString())) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Invoked for each file to filter.
     * If the file does not have one of the hard-coded extensions, then this method does nothing.
     *
     * @param  file        the file in which to reorganize imports.
     * @param  attributes  ignored.
     * @return whether to continue filtering.
     * @throws IOException if an error occurred while reading the file.
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes) throws IOException {
        if (file.getFileName().toString().endsWith(".java")) {
            final var source = new Source(Files.readAllLines(file), bitmask);
            if (!source.isEmpty()) {
                final Path relative = root.relativize(file);
                if (sources.put(relative, source) != null) {
                    throw new IOException("Duplicated file: " + file);
                }
                branchesOfFiles.merge(relative, bitmask, (oldValue, value) -> oldValue | value);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * The source code of a Java source file, with import statements handled separately.
     */
    private static final class Source {
        /**
         * Lines before (header) and after (body) the import statements.
         */
        private String[] header, body;

        /**
         * The imported class or package names, together with a bitmask telling in which branches they are found.
         */
        private final Map<String,Integer> imports;

        /**
         * If some import statements were followed by a comment, the comment.
         */
        private final Map<String,String> comments;

        /**
         * Column where to write the comments.
         * This computed from the length of the largest import statement having a comment.
         */
        private int commentPosition;

        /**
         * Parses the given lines of code.
         *
         * @param  lines    lines of code of a Java source file.
         * @param  bitmask  a mask with a bit set to identify the branch of the source.
         */
        Source(final List<String> lines, final Integer bitmask) {
            imports = new LinkedHashMap<>();
            comments = new HashMap<>();
            final var main = new ArrayList<String>();
            final var test = new ArrayList<String>();
            final int size = lines.size();
            for (int i=0; i<size; i++) {
                String line = lines.get(i).trim();
                if (header == null) {
                    if (line.startsWith(PACKAGE)) {
                        header = lines.subList(0, i+1).toArray(String[]::new);
                    }
                } else if (line.startsWith(IMPORT)) {
                    int s = line.indexOf(';');
                    final String element = line.substring(IMPORT.length(), s).trim();
                    if (isTestElement(element)) {
                        test.add(element);
                    } else {
                        main.add(element);
                    }
                    if (++s < line.length()) {
                        final String comment = line.substring(s).trim();
                        if (!comment.isEmpty()) {
                            comments.put(element, comment);
                            commentPosition = Math.max(commentPosition, line.indexOf(comment));
                        }
                    }
                } else if (!line.isEmpty() && !line.startsWith("//")) {
                    body = lines.subList(i, size).toArray(String[]::new);
                    break;
                }
            }
            main.sort(Source::compareImports);
            test.sort(Source::compareImports);
            for (final String element : sort(main)) {
                imports.put(element, bitmask);
            }
            for (final String element : sort(test)) {
                imports.put(element, bitmask);
            }
        }

        /**
         * Sorts import statements. This method does not use alphabetic order.
         * We rather preserve the existing order in source code.
         * This method only puts together the imports having the same package name.
         *
         * <h4>Performance note</h4>
         * This implementation is not efficient. However this class is not executed often,
         * so we do not bother to optimize it.
         */
        private static String[] sort(final List<String> elements) {
            final String[] ordered = new String[elements.size()];
            int count = 0;
            for (;;) {
                /*
                 * Take the next group of imports (usually a package name).
                 * Before to use it, check if a parent is defined afterward.
                 */
                int index = -1;
                String group = null;
                for (int i=0; i < elements.size(); i++) {
                    final String candidate = getGroupName(elements.get(i));
                    if (group != null) {
                        if ( candidate.startsWith(group)) continue;     // Include the case when same group.
                        if (!group.startsWith(candidate)) continue;     // Include the case when `group` is too short.
                        if (group.charAt(candidate.length()) != '.') continue;
                    }
                    group = candidate;
                    index = i;
                }
                /*
                 * Move together all imports of the same group (package).
                 * Classes inside the same group are sorted in alphabetical order.
                 * However the order of group is kept unchanged, because the order
                 * is usually "Java first, then Jakarta, then GeoAPI, then SIS".
                 */
                if (group == null) break;
                final int start = count;
                ordered[count++] = elements.remove(index);
                final Iterator<String> it = elements.iterator();
                while (it.hasNext()) {
                    final String element = it.next();
                    if (group.equals(getGroupName(element))) {
                        ordered[count++] = element;
                        it.remove();
                    }
                }
                if (SORT_LEXICOGRAPHY) {
                    Arrays.sort(ordered, start, count);
                }
            }
            if (count != ordered.length) {
                throw new AssertionError(count);
            }
            return ordered;
        }

        /**
         * Returns the name of a group of import statements.
         * This is usually the package name.
         *
         * @param  element  the name of the imported element(s).
         * @return the name of the group (usually package name).
         */
        private static String getGroupName(final String element) {
            String prefix = element.substring(0, element.lastIndexOf('.')).trim();
            if (!prefix.startsWith(STATIC)) {
                final int p = prefix.lastIndexOf('.');
                if (p >= 0 && Character.isUpperCase(prefix.codePointAt(p+1))) {
                    // Import of an inner class. Go up to the package name.
                    prefix = prefix.substring(0, p);
                }
            }
            // Consider "javax" and synonymous of "java" for sorting purpose.
            prefix = prefix.replace("javax",    "java");
            prefix = prefix.replace("java.nio", "java.io");
            return prefix;
        }

        /**
         * Returns the order in which to sort the given statement.
         * We only apply a gross sorting here, and otherwise preserve existing order.
         *
         * @param  element  name of the package to order.
         * @return a rank to give to the specified package.
         */
        private static int order(String element) {
            if (element.startsWith(STATIC)) {
                element = element.substring(STATIC.length()).trim();
            }
            int fallback = IMPORT_ORDER.length;
            for (int i=fallback; --i >= 0;) {
                final String c = IMPORT_ORDER[i];
                if (c == null) fallback = i;
                else if (element.startsWith(c)) {
                    return i;
                }
            }
            return fallback;
        }

        /**
         * Compares to package for imports order.
         *
         * @param  s1  first import to compare.
         * @param  s2  second import to compare.
         * @return negative if {@code s1} should be first, positive if {@code s2} should be first, or zero if equal.
         */
        private static int compareImports(final String s1, final String s2) {
            return order(s1) - order(s2);
        }

        /**
         * Returns {@code true} if this object contains no information.
         * It happens with {@code module-info.java} files because they
         * contain to {@code "package"} keyword.
         *
         * @return whether this object would write an empty file.
         */
        final boolean isEmpty() {
            return header == null;
        }

        /**
         * Returns whether the given imported class or package is for testing purpose.
         *
         * @param  element  name of the class or package to filter.
         * @return whether the specified class or package is for tests.
         */
        private static boolean isTestElement(String element) {
            if (element.startsWith(STATIC)) {
                element = element.substring(STATIC.length()).trim();
            }
            if (element.startsWith("org.apache.sis")) {
                if (element.contains(".test.") || element.endsWith("Test") || element.endsWith("TestCase")) {
                    return true;
                }
                if (element.regionMatches(element.lastIndexOf('.')+1, "Test", 0, 4)) {
                    return true;
                }
                if (element.endsWith("Mock")) {
                    return true;
                }
                if (element.contains("Assert") && !element.contains("ArgumentCheckByAssertion")) {
                    return true;
                }
            }
            for (final String c : TEST_ELEMENTS) {
                if (element.startsWith(c)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Writes the source code of this source file into the given list.
         * The {@link #updateUsageFlags(Source)} should have been invoked
         * before this method in order to categorize the import statements.
         * This method shall be invoked only once per {@code Source}.
         *
         * @param dest         where to write the lines of source code.
         * @param branchNames  names of each branch.
         * @param bitmask      bitmask of branches to consider.
         */
        final void writeTo(final List<String> dest, final String[] branchNames, int bitmask) {
            /*
             * Copyright header and "package" statement.
             */
            dest.addAll(Arrays.asList(header));
            /*
             * Write the import statements, starting with the imports that apply to all branches.
             * Then the imports for specific branches are written in separated block below a header
             * saying on which branches the imports apply.
             */
            boolean needSeparator = true;       // Whether to write a line separator before next line.
            boolean needHeader    = false;      // Whether to write a comment like "// Specific to main branch:".
            boolean isTestImports = false;      // Whether at least one import of a test class was found.
            final var buffer = new StringBuilder(80);
            while (bitmask > 0) {
                final Iterator<Map.Entry<String,Integer>> it = imports.entrySet().iterator();
                while (it.hasNext()) {
                    final Map.Entry<String,Integer> entry = it.next();
                    if (entry.getValue() == bitmask) {
                        /*
                         * If we are starting a new group of imports that are specific to some branches,
                         * write a comment with the names of all branches that are using those imports.
                         */
                        if (needHeader) {
                            dest.add("");
                            final var sb = new StringBuilder("// Specific to the ");
                            int namesToAdd = bitmask;
                            do {
                                final int i = (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(namesToAdd);
                                sb.append(branchNames[i]);
                                namesToAdd &= ~(1 << i);
                                final int remaining = Integer.bitCount(namesToAdd);
                                if (remaining > 0) {
                                    sb.append(remaining > 1 ? ", " : " and ");
                                }
                            } while (namesToAdd != 0);
                            sb.append(" branch");
                            if (Integer.bitCount(bitmask) > 1) sb.append("es");     // Make plural.
                            dest.add(sb.append(':').toString());
                            isTestImports = true;       // For preventing another separator for tests.
                            needSeparator = false;
                            needHeader    = false;
                        }
                        /*
                         * Write a empty line separator if we are moving to another
                         * group of imports, then add the import statement.
                         */
                        final String element = entry.getKey();
                        if (!isTestImports && isTestElement(element)) {
                            needSeparator = false;
                            isTestImports = true;
                            dest.add("");
                            dest.add("// Test dependencies");
                        } else if (needSeparator) {
                            needSeparator = false;
                            dest.add("");
                        }
                        buffer.append(IMPORT).append(element).append(';');
                        final String comment = comments.remove(element);
                        if (comment != null) {
                            for (int i = commentPosition - buffer.length(); --i >= 0;) {
                                buffer.append(' ');
                            }
                            buffer.append(comment);
                        }
                        dest.add(buffer.toString());
                        buffer.setLength(0);
                        it.remove();
                    }
                }
                needHeader = true;
                bitmask--;
            }
            if (!imports.isEmpty()) {
                throw new RuntimeException("Non-categorized import statements.");
            }
            /*
             * Actual source code after the import statements.
             */
            if (body != null) {
                dest.add("");
                dest.add("");
                dest.addAll(Arrays.asList(body));
            }
        }

        /**
         * Takes notes of all import statements that are also used in the given source.
         * This is used for comparing the source of the same class on two different branches.
         *
         * @param  other  the same source file on another branch.
         */
        final void updateUsageFlags(final Source other) {
            other.imports.forEach((element, bitmask) -> {
                imports.computeIfPresent(element, (key,value) -> value | bitmask);
            });
        }
    }

    /**
     * Compares the import statements between all branches.
     * A flag is associated to each import statement for remembering which branches use it.
     *
     * <h4>Performance note</h4>
     * Current implementation is not efficient because for each equal key,
     * the same value is computed in all branches. However this class is not
     * executed often, so we do not bother to optimize it.
     *
     * @param  organizers  source files of all branches.
     * @return names of all branches.
     */
    private static String[] compareUsages(final ReorganizeImports... organizers) {
        final String[] branchNames = new String[organizers.length];
        for (final ReorganizeImports organizer : organizers) {
            for (final ReorganizeImports other : organizers) {
                if (other != organizer) {
                    final Map<Path,Source> sources = organizer.sources;
                    other.sources.forEach((path, osrc) -> {
                        final Source source = sources.get(path);
                        if (source != null) {
                            source.updateUsageFlags(osrc);
                        }
                    });
                }
            }
            branchNames[Integer.numberOfTrailingZeros(organizer.bitmask)] = organizer.root.getFileName().toString();
        }
        return branchNames;
    }

    /**
     * Rewrites all source files of this branch with import statements reorganized.
     * The source files of other branches are used for categorizing the import statements.
     * This method can be invoked only once.
     *
     * @param  branchNames  name of all branches.
     * @throws IOException if an error occurred while writing a file.
     */
    private void rewrite(final String[] branchNames) throws IOException {
        final Integer allBranches = (1 << branchNames.length) - 1;
        final var lines = new ArrayList<String>();
        for (final Map.Entry<Path,Source> entry : sources.entrySet()) {
            final Path relative = entry.getKey();
            entry.getValue().writeTo(lines, branchNames, branchesOfFiles.getOrDefault(relative, allBranches));
            lines.replaceAll(ReorganizeImports::removeExtraneousSpaces);
            Files.write(root.resolve(relative), lines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            lines.clear();
        }
    }

    /**
     * Opportunistic cleanup: removes all zero-width spaces is source code.
     * Those spaces are inserted in HTML pages for allowing the browsers to split long statements on
     * two lines, for example after each dot in {@code org.apache.sis.referencing.operation.transform}.
     * Those characters are accidentally introduced in Java code when doing a copy-and-paste from Javadoc.
     * This method removes them.
     *
     * @param  line  the line to filter.
     * @return the filtered line.
     */
    private static String removeExtraneousSpaces(String line) {
        line = line.replace("\u200B", "");
        int i = line.length();
        while (i > 0 && Character.isWhitespace(line.codePointBefore(i))) i--;
        return line.substring(0, i);
    }
}
