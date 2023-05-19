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
package org.apache.sis.cql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.SortProperty;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Query {

    public final List<Projection> projections = new ArrayList<>();
    public Filter<Feature> filter;
    public Integer offset;
    public Integer limit;
    public final List<SortProperty> sortby = new ArrayList<>();

    public Query() {
    }

    public Query(List<Projection> projections, Filter filter, List<SortProperty> sortby, Integer offset, Integer limit) {
        if (projections != null) this.projections.addAll(projections);
        this.filter = filter;
        if (sortby != null) this.sortby.addAll(sortby);
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
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
        final Query other = (Query) obj;
        if (!Objects.equals(this.projections, other.projections)) {
            return false;
        }
        if (!Objects.equals(this.filter, other.filter)) {
            return false;
        }
        if (!Objects.equals(this.sortby, other.sortby)) {
            return false;
        }
        if (!Objects.equals(this.offset, other.offset)) {
            return false;
        }
        if (!Objects.equals(this.limit, other.limit)) {
            return false;
        }
        return true;
    }

    public static class Projection {
        public Expression<Feature, ?> expression;
        public String alias;

        public Projection(Expression<Feature, ?> expression, String alias) {
            this.expression = expression;
            this.alias = alias;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 19 * hash + Objects.hashCode(this.expression);
            hash = 19 * hash + Objects.hashCode(this.alias);
            return hash;
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
            final Projection other = (Projection) obj;
            if (!Objects.equals(this.alias, other.alias)) {
                return false;
            }
            if (!Objects.equals(this.expression, other.expression)) {
                return false;
            }
            return true;
        }

    }
}
