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
package org.apache.sis.storage.gsf.panama;


/**
 * Status of the native library.
 *
 * @see org.apache.sis.storage.panama.LibraryStatus
 */
public enum LibraryStatus {
    /**
     * The native library is ready for use.
     */
    LOADED,

    /**
     * The native library has not been found or cannot be loaded. Note: this is a merge of
     * {@link org.apache.sis.storage.panama.LibraryStatus#LIBRARY_NOT_FOUND} and
     * {@link org.apache.sis.storage.panama.LibraryStatus#UNAUTHORIZED}.
     */
    CANNOT_LOAD_LIBRARY,

    /**
     * The native library was found, but not symbol that we searched.
     */
    FUNCTION_NOT_FOUND
}