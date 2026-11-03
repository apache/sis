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
 * Support the use of Apache <abbr>SIS</abbr> in web containers.
 * Web applications having <abbr>SIS</abbr> in their dependencies can add this module
 * if they need to undeploy <abbr>SIS</abbr> cleanly while keeping the rest of the container running.
 * This module is compatible with:
 *
 * <ul>
 *   <li>JavaEE Servlet 4.0 ({@code javax.servlet} packages)</li>
 *   <li>Jakarta Servlet 6.1 ({@code jakarta.servlet} packages)</li>
 *   <li>OGSGi 8.0 ({@code org.osgi} packages)</li>
 * </ul>
 */
module org.apache.sis.webapp {
    requires transitive org.apache.sis.util;
    requires static jakarta.servlet;
    requires static javax.servlet.api;
    requires static osgi.core;

    provides jakarta.servlet.ServletContainerInitializer
            with org.apache.sis.services.ServletListener;

    provides javax.servlet.ServletContainerInitializer
            with org.apache.sis.services.ServletListenerLegacy;
}
