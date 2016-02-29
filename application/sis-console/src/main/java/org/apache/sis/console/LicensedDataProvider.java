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
package org.apache.sis.console;

import java.util.Locale;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.sql.Connection;                             // For javadoc.
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.factory.sql.InstallationScriptProvider;
import org.apache.sis.internal.util.Fallback;


/**
 * A provider for data licensed under different terms of use than the Apache license.
 * This class is in charge of asking user's agreement before to install those data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@Fallback
public class LicensedDataProvider extends InstallationScriptProvider {
    /**
     * Where to download the EPSG scripts after user has approved the terms of use.
     *
     * THIS IS A TEMPORARY LINK to be moved to another location after discussion on the mailing list.
     * This temporary link is provided in order to allow experimentations by other SIS developers.
     */
    private static final String DOWNLOAD_URL = "http://home.apache.org/~desruisseaux/Temporary/geotk-epsg-4.0-SNAPSHOT.jar";

    /**
     * The console to use for printing EPSG terms of use and asking for agreement, or {@code null} if none.
     */
    private final Console console;

    /**
     * The locale to use for text display.
     */
    private final Locale locale;

    /**
     * The provider to use for fetching the actual licensed data after we got user's agreement.
     */
    private InstallationScriptProvider provider;

    /**
     * {@code true} if the user has accepted the EPSG terms of use, {@code false} if (s)he refused,
     * or {@code null} if (s)he did not yet answered that question.
     */
    private Boolean accepted;

    /**
     * Creates a new installation scripts provider.
     */
    public LicensedDataProvider() {
        console = System.console();
        locale = Locale.getDefault();
    }

    /**
     * Returns the name of the authority who provides data under non-Apache terms of use.
     * If this {@code LicensedDataProvider} can not ask user's agreement because there is
     * no {@link Console} attached to the current Java virtual machine, then this method
     * returns {@code "unavailable"}.
     *
     * @return {@code "EPSG"} or {@code "unavailable"}.
     */
    @Override
    public String getAuthority() {
        return (console != null) ? Constants.EPSG : "unavailable";
    }

    /**
     * Downloads the provider to use for fetching the actual licensed data after we got user's agreement.
     */
    private static InstallationScriptProvider download() throws IOException {
        for (final InstallationScriptProvider c : ServiceLoader.load(InstallationScriptProvider.class,
                new URLClassLoader(new URL[] {new URL(DOWNLOAD_URL)})))
        {
            if (!c.getClass().isAnnotationPresent(Fallback.class)) {
                return c;
            }
        }
        throw new FileNotFoundException();      // Should not happen.
    }

    /**
     * Returns the provider to use for fetching the actual licensed data after we got user's agreement.
     * This method asks for user's agreement when first invoked.
     *
     * @param  requireAgreement {@code true} if license agreement is required.
     * @throws AccessDeniedException if the user does not accept to install the EPSG dataset.
     * @throws IOException if an error occurred while reading the {@link #DOWNLOAD_URL}.
     */
    private synchronized InstallationScriptProvider provider(final boolean requireAgreement) throws IOException {
        if (provider == null) {
            if (console == null) {
                throw new IllegalStateException();
            }
            console.format("%nInstallation of EPSG geodetic dataset is recommended for this operation.%n");
            if (!accept("Download and install now? (Yes/No) ")) {
                console.format("%n");
                throw new AccessDeniedException(null);
            }
            console.format("Downloading...%n");
            provider = download();
        }
        if (requireAgreement && accepted == null) {
            final String license = getLicense(locale, "text/plain");
            if (license != null) {
                console.format("%n").writer().write(license);
            }
            accepted = accept("%nAccept License Agreement? (Yes/No) ");
            if (accepted) {
                console.format("Installing...");
            }
            console.format("%n");
        }
        if (accepted != null && !accepted) {
            throw new AccessDeniedException(null);
        }
        return provider;
    }

    /**
     * Asks the user to answer by "Yes" or "No".
     *
     * @param prompt Message to show to the user.
     * @return The user's answer.
     */
    private boolean accept(final String prompt) {
        String line;
        do {
            line = console.readLine(prompt);
            if (line.equalsIgnoreCase("n") || line.equalsIgnoreCase("no")) {
                return false;
            }
        } while (!line.equalsIgnoreCase("y") && !line.equalsIgnoreCase("yes"));
        return true;
    }

    /**
     * Returns the terms of use of the dataset, or {@code null} if none.
     * The terms of use can be returned in either plain text or HTML.
     *
     * @param  mimeType Either {@code "text/plain"} or {@code "text/html"}.
     * @return The terms of use in plain text or HTML, or {@code null} if none.
     * @throws IOException if an error occurred while reading the license file.
     */
    @Override
    public String getLicense(Locale locale, String mimeType) throws IOException {
        return provider(false).getLicense(locale, mimeType);
    }

    /**
     * Returns the names of all SQL scripts to execute.
     * This method is invoked by {@link org.apache.sis.referencing.factory.sql.EPSGFactory#install(Connection)}
     * for listing the SQL scripts to execute during EPSG dataset installation.
     *
     * <p>If that question has not already been asked, this method asks to the user if (s)he accepts
     * EPSG terms of use. If (s)he refuses, an {@link AccessDeniedException} will be thrown.</p>
     *
     * @return The names of all SQL scripts to execute.
     * @throws IOException if an error occurred while fetching the script names.
     */
    @Override
    public String[] getScriptNames() throws IOException {
        return provider(true).getScriptNames();
    }

    /**
     * Returns a reader for the SQL script at the given index.
     * This method is invoked by {@link org.apache.sis.referencing.factory.sql.EPSGFactory#install(Connection)}
     * for getting the SQL scripts to execute during EPSG dataset installation.
     *
     * <p>If that question has not already been asked, this method asks to the user if (s)he accepts
     * EPSG terms of use. If (s)he refuses, an {@link AccessDeniedException} will be thrown.</p>
     *
     * @param  index Index of the SQL script to read, from 0 inclusive to
     *         <code>{@linkplain #getScriptNames()}.length</code> exclusive.
     * @return A reader for the content of SQL script to execute.
     * @throws IOException if an error occurred while creating the reader.
     */
    @Override
    public BufferedReader getScriptContent(final int index) throws IOException {
        return provider(true).getScriptContent(index);
    }

    /**
     * Unsupported operation.
     *
     * @param name Ignored.
     * @return {@code null}.
     */
    @Override
    protected InputStream open(String name) {
        return null;
    }
}
