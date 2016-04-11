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
 * Copyright object as defined in GPX.
 * 
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class CopyRight {

    private String author;
    private Integer year;
    private URI license;

    /**
     * Returns the author value.
     *
     * @return author, may be null
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Set author value.
     *
     * @param author, can be null
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Returns the copyright year.
     *
     * @return copyright year, may be null
     */
    public Integer getYear() {
        return year;
    }

    /**
     * Set copyright year value.
     *
     * @param year, can be null
     */
    public void setYear(Integer year) {
        this.year = year;
    }

    /**
     * Returns the license URI.
     *
     * @return license, may be null
     */
    public URI getLicense() {
        return license;
    }

    /**
     * Set license URI.
     *
     * @param license, can be null
     */
    public void setLicense(URI license) {
        this.license = license;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CopyRight(");
        sb.append(author).append(',').append(year).append(',').append(license);
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
        final CopyRight other = (CopyRight) obj;
        if (!Objects.equals(this.author, other.author)) {
            return false;
        }
        if (!Objects.equals(this.year, other.year)) {
            return false;
        }
        if (!Objects.equals(this.license, other.license)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 44;
    }

}
