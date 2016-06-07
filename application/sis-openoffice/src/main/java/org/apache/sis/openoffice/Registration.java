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
package org.apache.sis.openoffice;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.Pack200;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.comp.loader.FactoryHelper;
import com.sun.star.registry.XRegistryKey;
import org.apache.sis.referencing.CRS;


/**
 * The registration of all formulas provided in this package.
 *
 * <div class="section">Implementation note</div>
 * No GeoAPI or Apache SIS classes should appear in method signature. For example no method
 * should contain {@link org.opengis.util.FactoryException} in their {@code throws} declaration.
 * This is because those classes can be loaded only after the Pack200 files have been unpacked,
 * which is the work of this {@code Registration} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class Registration implements FilenameFilter {
    /**
     * Filename extension of Pack200 files.
     */
    private static final String PACK = "pack";

    /**
     * Name of the logger to use for the add-ins.
     */
    static final String LOGGER = "org.apache.sis.openoffice";

    /**
     * Do not allow instantiation of this class, except for internal use.
     */
    private Registration() {
    }

    /**
     * Returns the directory where the add-in is installed.
     *
     * @return the installation directory.
     * @throws URISyntaxException if the path to the add-in JAR file does not have the expected syntax.
     */
    private static File getInstallDirectory() throws URISyntaxException {
        String path = Registration.class.getResource("Registration.class").toExternalForm();
        int numParents = 5;                     // Number of calls to File.getParentFile() needed for reaching the root.
        if (path.startsWith("jar:")) {
            path = path.substring(4, path.indexOf('!'));
            numParents = 1;                     // The file should be the sis-openoffice.jar file in the root.
        }
        File file = new File(new URI(path));
        while (--numParents >= 0) {
            file = file.getParentFile();
        }
        return file;
    }

    /**
     * Ensures that the {@code sis.pack} files have been uncompressed.
     *
     * @throws URISyntaxException if the path to the add-in JAR file does not have the expected syntax.
     * @throws IOException if an error occurred while uncompressing the PACK200 file.
     */
    private static void ensureInstalled() throws URISyntaxException, IOException {
        final File directory = getInstallDirectory();
        final String[] content = directory.list(new Registration());
        if (content != null && content.length != 0) {
            final Pack200.Unpacker unpacker = Pack200.newUnpacker();
            for (final String filename : content) {
                final File packFile = new File(directory, filename);
                final File jarFile  = new File(directory, filename.substring(0, filename.length() - PACK.length()) + "jar");
                try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jarFile))) {
                    unpacker.unpack(packFile, out);
                }
                packFile.delete();
            }
            /*
             * Ensures that the EPSG database is installed. We force the EPSG installation at add-in
             * installation time rather than the first time a user ask for a referencing operations,
             * because users may be less surprised by a delay at installation time than at use time.
             * However if the EPSG database is deleted after the installation, it will be recreated
             * when first needed.
             *
             * Note: do not reach this code before all Pack200 files have been unpacked.
             * Remainder: no GeoAPI or Apache SIS classes in any method signature of this class!
             */
            try {
                CRS.forCode("EPSG:4326");
            } catch (Throwable e) {
                unexpectedException(CRS.class, "forCode", e);
                // Ignore. A new attempt to create the EPSG database will be performed
                // when first needed, and the user may see the error at that time.
            }
        }
    }

    /**
     * Logs the given exception.
     */
    private static void unexpectedException(final Class<?> classe, final String method, final Throwable exception) {
        final Logger logger = Logger.getLogger(LOGGER);
        final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
        record.setLoggerName(logger.getName());
        record.setSourceClassName(classe.getName());
        record.setSourceMethodName(method);
        record.setThrown(exception);
        logger.log(record);
    }

    /**
     * Filters a directory content in order to retain only the {@code "*.pack"} files.
     *
     * @param directory the add-in installation directory.
     * @param name the name of a file in the given directory.
     */
    @Override
    public boolean accept(final File directory, final String name) {
        return name.endsWith('.' + PACK);
    }

    /**
     * Returns a factory for creating the service.
     * This method is called by the {@code com.sun.star.comp.loader.JavaLoader}; do not rename!
     *
     * @param   implementation the name of the implementation for which a service is desired.
     * @param   factories the service manager to be used if needed.
     * @param   registry the registry key
     * @return  A factory for creating the component.
     * @throws  URISyntaxException if the path to the add-in JAR file does not have the expected syntax.
     * @throws  IOException if an error occurred while uncompressing the PACK200 file.
     */
    public static XSingleServiceFactory __getServiceFactory(
            final String               implementation,
            final XMultiServiceFactory factories,
            final XRegistryKey         registry) throws URISyntaxException, IOException
    {
        ensureInstalled();
        if (implementation.equals(Referencing.class.getName())) {
            return FactoryHelper.getServiceFactory(Referencing.class, Referencing.__serviceName, factories, registry);
        }
        return null;
    }

    /**
     * Writes the service information into the given registry key.
     * This method is called by the {@code com.sun.star.comp.loader.JavaLoader}; do not rename!
     *
     * @param  registry the registry key.
     * @return {@code true} if the operation succeeded.
     * @throws URISyntaxException if the path to the add-in JAR file does not have the expected syntax.
     * @throws IOException if an error occurred while uncompressing the PACK200 file.
     */
    public static boolean __writeRegistryServiceInfo(final XRegistryKey registry)
            throws URISyntaxException, IOException
    {
        ensureInstalled();
        return register(Referencing.class, Referencing.__serviceName, registry);
    }

    /**
     * Helper method for the above {@link #__writeRegistryServiceInfo} method.
     */
    private static boolean register(final Class<? extends CalcAddins> classe,
            final String serviceName, final XRegistryKey registry)
    {
        final String cn = classe.getName();
        return FactoryHelper.writeRegistryServiceInfo(cn, serviceName, registry) &&
               FactoryHelper.writeRegistryServiceInfo(cn, CalcAddins.ADDIN_SERVICE, registry);
    }
}
