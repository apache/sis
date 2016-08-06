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
package org.apache.sis.services.csw;

import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author haonguyen
 */
@XmlTransient
public abstract class CswConfigure {

    /**
     * The namespace of Dublin Core elements.
     */
    public static final String DUBLIN_CORE = "http://purl.org/dc/elements/1.1/";

    /**
     * The namespace of Dublin Core terms.
     */
    public static final String DUBLIN_TERMS = "http://purl.org/dc/terms/";

    /**
     * The namespace of OGC common objects.
     */
    public static final String OWS = "http://www.opengis.net/ows";
    public static final String OGC = "http://www.opengis.net/ogc";

    /**
     * For subclasses constructors.
     */
    public CswConfigure() {
    }
}
