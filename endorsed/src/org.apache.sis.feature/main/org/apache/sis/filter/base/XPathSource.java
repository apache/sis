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
package org.apache.sis.filter.base;


/**
 * Filters or expressions fetching their data from an XPath.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public interface XPathSource {
    /**
     * Returns the path to the property which will be used by the {@code test(R)} or {@code apply(R)} method.
     * Usually, this path is simply the name of a property in a feature.
     *
     * @return path to the property which will be used by the {@code apply(R)} method.
     */
    String getXPath();
}
