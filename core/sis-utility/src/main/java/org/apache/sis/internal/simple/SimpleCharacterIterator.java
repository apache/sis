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
package org.apache.sis.internal.simple;

import java.io.Serializable;
import java.text.CharacterIterator;
import org.apache.sis.util.ArgumentChecks;


/**
 * A simple implementation of the {@link CharacterIterator} interface as a wrapper
 * around a given {@link CharSequence}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class SimpleCharacterIterator implements CharacterIterator, CharSequence, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4211374670559434445L;

    /**
     * The wrapped character sequence.
     */
    protected final CharSequence text;

    /**
     * Index of the first character that we can return. Fixed to 0 for now, but declared
     * as a variable in case we want to support non-zero start index in a future version.
     */
    protected static final int lower = 0;

    /**
     * The upper index (index after the last character that we can return).
     * This field is not final because some classes need to update it, for
     * example if {@link #text} is a growing {@link StringBuffer}.
     */
    protected int upper;

    /**
     * The index of the next character to be returned by the iterator.
     */
    private int index;

    /**
     * Creates a new character iterator for the given character sequence.
     *
     * @param text The character sequence to wrap.
     */
    public SimpleCharacterIterator(final CharSequence text) {
        ArgumentChecks.ensureNonNull("text", text);
        this.text = text;
        upper = text.length();
    }

    /**
     * Sets the position to the beginning and returns the character at that position.
     */
    @Override
    public final char first() {
        if (upper == lower) {
            return DONE;
        }
        return text.charAt(index = lower);
    }

    /**
     * Sets the position to the end and returns the character at that position.
     */
    @Override
    public final char last() {
        if (upper == lower) {
            return DONE;
        }
        return text.charAt(index = upper-1);
    }

    /**
     * Gets the character at the current position.
     */
    @Override
    public final char current() {
        return (index != upper) ? text.charAt(index) : DONE;
    }

    /**
     * Increments the iterator's index by one and returns the character at the new index.
     */
    @Override
    public final char next() {
        if (++index < upper) {
            return text.charAt(index);
        }
        index = upper;
        return DONE;
    }

    /**
     * Decrements the iterator's index by one and returns the character at the new index.
     */
    @Override
    public final char previous() {
        if (--index >= lower) {
            return text.charAt(index);
        }
        index = lower;
        return DONE;
    }

    /**
     * Returns the character at the given index.
     */
    @Override
    public final char charAt(final int index) {
        return text.charAt(index);
    }

    /**
     * Sets the position to the specified position in the text and returns that character.
     */
    @Override
    public final char setIndex(final int position) {
        ArgumentChecks.ensureBetween("position", lower, upper, position);
        return ((index = position) != upper) ? text.charAt(position) : DONE;
    }

    /**
     * Returns the current index.
     */
    @Override
    public final int getIndex() {
        return index;
    }

    /**
     * Returns the start index of the text.
     */
    @Override
    public final int getBeginIndex() {
        return lower;
    }

    /**
     * Returns the end index of the text.
     */
    @Override
    public final int getEndIndex() {
        return upper;
    }

    /**
     * Returns the number of characters.
     */
    @Override
    public final int length() {
        return upper - lower;
    }

    /**
     * Returns a sub-sequence of the wrapped text.
     */
    @Override
    public final CharSequence subSequence(final int start, final int end) {
        return text.subSequence(start, end);
    }

    /**
     * Returns a copy of this iterator.
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns a string representation of the wrapped character sequence.
     */
    @Override
    public final String toString() {
        return text.subSequence(lower, upper).toString();
    }
}
