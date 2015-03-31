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
package org.kurron.example.rest.inbound

import org.kurron.feedback.exceptions.LengthRequiredError
import org.kurron.feedback.exceptions.PayloadTooLargeError
import org.kurron.feedback.exceptions.PreconditionFailedError
import org.kurron.example.rest.feedback.ExampleFeedbackContext
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.boot.actuate.metrics.GaugeService
import org.springframework.http.HttpStatus
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.util.NestedServletException
import org.kurron.example.rest.ApplicationPropertiesBuilder
import org.kurron.example.rest.BaseUnitTest
import org.kurron.example.rest.outbound.PersistenceOutboundGateway
import org.kurron.example.rest.outbound.RedisResourceBuilder

/**
 * Unit-level testing of the RestInboundGateway object.
 */
@SuppressWarnings( 'UnnecessaryGetter' )
class RestInboundGatewayUnitTest extends BaseUnitTest {

    def outboundGateway = Mock( PersistenceOutboundGateway )
    def configuration = new ApplicationPropertiesBuilder().build()
    def counter = Stub( CounterService )
    def gauge = Stub( GaugeService )
    def sut = new RestInboundGateway( outboundGateway, configuration, counter, gauge )
    def mapper = new Jackson2ObjectMapperBuilder().build()
    def mockMvc = MockMvcBuilders.standaloneSetup( sut ).build()
    def redisResource = new RedisResourceBuilder().build()

    def 'exercise asset storage'() {

        given: 'an expected resource id'
        def id = randomUUID()

        and: 'a number of minutes to wait until expiring the resource'
        def expirationMinutes = randomNumberExclusive( 10 )

        and: 'an expected resource uri'
        def expected = URI.create( "http://localhost/$id" )

        and: 'a valid request'
        def requestBuilder = MockMvcRequestBuilders.post( '/' )
                .content( redisResource.payload )
                .contentType( redisResource.contentType )
                .header( 'Content-Length', redisResource.payload.length )
                .header( CustomHttpHeaders.X_EXPIRATION_MINUTES, expirationMinutes )

        when: 'the POST request is made'
        def result = mockMvc.perform( requestBuilder ).andReturn()

        then: 'the outbound gateway is called'
        1 * outboundGateway.store( redisResource, expirationMinutes * 60 ) >> id

        and: 'a 201 (CREATED) status code is returned'
        result
        result.response.status == HttpStatus.CREATED.value()

        and: 'the response content type is set'
        result.response.contentType == HypermediaControl.MIME_TYPE

        and: 'the expected response is set in the body of the response'
        mapper.readValue( result.response.contentAsByteArray, HypermediaControl ).links.find { it.rel == 'self' }.href  == expected.toString()
    }

    def 'exercise asset retrieval request mappings'() {

        given: 'an id of an asset to retrieve'
        def id = randomUUID()

        and: 'a valid request'
        def requestBuilder = MockMvcRequestBuilders.get( '/{id}', id )

        when: 'the GET request is made'
        def result = mockMvc.perform( requestBuilder ).andReturn()

        then: 'the outbound gateway is called'
        1 * outboundGateway.retrieve( id ) >> redisResource

        then: 'a 200 (OK) status code is returned'
        result
        result.response.status == HttpStatus.OK.value()

        and: 'the response content type is set'
        result.response.contentType == redisResource.contentType

        and: 'the expected response is returned'
        result.response.contentAsByteArray == redisResource.payload
    }

    def 'exercise missing content length header'() {

        given: 'a request with no content length header set'
        def requestBuilder = MockMvcRequestBuilders.post( '/' ).content( redisResource.payload )

        when: 'the POST request is made'
        mockMvc.perform( requestBuilder ).andReturn()

        then: 'the expected error is thrown'
        def wrappedError = thrown( NestedServletException )
        def error = wrappedError.cause as LengthRequiredError
        error.code == ExampleFeedbackContext.CONTENT_LENGTH_REQUIRED.code
    }

    def 'exercise max payload size exceeded'() {

        given: 'a small maximum payload size'
        configuration.maxPayloadSize = 1    // in MB

        and: 'a byte size that exceeds the maximum'
        def tooBig = randomByteArray( 1024 * 1024 + 1 ) // convert to MB and add 1 byte

        and: 'a request with a payload that exceeds the maximum allowed size'
        def requestBuilder = MockMvcRequestBuilders.post( '/' )
                .header( 'Content-Length', 128 )
                .content( tooBig )

        when: 'the POST request is made'
        mockMvc.perform( requestBuilder ).andReturn()

        then: 'the expected error is thrown'
        def wrappedError = thrown( NestedServletException )
        def error = wrappedError.cause as PayloadTooLargeError
        error.code == ExampleFeedbackContext.PAYLOAD_TOO_LARGE.code
    }

    def 'exercise missing content type header'() {

        given: 'a request with no content type header set'
        def requestBuilder = MockMvcRequestBuilders.post( '/' )
                .header( 'Content-Length', 128 )
                .content( redisResource.payload )

        when: 'the POST request is made'
        mockMvc.perform( requestBuilder ).andReturn()

        then: 'the expected error is thrown'
        def wrappedError = thrown( NestedServletException )
        def error = wrappedError.cause as PreconditionFailedError
        error.code == ExampleFeedbackContext.PRECONDITION_FAILED.code
        error.message.contains( 'Content-Type' )
    }

    def 'exercise missing expiration minutes header'() {

        given: 'a request with no content type header set'
        def requestBuilder = MockMvcRequestBuilders.post( '/' )
                .contentType( redisResource.contentType )
                .header( 'Content-Length', 128 )
                .content( redisResource.payload )

        when: 'the POST request is made'
        mockMvc.perform( requestBuilder ).andReturn()

        then: 'the expected error is thrown'
        def wrappedError = thrown( NestedServletException )
        def error = wrappedError.cause as PreconditionFailedError
        error.code == ExampleFeedbackContext.PRECONDITION_FAILED.code
        error.message.contains( CustomHttpHeaders.X_EXPIRATION_MINUTES )
    }
}
