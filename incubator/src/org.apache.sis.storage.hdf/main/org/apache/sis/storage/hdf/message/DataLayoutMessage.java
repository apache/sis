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
package org.apache.sis.storage.hdf.message;

import java.io.IOException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Data Layout
 * <p>
 * Header Message Type: 0x0008
 * <p>
 * Length: Varies
 * <p>
 * Status: Required for datasets; may not be repeated.
 * <p>
 * Description: Data layout describes how the elements of a multi-dimensional
 * array are stored in the HDF5 file : compact, contiguous, chunked or virtual.
 * <p>
 * Version 3 of this message re-structured the format into specific properties
 * that are required for each layout class.
 * <p>
 * Version 4 of this message is similar to version 3 but has additional
 * information for the virtual layout class as well as indexing information for
 * the chunked layout class.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_layout">IV.A.2.i. The Data Layout Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class DataLayoutMessage extends Message {

    /** Storage layout : {@link Compact}, {@link Contiguous}, {@link Chunked} or {@link Virtual}. */
    public Layout layout;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public DataLayoutMessage() {
    }

    /**
     * Returns the storage layout of the dataset raw data.
     *
     * @return the storage layout of the dataset raw data
     */
    public Layout getLayout() {
        return layout;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        /*
        Version 5 is emitted by HDF5 2.0 with the latest format bounds and uses
        the same encoding as version 4.
        */
        final int version = channel.ensureVersion(1, 2, 3, 4, 5);

        /*
        An array has a fixed dimensionality. This field specifies the number of
        dimension size fields later in the message. The value stored for chunked
        storage is 1 greater than the number of dimensions in the dataset’s
        dataspace. For example, 2 is stored for a 1 dimensional dataset.
        */
        final int dimensionality;
        if (version == 1 || version == 2) {
            dimensionality = channel.readUnsignedByte();
        } else {
            dimensionality = 0;
        }

        /*
         * The layout class specifies the type of storage for the data and how the
         * other fields of the layout message are to be interpreted.
         * 0 : Compact Storage
         * 1 : Contiguous Storage
         * 2 : Chunked Storage
         * 3 : Virtual Storage
         */
        final int layoutClass = channel.readUnsignedByte();

        if (version == 1 || version == 2) {
            channel.skipFully(5);
        }

        if (layoutClass == 0) {
            final Compact layout = new Compact();
            this.layout = layout;

            if (version == 1 || version == 2) {
                layout.dimensionSizes = channel.readInts(dimensionality);
                layout.size = channel.readInt();
                //do not read the datas now
                layout.rawDataAddress = channel.getStreamPosition();
                channel.skipFully(layout.size);
            } else {
                layout.size = channel.readUnsignedShort();
                //do not read the datas now
                layout.rawDataAddress = channel.getStreamPosition();
                channel.skipFully(layout.size);
            }
        } else if (layoutClass == 1) {
            final Contiguous layout = new Contiguous();
            this.layout = layout;

            if (version == 1 || version == 2) {
                layout.address = channel.readOffset();
                layout.dimensionSizes = channel.readInts(dimensionality);
            } else {
                layout.address = channel.readOffset();
                layout.size = channel.readLength();
            }
        } else if (layoutClass == 2) {
            final Chunked layout = new Chunked();
            this.layout = layout;
            if (version == 1 || version == 2) {
                layout.dimensionality = dimensionality;
                layout.address = channel.readOffset();
                layout.dimensionSizes = channel.readInts(dimensionality);
                layout.datasetElementSize = channel.readInt();
            } else if (version == 3) {
                //TODO : chunk dimentionality is +1 in this version.
                layout.dimensionality = channel.readUnsignedByte() - 1;
                layout.address = channel.readOffset();
                layout.dimensionSizes = channel.readInts(layout.dimensionality);
                layout.datasetElementSize = channel.readInt();
            } else {
                /*
                Version 4 and 5 share the same encoding for the chunked layout
                class. Version 5 is emitted by HDF5 2.0 when the file is created
                with the latest format bounds.
                */
                /*
                This is the chunked layout feature flag:
                DONT_FILTER_PARTIAL_BOUND_CHUNKS (bit 0)
                Do not apply filter to a partial edge chunk.
                SINGLE_INDEX_WITH_FILTER (bit 1)
                A filtered chunk for Single Chunk indexing.
                */
                layout.flags = channel.readUnsignedByte();
                /*
                A chunk has fixed dimension. This field specifies the number of
                Dimension Size fields later in the message. As in version 3, the
                value is one greater than the dataspace rank, the last dimension
                being the dataset element size.
                */
                final int storedDimensionality = channel.readUnsignedByte();
                layout.dimensionality = storedDimensionality - 1;
                /*
                This is the size in bytes used to encode Dimension Size.
                */
                final int dimensionSizeEncodedLength = channel.readUnsignedByte();
                /*
                These values define the dimension size of a single chunk, in units
                of array elements (not bytes). The first dimension stored in the
                list of dimensions is the slowest changing dimension and the last
                dimension stored is the fastest changing dimension.
                */
                layout.dimensionSizes = new int[layout.dimensionality];
                for (int i = 0; i < layout.dimensionality; i++) {
                    layout.dimensionSizes[i] = Math.toIntExact(channel.readUnsignedInteger(dimensionSizeEncodedLength));
                }
                layout.datasetElementSize = Math.toIntExact(channel.readUnsignedInteger(dimensionSizeEncodedLength));
                /*
                There are five indexing types used to look up addresses of the
                chunks. For more information on each type, see “Appendix C: Types
                of Indexes for Dataset Chunks.”
                1 : Single Chunk indexing type.
                2 : Implicit indexing type.
                3 : Fixed Array indexing type.
                4 : Extensible Array indexing type.
                5 : Version 2 B-tree indexing type.
                */
                layout.chunkIndexingType = channel.readUnsignedByte();
                /*
                This variable-sized field encodes information specific to an
                indexing type, and is absent for some of them.
                */
                switch (layout.chunkIndexingType) {
                    case Chunked.INDEX_SINGLE_CHUNK: {
                        if ((layout.flags & Chunked.FLAG_SINGLE_INDEX_WITH_FILTER) != 0) {
                            layout.singleChunkSize = channel.readLength();
                            layout.singleChunkFilterMask = channel.readInt();
                        }
                        break;
                    }
                    case Chunked.INDEX_IMPLICIT: {
                        //no indexing information
                        break;
                    }
                    case Chunked.INDEX_FIXED_ARRAY: {
                        layout.pageBits = channel.readUnsignedByte();
                        break;
                    }
                    case Chunked.INDEX_EXTENSIBLE_ARRAY: {
                        layout.maxBits = channel.readUnsignedByte();
                        layout.indexElements = channel.readUnsignedByte();
                        layout.minPointers = channel.readUnsignedByte();
                        layout.minElements = channel.readUnsignedByte();
                        layout.pageBits = channel.readUnsignedByte();
                        break;
                    }
                    case Chunked.INDEX_BTREE_V2: {
                        layout.btreeNodeSize = channel.readInt();
                        layout.btreeSplitPercent = channel.readUnsignedByte();
                        layout.btreeMergePercent = channel.readUnsignedByte();
                        break;
                    }
                    default:
                        throw new IOException("Unexpected chunk indexing type " + layout.chunkIndexingType);
                }
                layout.address = channel.readOffset();
            }
        } else if (layoutClass == 3) {
            final Virtual layout = new Virtual();
            layout.address = channel.readOffset();
            layout.index = channel.readInt();
            this.layout = layout;
        } else {
            throw new IOException("Unexpected layout class " + layoutClass);
        }

    }

    /** Parent class of the class specific information of each storage layout. */
    public static abstract class Layout {
        /** Constructor for subclasses. */
        public Layout() {
        }
    }

    /**
     * Class-specific information for compact storage (layout class 0).
     * The raw data is stored in the layout message itself.
     * (Note: The dimensionality information is in the Dataspace message)
     */
    public static class Compact extends Layout {
        /**
         * For contiguous and compact storage the dimensions define the entire size
         * of the array while for chunked storage they define the size of a single
         * chunk. In all cases, they are in units of array elements (not bytes).
         * The first dimension stored in the list of dimensions is the slowest
         * changing dimension and the last dimension stored is the fastest changing
         * dimension.
         * <p>
         * Only defined in version 1 and 2 of DataLayout.
         */
        public int[] dimensionSizes;
        /**
         * This field contains the size of the raw data for the dataset array,
         * in bytes.
         */
        public int size;
        /**
         * This field contains the position in the file of the raw data for the dataset array,
         * which is stored within this message.
         */
        public long rawDataAddress;

        /** Creates an empty layout, filled when the enclosing message is read. */
        public Compact() {
        }
    }

    /**
     * Class-specific information for contiguous storage (layout class 1).
     * The raw data is stored in a single block of the file.
     * (Note: The dimensionality information is in the Dataspace message)
     */
    public static class Contiguous extends Layout {
        /**
         * For contiguous and compact storage the dimensions define the entire size
         * of the array while for chunked storage they define the size of a single
         * chunk. In all cases, they are in units of array elements (not bytes).
         * The first dimension stored in the list of dimensions is the slowest
         * changing dimension and the last dimension stored is the fastest changing
         * dimension.
         * <p>
         * Only defined in version 1 and 2 of DataLayout.
         */
        public int[] dimensionSizes;
        /**
         * This is the address of the raw data in the file. The address may
         * have the “undefined address” value, to indicate that storage has
         * not yet been allocated for this array.
         */
        public long address;
        /**
         * This field contains the size allocated to store the raw data, in
         * bytes.
         */
        public long size;

        /** Creates an empty layout, filled when the enclosing message is read. */
        public Contiguous() {
        }
    }

    /** Class-specific information for chunked storage (layout class 2). */
    public static class Chunked extends Layout {

        /** Single Chunk indexing type. */
        public static final int INDEX_SINGLE_CHUNK = 1;
        /** Implicit indexing type. */
        public static final int INDEX_IMPLICIT = 2;
        /** Fixed Array indexing type. */
        public static final int INDEX_FIXED_ARRAY = 3;
        /** Extensible Array indexing type. */
        public static final int INDEX_EXTENSIBLE_ARRAY = 4;
        /** Version 2 B-tree indexing type. */
        public static final int INDEX_BTREE_V2 = 5;
        /** Version 1 B-tree, implied by layout message versions 1 to 3. */
        public static final int INDEX_BTREE_V1 = 0;

        /** Do not apply filters to a partial edge chunk. */
        public static final int FLAG_DONT_FILTER_PARTIAL_BOUND_CHUNKS = 0x01;
        /** The single chunk is filtered, and its size and filter mask are stored. */
        public static final int FLAG_SINGLE_INDEX_WITH_FILTER = 0x02;

        /**
         * These values define the dimension size of a single chunk, in units
         * of array elements (not bytes). The first dimension stored in the
         * list of dimensions is the slowest changing dimension and the last
         * dimension stored is the fastest changing dimension.
         */
        public int[] dimensionSizes;
        /**
         * A chunk has a fixed dimensionality. This field specifies the number
         * of dimension size fields later in the message.
         */
        public int dimensionality;
        /**
         * This is the address of the v1 B-tree that is used to look up the
         * addresses of the chunks that actually store portions of the array
         * data. The address may have the “undefined address” value, to
         * indicate that storage has not yet been allocated for this array.
         * <p>
         * For version 4 and 5 of DataLayout, this is the address of the chunk
         * index, whose structure depends on {@link #chunkIndexingType}.
         */
        public long address;
        /** The size of a dataset element, in bytes. */
        public int datasetElementSize;

        /** Chunked layout feature flags, version 4 and 5 only. */
        public int flags;
        /**
         * Indexing type used to look up chunk addresses. Version 4 and 5 store it
         * explicitly; earlier versions always use a version 1 B-tree.
         */
        public int chunkIndexingType = INDEX_BTREE_V1;
        /** Single Chunk indexing, filtered variant : size of the chunk in the file. */
        public long singleChunkSize;
        /** Single Chunk indexing, filtered variant : filter mask of the chunk. */
        public long singleChunkFilterMask;
        /**
         * Fixed and Extensible Array indexing : log2 of the number of elements
         * held by a data block page.
         */
        public int pageBits;
        /** Extensible Array indexing : log2 of the maximum number of elements. */
        public int maxBits;
        /** Extensible Array indexing : number of elements stored directly in the index block. */
        public int indexElements;
        /** Extensible Array indexing : minimum number of data block pointers of a super block. */
        public int minPointers;
        /** Extensible Array indexing : minimum number of elements of a data block. */
        public int minElements;
        /** Version 2 B-tree indexing : size of the B-tree nodes. */
        public int btreeNodeSize;
        /** Version 2 B-tree indexing : split percentage of the B-tree nodes. */
        public int btreeSplitPercent;
        /** Version 2 B-tree indexing : merge percentage of the B-tree nodes. */
        public int btreeMergePercent;

        /** Creates an empty layout, filled when the enclosing message is read. */
        public Chunked() {
        }

        /**
         * Returns the size in bytes of a full decoded chunk.
         *
         * @return the size in bytes of a full decoded chunk
         */
        public long getChunkByteSize() {
            long size = datasetElementSize;
            for (int d : dimensionSizes) {
                size *= d;
            }
            return size;
        }
    }

    /**
     * Class-specific information for virtual storage (layout class 3).
     * The raw data is mapped from other datasets, the mapping being stored in a global heap.
     */
    public static class Virtual extends Layout {
        /**
         * This is the address of the global heap collection where the VDS
         * mapping entries are stored. See “Disk Format: Level 1F - Global
         * Heap Block for Virtual Datasets.”
         */
        public long address;
        /**
         * This is the index of the data object within the global heap
         * collection.
         */
        public int index;

        /** Creates an empty layout, filled when the enclosing message is read. */
        public Virtual() {
        }
    }
}
