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

import org.kurron.feedback.exceptions.PreconditionFailedError
import org.kurron.example.rest.feedback.ExampleFeedbackContext
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.kurron.example.rest.inbound.CustomHttpHeaders

/**
 * Unit-level testing of the CorrelationIdHandlerInterceptor object.
 */
class CorrelationIdHandlerInterceptorUnitTest extends BaseUnitTest {

    def configuration = new ApplicationPropertiesBuilder().build()
    def sut = new CorrelationIdHandlerInterceptor( configuration )

    def 'exercise correlation id extraction'() {

        given: 'a correlation id'
        def correlationId = randomHexString()

        and: 'a valid http request'
        def request = new MockHttpServletRequest()
        request.addHeader( CustomHttpHeaders.X_CORRELATION_ID, correlationId )

        when: 'pre handle is called'
        def result = sut.preHandle( request, null, null )

        then: 'the correlation id is set in the thread local MDC'
        MDC.get( CorrelationIdHandlerInterceptor.CORRELATION_ID ) == correlationId

        and: 'true is returned'
        result
    }

    def 'exercise missing header error handling'() {

        given: 'a valid http request with no correlation id'
        def request = new MockHttpServletRequest()

        and: 'the correlation id is required'
        configuration.requireCorrelationId = true

        when: 'pre handle is called'
        sut.preHandle( request, null, null )

        then: 'a missing header error is thrown'
        def error = thrown( PreconditionFailedError )
        error.code == ExampleFeedbackContext.PRECONDITION_FAILED.code
    }

    def 'exercise missing header when it is not required'() {

        given: 'a valid http request with no correlation id'
        def request = new MockHttpServletRequest()

        and: 'the correlation id is not required'
        configuration.requireCorrelationId = false

        when: 'pre handle is called'
        sut.preHandle( request, null, null )

        then: 'a missing header error is not thrown'
        notThrown( PreconditionFailedError )

        and: 'a correlation id is generated'
        def generated = MDC.get( CorrelationIdHandlerInterceptor.CORRELATION_ID )
        generated
        // make sure it's a UUID string
        generated == UUID.fromString( generated ).toString()

    }
}
