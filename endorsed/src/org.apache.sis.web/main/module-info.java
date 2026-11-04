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
 * Support for Apache <abbr>SIS</abbr> execution in the context of web containers.
 * This module registers listeners for stopping Apache <abbr>SIS</abbr> background threads
 * when a web application using <abbr>SIS</abbr> is undeployed from a web container.
 * The listeners in this module are compatible with the following frameworks:
 *
 * <ul>
 *   <li>JavaEE Servlet 4.0 ({@code javax.servlet} package)</li>
 *   <li>Jakarta Servlet 6.1 ({@code jakarta.servlet} package)</li>
 *   <li>OGSGi 8.0 ({@code org.osgi.framework} package)</li>
 * </ul>
 *
 * Web applications should consider adding this module in their dependencies
 * if they use Apache <abbr>SIS</abbr> and need clean undeploy while keeping the rest of the container running.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.8
 * @since SIS 1.8
 */
module org.apache.sis.web {
    requires transitive org.apache.sis.util;
    requires static jakarta.servlet;
    requires static javax.servlet.api;
    requires static osgi.core;

    provides jakarta.servlet.ServletContainerInitializer
            with org.apache.sis.web.jakarta.ServletListener;

    provides javax.servlet.ServletContainerInitializer
            with org.apache.sis.web.javaee.ServletListener;
}
