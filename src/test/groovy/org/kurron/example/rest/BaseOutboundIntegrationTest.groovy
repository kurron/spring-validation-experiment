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
package org.kurron.example.rest

import org.junit.experimental.categories.Category
import org.kurron.categories.OutboundIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ContextConfiguration

/**
 * Base class for tests of the outbound gateways.
 */
@ContextConfiguration( loader = SpringApplicationContextLoader, classes = Application )
@SuppressWarnings( 'AbstractClassWithoutAbstractMethod' )
@WebIntegrationTest( randomPort = true ) // needed because the CustomErrorController will cause the context to not load
@Category( OutboundIntegrationTest )
abstract class BaseOutboundIntegrationTest extends BaseTest {

    @Autowired
    protected RedisTemplate redisTemplate
}
