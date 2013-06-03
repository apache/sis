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

import java.util.Map;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;


/**
 * Keys in a map of options, together with static constants for commonly-used options.
 * Developers can subclass this class for defining their own options.
 *
 * @param <T> The type of option values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class OptionKey<T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7580514229639750246L;

    /**
     * The encoding of a URL (<strong>not</strong> the encoding of the document content).
     * This option may be used when converting a {@link String} or a {@link java.net.URL}
     * to a {@link java.net.URI} or a {@link java.io.File}:
     *
     * <ul>
     *   <li>URI are always encoded in UTF-8.</li>
     *   <li>URL are often encoded in UTF-8, but not necessarily. Other encodings are possible
     *       (while not recommended), or some URL may not be encoded at all.</li>
     * </ul>
     *
     * If this option is not provided, then the URL is assumed <strong>not</strong> encoded.
     *
     * <p><b>Example:</b> Given the {@code "file:Map%20with%20spaces.png"} URL, then:</p>
     * <ul>
     *   <li>If the URL encoding option is set to {@code "UTF-8"} or {@code "ISO-8859-1"}, then:<ul>
     *     <li>the encoded URI will be {@code "file:Map%20with%20spaces.png"};</li>
     *     <li>the decoded URI or the file will be {@code "file:Map with spaces.png"}.</li>
     *   </ul></li>
     *   <li>If the URL encoding option is set to {@code null} or is not provided, then:<ul>
     *     <li>the encoded URI will be {@code "file:Map%2520with%2520spaces.png"},
     *         i.e. the percent sign will be encoded as {@code "%25"};</li>
     *     <li>the decoded URI or the file will be {@code "file:Map%20with%20spaces.png"}.</li>
     *   </ul></li>
     * </ul>
     *
     * This option has not effect on URI encoding, which is always UTF-8.
     *
     * @see java.net.URLDecoder
     */
    public static final OptionKey<String> URL_ENCODING = new OptionKey<>("URL_ENCODING", String.class);

    /**
     * The byte buffer to use for input/output operations. Some {@link org.apache.sis.storage.DataStore}
     * implementations allow a byte buffer to be specified, thus allowing users to choose the buffer
     * {@linkplain ByteBuffer#capacity() capacity}, whether the buffer {@linkplain ByteBuffer#isDirect()
     * is direct}, or to recycle existing buffers.
     *
     * <p>It is user's responsibility to ensure that:</p>
     * <ul>
     *   <li>The buffer does not contains any valuable data, as it will be {@linkplain ByteBuffer#clear() cleared}.</li>
     *   <li>The same buffer is not used concurrently by two different {@code DataStore} instances.</li>
     * </ul>
     */
    public static final OptionKey<ByteBuffer> BYTE_BUFFER = new OptionKey<>("BYTE_BUFFER", ByteBuffer.class);

    /**
     * The name of this key. For {@code OptionKey} instances, it shall be the name of the static constants.
     * For subclasses of {@code OptionKey}, there is no restriction.
     */
    private final String name;

    /**
     * The type of values.
     */
    private final Class<T> type;

    /**
     * Creates a new key of the given name for values of the given type.
     *
     * @param name The key name.
     * @param type The type of values.
     */
    protected OptionKey(final String name, final Class<T> type) {
        ArgumentChecks.ensureNonEmpty("name", name);
        ArgumentChecks.ensureNonNull ("type", type);
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the name of this option key.
     *
     * @return The name of this option key.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of values associated to this option key.
     *
     * @return The type of values.
     */
    public final Class<T> getElementType() {
        return type;
    }

    /**
     * Returns the option value in the given map for this key, or {@code null} if none.
     * This is a convenience method for implementors, which can be used as below:
     *
     * {@preformat java
     *     public <T> T getOption(final OptionKey<T> key) {
     *         ArgumentChecks.ensureNonNull("key", key);
     *         return key.getValueFrom(options);
     *     }
     * }
     *
     * @param  options The map where to search for the value, or {@code null} if not yet created.
     * @return The current value in the map for the this option, or {@code null} if none.
     */
    public T getValueFrom(final Map<OptionKey<?>,?> options) {
        return (options != null) ? type.cast(options.get(this)) : null;
    }

    /**
     * Sets a value for this option key in the given map, or in a new map if the given map is {@code null}.
     * This is a convenience method for implementors, which can be used as below:
     *
     * {@preformat java
     *     public <T> void setOption(final OptionKey<T> key, final T value) {
     *         ArgumentChecks.ensureNonNull("key", key);
     *         options = key.setValueInto(options, value);
     *     }
     * }
     *
     * @param  options The map where to set the value, or {@code null} if not yet created.
     * @param  value   The new value for the given option, or {@code null} for removing the value.
     * @return The given map of options, or a new map if the given map was null. The returned value
     *         may be null if the given map and the given value are both null.
     */
    public Map<OptionKey<?>,Object> setValueInto(Map<OptionKey<?>,Object> options, final T value) {
        if (value != null) {
            if (options == null) {
                options = new HashMap<>();
            }
            options.put(this, value);
        } else if (options != null) {
            options.remove(this);
        }
        return options;
    }

    /**
     * Returns {@code true} if the given object is an instance of the same class having the same name and type.
     *
     * @param object The object to compare with this {@code OptionKey} for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final OptionKey<?> that = (OptionKey<?>) object;
            return name.equals(that.name) && type == that.type;
        }
        return false;
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return name.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this option key.
     * The default implementation returns the value of {@link #getName()}.
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Resolves this option key on deserialization. This method is invoked
     * only for instance of the exact {@code OptionKey} class, not subclasses.
     */
    private Object readResolve() throws ObjectStreamException {
        try {
            return OptionKey.class.getField(name).get(null);
        } catch (ReflectiveOperationException e) {
            /*
             * This may happen if we are deserializing a stream produced by a more recent SIS library
             * than the one running in this JVM. This class should be robust to this situation, since
             * we override the 'equals' and 'hashCode' methods. This option is likely to be ignored,
             * but options are expected to be optional...
             */
            Logging.recoverableException(OptionKey.class, "readResolve", e);
            return this;
        }
    }
}
