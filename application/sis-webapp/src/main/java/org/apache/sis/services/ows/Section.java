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
package org.apache.sis.services.ows;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;
import org.opengis.util.CodeList;

/**
 *
 * @author haonguyen
 */
public final class Section extends CodeList<Section> {
    private static final List<Section> VALUES = new ArrayList<>(5);
    public static final Section SERVICE_IDENTIFICATION = new Section("serviceIdentification");
    public static final Section SERVICEPROVIDER = new Section("serviceProvider");
    public static final Section OPERATIONMETADATA = new Section("operationMetadata");
    public static final Section CONTENTS = new Section("contents");
    public static final Section ALL = new Section("All");
    private Section(final String name) {
        super(name, VALUES);
    }

    @Override
    public Section[] family() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
