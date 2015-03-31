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

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.experimental.categories.Category
import org.kurron.categories.InboundIntegrationTest
import org.springframework.boot.test.TestRestTemplate
import org.springframework.hateoas.hal.Jackson2HalModule
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder

/**
 * Base class for tests of the inbound gateways.
 */
@SuppressWarnings( ['GStringExpressionWithinString'] )
@Category( InboundIntegrationTest )
abstract class BaseInboundIntegrationTest extends BaseTest {

    protected static int getPort() {
        System.properties['integration.test.port'] as int
    }

    protected static RestOperations getRestOperations() {
        def template = new TestRestTemplate()
        // filter out the existing Jackson convert so we can replace with our own that is configured to deal with HAL links
        def toKeep = template.messageConverters.findAll { !it.class.isAssignableFrom( MappingJackson2HttpMessageConverter ) }
        toKeep.add( new MappingJackson2HttpMessageConverter( mapper ) )
        template.messageConverters.clear()
        template.messageConverters.addAll( toKeep )
        template
    }

    protected static ObjectMapper getMapper() {
        new Jackson2ObjectMapperBuilder().modules( new Jackson2HalModule() ).build()
    }

    protected static URI getServerUri() {
        UriComponentsBuilder.newInstance()
                .scheme( 'http' )
                .host( 'localhost' )
                .port( port )
                .path( '/' )
                .build()
                .toUri()
    }
}
