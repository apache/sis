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
package org.apache.sis.internal.map;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.sis.measure.NumberRange;

/**
 * Decorate a CopyOnWriteArrayList and notify changes when elements are added or removed.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.2
 */
public abstract class NotifiedList<T> extends AbstractList<T> {

    private final CopyOnWriteArrayList<T> parent = new CopyOnWriteArrayList<>();

    @Override
    public T get(int index) {
        return parent.get(index);
    }

    @Override
    public int size() {
        return parent.size();
    }

    @Override
    public T set(int index, T element) {
        final T old = parent.set(index, element);
        notifyReplace(old, element, index);
        return old;
    }

    @Override
    public void add(int index, T element) {
        parent.add(index, element);
        notifyAdd(element, index);
    }

    @Override
    public T remove(int index) {
        final T old = parent.remove(index);
        notifyRemove(old, index);
        return old;
    }

    protected abstract void notifyAdd(final T item, int index);

    protected abstract void notifyAdd(final List<T> items, NumberRange<Integer> range);

    protected abstract void notifyRemove(final T item, int index);

    protected abstract void notifyRemove(final List<T> items, NumberRange<Integer> range);

    protected abstract void notifyReplace(final T olditem, final T newitem, int index);
}
