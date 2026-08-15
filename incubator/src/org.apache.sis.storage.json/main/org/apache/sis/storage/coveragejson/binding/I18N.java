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
package org.apache.sis.storage.coveragejson.binding;

import java.util.Locale;
import org.opengis.util.InternationalString;
import org.apache.sis.storage.json.DataTransferObject;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * The special language tag "und" can be used to identify a value whose language
 * is unknown or undetermined.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class I18N extends DataTransferObject implements InternationalString {

    public static final String UNDETERMINED = "und";

    public I18N() {
    }

    public I18N(String lang, String text) {
        setOtherField(lang, text);
    }

    private String getDefault() {
        Object str = unknownFields.get(UNDETERMINED);
        if (str == null && !unknownFields.isEmpty()) str = unknownFields.get(unknownFields.keySet().iterator().next());
        if (str == null) str = "";
        return String.valueOf(str);
    }

    @Override
    public String toString(Locale locale) {
        Object str = unknownFields.get(locale.getLanguage());
        if (str == null) str = unknownFields.get(locale.getISO3Language());
        if (str == null) getDefault();
        return String.valueOf(str);
    }

    @Override
    public int length() {
        return getDefault().length();
    }

    @Override
    public char charAt(int index) {
        return getDefault().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return getDefault().subSequence(start, end);
    }

    @Override
    public int compareTo(InternationalString o) {
        return getDefault().compareTo(o.toString());
    }

}
