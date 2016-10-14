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
import java.util.Objects;
import javax.measure.Unit;
import javax.measure.Quantity;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.WeakHashSet;


/**
 * Base class of all unit implementations. All unit instances shall be immutable.
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable and thus inherently thread-safe.
 * All subclasses shall be immutable too.
 *
 * @param  <Q>  the kind of quantity to be measured using this units.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
abstract class AbstractUnit<Q extends Quantity<Q>> implements Unit<Q>, Serializable {
    /**
     * Pool of all {@code AbstractUnit} instances created up to date.
     */
    @SuppressWarnings("rawtypes")
    private static final WeakHashSet<AbstractUnit> POOL = new WeakHashSet<>(AbstractUnit.class);

    /**
     * The unit symbol, or {@code null} if this unit has no specific symbol. If {@code null},
     * then the {@link #toString()} method is responsible for creating a representation on the fly.
     *
     * <p>Users can override this symbol by call to {@link UnitFormat#label(Unit, String)},
     * but such overriding applies only to the target {@code UnitFormat} instance.</p>
     *
     * @see #getSymbol()
     */
    private final String symbol;

    /**
     * The unit name, or {@code null} if this unit has no specific name. If this unit exists
     * in the EPSG database, then the value should be the name as specified in the database.
     *
     * @see #getName()
     */
    private final String name;

    /**
     * The EPSG code, or 0 if this unit has no EPSG code.
     */
    final short epsg;

    /**
     * Creates a new unit having the given symbol and EPSG code.
     *
     * @param  name    the unit name,   or {@code null} if this unit has no specific name.
     * @param  symbol  the unit symbol, or {@code null} if this unit has no specific symbol.
     * @param  epsg    the EPSG code,   or 0 if this unit has no EPSG code.
     */
    AbstractUnit(final String name, final String symbol, final short epsg) {
        this.name   = name;
        this.symbol = symbol;
        this.epsg   = epsg;
    }

    /**
     * Returns the symbol (if any) of this unit. A unit may have no symbol, in which case
     * the {@link #toString()} method is responsible for creating a string representation.
     *
     * @return the unit symbol, or {@code null} if this unit has no specific symbol.
     *
     * @see #toString()
     */
    @Override
    public final String getSymbol() {
        return symbol;
    }

    /**
     * Returns the name (if any) of this unit. For example {@link Units#METRE} has the "m" symbol and the "metre" name.
     * If this unit exists in the EPSG database, then this method should return the name as specified in the database.
     *
     * @return the unit name, or {@code null} if this unit has no specific name.
     */
    @Override
    public final String getName() {
        return name;
    }

    /**
     * Indicates if this unit is compatible with the given unit.
     * This implementation delegates to:
     *
     * {@preformat java
     *   return getDimension().equals(that.getDimension());
     * }
     *
     * @param  that the other unit to compare for compatibility.
     * @return {@code true} if the given unit is compatible with this unit.
     *
     * @see #getDimension()
     */
    @Override
    public final boolean isCompatible(final Unit<?> that) {
        ArgumentChecks.ensureNonNull("that", that);
        return getDimension().equals(that.getDimension());
    }

    /**
     * Returns a unique instance of this unit. An initially empty pool of {@code AbstractUnit}
     * instances is maintained. When invoked, this method first checks if an instance equals
     * to this unit exists in the pool. If such instance is found, then it is returned.
     * Otherwise this instance is added in the pool using weak references and returned.
     */
    final AbstractUnit<Q> intern() {
        return POOL.unique(this);
    }

    /**
     * Returns an instance equals to this unit, ignoring the symbol.
     * If such instance exists in the pool of existing units, it is
     * returned. Otherwise this method returns {@code this}.
     */
    final AbstractUnit<Q> internIgnoreSymbol() {
        @SuppressWarnings("unchecked")
        AbstractUnit<Q> unit = POOL.get(new Unamed(this));
        if (unit == null) {
            unit = this;
        }
        return unit;
    }

    /**
     * Compares this unit with the given unit, ignoring symbol.
     * Implementations shall check the {@code obj} type.
     *
     * @param  obj The object to compare with this unit, or {@code null}.
     * @return {@code true} If the given unit is equals to this unit, ignoring symbol.
     */
    protected abstract boolean equalsIgnoreSymbol(Object obj);

    /**
     * Compares this unit with the given object for equality.
     *
     * @param  other The other object to compares with this unit, or {@code null}.
     * @return {@code true} if the given object is equals to this unit.
     */
    @Override
    public final boolean equals(Object other) {
        if (other instanceof Unamed) {
            other = ((Unamed) other).unit;
        }
        if (equalsIgnoreSymbol(other)) {
            return Objects.equals(symbol, ((AbstractUnit<?>) other).symbol);
        }
        return false;
    }

    /**
     * A temporary proxy used by {@link #internIgnoreSymbol()} for finding an existing units,
     * ignoring the symbol.
     */
    private static final class Unamed {
        final AbstractUnit<?> unit;

        Unamed(final AbstractUnit<?> unit)          {this.unit = unit;}
        @Override public int hashCode()             {return unit.hashCode();}
        @Override public boolean equals(Object obj) {return unit.equalsIgnoreSymbol(obj);}
    }

    /**
     * Returns a hash code value for this unit, ignoring symbol.
     */
    @Override
    public abstract int hashCode();

    /**
     * Replaces the deserialized unit instance by a unique instance, if any.
     *
     * @return The unique unit instance.
     */
    protected final Object readResolve() {
        return intern();
    }

    /**
     * Returns the exception to throw when the given unit arguments are illegal
     * for the operation to perform.
     */
    final ArithmeticException illegalUnitOperation(final String operation, final AbstractUnit<?> that) {
        return new ArithmeticException(); // TODO: provide a message.
    }

    /**
     * Returns the unlocalized string representation of this unit, either as a single symbol
     * or a product of symbols or scale factors, eventually with an offset.
     *
     * @see #getSymbol()
     */
    @Override
    public final String toString() {
        if (symbol != null) {
            return symbol;
        } else {
            return UnitFormat.INSTANCE.format(this);
        }
    }
}
