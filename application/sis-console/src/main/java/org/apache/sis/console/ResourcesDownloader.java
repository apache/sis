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

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.net.URLClassLoader;
import java.net.URL;
import java.io.Console;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.sql.Connection;                             // For javadoc.
import org.apache.sis.internal.system.DataDirectory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.X364;
import org.apache.sis.internal.util.Fallback;
import org.apache.sis.setup.InstallationResources;

import static org.apache.sis.internal.util.Constants.EPSG;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.AccessDeniedException;
import org.apache.sis.internal.jdk7.Path;


/**
 * A provider for data licensed under different terms of use than the Apache license.
 * This class is in charge of downloading the data if necessary and asking user's agreement
 * before to install them. Authorities managed by the current implementation are:
 *
 * <ul>
 *   <li>{@code "EPSG"} for the EPSG geodetic dataset.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@Fallback
public class ResourcesDownloader extends InstallationResources {
    /**
     * Where to download the EPSG scripts after user has approved the terms of use.
     *
     * THIS IS A TEMPORARY LINK to be moved to another location after discussion on the mailing list.
     * This temporary link is provided in order to allow experimentations by other SIS developers.
     */
    private static final String DOWNLOAD_URL = "http://home.apache.org/~desruisseaux/Temporary/geotk-epsg-4.0-SNAPSHOT.jar";

    /**
     * Estimation of the EPSG database size after installation, in mega-bytes.
     * This is for information purpose only.
     */
    private static final int DATABASE_SIZE = 20;

    /**
     * The console to use for printing EPSG terms of use and asking for agreement, or {@code null} if none.
     */
    private final Console console;

    /**
     * The locale to use for text display.
     */
    private final Locale locale;

    /**
     * {@code true} if colors can be applied for ANSI X3.64 compliant terminal.
     */
    private final boolean colors;

    /**
     * The provider to use for fetching the actual licensed data after we got user's agreement.
     */
    private InstallationResources provider;

    /**
     * The target directory where to install the database.
     */
    private final Path directory;

    /**
     * The localized answers expected from the users. Keys are words like "Yes" or "No"
     * and boolean values are the meaning of the keys.
     */
    private final Map<String,Boolean> answers = new HashMap<String,Boolean>();

    /**
     * {@code true} if the user has accepted the EPSG terms of use, {@code false} if (s)he refused,
     * or {@code null} if (s)he did not yet answered that question.
     */
    private Boolean accepted;

    /**
     * Creates a new installation scripts provider.
     */
    public ResourcesDownloader() {
        final CommandRunner command = CommandRunner.instance;
        if (command != null) {
            locale = command.locale;
            colors = command.colors;
        } else {
            locale = Locale.getDefault();
            colors = false;
        }
        console   = System.console();
        directory = DataDirectory.DATABASES.getDirectory();
    }

    /**
     * Returns the name of the authority who provides data under non-Apache terms of use.
     * If this {@code ResourcesDownloader} can not ask user's agreement because there is
     * no {@link Console} attached to the current Java virtual machine, then this method
     * returns an empty set.
     *
     * @return {@code "EPSG"} or an empty set.
     */
    @Override
    public Set<String> getAuthorities() {
        return (console != null && directory != null) ? Collections.singleton(EPSG) : Collections.<String>emptySet();
    }

    /**
     * Downloads the provider to use for fetching the actual licensed data after we got user's agreement.
     */
    private static InstallationResources download() throws IOException {
        for (final InstallationResources c : ServiceLoader.load(InstallationResources.class,
                new URLClassLoader(new URL[] {new URL(DOWNLOAD_URL)})))
        {
            if (!c.getClass().isAnnotationPresent(Fallback.class) && c.getAuthorities().contains(EPSG)) {
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
    private synchronized InstallationResources provider(final String authority, final boolean requireAgreement)
            throws IOException
    {
        if (!EPSG.equals(authority)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "authority", authority));
        }
        final ResourceBundle resources = ResourceBundle.getBundle("org.apache.sis.console.Messages", locale);
        if (answers.isEmpty()) {
            for (final String r : resources.getString("yes").split("\\|")) answers.put(r, Boolean.TRUE);
            for (final String r : resources.getString("no" ).split("\\|")) answers.put(r, Boolean.FALSE);
        }
        final String textColor, linkColor, linkOff, actionColor, resetColor;
        if (colors) {
            textColor   = X364.FOREGROUND_YELLOW .sequence();
            linkColor   = X364.UNDERLINE         .sequence();
            linkOff     = X364.NO_UNDERLINE      .sequence();
            actionColor = X364.FOREGROUND_GREEN  .sequence();
            resetColor  = X364.FOREGROUND_DEFAULT.sequence();
        } else {
            textColor = linkColor = linkOff = actionColor = resetColor = "";
        }
        /*
         * Start the download if the user accepts. We need to begin the download in order to get the
         * license text bundled in the JAR file. We will not necessarily ask for user agreement here.
         */
        if (provider == null) {
            if (console == null) {
                throw new IllegalStateException();
            }
            console.format(resources.getString("install"), textColor, DATABASE_SIZE, linkColor, directory, linkOff, resetColor);
            if (!accept(resources.getString("download"), textColor, resetColor)) {
                console.format("%n");
                throw new AccessDeniedException(null);
            }
            console.format(resources.getString("downloading"), actionColor, resetColor);
            provider = download();
        }
        /*
         * If there is a need to ask for user agreement and we didn't asked yet, ask now.
         */
        if (requireAgreement && accepted == null) {
            final String license = getLicense(authority, locale, "text/plain");
            if (license == null) {
                accepted = Boolean.TRUE;
            } else {
                console.format("%n").writer().write(license);
                console.format("%n");
                accepted = accept(resources.getString("accept"), textColor, resetColor);
                if (accepted) {
                    console.format(resources.getString("installing"), actionColor, resetColor);
                }
            }
        }
        if (accepted != null && !accepted) {
            throw new AccessDeniedException(null);
        }
        return provider;
    }

    /**
     * Asks the user to answer by "Yes" or "No". Callers is responsible for ensuring
     * that the {@link #answers} map is non-empty before to invoke this method.
     *
     * @param prompt Message to show to the user.
     * @return The user's answer.
     */
    private boolean accept(final String prompt, final Object... arguments) {
        Boolean answer;
        do {
            answer = answers.get(console.readLine(prompt, arguments).toLowerCase(locale));
        } while (answer == null);
        return answer;
    }

    /**
     * Returns the terms of use of the dataset provided by the given authority, or {@code null} if none.
     * The terms of use can be returned in either plain text or HTML.
     *
     * @param  authority One of the values returned by {@link #getAuthorities()}.
     * @param  mimeType Either {@code "text/plain"} or {@code "text/html"}.
     * @return The terms of use in plain text or HTML, or {@code null} if none.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IOException if an error occurred while reading the license file.
     */
    @Override
    public String getLicense(String authority, Locale locale, String mimeType) throws IOException {
        return provider(authority, false).getLicense(authority, locale, mimeType);
    }

    /**
     * Returns the names of installation scripts provided by the given authority.
     * This method is invoked by {@link org.apache.sis.referencing.factory.sql.EPSGFactory#install(Connection)}
     * for listing the SQL scripts to execute during EPSG dataset installation.
     *
     * <p>If that question has not already been asked, this method asks to the user if (s)he accepts
     * EPSG terms of use. If (s)he refuses, an {@link AccessDeniedException} will be thrown.</p>
     *
     * @param  authority One of the values returned by {@link #getAuthorities()}.
     * @return The names of all SQL scripts to execute.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IOException if an error occurred while fetching the script names.
     */
    @Override
    public String[] getResourceNames(final String authority) throws IOException {
        return provider(authority, true).getResourceNames(authority);
    }

    /**
     * Returns a reader for the installation script at the given index.
     * This method is invoked by {@link org.apache.sis.referencing.factory.sql.EPSGFactory#install(Connection)}
     * for getting the SQL scripts to execute during EPSG dataset installation.
     *
     * <p>If that question has not already been asked, this method asks to the user if (s)he accepts
     * EPSG terms of use. If (s)he refuses, an {@link AccessDeniedException} will be thrown.</p>
     *
     * @param  authority One of the values returned by {@link #getAuthorities()}.
     * @param  resource Index of the script to open, from 0 inclusive to
     *         <code>{@linkplain #getResourceNames(String) getResourceNames}(authority).length</code> exclusive.
     * @return A reader for the installation script content.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IndexOutOfBoundsException if the given {@code resource} argument is out of bounds.
     * @throws IOException if an error occurred while creating the reader.
     */
    @Override
    public BufferedReader openScript(final String authority, final int resource) throws IOException {
        return provider(authority, true).openScript(authority, resource);
    }
}
