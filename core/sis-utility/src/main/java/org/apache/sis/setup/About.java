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
package org.apache.sis.setup;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.util.MissingResourceException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.Format;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Version;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.LoggerFactory;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TreeTables;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.internal.util.MetadataServices;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.Shutdown;
import org.apache.sis.internal.system.DataDirectory;

import static java.lang.System.getProperty;
import static org.apache.sis.util.collection.TableColumn.NAME;
import static org.apache.sis.util.collection.TableColumn.VALUE_AS_TEXT;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Path;


/**
 * Provides information about the Apache SIS running environment.
 * This class collects information from various places like {@link Version#SIS},
 * {@link System#getProperties()}, {@link Locale#getDefault()} or {@link TimeZone#getDefault()}.
 * This class does not collect every possible information. Instead, it tries to focus on the most
 * important information for SIS, as determined by experience in troubleshooting.
 * Some of those information are:
 *
 * <ul>
 *   <li>Version numbers (SIS, Java, Operation system).</li>
 *   <li>Default locale, timezone and character encoding.</li>
 *   <li>Current directory, user home and Java home.</li>
 *   <li>Libraries on the classpath and extension directories.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public enum About {
    /**
     * Information about software version numbers.
     * This section includes:
     *
     * <ul>
     *   <li>Apache SIS version</li>
     *   <li>Java runtime version and vendor</li>
     *   <li>Operation system name and version</li>
     * </ul>
     */
    VERSIONS(Vocabulary.Keys.Versions),

    /**
     * Information about default locale, timezone and character encoding.
     * This section includes:
     *
     * <ul>
     *   <li>Default locale, completed by ISO 3-letter codes</li>
     *   <li>Default timezone, completed by timezone offset</li>
     *   <li>Current date and time in the default timezone</li>
     *   <li>Default character encoding</li>
     * </ul>
     */
    LOCALIZATION(Vocabulary.Keys.Localization),

    /**
     * Information about logging.
     */
    LOGGING(Vocabulary.Keys.Logging),

    /**
     * Information about user home directory, java installation directory or other kind of data.
     * This section includes:
     *
     * <ul>
     *   <li>User directory</li>
     *   <li>Default directory</li>
     *   <li>SIS data directory</li>
     *   <li>Temporary directory</li>
     *   <li>Java home directory</li>
     * </ul>
     */
    PATHS(Vocabulary.Keys.Paths),

    /**
     * Information about the libraries.
     * This section includes:
     *
     * <ul>
     *   <li>JAR files in the extension directories</li>
     *   <li>JAR files and directories in the application classpath</li>
     * </ul>
     */
    LIBRARIES(Vocabulary.Keys.Libraries);

    /**
     * The resource key for this section in the {@link Vocabulary} resources bundle.
     */
    private final short resourceKey;

    /**
     * Creates a new section to be formatted using the given resource.
     */
    private About(final short resourceKey) {
        this.resourceKey = resourceKey;
    }

    /**
     * Returns all known information about the current Apache SIS running environment.
     * The information are formatted using the system default locale and timezone.
     *
     * <p>This convenience method is equivalent to the following code:</p>
     *
     * {@preformat java
     *     return configuration(EnumSet.allOf(About.class), null, null);
     * }
     *
     * @return Configuration information, as a tree for grouping some configuration by sections.
     */
    public static TreeTable configuration() {
        return configuration(EnumSet.allOf(About.class), null, null);
    }

    /**
     * Returns a subset of the information about the current Apache SIS running environment.
     *
     * @param  sections The section for which information are desired.
     * @param  locale   The locale to use for formatting the texts in the tree, or {@code null} for the default.
     * @param  timezone The timezone to use for formatting the dates, or {@code null} for the default.
     * @return Configuration information, as a tree for grouping some configuration by sections.
     */
    public static TreeTable configuration(final Set<About> sections, Locale locale, final TimeZone timezone) {
        ArgumentChecks.ensureNonNull("sections", sections);
        final Locale formatLocale;
        if (locale != null) {
            formatLocale = locale;
        } else {
            locale       = Locale.getDefault();
            formatLocale = locale; // On the JDK7 branch, this is not necessarily the same.
        }
        String userHome = null;
        String javaHome = null;
        final Date now = new Date();
        final Vocabulary resources = Vocabulary.getResources(locale);
        final DefaultTreeTable table = new DefaultTreeTable(NAME, VALUE_AS_TEXT);
        final TreeTable.Node root = table.getRoot();
        root.setValue(NAME, resources.getString(Vocabulary.Keys.LocalConfiguration));
        table.setRoot(root);
        /*
         * Begin with the "Versions" section. The 'newSection' variable will be updated in the
         * switch statement when new section will begin, and reset to 'null' after the 'section'
         * variable has been updated accordingly.
         */
        TreeTable.Node section = null;
        About newSection = VERSIONS;
fill:   for (int i=0; ; i++) {
            short    nameKey  = 0;          // The Vocabulary.Key for 'name', used only if name is null.
            String   name     = null;       // The value to put in the 'Name' column of the table.
            Object   value    = null;       // The value to put in the 'Value' column of the table.
            String[] children = null;       // Optional children to write below the node.
            switch (i) {
                case 0: {
                    if (sections.contains(VERSIONS)) {
                        name  = "Apache SIS";
                        value = Version.SIS;
                    }
                    break;
                }
                case 1: {
                    if (sections.contains(VERSIONS)) {
                        name  = "Java";
                        value = concatenate(getProperty("java.version"), getProperty("java.vendor"), true);
                    }
                    break;
                }
                case 2: {
                    if (sections.contains(VERSIONS)) {
                        nameKey = Vocabulary.Keys.OperatingSystem;
                        value = concatenate(concatenate(getProperty("os.name"),
                                getProperty("os.version"), false), getProperty("os.arch"), true);
                    }
                    break;
                }
                case 3: {
                    if (sections.contains(VERSIONS)) {
                        nameKey = Vocabulary.Keys.Container;
                        value = Shutdown.getContainer();        // Sometime contains version information.
                    }
                    break;
                }
                case 4: {
                    if (sections.contains(VERSIONS)) {
                        nameKey = Vocabulary.Keys.GeodeticDataset;
                        value = MetadataServices.getInstance().getInformation(Constants.EPSG, locale);
                    }
                    break;
                }
                case 5: {
                    newSection = LOCALIZATION;
                    if (sections.contains(LOCALIZATION)) {
                        final Locale current = Locale.getDefault();
                        if (current != null) {
                            nameKey = Vocabulary.Keys.Locale;
                            value = current.getDisplayName(locale);
                            final CharSequence code = concatenate(getCode(locale, false), getCode(locale, true), true);
                            if (code != null) {
                                children = new String[] {resources.getString(Vocabulary.Keys.Code_1, "ISO"), code.toString()};
                            }
                        }
                    }
                    break;
                }
                case 6: {
                    if (sections.contains(LOCALIZATION)) {
                        final TimeZone current = TimeZone.getDefault();
                        if (current != null) {
                            nameKey = Vocabulary.Keys.Timezone;
                            final boolean inDaylightTime = current.inDaylightTime(now);
                            value = concatenate(current.getDisplayName(inDaylightTime, TimeZone.LONG, locale), current.getID(), true);
                            final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, formatLocale);
                            df.setTimeZone(TimeZone.getTimeZone("UTC"));
                            int offset = current.getOffset(now.getTime());
                            StringBuffer buffer = format(df, offset, new StringBuffer("UTC "));
                            offset -= current.getRawOffset();
                            if (offset != 0) {
                                buffer = format(df, offset, buffer.append(" (")
                                        .append(resources.getString(Vocabulary.Keys.DaylightTime)).append(' ')).append(')');
                            }
                            children = new String[] {resources.getString(Vocabulary.Keys.Offset), buffer.toString()};
                        }
                    }
                    break;
                }
                case 7: {
                    if (sections.contains(LOCALIZATION)) {
                        nameKey = Vocabulary.Keys.CurrentDateTime;
                        final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, formatLocale);
                        if (timezone != null) {
                            df.setTimeZone(timezone);
                        }
                        value = df.format(now);
                    }
                    break;
                }
                case 8: {
                    if (sections.contains(LOCALIZATION)) {
                        final Charset current = Charset.defaultCharset();
                        if (current != null) {
                            nameKey = Vocabulary.Keys.CharacterEncoding;
                            value = current.displayName(locale);
                            final Set<String> aliases = current.aliases();
                            if (aliases != null && !aliases.isEmpty()) {
                                final StringBuilder buffer = new StringBuilder((String) value);
                                String separator = " (";
                                for (final String alias : aliases) {
                                    buffer.append(separator).append(alias);
                                    separator = ", ";
                                }
                                value = buffer.append(')');
                            }
                        }
                    }
                    break;
                }
                case 9: {
                    newSection = LOGGING;
                    if (sections.contains(LOGGING)) {
                        nameKey = Vocabulary.Keys.Implementation;
                        final LoggerFactory<?> factory = Logging.getLoggerFactory();
                        value = (factory != null) ? factory.getName() : "java.util.logging";
                    }
                    break;
                }
                case 10: {
                    if (sections.contains(LOGGING)) {
                        nameKey = Vocabulary.Keys.Level;
                        final Level level = Logging.getLogger("").getLevel();   // Root logger level.
                        value = level.getLocalizedName();
                        final Map<String,Level> levels = Loggers.getEffectiveLevels();
                        if (levels.size() != 1 || !level.equals(levels.get(Loggers.ROOT))) {
                            int j = 0;
                            children = new String[levels.size() * 2];
                            for (final Map.Entry<String,Level> entry : levels.entrySet()) {
                                children[j++] = entry.getKey();
                                children[j++] = entry.getValue().getLocalizedName();
                            }
                        }
                    }
                    break;
                }
                case 11: {
                    newSection = PATHS;
                    if (sections.contains(PATHS)) {
                        nameKey = Vocabulary.Keys.UserHome;
                        value = userHome = getProperty("user.home");
                    }
                    break;
                }
                case 12: {
                    if (sections.contains(PATHS)) {
                        nameKey = Vocabulary.Keys.CurrentDirectory;
                        value = getProperty("user.dir");
                    }
                    break;
                }
                case 13: {
                    if (sections.contains(PATHS)) {
                        nameKey = Vocabulary.Keys.DataDirectory;
                        try {
                            value = AccessController.doPrivileged(new PrivilegedAction<String>() {
                                @Override public String run() {
                                    return System.getenv(DataDirectory.ENV);
                                }
                            });
                        } catch (SecurityException e) {
                            value = e.toString();
                        }
                        if (value == null) {
                            value = Messages.getResources(locale).getString(Messages.Keys.DataDirectoryNotSpecified_1, DataDirectory.ENV);
                        } else {
                            final Path path = DataDirectory.getRootDirectory();
                            if (path != null) {
                                value = path.toString();
                            } else {
                                value = value + " (" + resources.getString(Vocabulary.Keys.Invalid) + ')';
                            }
                        }
                    }
                    break;
                }
                case 14: {
                    if (sections.contains(PATHS)) {
                        nameKey = Vocabulary.Keys.DataBase;
                        value = MetadataServices.getInstance().getInformation("DataSource", locale);
                    }
                    break;
                }
                case 15: {
                    if (sections.contains(PATHS)) {
                        nameKey = Vocabulary.Keys.TemporaryFiles;
                        value = getProperty("java.io.tmpdir");
                    }
                    break;
                }
                case 16: {
                    if (sections.contains(PATHS)) {
                        nameKey = Vocabulary.Keys.JavaHome;
                        value = javaHome = getProperty("java.home");
                    }
                    break;
                }
                case 17: {
                    newSection = LIBRARIES;
                    if (sections.contains(LIBRARIES)) {
                        nameKey = Vocabulary.Keys.JavaExtensions;
                        value = classpath(getProperty("java.ext.dirs"), true);
                    }
                    break;
                }
                case 18: {
                    if (sections.contains(LIBRARIES)) {
                        nameKey = Vocabulary.Keys.Classpath;
                        value = classpath(getProperty("java.class.path"), false);
                    }
                    break;
                }
                default: break fill;
            }
            /*
             * At this point, we have the information about one node to create.
             * If the 'newSection' variable is non-null, then this new node shall
             * appear in a new section.
             */
            if (value == null) {
                continue;
            }
            if (newSection != null) {
                section = root.newChild();
                section.setValue(NAME, resources.getString(newSection.resourceKey));
                newSection = null;
            }
            if (name == null) {
                name = resources.getString(nameKey);
            }
            @SuppressWarnings("null")
            final TreeTable.Node node = section.newChild();
            node.setValue(NAME, name);
            if (children != null) {
                for (int j=0; j<children.length; j+=2) {
                    final String c = children[j+1];
                    if (c != null) {
                        final TreeTable.Node child = node.newChild();
                        child.setValue(NAME, children[j]);
                        child.setValue(VALUE_AS_TEXT, c);
                    }
                }
            }
            if (!(value instanceof Map<?,?>)) {
                node.setValue(VALUE_AS_TEXT, value.toString());
                continue;
            }
            /*
             * Special case for values of kind Map<File,String>.
             * They are extension paths or application class paths.
             */
            @SuppressWarnings("unchecked")
            final Map<File,String> paths = (Map<File,String>) value;
pathTree:   for (int j=0; ; j++) {
                TreeTable.Node directory = null;
                final String home;
                final short homeKey;
                switch (j) {
                    case 0: home = javaHome; homeKey = Vocabulary.Keys.JavaHome; break;
                    case 1: home = userHome; homeKey = Vocabulary.Keys.UserHome; break;
                    case 2: home = "";       homeKey = 0; directory = node;      break;
                    default: break pathTree;
                }
                if (home == null) {
                    // Should never happen since "user.home" and "java.home" are
                    // standard properties of the Java platform, but let be safe.
                    continue;
                }
                final File homeDirectory = home.isEmpty() ? null : new File(home);
                for (final Iterator<Map.Entry<File,String>> it=paths.entrySet().iterator(); it.hasNext();) {
                    final Map.Entry<File,String> entry = it.next();
                    File file = entry.getKey();
                    if (homeDirectory != null) {
                        file = relativize(homeDirectory, file);
                        if (file == null) continue;
                    }
                    if (directory == null) {
                        directory = node.newChild();
                        directory.setValue(NAME, parenthesis(resources.getString(homeKey)));
                    }
                    CharSequence title = entry.getValue();
                    if (title == null || title.length() == 0) {
                        title = parenthesis(resources.getString(entry.getKey().isDirectory() ?
                                Vocabulary.Keys.Directory : Vocabulary.Keys.Untitled).toLowerCase(locale));
                    }
                    TreeTables.nodeForPath(directory, NAME, file).setValue(VALUE_AS_TEXT, title);
                    it.remove();
                }
                if (directory != null) {
                    concatenateSingletons(directory, true);
                    omitMavenRedundancy(directory);
                }
            }
        }
        TreeTables.replaceCharSequences(table, locale);
        return table;
    }

    /**
     * Returns a map of all JAR files or class directories found in the given paths,
     * associated to a description obtained from their {@code META-INF/MANIFEST.MF}.
     *
     * @param  paths         The paths using the {@link File#pathSeparatorChar} separator.
     * @param  asDirectories {@code true} if the paths are directories, or {@code false} for JAR files.
     * @return The paths, or {@code null} if none.
     */
    private static Map<File,CharSequence> classpath(final String paths, final boolean asDirectories) {
        final Map<File,CharSequence> files = new LinkedHashMap<File,CharSequence>();
        return classpath(paths, null, asDirectories, files) ? files : null;
    }

    /**
     * Implementation of {@link #classpath(String, boolean)} to be invoked recursively.
     * The {@code paths} argument may contains many path separated by one of the
     * following separators:
     *
     * <ul>
     *   <li>If {@code directory} is null, then {@code paths} is assumed to be a
     *       system property value using the {@link File#pathSeparatorChar}.</li>
     *   <li>If {@code directory} is non-null, then {@code paths} is assumed to be
     *       a {@code MANIFEST.MF} attribute using space as the path separator.</li>
     * </ul>
     *
     * @param  paths         The paths using the separator described above.
     * @param  directory     The directory of {@code MANIFEST.MF} classpath, or {@code null}.
     * @param  asDirectories {@code true} if the paths are directories, or {@code false} for JAR files.
     * @param  files         Where to add the paths.
     * @return {@code true} if the given map has been changed as a result of this method call.
     */
    private static boolean classpath(final String paths, final File directory,
            final boolean asDirectories, final Map<File,CharSequence> files)
    {
        if (paths == null) {
            return false;
        }
        boolean changed = false;
        for (final CharSequence path : CharSequences.split(paths, (directory == null) ? File.pathSeparatorChar : ' ')) {
            final File file = new File(directory, path.toString());
            if (file.exists()) {
                if (!asDirectories) {
                    if (!files.containsKey(file)) {
                        files.put(file, null);
                        changed = true;
                    }
                } else {
                    // If we are scanning extensions, then the path are directories
                    // rather than files. So we need to scan the directory content.
                    final JARFilter filter = new JARFilter();
                    final File[] list = file.listFiles(filter);
                    if (list != null) {
                        Arrays.sort(list);
                        for (final File ext : list) {
                            if (!files.containsKey(ext)) {
                                files.put(ext, null);
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
        if (!changed) {
            return false;
        }
        /*
         * At this point, we have collected all JAR files. Now set the description from the
         * MANIFEST.MF file and scan recursively for the classpath declared in the manifest.
         */
        IOException error = null;
        for (final Map.Entry<File,CharSequence> entry : files.entrySet()) {
            CharSequence title = entry.getValue();
            if (title != null) {
                continue;               // This file has already been processed by a recursive method invocation.
            }
            final File file = entry.getKey();
            if (file.isFile() && file.canRead()) {
                try {
                    final JarFile jar = new JarFile(file);
                    final Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        final Attributes attributes = manifest.getMainAttributes();
                        if (attributes != null) {
                            title = concatenate(attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE),
                                    attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION), false);
                            if (title == null) {
                                title = concatenate(attributes.getValue(Attributes.Name.SPECIFICATION_TITLE),
                                        attributes.getValue(Attributes.Name.SPECIFICATION_VERSION), false);
                                if (title == null) {
                                    // We really need a non-null value in order to protect this code
                                    // against infinite recursivity.
                                    title = "";
                                }
                            }
                            entry.setValue(title);
                            if (classpath(attributes.getValue(Attributes.Name.CLASS_PATH),
                                    file.getParentFile(), false, files))
                            {
                                break;          // Necessary for avoiding ConcurrentModificationException.
                            }
                        }
                    }
                    jar.close();
                } catch (IOException e) {
                    if (error == null) {
                        error = e;
                    } else {
                        // error.addSuppressed(e) on JDK7 branch.
                    }
                }
            }
        }
        if (error != null) {
            Logging.unexpectedException(Logging.getLogger(Modules.UTILITIES), About.class, "configuration", error);
        }
        return true;
    }

    /**
     * If a file path in the given node or any children follow the Maven pattern, remove the
     * artifact name and version numbers redundancies in order to make the name more compact.
     * For example this method replaces {@code "org/opengis/geoapi/3.0.0/geoapi-3.0.0.jar"}
     * by {@code "org/opengis/(…)/geoapi-3.0.0.jar"}.
     */
    private static void omitMavenRedundancy(final TreeTable.Node node) {
        for (final TreeTable.Node child : node.getChildren()) {
            omitMavenRedundancy(child);
        }
        final CharSequence name = node.getValue(NAME);
        final int length = name.length();
        final int s2 = CharSequences.lastIndexOf(name, File.separatorChar, 0, length);
        if (s2 >= 0) {
            final int s1 = CharSequences.lastIndexOf(name, File.separatorChar, 0, s2);
            if (s1 >= 0) {
                final int s0 = CharSequences.lastIndexOf(name, File.separatorChar, 0, s1) + 1;
                final StringBuilder buffer = new StringBuilder(s2 - s0).append(name, s0, s2);
                buffer.setCharAt(s1 - s0, '-');
                if (CharSequences.regionMatches(name, s2+1, buffer)) {
                    buffer.setLength(0);
                    node.setValue(NAME, buffer.append(name, 0, s0).append("(…)").append(name, s2, length));
                }
            }
        }
    }

    /**
     * For every branch containing only one child and no value, merges in-place that branch and the
     * node together. This method is used for simplifying depth trees into something less verbose.
     * However for any column other than {@code NAME}, this method preserves the values of the child
     * node but lost all value of the parent node. For this reason, we perform the merge only if the
     * parent has no value.
     *
     * <p>See the <cite>"Reduce the depth of a tree"</cite> example in {@link TreeTables} for more information.
     * In particular, note that this implementation assumes that children collections are {@link List} (this is
     * guaranteed for {@link DefaultTreeTable.Node} implementations).</p>
     *
     * @param  node The root of the node to simplify.
     * @param  skip {@code true} for disabling concatenation of root node.
     * @return The root of the simplified tree. May be the given {@code node} or a child.
     */
    private static TreeTable.Node concatenateSingletons(final TreeTable.Node node, final boolean skip) {
        // DefaultTreeTable.Node instances are known to handle their children in a List.
        final List<TreeTable.Node> children = (List<TreeTable.Node>) node.getChildren();
        final int size = children.size();
        for (int i=0; i<size; i++) {
            children.set(i, concatenateSingletons(children.get(i), false));
        }
        if (!skip && size == 1) {
            if (node.getValue(VALUE_AS_TEXT) == null) {
                final TreeTable.Node child = children.remove(0);
                final StringBuilder name = new StringBuilder(node.getValue(NAME));
                if (!File.separator.contentEquals(name)) {
                    name.append(File.separatorChar);
                }
                child.setValue(NAME, name.append(child.getValue(NAME)));
                return child;
            }
        }
        return node;
    }

    /**
     * Concatenates the given strings in the format "main (complement)".
     * Any of the given strings can be null.
     *
     * @param  main        The main string to show first, or {@code null}.
     * @param  complement  The string to show after the main one, or {@code null}.
     * @param  parenthesis {@code true} for writing the complement between parenthesis, or {@code null}.
     * @return The concatenated string, or {@code null} if all components are null.
     */
    private static CharSequence concatenate(final CharSequence main, final CharSequence complement, final boolean parenthesis) {
        if (main != null && main.length() != 0) {
            if (complement != null && complement.length() != 0) {
                final StringBuilder buffer = (main instanceof StringBuilder)
                        ? (StringBuilder) main : new StringBuilder(main);
                buffer.append(' ');
                if (parenthesis) buffer.append('(');
                buffer.append(complement);
                if (parenthesis) buffer.append(')');
                return buffer;
            }
            return main;
        }
        return complement;
    }

    /**
     * Returns the given text between parenthesis.
     */
    private static CharSequence parenthesis(final String text) {
        return new StringBuilder(text.length() + 2).append('(').append(text).append(')');
    }

    /**
     * Returns the ISO language or country code for the given locale.
     * Whether we use 2-letters or 3-letters code shall be consistent
     * with {@link org.apache.sis.xml.ValueConverter}.
     */
    private static String getCode(final Locale locale, final boolean country) {
        try {
            return country ? locale.getCountry() : locale.getISO3Language();
        } catch (MissingResourceException e) {
            Logging.recoverableException(Logging.getLogger(Loggers.LOCALIZATION), About.class, "configuration", e);
            return null;
        }
    }

    /**
     * Formats the given value preceded by a plus or minus sign.
     * This method is used for formatting timezone offset.
     *
     * @param df     The {@link DateFormat} to use for formatting the offset.
     * @param offset The offset to format, as a positive or negative value.
     * @param buffer The buffer where to format the offset.
     * @return       The given buffer, returned for convenience.
     */
    private static StringBuffer format(final Format df, final int offset, final StringBuffer buffer) {
        return df.format(Math.abs(offset), buffer.append(offset < 0 ? '-' : '+').append(' '), new FieldPosition(0));
    }

    /**
     * Filters the JAR files in an extension directory.
     */
    private static final class JARFilter implements FileFilter {
        @Override public boolean accept(final File pathname) {
            return pathname.getName().endsWith(".jar");
        }
    }

    /**
     * Returns the given file relative to the given root, or {@code null} if the root is not
     * a parent of that file.
     *
     * @param  root The root directory (typically Java home or user home directory).
     * @param  file The file to make relative to the root.
     * @return The file relative to the given root, or {@code null} if none.
     */
    private static File relativize(final File root, final File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            return null;
        }
        if (root.equals(parent)) {
            parent = null;
        } else {
            parent = relativize(root, parent);
            if (parent == null) {
                return null;
            }
        }
        return new File(parent, file.getName());
    }
}
