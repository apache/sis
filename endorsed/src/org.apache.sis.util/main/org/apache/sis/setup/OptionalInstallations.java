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

import java.util.Set;
import java.util.Locale;
import java.util.ServiceLoader;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.AccessDeniedException;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.system.Fallback;
import org.apache.sis.system.DataDirectory;
import static org.apache.sis.util.internal.shared.Constants.EPSG;


/**
 * A predefined set of data important to Apache SIS but not redistributed for space or licensing reasons.
 * This class is in charge of downloading the data if necessary and asking user's agreement before to install them.
 * Authorities managed by the current implementation are:
 *
 * <ul>
 *   <li>{@code "EPSG"} for the EPSG geodetic dataset.</li>
 * </ul>
 *
 * Data are downloaded from URLs hard-coded in this class. Those URLs depend on the Apache SIS versions in use,
 * typically because more recent SIS versions will reference more recent data.
 * The default URLs can be overridden using system properties documented in {@link #getDownloadURL(String)}.
 * This is useful as a workaround if a URL is no longer accessible.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.1
 */
public abstract class OptionalInstallations extends InstallationResources implements Localized {
    /**
     * The Maven repository from where to download artifacts.
     * This repository can be overridden by setting the {@code "org.apache.sis.epsg.downloadURL"} property.
     */
    private static final String REPOSITORY = "https://repo1.maven.org/maven2";

    /**
     * Path to the <abbr>EPSG</abbr> scripts relative to the selected repository.
     * This is where to download the database after user has approved the terms of use.
     */
    private static final String EPSG_DOWNLOAD_PATH = "/org/apache/sis/non-free/sis-epsg/1.5/sis-epsg-1.5.jar";

    /**
     * Estimation of the EPSG database size after installation, in megabytes.
     * This is for information purpose only.
     */
    private static final int DATABASE_SIZE = 24;

    /**
     * The MIME type to use for fetching license texts.
     * Value can be {@code "text/plain"} or {@code "text/html"}.
     */
    private final String licenseMimeType;

    /**
     * The target directory where to install the resources, or {@code null} if none.
     * This is the directory specified by the {@code SIS_DATA} environment variable.
     */
    protected final Path destinationDirectory;

    /**
     * The provider to use for fetching the actual licensed data after we got user's agreement.
     */
    private InstallationResources provider;

    /**
     * {@code true} if the user has accepted the EPSG terms of use, {@code false} if (s)he refused,
     * or {@code null} if (s)he did not yet answered that question.
     */
    private Boolean accepted;

    /**
     * Creates a new installation resources downloader.
     *
     * @param  licenseMimeType  either {@code "text/plain"} or {@code "text/html"}.
     */
    protected OptionalInstallations(final String licenseMimeType) {
        ArgumentChecks.ensureNonEmpty("licenseMimeType", licenseMimeType);
        this.licenseMimeType = licenseMimeType;
        destinationDirectory = DataDirectory.DATABASES.getDirectory();
    }

    /**
     * Asks to the user if (s)he agree to download and install the resource for the given authority.
     * This method may be invoked twice for the same {@code authority} argument:
     *
     * <ol>
     *   <li>With a null {@code license} argument for asking if the user agrees to download the data.</li>
     *   <li>With a non-null {@code license} argument for asking if the user agrees with the license terms.</li>
     * </ol>
     *
     * <h4>Design note</h4>
     * The download action needs to be initiated before to ask for license agreement
     * because the license text is bundled in the resource to download.
     *
     * @param  authority  one of the authorities returned by {@link #getAuthorities()}.
     * @param  license    the license, or {@code null} for asking if the user wants to download the data.
     * @return whether user accepted.
     */
    protected abstract boolean askUserAgreement(final String authority, final String license);

    /**
     * Returns the locale to use for messages shown to the user.
     * The default implementation returns the system default locale.
     *
     * @return the locale of messages shown to the user.
     */
    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    /**
     * Returns the names of the authorities providing data that can be installed.
     * The default implementation returns the authorities listed in class javadoc,
     * or a subset of those authorities if some of them cannot be installed
     * (for example because the {@code SIS_DATA} environment variable is not set).
     *
     * @return authorities of data that can be installed (may be an empty set).
     */
    @Override
    public Set<String> getAuthorities() {
        return (destinationDirectory != null) ? Set.of(EPSG) : Set.of();
    }

    /**
     * Returns the exception to throw for an unsupported authority.
     */
    private IllegalArgumentException unsupported(final String authority) {
        return new IllegalArgumentException(Errors.forLocale(getLocale())
                .getString(Errors.Keys.IllegalArgumentValue_2, "authority", authority));
    }

    /**
     * Returns an estimation of the space required on the host computer after download and installation.
     * This information can be shown to the user before to ask for confirmation.
     *
     * @param  authority  one of the authorities returned by {@link #getAuthorities()}.
     * @return an estimation of space requirement in megabytes.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     */
    public int getSpaceRequirement(final String authority) {
        switch (authority) {
            case EPSG: return DATABASE_SIZE;
            default: throw unsupported(authority);      // More authorities may be added in the future.
        }
    }

    /**
     * Returns the URL from where to download data for the specified authority.
     * The URLs are hard-coded and may change in any Apache SIS version.
     * The default URLs can be overridden using system properties documented below:
     *
     * <table class="sis">
     *   <caption>Configuration of download URLs</caption>
     *   <tr><th>Authority</th>  <th>System property</th></tr>
     *   <tr><td>EPSG</td>       <td>{@systemProperty org.apache.sis.epsg.downloadURL}</td></tr>
     * </table>
     *
     * The use of above-listed system properties is usually not needed,
     * except as a workaround if a hard-coded URL is no longer accessible.
     */
    private String getDownloadURL(final String authority) {
        final String base;
        switch (authority) {
            case EPSG: base = System.getProperty("org.apache.sis.epsg.downloadURL", REPOSITORY); break;
            default: throw unsupported(authority);      // More authorities may be added in the future.
        }
        return base + EPSG_DOWNLOAD_PATH;
    }

    /**
     * Downloads the provider to use for fetching the actual licensed data after we got user's agreement.
     *
     * @param  authority  one of the authorities returned by {@link #getAuthorities()}.
     * @return the actual provider for the given authority.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IOException if an error occurred while downloading the provider.
     * @throws IllegalArgumentException if the specified authority is not recognized.
     */
    private InstallationResources download(final String authority) throws IOException {
        final String source = getDownloadURL(authority);
        final URLClassLoader loader;
        try {
            loader = new URLClassLoader(new URL[] {new URI(source).toURL()});
        } catch (URISyntaxException e) {
            throw (MalformedURLException) new MalformedURLException().initCause(e);
        }
        for (final InstallationResources c : ServiceLoader.load(InstallationResources.class, loader)) {
            if (!c.getClass().isAnnotationPresent(Fallback.class) && c.getAuthorities().contains(authority)) {
                return c;
            }
        }
        // May happen if the URL is wrong.
        throw new FileNotFoundException(Errors.forLocale(getLocale()).getString(Errors.Keys.FileNotFound_1, source));
    }

    /**
     * Returns the provider to use for fetching the actual licensed data after we got user's agreement.
     * This method asks for user's agreement when first invoked.
     *
     * @param  requireAgreement  {@code true} if license agreement is required.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws AccessDeniedException if the user does not accept to install the licensed resource.
     * @throws IOException if an error occurred while downloading the resource.
     */
    private synchronized InstallationResources provider(final String authority, final boolean requireAgreement)
            throws IOException
    {
        if (!EPSG.equals(authority)) {
            throw unsupported(authority);
        }
        /*
         * Start the download if the user accepts. We need to begin the download in order to get the
         * license text bundled in the JAR file. Agreement with license terms will be asked later.
         */
        if (provider == null) {
            if (!askUserAgreement(authority, null)) {
                throw new AccessDeniedException(getDownloadURL(authority));
            }
            provider = download(authority);
        }
        /*
         * If there is a need to ask for user agreement and we didn't asked yet, ask now.
         */
        if (requireAgreement) {
            if (accepted == null) {
                final String license = getLicense(authority, getLocale(), licenseMimeType);
                accepted = (license == null) || askUserAgreement(authority, license);
            }
            if (!accepted) {
                throw new AccessDeniedException(getDownloadURL(authority));
            }
        }
        return provider;
    }

    /**
     * Returns the terms of use of the dataset provided by the given authority, or {@code null} if none.
     * The terms of use can be returned in either plain text or HTML.
     *
     * @param  authority  one of the values returned by {@link #getAuthorities()}.
     * @param  mimeType   either {@code "text/plain"} or {@code "text/html"}.
     * @return the terms of use in plain text or HTML, or {@code null} if none.
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
     * EPSG terms of use. If (s)he refuses, then an {@link AccessDeniedException} will be thrown.</p>
     *
     * @param  authority  one of the values returned by {@link #getAuthorities()}.
     * @return the names of all SQL scripts to execute.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IOException if an error occurred while fetching the script names.
     */
    @Override
    public String[] getResourceNames(final String authority) throws IOException {
        return provider(authority, true).getResourceNames(authority);
    }

    /**
     * Returns an installation resource for the given authority.
     * If that question has not already been asked, this method asks to the user if (s)he accepts
     * EPSG terms of use. If (s)he refuses, then an {@link AccessDeniedException} will be thrown.
     *
     * @param  authority  one of the values returned by {@link #getAuthorities()}.
     * @param  index      index of the resource to get, from 0 inclusive to
     *         <code>{@linkplain #getResourceNames(String) getResourceNames}(authority).length</code> exclusive.
     * @return the resource as an URL or any other type, at implementation choice.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IndexOutOfBoundsException if the given {@code resource} argument is out of bounds.
     * @throws IOException if an error occurred while fetching the resource.
     */
    @Override
    public Object getResource(final String authority, final int index) throws IOException {
        return provider(authority, true).getResource(authority, index);
    }

    /**
     * Returns a reader for the installation script at the given index.
     * This method is invoked by {@link org.apache.sis.referencing.factory.sql.EPSGFactory#install(Connection)}
     * for getting the SQL scripts to execute during EPSG dataset installation.
     *
     * <p>If that question has not already been asked, this method asks to the user if (s)he accepts
     * EPSG terms of use. If (s)he refuses, then an {@link AccessDeniedException} will be thrown.</p>
     *
     * @param  authority  one of the values returned by {@link #getAuthorities()}.
     * @param  resource   index of the script to open, from 0 inclusive to
     *                    <code>{@linkplain #getResourceNames(String) getResourceNames}(authority).length</code> exclusive.
     * @return a reader for the installation script content.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IndexOutOfBoundsException if the given {@code resource} argument is out of bounds.
     * @throws FileNotFoundException if the SQL script of the given name has not been found.
     * @throws IOException if an error occurred while creating the reader.
     */
    @Override
    public BufferedReader openScript(final String authority, final int resource) throws IOException {
        return provider(authority, true).openScript(authority, resource);
    }
}
