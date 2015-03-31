/*
 * Copyright (c) 2015. Ronald D. Kurr kurr@jvmguy.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kurron.example.rest.outbound

/**
 * Interacts with the outbound persistence layer.
 */
interface PersistenceOutboundGateway {

    /**
     * Store the provided resource.
     * @param resource the resource to store, which includes the content type and bytes.
     * @param expirationSeconds the number of seconds to wait before expiring the resource.
     * @return the assigned id of the stored resource.
     */
    UUID store( final RedisResource resource, final long expirationSeconds )

    /**
     * Retrieves the resource associated with the provided id.
     * @param id the id of the resource to retrieve.
     * @return the resource.
     */
    RedisResource retrieve( final UUID id )
}
