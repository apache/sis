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
package org.apache.sis.internal.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;


/**
 * A value for {@link TableColumn#setCellValueFactory(Callback)} which just forwards
 * given values as-is (ignoring the observable wrapper).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param <S>  the type of objects that represent rows in the table.
 * @param <T>  the type of values in table cells.
 *
 * @since 1.1
 * @module
 */
public final class IdentityValueFactory<S extends T, T>
        implements Callback<TableColumn.CellDataFeatures<S,T>, ObservableValue<T>>
{
    /**
     * The singleton instance.
     */
    private static final IdentityValueFactory<?,?> INSTANCE = new IdentityValueFactory<>();

    /**
     * Returns the factory instance.
     *
     * @param  <S>  the type of objects that represent rows in the table.
     * @param  <T>  the type of values in table cells.
     * @return the factory instance.
     */
    @SuppressWarnings("unchecked")
    public static <S extends T, T> IdentityValueFactory<S,T> instance() {
        return (IdentityValueFactory<S,T>) INSTANCE;
    }

    /**
     * For the singleton constructor only.
     */
    private IdentityValueFactory() {
    }

    /**
     * Wraps the value in an observable.
     * The argument are the return values are non-null, but the wrapped value may be null.
     *
     * @param  cell  the cell containing a value.
     * @return the wrapped value.
     */
    @Override
    public ObservableValue<T> call(final TableColumn.CellDataFeatures<S,T> cell) {
        return new ReadOnlyObjectWrapper<>(cell.getValue());
    }
}
