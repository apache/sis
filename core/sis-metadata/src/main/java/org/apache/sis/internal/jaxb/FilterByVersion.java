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
package org.apache.sis.internal.jaxb;


/**
 * An enumeration of metadata or GML versions which determine which XML elements to include or exclude
 * during XML marshalling. If no marshalling is in progress, then all elements are considered included.
 * This enumeration is used in getter methods for XML elements that exist only in some versions of ISO
 * standards. They may be either deprecated elements to marshal only in legacy XML document formats,
 * or new elements to marshal only in new XML document formats.
 *
 * @author  Cullen Rombach (Image Matters)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public enum FilterByVersion {
    /**
     * {@code accept() == true} if marshalling of an ISO 19139:2007 document in in progress,
     * or if no marshalling in underway. Those documents are based on ISO 19115:2003 model.
     */
    LEGACY_METADATA(Context.MARSHALLING | Context.LEGACY_METADATA,
                    Context.MARSHALLING),

    /**
     * {@code accept() == true} if marshalling of an ISO 19115-3 document in in progress,
     * or if no marshalling in underway. Those documents are based on ISO 19115:2014 model.
     */
    CURRENT_METADATA(Context.MARSHALLING | Context.LEGACY_METADATA,
                     Context.MARSHALLING | Context.LEGACY_METADATA);

    /**
     * Mask to apply on {@link Context#bitMasks} in order to determine the version of the XML document
     * being marshalled.
     */
    private final int mask;

    /**
     * {@code accept() == false} if the XML document version being marshalled is this version.
     * We use exclusion instead of inclusion as an opportunistic way to get {@code true} if
     * no marshalling is in progress. This strategy works only if we have only two versions to
     * support and will need to be changed when we will have more versions.
     */
    private final int exclude;

    /**
     * Creates an enumeration value.
     */
    private FilterByVersion(final int mask, final int exclude) {
        this.mask    = mask;
        this.exclude = exclude;
    }

    /**
     * Returns {@code true} if we are marshalling the metadata or GML format identified by this constant,
     * or if no marshalling is in progress.
     *
     * @return {@code false} if the caller should omit XML element specific to the standard identified
     *         by this enumeration value.
     */
    public boolean accept() {
        final Context context = Context.current();
        return (context == null) || (context.bitMasks & mask) != exclude;
    }
}
