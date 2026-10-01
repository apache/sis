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
package org.apache.sis.storage.internal.shared;

import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.Resource;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TilingContentEvent extends ContentEvent {

    public static enum Type{
        /** tiles added */
        TILE_ADD,
        /** tiles updated */
        TILE_UPDATE,
        /** tiles deleted */
        TILE_DELETE
    };

    private final Type type;
    private final String pyramidId;
    private final String mosaicId;
    private final GridExtent tiles;


    public TilingContentEvent(Resource source, Type type,
            String pyramidId, String mosaicId, GridExtent tiles) {
        super(source);
        this.type = type;
        this.pyramidId = pyramidId;
        this.mosaicId = mosaicId;
        this.tiles = tiles;
    }

    public Type getType() {
        return type;
    }

    public String getPyramidId() {
        return pyramidId;
    }

    public String getMosaicId() {
        return mosaicId;
    }

    public GridExtent getTiles() {
        return tiles;
    }

    @Override
    public TilingContentEvent copy(final Resource source){
        return new TilingContentEvent(source, type, pyramidId, mosaicId, tiles);
    }

    public static TilingContentEvent createTileAddEvent(final Resource source,
            final String pyramidId, final String mosaicId, final GridExtent tiles){
        return new TilingContentEvent(source, Type.TILE_ADD, pyramidId, mosaicId, tiles);
    }

    public static TilingContentEvent createTileUpdateEvent(final Resource source,
            final String pyramidId, final String mosaicId, final GridExtent tiles){
        return new TilingContentEvent(source, Type.TILE_UPDATE, pyramidId, mosaicId, tiles);
    }

    public static TilingContentEvent createTileDeleteEvent(final Resource source,
            final String pyramidId, final String mosaicId, final GridExtent tiles){
        return new TilingContentEvent(source, Type.TILE_DELETE, pyramidId, mosaicId, tiles);
    }

}
