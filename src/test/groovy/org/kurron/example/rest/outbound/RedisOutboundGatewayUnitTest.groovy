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

import static org.kurron.example.rest.feedback.ExampleFeedbackContext.REDIS_RESOURCE_NOT_FOUND
import static RedisOutboundGateway.CONTENT_TYPE_KEY
import static RedisOutboundGateway.PAYLOAD_KEY
import org.kurron.feedback.exceptions.NotFoundError
import java.util.concurrent.TimeUnit
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisOperations
import org.kurron.example.rest.BaseUnitTest

/**
 * Unit-level testing of the RedisOutboundGateway object.
 */
class RedisOutboundGatewayUnitTest extends BaseUnitTest {

    def redisOperations = Mock( RedisOperations )
    def hashOperations = Mock( HashOperations )
    def sut = new RedisOutboundGateway( redisOperations )
    def redisResource = new RedisResourceBuilder().build()
    def expectedRedisEntries = [(CONTENT_TYPE_KEY): redisResource.contentType, (PAYLOAD_KEY): redisResource.payload]

    def 'exercise resource storage'() {

        given: 'seconds to wait until the resource is expired'
        def expirationSeconds = randomNumberExclusive( 10 )

        when: 'the resource is stored'
        def result = sut.store( redisResource, expirationSeconds )

        then: 'the resource is stored in redis as expected'
        1 * redisOperations.opsForHash() >> hashOperations
        1 * hashOperations.putAll( !null as UUID, expectedRedisEntries )

        and: 'the expected expiration duration is set'
        1 * redisOperations.expire( !null as UUID, expirationSeconds, TimeUnit.SECONDS )

        and: 'a valid id is returned'
        result
        result == UUID.fromString( result.toString() )
    }

    def 'exercise resource retrieval'() {

        given: 'a resource id'
        def id = randomUUID()

        when: 'the resource is retrieved by id'
        def result = sut.retrieve( id )

        then: 'redis is called as expected'
        1 * redisOperations.opsForHash() >> hashOperations
        1 * hashOperations.entries( id ) >> expectedRedisEntries

        and: 'the expected resource is returned'
        result == redisResource
    }

    def 'exercise resource not found error handling'() {

        given: 'a resource id'
        def id = randomUUID()

        when: 'the resource is retrieved by id'
        sut.retrieve( id )

        then: 'no data is found in redis'
        1 * redisOperations.opsForHash() >> hashOperations
        1 * hashOperations.entries( id ) >> [:]

        and: 'the expected error is thrown'
        def error = thrown( NotFoundError )
        error.code == REDIS_RESOURCE_NOT_FOUND.code
    }
}
