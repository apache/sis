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
package org.apache.sis.internal.gpx;

import java.net.URI;
import java.util.Objects;

/**
 * Person object as defined in GPX.
 * 
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class Person {

    private String name;
    private String email;
    private URI link;

    /**
     * Returns the person name.
     *
     * @return name, may be null
     */
    public String getName() {
        return name;
    }

    /**
     * Set person name.
     *
     * @param name, can be null
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the person email.
     *
     * @return email, may be null
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set person email.
     *
     * @param email, can be null
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the person information link.
     *
     * @return uri, may be null
     */
    public URI getLink() {
        return link;
    }

    /**
     * Set person information link.
     *
     * @param link, can be null
     */
    public void setLink(URI link) {
        this.link = link;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Person(");
        sb.append(name).append(',').append(email).append(',').append(link);
        sb.append(')');
        return sb.toString();
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
        final Person other = (Person) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.email, other.email)) {
            return false;
        }
        if (!Objects.equals(this.link, other.link)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 45;
    }

}
