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
package org.apache.sis.cloud.aws.s3;

import java.util.Set;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Object;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * A fake S3 client for querying only the root of a given set of buckets.
 * Each bucket contains a {@code "common/"} pseudo-directory and a {@code "data.nc"} object at its root.
 *
 * @author  Quentin Bialota (Geomatys)
 */
final class S3ClientMock implements S3Client {
    /**
     * The buckets recognized by the client.
     */
    private final Set<String> buckets;

    /**
     * Creates a new client mock.
     *
     * @param  buckets  the buckets recognized by the client.
     */
    S3ClientMock(final String[] buckets) {
        this.buckets = Set.of(buckets);
    }

    /**
     * Returns the name of the service, which is {@code "s3}.
     */
    @Override
    public String serviceName() {
        return "s3";
    }

    /**
     * Determines if a bucket exists and if the caller has permission to access it.
     * The response contains a 200 OK code if the bucket exists and is accessible.
     *
     * @param  request  a request with the bucket to access.
     * @return whether is bucket is accessible.
     */
    @Override
    public HeadBucketResponse headBucket(final HeadBucketRequest request) {
        if (!buckets.contains(request.bucket())) {
            throw NoSuchBucketException.builder().message(request.bucket()).build();
        }
        return HeadBucketResponse.builder().build();
    }

    /**
     * Returns the objects in a bucket with each request.
     * This method assumes that only the bucket root is requested.
     *
     * @param  request  a request for the bucket root.
     * @return {@code "common/data.nc"}.
     */
    @Override
    public ListObjectsV2Response listObjectsV2(final ListObjectsV2Request request) {
        assertNull(request.prefix(), "Listing the root of a bucket should not use a prefix.");
        return ListObjectsV2Response.builder()
                .commonPrefixes(CommonPrefix.builder().prefix("common/").build())
                .contents(S3Object.builder().key("data.nc").size(10L).build())
                .isTruncated(false)
                .build();
    }

    /**
     * No-operation (nothing to release).
     */
    @Override
    public void close() {
    }
}
