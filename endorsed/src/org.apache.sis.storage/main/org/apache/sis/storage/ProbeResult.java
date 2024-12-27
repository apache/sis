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
package org.apache.sis.storage;

import java.util.Objects;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.apache.sis.util.Version;
import org.apache.sis.util.privy.Strings;


/**
 * Tells whether a storage (file, database) appears to be supported by a {@code DataStore}.
 * {@code ProbeResult} may also provide additional information, like file MIME type and the
 * format version.
 *
 * <h2>Usage</h2>
 * When a {@link DataStores#open DataStores.open(…)} method is invoked, SIS will iterate over the list of known
 * providers and invoke the {@link DataStoreProvider#probeContent(StorageConnector)} method for each of them.
 * The {@code ProbeResult} value returned by {@code probeContent(…)} tells to SIS whether a particular
 * {@code DataStoreProvider} instance has reasonable chances to be able to handle the given storage.
 *
 * <p>Whether a storage appears to be supported or not is given by the {@link #isSupported()} property.
 * Other properties like {@link #getVersion()} are sometimes available for both supported and unsupported storages.
 * For example, a file may be encoded in a known format, but may be using an unsupported version of that format.</p>
 *
 * <h2>Special values</h2>
 * In addition to the supported/unsupported information, {@code ProbeResult} defines two constants having
 * a special meaning: {@link #INSUFFICIENT_BYTES} and {@link #UNDETERMINED}, which indicate that the provider does
 * not have enough information for telling whether the storage can be opened.
 * In such cases, SIS will revisit those providers only if no better suited provider is found.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see DataStoreProvider#probeContent(StorageConnector)
 *
 * @since 0.4
 */
public class ProbeResult implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4977853847503500550L;

    /**
     * The {@code DataStoreProvider} will create a new file.
     * The file does not exist yet or is empty.
     *
     * @since 1.5
     */
    public static final ProbeResult CREATE_NEW = new Constant(true, "CREATE_NEW");

    /**
     * The {@code DataStoreProvider} recognizes the given storage, but has no additional information.
     * The {@link #isSupported()} method returns {@code true}, but the {@linkplain #getMimeType() MIME type}
     * and {@linkplain #getVersion() version} properties are {@code null}.
     *
     * <p>{@link DataStoreProvider#probeContent(StorageConnector)} implementations should consider returning a
     * {@linkplain #ProbeResult(boolean, String, Version) new instance} instead of this constant
     * if they can provide the file MIME type or the format version number.</p>
     */
    public static final ProbeResult SUPPORTED = new Constant(true, "SUPPORTED");

    /**
     * The {@code DataStoreProvider} does not recognize the given storage object, file format or database schema.
     * No other information is available: the {@link #isSupported()} method returns {@code false}, and the
     * {@linkplain #getMimeType() MIME type} and {@linkplain #getVersion() version} properties are {@code null}.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>The storage is a file while the provider expected a database connection (or conversely).</li>
     *   <li>The file does not contains the expected magic number.</li>
     *   <li>The database schema does not contain the expected tables.</li>
     * </ul>
     *
     * {@link DataStoreProvider#probeContent(StorageConnector)} implementations should consider returning a
     * {@linkplain #ProbeResult(boolean, String, Version) new instance} instead of this constant
     * if the {@code DataStoreProvider} recognizes the given storage, but the data are structured
     * according a file or schema version not yet supported by the current implementation.
     */
    public static final ProbeResult UNSUPPORTED_STORAGE = new Constant(false, "UNSUPPORTED_STORAGE");

    /**
     * The open capability cannot be determined because the {@link java.nio.ByteBuffer} contains an insufficient
     * number of bytes. This value can be returned by {@link DataStoreProvider#probeContent(StorageConnector)}
     * implementations as below:
     *
     * {@snippet lang="java" :
     *     public ProbeResult probeContent(StorageConnector storage) throws DataStoreException {
     *         final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
     *         if (buffer == null) {
     *             return ProbeResult.UNSUPPORTED_STORAGE;
     *         }
     *         if (buffer.remaining() < Integer.BYTES) {
     *             return ProbeResult.INSUFFICIENT_BYTES;
     *         }
     *         // Other verifications here.
     *     }
     * }
     *
     * When searching for a provider capable to read a given file,
     * if at least one {@code DataStoreProvider} returns {@code INSUFFICIENT_BYTES}, then:
     *
     * <ol>
     *   <li>SIS will continue to search for another provider for which {@code probeContent(…)}
     *       declares to support the storage, using only the available bytes.</li>
     *   <li>Only if no such provider can be found, then SIS will fetch more bytes and query again
     *       the providers that returned {@code INSUFFICIENT_BYTES} in the previous iteration.</li>
     * </ol>
     *
     * SIS tries to work with available bytes before to ask more in order to reduce latencies on network connections.
     */
    public static final ProbeResult INSUFFICIENT_BYTES = new Constant(false, "INSUFFICIENT_BYTES");

    /**
     * The open capability cannot be determined.
     * This value may be returned by {@code DataStore} implementations that could potentially open anything,
     * for example the RAW image format.
     *
     * <p><strong>This is a last resort value!</strong> {@code probeContent(…)} implementations are strongly encouraged
     * to return a more accurate enumeration value for allowing {@link DataStores#open(Object)} to perform a better
     * choice. Generally, this value should be returned only by the RAW image format.</p>
     */
    public static final ProbeResult UNDETERMINED = new Constant(false, "UNDETERMINED");

    /**
     * {@code true} if the storage is supported by the {@link DataStoreProvider}.
     *
     * @see #isSupported()
     */
    private final boolean isSupported;

    /**
     * The storage MIME type, or {@code null} if unknown or not applicable.
     *
     * @see #getMimeType()
     */
    private final String mimeType;

    /**
     * The version of file format or database schema used by the storage, or {@code null} if unknown or not applicable.
     *
     * @see #getVersion()
     */
    private final Version version;

    /**
     * Creates a new {@code ProbeResult} with the given support status, MIME type and version number.
     *
     * @param isSupported  {@code true} if the storage is supported by the {@link DataStoreProvider}.
     * @param mimeType     the storage MIME type, or {@code null} if unknown or not applicable.
     * @param version      the version of file format or database schema used by the storage,
     *                     or {@code null} if unknown or not applicable.
     */
    public ProbeResult(final boolean isSupported, final String mimeType, final Version version) {
        this.isSupported = isSupported;
        this.mimeType    = mimeType;
        this.version     = version;
    }

    /**
     * Returns {@code true} if the storage is supported by the {@code DataStoreProvider}.
     * {@code DataStore} instances created by that provider are likely (but not guaranteed)
     * to be able to read from - and eventually write to - the given storage.
     *
     * @return {@code true} if the storage is supported by the {@link DataStoreProvider}.
     */
    public boolean isSupported() {
        return isSupported;
    }

    /**
     * Returns the MIME type of the storage file format, or {@code null} if unknown or not applicable.
     * The {@link DataStoreProvider} may (at implementation choice) inspect the storage content for
     * determining a more accurate MIME type.
     *
     * <h4>XML types</h4>
     * A generic MIME type for XML documents is {@code "application/xml"}.
     * However, many other MIME types exist for XML documents compliant to some particular shema.
     * Those types can be determined by inspecting the namespace of XML root element.
     * The following table gives some examples:
     *
     * <table class="sis">
     * <caption>MIME type examples</caption>
     *   <tr><th>MIME type</th>                                                    <th>Description</th>                                 <th>Namespace</th></tr>
     *   <tr><td>{@code "application/gml+xml"}</td>                                <td>Official mime type for OGC GML</td>              <td>{@value org.apache.sis.xml.Namespaces#GML}</td></tr>
     *   <tr><td>{@code "application/vnd.eu.europa.ec.inspire.resource+xml"}</td>  <td>Official mime type for INSPIRE Resources</td>    <td></td></tr>
     *   <tr><td>{@code "application/vnd.iso.19139+xml"}</td>                      <td>Unofficial mime type for ISO 19139 metadata</td> <td>{@value org.apache.sis.xml.Namespaces#GMD}</td></tr>
     *   <tr><td>{@code "application/vnd.ogc.wms_xml"}</td>                        <td>Unofficial mime type for OGC WMS</td>            <td></td></tr>
     *   <tr><td>{@code "application/vnd.ogc.wfs_xml"}</td>                        <td>Unofficial mime type for OGC WFS </td>           <td></td></tr>
     *   <tr><td>{@code "application/vnd.ogc.csw_xml"}</td>                        <td>Unofficial mime type for OGC CSW</td>            <td>{@value org.apache.sis.xml.Namespaces#CSW}</td></tr>
     *   <tr><td>{@code "application/vnd.google-earth.kml+xml"}</td>               <td></td><td></td></tr>
     *   <tr><td>{@code "application/rdf+xml"}</td>                                <td></td><td></td></tr>
     *   <tr><td>{@code "application/soap+xml"}</td>                               <td></td><td></td></tr>
     * </table>
     *
     * @return the storage MIME type, or {@code null} if unknown or not applicable.
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the version of file format or database schema used by the storage,
     * or {@code null} if unknown or not applicable.
     *
     * @return the version of file format or database schema used by the storage,
     *         or {@code null} if unknown or not applicable.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Returns a hash code value for this instance.
     */
    @Override
    public int hashCode() {
        return Objects.hash(isSupported, mimeType, version) ^ (int) serialVersionUID;
    }

    /**
     * Compares this {@code ProbeResult} with the given object for equality.
     * Two {@code ProbeResult}s are equal if they are instances of the same class
     * and all their properties are equal.
     *
     * @param  object  the object to compare with this {@code ProbeResult}.
     * @return {@code true} if the two objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final ProbeResult other = (ProbeResult) object;
            return isSupported == other.isSupported &&
                    Objects.equals(mimeType, other.mimeType) &&
                    Objects.equals(version,  other.version);
        }
        return false;
    }

    /**
     * Returns a string representation of this {@code ProbeResult} for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "isSupported", isSupported, "mimeType", mimeType, "version", version);
    }

    /**
     * Implementation of static constants defined in {@link ProbeResult}.
     * We need a special implementation class in order to resolve deserialized instances to their unique instance.
     */
    private static final class Constant extends ProbeResult {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5239064423134309133L;

        /**
         * The name of the public static field constant.
         * Each name shall be unique.
         */
        private final String name;

        /**
         * Creates a new constant for a public static field of the given name.
         */
        Constant(final boolean isSupported, final String name) {
            super(isSupported, null, null);
            this.name = name;
        }

        /**
         * Invoked on deserialization for fetching the unique instance, if possible.
         * If we fail to resolve (which may happen if the instance has been serialized
         * by a more recent SIS version), returns the instance unchanged. It should be
         * okay if all comparisons are performed by the {@code equals} method instead
         * than the {@code ==} operator.
         */
        Object readResolve() throws ObjectStreamException {
            try {
                return ProbeResult.class.getField(name).get(null);
            } catch (ReflectiveOperationException e) {
                return this;                                            // See javadoc
            }
        }

        /**
         * Compares the name, which is okay since each name are unique.
         */
        @Override
        public boolean equals(final Object object) {
            return (object instanceof Constant) && name.equals(((Constant) object).name);
        }

        /**
         * Returns a hash code derived from the name, which is okay since each name are unique.
         */
        @Override
        public int hashCode() {
            return name.hashCode() ^ (int) serialVersionUID;
        }

        /**
         * Returns the constant name for debugging purpose.
         */
        @Override
        public String toString() {
            return Strings.bracket(ProbeResult.class, name);
        }
    }
}
