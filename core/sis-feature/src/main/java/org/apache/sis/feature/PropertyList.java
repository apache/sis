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
package org.apache.sis.feature;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.CheckedArrayList;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureNonNullElement;


/**
 * A list which may contains more than 1 attribute. This class perform some minimal validation checks
 * before to delegate the work to the standard JDK implementation.
 *
 * <p><b>Limitation:</b> the validation performed by this class is not exhaustive.
 * Furthermore this class has many holes (e.g. when using sublists or list iterator).
 * The intend is only to catch some of the most common errors.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class PropertyList extends ArrayList<DefaultAttribute<?>> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5952391259488363686L;

    /**
     * The type of all property elements in this list.
     */
    private final AbstractIdentifiedType type;

    /**
     * The maximum number of occurrences allowed by this list.
     *
     * <div class="note"><b>Note:</b> we do not check the minimum number of occurrences because it is common
     * to have less elements than allowed when feature construction is under progress. Furthermore, we want
     * {@code PropertyList} list behavior to be consistent with {@link PropertySingleton} behavior regarding
     * cardinality.</div>
     */
    private final int maximumOccurs;

    /**
     * Creates a new list of attributes of the given type.
     */
    PropertyList(final AbstractIdentifiedType type, final int maximumOccurs) {
        this.type = type;
        this.maximumOccurs = maximumOccurs;
    }

    /**
     * Verify if this list can accept the given element.
     * The caller shall ensure that the element is non-null before to invoke this method.
     */
    private void ensureCanAdd(final DefaultAttribute<?> element) {
        if (size() >= maximumOccurs && maximumOccurs != Integer.MAX_VALUE) {
            throw new IllegalStateException(Errors.format(Errors.Keys.TooManyOccurrences_2, size(), type.getName()));
        }
        Validator.ensureValidType(type, element);
    }

    /**
     * Ensures that all elements of the given collection can be added to this list.
     *
     * @param  collection the collection to check, or {@code null}.
     * @return The a wrapper over the elements to add.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private List<DefaultAttribute<?>> ensureCanAdd(final Collection<? extends DefaultAttribute<?>> collection) {
        final Object[] array = collection.toArray();
        /*
         * The first condition below is equivalent to 'if (size() + array.length > maximumOccurs)',
         * but rearranged in a way to avoid integer overflow.
         */
        if (size() > maximumOccurs - array.length && maximumOccurs != Integer.MAX_VALUE) {
            throw new IllegalStateException(Errors.format(Errors.Keys.TooManyOccurrences_2, size(), type.getName()));
        }
        for (int i=0; i<array.length; i++) {
            final DefaultAttribute<?> element = (DefaultAttribute<?>) array[i];
            ensureNonNullElement("collection", i, element);
            Validator.ensureValidType(type, element);
        }
        /*
         * Cast to List<DefaultAttribute<?>> is safe since we checked the type of all elements in the above loop.
         */
        return new CheckedArrayList.Mediator(array);
    }

    /**
     * If the users request an increase in list capacity, limit that increase
     * to the maximum number o occurrences allowed for the attribute.
     */
    @Override
    public void ensureCapacity(final int minCapacity) {
        super.ensureCapacity(Math.min(minCapacity, maximumOccurs));
    }

    /**
     * Validates the given element before to store it in this list.
     */
    @Override
    public DefaultAttribute<?> set(final int index, final DefaultAttribute<?> element) {
        ensureNonNull("element", element);
        Validator.ensureValidType(type, element);
        return super.set(index, element);
    }

    /**
     * Validates the given element before to add it in this list.
     */
    @Override
    public boolean add(final DefaultAttribute<?> element) {
        ensureNonNull("element", element);
        ensureCanAdd(element);
        return super.add(element);
    }

    /**
     * Validates the given element before to add it in this list.
     */
    @Override
    public void add(final int index, final DefaultAttribute<?> element) {
        ensureNonNull("element", element);
        ensureCanAdd(element);
        super.add(index, element);
    }

    /**
     * Appends all of the elements in the specified collection to the end of this list,
     * in the order that they are returned by the specified Collection's Iterator.
     */
    @Override
    public boolean addAll(final Collection<? extends DefaultAttribute<?>> collection) {
        return super.addAll(ensureCanAdd(collection));
    }

    /**
     * Inserts all of the elements in the specified collection into this list,
     * starting at the specified position.
     */
    @Override
    public boolean addAll(final int index, final Collection<? extends DefaultAttribute<?>> collection) {
        return super.addAll(index, ensureCanAdd(collection));
    }
}
