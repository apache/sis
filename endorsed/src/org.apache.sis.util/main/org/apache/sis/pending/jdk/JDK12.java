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
package org.apache.sis.pending.jdk;


/**
 * Place holder for some functionalities defined in a JDK more recent than Java 11.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class JDK12 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK12() {
    }

    public static String descriptorString(Class<?> c) {
        if (c == Double.TYPE)    return "D";
        if (c == Float.TYPE)     return "F";
        if (c == Long.TYPE)      return "J";
        if (c == Integer.TYPE)   return "I";
        if (c == Short.TYPE)     return "S";
        if (c == Byte.TYPE)      return "B";
        if (c == Character.TYPE) return "C";
        if (c == Boolean.TYPE)   return "Z";
        if (c == Void.TYPE)      return "V";
        return "L" + c.getCanonicalName() + ';';
    }
}
