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
package org.apache.sis.measure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.text.Format;
import java.text.AttributedCharacterIterator;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.converter.SurjectiveConverter;
import org.apache.sis.internal.simple.SimpleCharacterIterator;


/**
 * The attributed character iterator to be returned by {@link Format}
 * implementations in the {@code org.apache.sis.measure} package.
 *
 * <div class="section">Implementation assumption</div>
 * Every {@code getRunStart(…)} and {@code getRunLimit(…)} methods defined in this class check
 * only for attribute existence, ignoring the actual attribute value. This is a departure from
 * the {@link java.text.AttributedCharacterIterator} contract, but should be invisible to the
 * users if there is no juxtaposed fields with the same attribute value (which is usually the
 * case). A violation occurs if different fields are formatted without separator. For example
 * if an angle is formatted as "DDMMSS" without any field separator, then we have 3 juxtaposed
 * integer fields. If those fields have the same value, then the whole "DDMMSS" text should be
 * seen as a single run according the {@code AttributedCharacterIterator} contract, while they
 * will still been seen as 3 separated fields by this implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class FormattedCharacterIterator extends SimpleCharacterIterator implements AttributedCharacterIterator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5864519830922231670L;

    /**
     * Holds a field value, together with the run range in which this value is valid.
     * Contains also a reference to the previous {@code Entry} in order to build a chained
     * list (in reverse of insertion order) if many values exist for the same field.
     *
     * <p>To be more specific:</p>
     * <ul>
     *   <li>The map key is one of the static constants defined in the formatter {@code Field} inner class.</li>
     *   <li>{@link #value} is the numeric value being formatted for that particular field.</li>
     *   <li>{@link #start} and {@link #limit} are the range of index in the
     *       {@link SimpleCharacterIterator#text} where the field value has been formatted.</li>
     * </ul>
     *
     * <b>Example:</b> if {@link AngleFormat} formats "10°30′" and the user wants information
     * about the degrees field, then:
     *
     * <ul>
     *   <li>The map key is {@link AngleFormat.Field#DEGREES};</li>
     *   <li>{@link #value} is {@code Double.valueOf(10)};</li>
     *   <li>{@link #start} is 0;</li>
     *   <li>{@link #limit} is 3.</li>
     * </ul>
     */
    private static final class Entry {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 3297480138621390486L;

        /**
         * The attribute value.
         */
        final Object value;

        /**
         * The range of index in {@link SimpleCharacterIterator#text} where the value has
         * been formatted. See class javadoc for more information.
         */
        final int start, limit;

        /**
         * The previous entry in a chained list of entries for the same key, or {@code null} if none.
         */
        final Entry previous;

        /**
         * Creates a new entry for the given value, together with the range of index where
         * the field value has been formatted. See class javadoc for more information.
         */
        Entry(final Attribute field, final Object value, final int start, final int limit,
                final Map<Attribute,Entry> attributes)
        {
            this.value = value;
            this.start = start;
            this.limit = limit;
            previous = attributes.put(field, this);
        }
    }

    /**
     * All fields associated to the {@linkplain #text text}.
     *
     * <p>This map shall not be modified after this {@code FormattedCharacterIterator} become
     * visible to the user. If this map could be modified, then we would need to override the
     * {@link #clone()} method in order to clone this map too.</p>
     */
    private final Map<Attribute,Entry> attributes;

    /**
     * An unmodifiable view over the keys of the {@link #attributes} map.
     * This view is created when first needed.
     *
     * @see #getAllAttributeKeys()
     */
    private transient Set<Attribute> attributeKeys;

    /**
     * The attribute given in the last call to a {@code getRunStart(…)} or {@code getRunLimit(…)}
     * method. Used for determining if the {@link #start} and {@link #limit} fields need a update.
     * A {@code null} value means that the run range needs an unconditional update (this is the
     * case after construction and after deserialization).
     */
    private transient Attribute runAttribute;

    /**
     * The value to be returned by {@code getRunStart(…)} and {@code getRunLimit(…)}
     * when the index value is {@code validity}. Those values are updated when needed
     * by the {@link #update(Set)} method.
     */
    private transient int start, limit, validity;

    /**
     * Creates a new character iterator for the given character sequence.
     *
     * @param text The formatted text. Can be a {@link StringBuilder} to be filled later.
     */
    FormattedCharacterIterator(final CharSequence text) {
        super(text);
        attributes = new IdentityHashMap<Attribute,Entry>(8);
    }

    /**
     * Invoked by {@code Format} implementations when a field ended. This method stores the
     * given attribute value for the run ranging from {@code start} inclusive to the current
     * {@linkplain #text} length, exclusive.
     */
    final void addFieldLimit(final Attribute field, final Object value, final int start) {
        // The Entry constructor adds itself to the attributes map.
        // The returned intance is used only for assertions checks.
        Entry e = new Entry(field, value, start, upper = text.length(), attributes);
        assert ((e = e.previous) == null) || (start >= e.limit); // Check for non-overlapping fields.
    }

    /**
     * Appends all characters and attributes from the given iterator.
     *
     * @param toAppendTo Shall be the same instance than {@link #text}.
     */
    final void append(final AttributedCharacterIterator it, final StringBuffer toAppendTo) {
        final int offset = toAppendTo.length();
        int currentRunLimit = 0; // Next index where to check for attributes.
        for (char c=it.first(); c!=DONE; c=it.next()) {
            toAppendTo.append(c);
            if (it.getIndex() == currentRunLimit) {
                currentRunLimit = it.getRunLimit();
                for (final Map.Entry<Attribute,Object> entry : it.getAttributes().entrySet()) {
                    final Attribute attribute = entry.getKey();
                    if (it.getRunLimit(attribute) == currentRunLimit) {
                        new Entry(attribute, entry.getValue(), // Constructeur adds itself to the map.
                                offset + it.getRunStart(attribute),
                                offset + currentRunLimit, attributes);
                    }
                }
            }
        }
        upper = toAppendTo.length();
    }

    /**
     * Ensures that the {@link #start}, {@link #limit} and {@link #attributes} fields
     * are valid for the current index position and the given attribute.
     *
     * @param attribute The attribute which shall have the same value in the run range.
     * @param entries   The entries on which to iterate for computing the run range.
     *                  Mandatory if {@code attribute} is {@code null}.
     */
    private void update(final Attribute attribute, Collection<Entry> entries) {
        final int index = getIndex();
        if (attribute == null || attribute != runAttribute || index != validity) {
            runAttribute = attribute;
            validity     = index;
            start        = lower;
            limit        = upper;
            if (entries == null) {
                if (attribute == FormatField.ALL) {
                    entries = attributes.values();
                } else {
                    entries = Collections.singleton(attributes.get(attribute));
                }
            }
            for (Entry entry : entries) {
                Entry notFound = entry;
                while (entry != null) {
                    if (index >= entry.start && index < entry.limit) {
                        if (entry.start > start) start = entry.start;
                        if (entry.limit < limit) limit = entry.limit;
                        notFound = null; // Found the attribute.
                    }
                    entry = entry.previous;
                }
                /*
                 * If the attribute has not been found for the current character position,
                 * then we need to reverse the condition: if the attribute become defined
                 * in another position, we must stop the run at that position.
                 */
                while (notFound != null) {
                    if (notFound.start >  index && notFound.start < limit) limit = notFound.start;
                    if (notFound.limit <= index && notFound.limit > start) start = notFound.limit;
                    notFound = notFound.previous;
                }
            }
        }
    }

    /**
     * Returns the index of the first character of the run having all the same attributes
     * than the current character. See this class javadoc for a note about which attributes
     * are considered equal.
     */
    @Override
    public int getRunStart() {
        update(FormatField.ALL, null);
        return start;
    }

    /**
     * Returns the index of the first character of the run having the same "value" for the
     * given attribute than the current character. See this class javadoc for a note about
     * which attribute "values" are considered equal.
     */
    @Override
    public int getRunStart(final Attribute attribute) {
        ArgumentChecks.ensureNonNull("attribute", attribute);
        update(attribute, null);
        return start;
    }

    /**
     * Returns the index of the first character of the run having the same "values" for the
     * given attributes than the current character. See this class javadoc for a note about
     * which attribute "values" are considered equal.
     */
    @Override
    public int getRunStart(final Set<? extends Attribute> attributes) {
        update(null, entries(attributes));
        return start;
    }

    /**
     * Returns the index of the first character following the run having all the same attributes
     * than the current character. See this class javadoc for a note about which attributes are
     * considered equal.
     */
    @Override
    public int getRunLimit() {
        update(FormatField.ALL, null);
        return limit;
    }

    /**
     * Returns the index of the first character following the run having the same "value" for
     * the given attribute than the current character. See this class javadoc for a note about
     * which attribute "values" are considered equal.
     */
    @Override
    public int getRunLimit(final Attribute attribute) {
        ArgumentChecks.ensureNonNull("attribute", attribute);
        update(attribute, null);
        return limit;
    }

    /**
     * Returns the index of the first character following the run having the same "values" for
     * the given attributes than the current character. See this class javadoc for a note about
     * which attribute "values" are considered equal.
     */
    @Override
    public int getRunLimit(final Set<? extends Attribute> attributes) {
        update(null, entries(attributes));
        return limit;
    }

    /**
     * Returns the entries for the given attributes. This is a helper method for the
     * {@code getRunStart(Set)} and {@code getRunLimit(Set)} methods.
     */
    private Collection<Entry> entries(final Set<? extends Attribute> requested) {
        final Collection<Entry> entries = new ArrayList<Entry>(requested.size());
        for (final Attribute r : requested) {
            final Entry e = attributes.get(r);
            if (e != null) {
                entries.add(e);
            }
        }
        return entries;
    }

    /**
     * The object converter to use for extracting {@link Entry#value} in the map returned
     * by {@link FormattedCharacterIterator#getAttributes()}. The value to extract depends
     * on the character index.
     */
    private static final class Selector extends SurjectiveConverter<Entry,Object> implements Serializable {
        private static final long serialVersionUID = -7281235148346378214L;

        /** Index of the character for which the map of attributes is requested. */
        private final int index;

        /** Creates a new value converter for the character at the given index. */
        Selector(final int index) {
            this.index = index;
        }

        /** Returns the value for the given entry, or {@code null} if none. */
        @Override
        public Object apply(Entry entry) {
            while (entry != null) {
                if (index >= entry.start && index < entry.limit) {
                    return entry.value;
                }
                entry = entry.previous;
            }
            return null;
        }

        @Override public Class<Entry>  getSourceClass()  {return Entry.class;}
        @Override public Class<Object> getTargetClass()  {return Object.class;}
    }

    /**
     * The object converter to use for filtering the keys in the map returned by
     * {@link FormattedCharacterIterator#getAttributes()}.
     */
    private static class Filter extends SurjectiveConverter<Attribute,Attribute> implements Serializable {
        private static final long serialVersionUID = 6951804952836918035L;

        /** A reference to {@link FormattedCharacterIterator#attributes}. */
        private final Map<Attribute,Entry> attributes;

        /** Index of the character for which the map of attribute is requested. */
        private final int index;

        /** Creates a new key filter for the character at the given index. */
        Filter(final Map<Attribute,Entry> attributes, final int index) {
            this.attributes = attributes;
            this.index = index;
        }

        /** Returns {@code attribute} if it shall be included in the derived map, or {@code null} otherwise. */
        @Override
        public Attribute apply(final Attribute attribute) {
            for (Entry e=attributes.get(attribute); e!=null; e=e.previous) {
                if (index >= e.start && index < e.limit) {
                    return attribute;
                }
            }
            return null;
        }

        @Override public Class<Attribute> getSourceClass()  {return Attribute.class;}
        @Override public Class<Attribute> getTargetClass()  {return Attribute.class;}
    }

    /**
     * Returns the attributes defined on the current character.
     */
    @Override
    public Map<Attribute, Object> getAttributes() {
        final int index = getIndex();
        return Containers.derivedMap(attributes, new Filter(attributes, index), new Selector(index));
    }

    /**
     * Returns the value of the named attribute for the current character, or {@code null} if none.
     */
    @Override
    public Object getAttribute(final Attribute attribute) {
        final int index = getIndex();
        for (Entry e=attributes.get(attribute); e!=null; e=e.previous) {
            if (index >= e.start && index < e.limit) {
                return e.value;
            }
        }
        return null;
    }

    /**
     * Returns the keys of all attributes defined in the iterator text range.
     */
    @Override
    public Set<Attribute> getAllAttributeKeys() {
        if (attributeKeys == null) {
            attributeKeys = Collections.unmodifiableSet(attributes.keySet());
        }
        return attributeKeys;
    }

    /*
     * We do not override the clone() method because the 'attributes' map will not
     * be modified after this FormattedCharacterIterator become visible to the user,
     * so we don't need to clone the map.
     */
}
