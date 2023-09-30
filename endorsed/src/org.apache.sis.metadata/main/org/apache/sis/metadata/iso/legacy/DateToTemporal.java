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
package org.apache.sis.metadata.iso.legacy;

import java.time.temporal.Temporal;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;


/**
 * A collection where conversion from {@link Date} to {@link Temporal} objects are performed on-the-fly.
 * This is used for handling legacy metadata, before the move to {@link java.time}.
 * This adapter may be deleted after deprecated metadata methods have been removed from Apache SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DateToTemporal extends AbstractCollection<Temporal> {
    /**
     * The collection of dates.
     */
    private final Collection<? extends Date> source;

    /**
     * Creates a new view over the given collection.
     *
     * @param source the collection of dates to wrap.
     */
    public DateToTemporal(final Collection<? extends Date> source) {
        this.source = source;
    }

    /**
     * Returns the number of dates in this collection.
     *
     * @return number of dates.
     */
    @Override
    public int size() {
        return source.size();
    }

    /**
     * Returns an iterator over the dates in this collection.
     *
     * @return an iterator over the dates.
     */
    @Override
    public Iterator<Temporal> iterator() {
        final Iterator<? extends Date> dates = source.iterator();
        return new Iterator<Temporal>() {
            /** Tests whether there is more dates to return. */
            @Override public boolean hasNext() {
                return dates.hasNext();
            }

            /** Returns the next date, converted to a temporal object. */
            @Override public Temporal next() {
                final Date t = dates.next();
                return (t != null) ? t.toInstant() : null;
            }

            /** Remove the last date returned by the iterator. */
            @Override public void remove() {
                dates.remove();
            }
        };
    }

    /**
     * Returns a hash code value for this collection.
     * The hash code is determined by the backing collection.
     *
     * @return a hash code value for this collection.
     */
    @Override
    public int hashCode() {
        return source.hashCode() ^ 1137016072;
    }

    /**
     * Compares the specified object with this collection for equality.
     * Two {@code DateToTemporal} collections are equal if their backing collection are equal.
     *
     * @param  obj  the other object to compare to.
     * @return whether the two collections are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof DateToTemporal) && source.equals(((DateToTemporal) obj).source);
    }
}
