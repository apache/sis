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
package org.apache.sis.system;

import java.lang.ref.Cleaner;


/**
 * The system-wide cleaner for garbage-collected objects. This class should be used instead of
 * {@link ReferenceQueueConsumer} when the caller do not need to access the referenced object.
 * In other words, this class should be used when a phantom reference is sufficient.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Cleaners {
    /**
     * The system-wide cleaner shared by all classes of the <abbr>SIS</abbr> library.
     */
    public static final Cleaner SHARED = Cleaner.create();

    /**
     * Do not allow instantiation of this class.
     */
    private Cleaners() {
    }
}
