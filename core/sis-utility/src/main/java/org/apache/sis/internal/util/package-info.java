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

/**
 * A set of helper classes for the SIS implementation.
 *
 * <STRONG>Do not use!</STRONG>
 *
 * This package is for internal use by SIS only. Classes in this package
 * may change in incompatible ways in any future version without notice.
 *
 * <div class="section">Note on serialization</div>
 * Developers should avoid putting serializable classes in this package as much as possible,
 * since the serialization forms may be considered as a kind of API contract (depending how
 * much strict we want to be regarding compatibility). This is not always practical however,
 * so some serialized classes still exist in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
package org.apache.sis.internal.util;
