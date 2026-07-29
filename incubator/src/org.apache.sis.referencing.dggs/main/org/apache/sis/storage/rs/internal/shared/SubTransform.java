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
package org.apache.sis.storage.rs.internal.shared;

import org.apache.sis.referencing.rs.Code;
import org.apache.sis.storage.rs.CodeTransform;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class SubTransform implements CodeTransform {

    @Override
    public Code toCode(int[] gridPosition) throws TransformException {
        final Object[] ordinates = new Object[getDimension()];
        toAddress(gridPosition, ordinates, 0);
        return new Code(getRS(), ordinates);
    }

    @Override
    public int[] toGrid(Code location) throws TransformException {
        final int[] gridPosition = new int[getDimension()];
        toGrid(location.getOrdinates(), gridPosition, 0);
        return gridPosition;
    }

    public abstract void toAddress(int[] gridPosition, Object[] location, int offset) throws TransformException;

    public abstract void toGrid(Object[] location, int[] gridPosition, int offset) throws TransformException;

}
