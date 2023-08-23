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
package org.apache.sis.buildtools.resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Pattern;
import java.text.MessageFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.Writer;


/**
 * Reads a given list of {@code .properties} files and copies their content to {@code .utf} files using UTF-8 encoding.
 * This class also checks for key validity and checks the property values for {@link MessageFormat} compatibility.
 * Finally, it writes the key values in the Java source files.
 *
 * <p>Instances of this class are not thread safe.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.3
 */
public class IndexedResourceCompiler {
    /**
     * Extension for java source files.
     */
    private static final String JAVA_EXT = ".java";

    /**
     * Extension for properties source files.
     */
    private static final String PROPERTIES_EXT = ".properties";

    /**
     * Extension for resource target files.
     */
    private static final String RESOURCES_EXT = ".utf";

    /**
     * Prefix for argument count in resource key names. For example, a resource
     * expecting one argument may have a key name like "{@code HelloWorld_1}".
     */
    private static final String ARGUMENT_COUNT_PREFIX = "_";

    /**
     * The maximal length of comment lines.
     */
    private static final int COMMENT_LENGTH = 92;

    /**
     * The name of the inner class which will contains key values.
     */
    private static final String KEYS_INNER_CLASS = "Keys";

    /**
     * Encoding of Java source file (<strong>not</strong> property files).
     */
    private static final String JAVA_ENCODING = "UTF-8";

    /**
     * The Java modifiers applies on the key constants to be generated.
     */
    private static final String KEY_MODIFIERS = "public static final short ";

    /**
     * Margin to write before the {@link #KEY_MODIFIERS}.
     */
    private static final String KEY_MARGIN = "        ";        // 8 spaces

    /**
     * Directory to exclude when parsing sub-directories recursively.
     */
    private static final String EXCLUDE_DIRECTORY = "doc-files";

    /**
     * The source directory of the Java and properties files.
     */
    private final File sourceDirectory;

    /**
     * The target directory where to write the UTF files.
     */
    private final File buildDirectory;

    /**
     * The resource bundle base class being processed.
     * Example: {@code org/apache/sis/util/resources/Vocabulary.java}.
     */
    private File bundleClass;

    /**
     * Integer IDs allocated to resource keys.
     * This map will be shared for all languages of a given resource bundle.
     */
    private final Map<Integer,String> allocatedIDs = new HashMap<>();

    /**
     * Resource keys and their localized values.
     * This map will be cleared for each language in a resource bundle.
     */
    private final Map<Object,Object> resources = new HashMap<>();

    /**
     * Buffer to use for writing UTF data in an array of bytes.
     *
     * @see #writeUTF()
     */
    private final ByteArrayOutputStream bufferUTF = new ByteArrayOutputStream(1024 * 8);

    /**
     * The result of compiling UTF files. Keys are the files where to write the results,
     * and values are the bytes to write for the associated file.
     */
    private final Map<File, ? super byte[]> destination;

    /**
     * Number of logical errors found.
     */
    private int errors;

    /**
     * Constructs a new {@code IndexedResourceCompiler}.
     * The compilation results are not written immediately to the build directory.
     * Instead they are saved in the given map and written later by the caller.
     *
     * @param  sourceDirectory  the source directory.
     * @param  buildDirectory   the target directory where to write UTF files.
     * @param  destination      where to write the compilation results.
     */
    public IndexedResourceCompiler(final File sourceDirectory, final File buildDirectory,
                                   final Map<File, ? super byte[]> destination)
    {
        this.sourceDirectory = sourceDirectory;
        this.buildDirectory  = buildDirectory;
        this.destination     = destination;
    }

    /**
     * Flags the properties files which seem to be about internationalized resources.
     * For example if the given map contains the following files:
     * <ul>
     *   <li>{@code "Errors.properties"}</li>
     *   <li>{@code "Errors_en.properties"}</li>
     *   <li>{@code "Errors_fr.properties"}</li>
     *   <li>{@code "Messages.properties"}</li>
     *   <li>{@code "Messages_en.properties"}</li>
     *   <li>{@code "Messages_fr.properties"}</li>
     *   <li>{@code "NotAnInternationalResource.properties"}</li>
     * </ul>
     *
     * Then this method will set to {@code Boolean.TRUE} the values associated to the following files
     * and remove the entries for their language variants:
     * <ul>
     *   <li>{@code "Errors.properties"}</li>
     *   <li>{@code "Messages.properties"}</li>
     * </ul>
     *
     * The entries that do not seem to be about internationalized resources will be left unchanged.
     * Their associated values should be {@code Boolean.FALSE}.
     *
     * @param  resourcesToProcess  the files to filter. This map will be modified in-place.
     * @param  checkSourceExists   whether to check that the Java source file exists.
     */
    static void filterLanguages(final SortedMap<File,Boolean> resourcesToProcess, final boolean checkSourceExists) {
        final Iterator<Map.Entry<File,Boolean>> it = resourcesToProcess.entrySet().iterator();
        Map.Entry<File,Boolean> baseEntry = null;
        String baseName = null;
        while (it.hasNext()) {
            final Map.Entry<File,Boolean> entry = it.next();
            final File file = entry.getKey();
            if (baseName != null && file.getName().startsWith(baseName)) {
                if (baseEntry != null) {
                    baseEntry.setValue(Boolean.TRUE);
                    it.remove();
                }
                continue;
            }
            baseEntry  = entry;
            baseName   = getBaseName(file);
            var source = new File(file.getParentFile(), baseName + JAVA_EXT);
            baseName  += '_';
            if (checkSourceExists && !source.isFile()) {
                baseEntry = null;
            }
        }
    }

    /**
     * Returns the file name without the {@value #PROPERTIES_EXT} extension.
     * For multilingual resources, this is the base resource name.
     */
    private static String getBaseName(final File file) {
        final String name = file.getName();
        return name.substring(0, name.length() - PROPERTIES_EXT.length());
    }

    /**
     * Recursively scans the source directory and finds all Java classes having a property files of the same name.
     * Then invokes the resource compiler for those files by calls to {@link #onJavaSource(File)}.
     * Property files that cannot be compiled are linked or copied verbatim to the destination directory.
     *
     * @return the number of logical errors found in properties.
     * @throws IOException if an error occurred while reading or writing a file.
     */
    public int onJavaSourceDirectory() throws IOException {
        onJavaSourceDirectory(sourceDirectory);
        return errors;
    }

    /**
     * Implementation of {@link #onJavaSourceDirectory()} invoked recursively for scanning down the tree.
     *
     * @param  directory  root directory of Java source files for a single module.
     * @throws IOException if an error occurred while reading or writing a file.
     */
    private void onJavaSourceDirectory(final File directory) throws IOException {
        final File[] files = directory.listFiles((dir, name) -> !name.endsWith(JAVA_EXT));
        if (files == null) {
            throw new FileNotFoundException(directory + " not found or is not a directory.");
        }
        final var resourcesToProcess = new TreeMap<File,Boolean>();
        for (final File file : files) {
            final String name = file.getName();
            if (!name.isEmpty() && name.charAt(0) != '.') {
                if (file.isDirectory()) {
                    if (!EXCLUDE_DIRECTORY.equalsIgnoreCase(name)) {
                        onJavaSourceDirectory(file);
                    }
                } else if (name.endsWith(PROPERTIES_EXT)) {
                    resourcesToProcess.put(file, Boolean.FALSE);
                } else {
                    otherResource(file, sourceToTarget(file));
                }
            }
        }
        filterLanguages(resourcesToProcess, true);
        for (final Map.Entry<File,Boolean> entry : resourcesToProcess.entrySet()) {
            final File source = entry.getKey();
            if (entry.getValue() && acceptCompilableResource(source)) {
                onJavaSource(new File(source.getParentFile(), getBaseName(source) + JAVA_EXT));
            } else {
                otherResource(source, sourceToTarget(source));
            }
        }
    }

    /**
     * Runs the resource compiler on the given Java source file.
     * The source file may be modified.
     *
     * @param  resourceToProcess  the resource bundle base classes
     *         (e.g. {@code org/apache/sis/util/resources/Vocabulary.java}).
     * @return the number of logical errors found in properties.
     * @throws IOException if an error occurred while reading or writing a file.
     */
    public int onJavaSource(final File resourceToProcess) throws IOException {
        bundleClass = resourceToProcess;
        allocatedIDs.clear();
        resources.clear();
        loadKeyValues();
        scanForResources();
        return errors;
    }

    /**
     * Loads the existing key values from the source file. This is used in order to avoid
     * the need to recompile the whole application when new entries are added.
     *
     * @throws IOException if an error occurred while reading the source file.
     */
    private void loadKeyValues() throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(bundleClass), JAVA_ENCODING))) {
            String line;
            while ((line = in.readLine()) != null) {
                if ((line = line.trim()).startsWith(KEY_MODIFIERS)) {
                    final int s = line.indexOf('=', KEY_MODIFIERS.length());
                    if (s >= 0) {
                        final int c = line.indexOf(';', s);
                        if (c >= 0) {
                            final String key = line.substring(KEY_MODIFIERS.length(), s).trim();
                            final int    id  = Integer.parseInt(line.substring(s+1, c).trim());
                            final String old = allocatedIDs.put(id, key);
                            if (old != null) {
                                warning("Key " + id + " is used by " + old + " and " + key);
                                errors++;
                            } else if (id <= 0) {
                                final StringBuilder buffer = new StringBuilder(key).append(" = ")
                                        .append(id).append(" is not a valid value.");
                                if (id == 0) {
                                    buffer.append(" Zero is reserved for meaning “no localized message”.");
                                }
                                warning(buffer.toString());
                                errors++;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Converts a path relative to the source directory into a path relative to the target directory.
     *
     * @param  file  an absolute path to a source file.
     * @return absolute path to the destination file in the build directory.
     */
    private File sourceToTarget(final File file) throws IOException {
        String path = file.getPath();
        final String expected = sourceDirectory.getPath();
        if (!path.startsWith(expected)) {
            throw new IOException(path + " is not relative to " + expected);
        }
        path = path.substring(expected.length());
        if (!path.startsWith(File.separator)) {
            path = path.substring(File.separator.length());
        }
        return new File(buildDirectory, path);
    }

    /**
     * Searches {@code .properties} files in the package which contains {@link #bundleClass}.
     * The following methods are invoked by this method:
     *
     * <ul>
     *   <li>{@link #loadProperties(File)}</li>
     *   <li>{@link #writeUTF()}</li>
     *   <li>{@link #writeJavaSource()}</li>
     * </ul>
     *
     * @throws IOException if an input/output operation failed.
     */
    private void scanForResources() throws IOException {
        String classname = bundleClass.getName();
        classname = classname.substring(0, classname.lastIndexOf('.'));
        final File srcDir = bundleClass.getParentFile();
        final File utfDir = sourceToTarget(srcDir);
        if (!srcDir.isDirectory()) {
            throw new FileNotFoundException("\"" + srcDir + "\" is not a directory.");
        }
        if (utfDir.exists() && !utfDir.isDirectory()) {
            throw new FileNotFoundException("\"" + utfDir + "\" is not a directory.");
        }
        File defaultLanguage = null;
        final String prefix = classname;
        for (final File file : srcDir.listFiles((directory, name) -> name.endsWith(PROPERTIES_EXT) && name.startsWith(prefix))) {
            loadProperties(file);
            final String filename = file.getName();
            final String noExt = filename.substring(0, filename.length() - PROPERTIES_EXT.length());
            final File utfFile = new File(utfDir, noExt + RESOURCES_EXT);
            destination.put(utfFile, writeUTF());
            if (noExt.equals(classname)) {
                defaultLanguage = file;
            }
        }
        if (defaultLanguage != null) {
            resources.clear();
            resources.putAll(loadRawProperties(defaultLanguage));
        }
        writeJavaSource();
    }

    /**
     * Loads the specified property file. No processing are performed on them.
     *
     * @param  file  the property file to load.
     * @return the properties.
     * @throws IOException if the file cannot be read.
     */
    private static Properties loadRawProperties(final File file) throws IOException {
        final Properties properties;
        try (InputStream input = new FileInputStream(file)) {
            properties = new Properties();
            properties.load(input);
        }
        return properties;
    }

    /**
     * Loads all properties from a {@code .properties} file. Resource keys are checked for naming
     * conventions (i.e. resources expecting some arguments must have a key name ending with
     * {@code "_$n"} where {@code "n"} is the number of arguments). This method transforms resource
     * values into legal {@link MessageFormat} patterns when necessary.
     *
     * <p>The following methods must be invoked before this one:</p>
     *
     * <ul>
     *   <li>{@link #loadKeyValues()}</li>
     * </ul>
     *
     * @param  file  the properties file to read.
     * @throws IOException if an input/output operation failed.
     */
    private void loadProperties(final File file) throws IOException {
        resources.clear();
        final Properties properties = loadRawProperties(file);
        for (final Map.Entry<Object,Object> entry : properties.entrySet()) {
            final String key   = (String) entry.getKey();
            final String value = (String) entry.getValue();
            /*
             * Check key and value validity.
             */
            if (key.trim().isEmpty()) {
                warning(file, key, "Empty key.", null);
                continue;
            }
            if (value.trim().isEmpty()) {
                warning(file, key, "Empty value.", null);
                continue;
            }
            /*
             * Check if the resource value is a legal MessageFormat pattern.
             */
            final MessageFormat message;
            try {
                message = new MessageFormat(toMessageFormatString(value));
            } catch (IllegalArgumentException exception) {
                warning(file, key, "Bad resource value", exception);
                continue;
            }
            /*
             * Check if the expected arguments count (according to naming conventions)
             * matches the arguments count found in the MessageFormat pattern.
             */
            int argumentCount = 0;
            String resource = value;
            final int index = key.lastIndexOf(ARGUMENT_COUNT_PREFIX);
            if (index >= 0) try {
                String suffix = key.substring(index + ARGUMENT_COUNT_PREFIX.length());
                argumentCount = Integer.parseInt(suffix);
                resource = message.toPattern();
            } catch (NumberFormatException exception) {
                // No warning - allow use of underscore for other purpose.
            }
            if (resources.put(key, resource) != null) {
                warning(file, key, "Duplicated key", null);
            }
            final int expected = message.getFormatsByArgumentIndex().length;
            if (argumentCount != expected) {
                final String suffix = ARGUMENT_COUNT_PREFIX + expected;
                warning(file, key, "Key name should ends with \"" + suffix + "\".", null);
            }
        }
        /*
         * Allocate an ID for each new key. We start numbering at 1
         * because some classes use key value 0 for meaning "no message".
         */
        final String[] keys = resources.keySet().toArray(new String[resources.size()]);
        Arrays.sort(keys);
        int freeID = 0;
        for (final String key : keys) {
            if (!allocatedIDs.containsValue(key)) {
                Integer id;
                do {
                    id = ++freeID;
                } while (allocatedIDs.containsKey(id));
                allocatedIDs.put(id, key);
            }
        }
    }

    /**
     * Changes a "normal" text string into a pattern compatible with {@link MessageFormat}.
     * The main operation consists of changing ' for '', except for '{' and '}' strings.
     */
    private static String toMessageFormatString(final String text) {
        int level =  0;
        int last  = -1;
        final StringBuilder buffer = new StringBuilder(text);
search: for (int i=0; i<buffer.length(); i++) {                 // Length of `buffer` will vary.
            switch (buffer.charAt(i)) {
                /*
                 * Left and right braces take us up or down a level.  Quotes will only be doubled
                 * if we are at level 0.  If the brace is between quotes it will not be taken into
                 * account as it will have been skipped over during the previous pass through the
                 * loop.
                 */
                case '{' : level++; last=i; break;
                case '}' : level--; last=i; break;
                case '\'': {
                    /*
                     * If a brace ('{' or '}') is found between quotes, the entire block is
                     * ignored and we continue with the character following the closing quote.
                     */
                    if (i+2 < buffer.length()  &&  buffer.charAt(i+2) == '\'') {
                        switch (buffer.charAt(i+1)) {
                            case '{': i += 2; continue;
                            case '}': i += 2; continue;
                        }
                    }
                    if (level <= 0) {
                        /*
                         * If we weren't between braces, we must double the quotes.
                         */
                        buffer.insert(i++, '\'');
                        continue;
                    }
                    /*
                     * If we find ourselves between braces, we don't normally need to double our quotes.
                     * However, the format {0,choice,...} is an exception.
                     */
                    if (last >= 0  &&  buffer.charAt(last) == '{') {
                        int scan = last;
                        do if (scan >= i) continue search;
                        while (Character.isDigit(buffer.charAt(++scan)));
                        final String choice = ",choice,";
                        final int end = scan + choice.length();
                        if (end < buffer.length() && buffer.substring(scan, end).equalsIgnoreCase(choice)) {
                            buffer.insert(i++, '\'');
                        }
                    }
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Writes UTF data in an array.
     * The following methods must be invoked before this one:
     *
     * <ul>
     *   <li>{@link #loadKeyValues()}</li>
     *   <li>{@link #loadProperties(File)}</li>
     * </ul>
     *
     * @throws IOException if an input/output operation failed.
     */
    private byte[] writeUTF() throws IOException {
        bufferUTF.reset();
        final int count = allocatedIDs.isEmpty() ? 0 : Collections.max(allocatedIDs.keySet());
        try (DataOutputStream out = new DataOutputStream(bufferUTF)) {
            out.writeInt(count);
            for (int i=1; i<=count; i++) {
                final String value = (String) resources.get(allocatedIDs.get(i));
                out.writeUTF((value != null) ? value : "");
            }
        }
        return bufferUTF.toByteArray();
    }

    /**
     * Creates a source file for resource keys.
     * The following methods must be invoked before this one:
     *
     * <ul>
     *   <li>{@link #loadKeyValues()}</li>
     *   <li>{@link #loadProperties(File)}</li>
     * </ul>
     *
     * @throws IOException if an input/output operation failed.
     */
    private void writeJavaSource() throws IOException {
        /*
         * Opens the source file for reading. We will copy a subset of its content in a buffer.
         */
        final File file = bundleClass;
        if (!file.getParentFile().isDirectory()) {
            throw new FileNotFoundException("Parent directory not found for " + file);
        }
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final StringBuilder buffer = new StringBuilder(4096);
        /*
         * Copies everything up to (including) the constructor of the Keys inner class.
         * The declaration must follow Sun's convention on brace location (i.e. must be
         * on the same line than the class declaration).
         */
        boolean modified;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), JAVA_ENCODING))) {
            for (int state=0; state<=2; state++) {
                final String regex;
                switch (state) {
                    case 0: regex = "[\\s\\w]*class\\s+" + KEYS_INNER_CLASS + "\\s+[\\s\\w]*\\{";      break; // Class declaration
                    case 1: regex = "[\\s\\w]*\\s+" + KEYS_INNER_CLASS + "\\s*\\([\\s\\w]*\\)\\s+\\{"; break; // Constructor declaration
                    case 2: regex = "\\s*\\}"; break; // Constructor end.
                    default: throw new AssertionError(state);
                }
                final Pattern pattern = Pattern.compile(regex);
                String line;
                do {
                    line = in.readLine();
                    if (line == null) {
                        in.close();
                        throw new EOFException(file.toString());
                    }
                    buffer.append(line).append(lineSeparator);
                } while (!pattern.matcher(line).matches());
            }
            /*
             * Starting from this point, the content that we are going to write in the buffer
             * may be different than the file content.  Remember the buffer position in order
             * to allow us to compare the buffer with the file content.
             *
             * We stop reading the file for now. We will continue reading the file after the
             * `for` loop below. Instead, we now write the constructor followed by keys values.
             */
            int startLineToCompare = buffer.length();
            final Map.Entry<?,?>[] entries = allocatedIDs.entrySet().toArray(new Map.Entry<?,?>[allocatedIDs.size()]);
            Arrays.sort(entries, (o1, o2) -> ((String) o1.getValue()).compareTo((String) o2.getValue()));
            for (final Map.Entry<?,?> entry : entries) {
                buffer.append(lineSeparator);
                final String key = (String) entry.getValue();
                final String ID  = entry.getKey().toString();
                String message = (String) resources.get(key);
                if (message != null) {
                    message = message.replace('\t', ' ');
                    buffer.append(KEY_MARGIN).append("/**").append(lineSeparator);
                    while (((message=message.trim()).length()) != 0) {
                        buffer.append(KEY_MARGIN).append(" * ");
                        int stop = message.indexOf('\n');
                        if (stop < 0) {
                            stop = message.length();
                        }
                        if (stop > COMMENT_LENGTH) {
                            stop = COMMENT_LENGTH;
                            while (stop>20 && !Character.isWhitespace(message.charAt(stop))) {
                                stop--;
                            }
                        }
                        buffer.append(message.substring(0, stop).trim()).append(lineSeparator);
                        message = message.substring(stop);
                    }
                    buffer.append(KEY_MARGIN).append(" */").append(lineSeparator);
                }
                buffer.append(KEY_MARGIN).append(KEY_MODIFIERS).append(key).append(" = ")
                        .append(ID).append(';').append(lineSeparator);
            }
            /*
             * At this point, all key values have been written in the buffer. Skip the corresponding
             * lines from the files without adding them to the buffer. However, we will compare them
             * to the buffer content in order to detect if we really need to write the file.
             *
             * This operation will stop when we reach the closing bracket. Note that opening brackets
             * may exist in the code that we are skipping, so we need to count them.
             */
            modified = false;
            int brackets = 1;
            String line;
            do {
                line = in.readLine();
                if (line == null) {
                    in.close();
                    throw new EOFException(file.toString());
                }
                for (int i=0; i<line.length(); i++) {
                    switch (line.charAt(i)) {
                        case '{': brackets++; break;
                        case '}': brackets--; break;
                    }
                }
                if (!modified) {
                    final int endOfLine = buffer.indexOf(lineSeparator, startLineToCompare);
                    if (endOfLine >= 0) {
                        if (buffer.substring(startLineToCompare, endOfLine).equals(line)) {
                            startLineToCompare = endOfLine + lineSeparator.length();
                            continue;                   // Content is equal, do not set the `modified` flag.
                        }
                    } else if (brackets == 0) {
                        break;              // Content finished at the same time, do not set the `modified` flag.
                    }
                    modified = true;
                }
            } while (brackets != 0);
            /*
             * Only if we detected some changes in the file content, read all remaining parts of
             * the file then write the result to disk. Note that this overwite the original file.
             */
            if (modified) {
                buffer.append(line).append(lineSeparator);
                while ((line = in.readLine()) != null) {
                    buffer.append(line).append(lineSeparator);
                }
            }
        }
        if (modified) {
            try (Writer out = new OutputStreamWriter(new FileOutputStream(file), JAVA_ENCODING)) {
                out.write(buffer.toString());
            }
        }
    }

    /**
     * Invoked for each resource file that {@code IndexedResourceCompiler} thinks is compilable.
     * Subclasses can override this method for excluding some false positives.
     * The default implementation always returns {@code true}.
     *
     * @param  source  the {@code .properties} file that may be compiled.
     * @return whether to compile the specified file.
     */
    protected boolean acceptCompilableResource(final File source) {
        return true;
    }

    /**
     * Invoked when a file is not a Java source file, a properties file being compiled or a directory.
     * The default implementation does nothing.
     * Subclasses can override for copying of filtering the resource if desired.
     *
     * @param  source  the file which is not a Java source of properties file being compiled.
     * @param  target  destination file in the build directory if the implementation wants to copy.
     * @throws IOException if an error occurred while processing the resource.
     */
    protected void otherResource(File source, File target) throws IOException {
    }

    /**
     * Logs the given message at the {@code INFO} level.
     * The default implementation just sent it to the standard output stream.
     *
     * @param  message  the message to log.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    protected void info(final String message) {
        System.out.println(message);
    }

    /**
     * Logs the given message at the {@code WARNING} level.
     * The default implementation just sent it to the standard output stream.
     *
     * @param  message  the message to log.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    protected void warning(final String message) {
        System.out.println(message);
    }

    /**
     * Logs the given message at the {@code WARNING} level.
     *
     * @param  file       file that produced the error, or {@code null} if none.
     * @param  key        resource key that produced the error, or {@code null} if none.
     * @param  message    the message string.
     * @param  exception  an optional exception that is the cause of this warning.
     */
    private void warning(final File file,      final String key,
                         final String message, final Exception exception)
    {
        final StringBuilder buffer = new StringBuilder("ERROR ");
        if (file != null) {
            String filename = file.getPath();
            if (filename.endsWith(PROPERTIES_EXT)) {
                filename = filename.substring(0, filename.length() - PROPERTIES_EXT.length());
            }
            buffer.append('(').append(filename).append(')');
        }
        buffer.append(": ");
        if (key != null) {
            buffer.append('"').append(key).append('"');
        }
        warning(buffer.toString());
        buffer.setLength(0);
        buffer.append(message);
        if (exception != null) {
            buffer.append(": ").append(exception.getLocalizedMessage());
        }
        warning(buffer.toString());
        errors++;
    }
}
