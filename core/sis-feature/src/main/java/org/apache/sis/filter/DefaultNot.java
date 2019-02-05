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
package org.apache.sis.filter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Not;

/**
 * Negation filter.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
final class DefaultNot implements Not, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1216799270370223357L;

    private final Filter filter;

    public DefaultNot(Filter filter) {
        ArgumentChecks.ensureNonNull("filter", filter);
        this.filter = filter;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public boolean evaluate(Object object) {
        return !filter.evaluate(object);
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultNot other = (DefaultNot) obj;
        return Objects.equals(this.filter, other.filter);
    }

    @Override
    public int hashCode() {
        return 32 * filter.hashCode();
    }

    @Override
    public String toString() {
        return AbstractExpression.toStringTree("Not", Arrays.asList(filter));
    }

}
