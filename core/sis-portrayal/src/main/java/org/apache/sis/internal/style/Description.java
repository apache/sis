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
package org.apache.sis.internal.style;

import java.util.Objects;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.style.StyleVisitor;
import org.opengis.util.InternationalString;

/**
 * Mutable implementation of {@link org.opengis.style.Description}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Description implements org.opengis.style.Description {

    private InternationalString title;
    private InternationalString abstrat;

    public Description() {
        this(StyleFactory.EMPTY_STRING, StyleFactory.EMPTY_STRING);
    }

    public Description(InternationalString title, InternationalString abstrat) {
        ArgumentChecks.ensureNonNull("title", title);
        ArgumentChecks.ensureNonNull("abstrat", abstrat);
        this.title = title;
        this.abstrat = abstrat;
    }

    @Override
    public InternationalString getTitle() {
        return title;
    }

    public void setTitle(InternationalString title) {
        ArgumentChecks.ensureNonNull("title", title);
        this.title = title;
    }

    @Override
    public InternationalString getAbstract() {
        return abstrat;
    }

    public void setAbstract(InternationalString abstrat) {
        ArgumentChecks.ensureNonNull("abstrat", abstrat);
        this.abstrat = abstrat;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, abstrat);
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
        final Description other = (Description) obj;
        return Objects.equals(this.title, other.title)
            && Objects.equals(this.abstrat, other.abstrat);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static Description castOrCopy(org.opengis.style.Description candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof Description) {
            return (Description) candidate;
        }
        return new Description(candidate.getTitle(), candidate.getAbstract());
    }
}
