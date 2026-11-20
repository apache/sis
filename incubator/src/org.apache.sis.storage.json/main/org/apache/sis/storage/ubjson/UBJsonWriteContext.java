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
package org.apache.sis.storage.ubjson;

import com.fasterxml.jackson.core.JsonStreamContext;

final class UBJsonWriteContext extends JsonStreamContext {

    /**
     * Parent context for this context; null for root context.
     */
    protected final UBJsonWriteContext _parent;

    // *********************************************************************
    // Simple instance reuse slots; speed up things a bit
    // *********************************************************************
    protected UBJsonWriteContext _child = null;

    // *********************************************************************
    // Location/state information (minus source reference)
    // *********************************************************************

    /**
     * Value that is being serialized and caused this context to be created; typically a POJO or container type.
     */
    protected Object _currentValue;

    /**
     * Marker used to indicate that we just received a name, and now expect a value
     */
    protected boolean _gotName;

    /**
     * Name of the field of which value is to be parsed; only used for OBJECT contexts
     */
    protected String _currentName;

    protected int _basePathLength;

    boolean _inline;

    // *********************************************************************
    //  Life-cycle
    // *********************************************************************

    UBJsonWriteContext(int type, UBJsonWriteContext parent,
            Object currValue, int basePathLength) {
        super();
        _type = type;
        _parent = parent;
        _nestingDepth = parent == null ? 0 : parent._nestingDepth + 1;
        _basePathLength = basePathLength;
        _index = -1;
        _currentValue = currValue;
        _inline = (type == TYPE_ARRAY) || (parent != null && parent._inline);
    }

    private void reset(int type, Object currValue, int basePathLength) {
        _type = type;
        _basePathLength = basePathLength;
        _currentValue = null;
        _index = -1;
        _currentValue = currValue;
        // 09-Apr-2021, tatu: [dataformats-text#260]: must reset this flag as well
        _inline = (type == TYPE_ARRAY) || (_parent != null && _parent._inline);
    }

    static UBJsonWriteContext createRootContext() {
        return new UBJsonWriteContext(TYPE_ROOT, null, null, 0);
    }

    public UBJsonWriteContext createChildArrayContext(Object currValue, int basePathLength) {
        UBJsonWriteContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new UBJsonWriteContext(TYPE_ARRAY, this, currValue, basePathLength);
            return ctxt;
        }
        ctxt.reset(TYPE_ARRAY, currValue, basePathLength);
        return ctxt;
    }

    public UBJsonWriteContext createChildObjectContext(Object currValue, int basePathLength) {
        UBJsonWriteContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new UBJsonWriteContext(TYPE_OBJECT, this, currValue, basePathLength);
            return ctxt;
        }
        ctxt.reset(TYPE_OBJECT, currValue, basePathLength);
        return ctxt;
    }

    // *********************************************************************
    // State changes
    // *********************************************************************

    public boolean writeName(String name) {
        if (_gotName) {
            return false;
        }
        _gotName = true;
        _currentName = name;
        return true;
    }

    public boolean writeValue() {
        // Most likely, object:
        if (_type == TYPE_OBJECT) {
            if (!_gotName) {
                return false;
            }
            _gotName = false;
        }
        // Array fine, and must allow root context for Object values too so...
        ++_index;
        return true;
    }


    // *********************************************************************
    // Simple accessors, mutators
    // *********************************************************************

    @Override
    public final UBJsonWriteContext getParent() {
        return _parent;
    }

    @Override
    public String getCurrentName() {
        return _currentName;
    }

    @Override
    public Object getCurrentValue() {
        return _currentValue;
    }

    @Override
    public void setCurrentValue(Object v) {
        _currentValue = v;
    }

}
