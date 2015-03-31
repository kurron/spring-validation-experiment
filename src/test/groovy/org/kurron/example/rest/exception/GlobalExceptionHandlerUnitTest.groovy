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
package org.kurron.example.rest.exception

import org.kurron.feedback.exceptions.AbstractError
import org.kurron.feedback.exceptions.LengthRequiredError
import org.kurron.feedback.exceptions.NotFoundError
import org.kurron.feedback.exceptions.PayloadTooLargeError
import org.kurron.feedback.exceptions.PreconditionFailedError
import org.kurron.example.rest.feedback.ExampleFeedbackContext
import org.springframework.http.HttpStatus
import org.kurron.example.rest.BaseUnitTest
import org.kurron.example.rest.inbound.HypermediaControl

/**
 * Unit-level testing of the GlobalExceptionHandler object.
 */
@SuppressWarnings( 'UnnecessaryGetter' )
class GlobalExceptionHandlerUnitTest extends BaseUnitTest {

    def sut = new GlobalExceptionHandler()

    def 'exercise error handling'( Class clazz ) {

        given: 'a valid application exception'
        def feedback = randomEnum( ExampleFeedbackContext )
        def error = clazz.newInstance( feedback ) as AbstractError

        when: 'an exception is handled'
        def result = sut.handleApplicationException( error )

        then: 'a valid http response is returned'
        result
        result.statusCode == error.httpStatus
        result.headers.getContentType() == HypermediaControl.MEDIA_TYPE

        and: 'the body contains the expected fault document'
        def control = result.body
        control
        control.httpCode == error.httpStatus.value()
        control.errorBlock.code == feedback.code
        control.errorBlock.message == error.message
        control.errorBlock.developerMessage == error.developerMessage

        where: 'each error type is provided'
        clazz << [NotFoundError, LengthRequiredError, PayloadTooLargeError, PreconditionFailedError]
    }

    def 'exercise non-application error handling'() {

        given: 'a non-application error'
        def error = new RuntimeException( 'expected to fail' )

        when: 'an error is handled'
        def result = sut.handleException( error, null )

        then: 'a valid http response is returned'
        result
        result.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        result.headers.getContentType() == HypermediaControl.MEDIA_TYPE

        and: 'the body contains a valid fault document'
        def control = result.body as HypermediaControl
        control
        control.httpCode == result.statusCode.value()
        control.errorBlock.code == ExampleFeedbackContext.GENERIC_ERROR.code
    }
}
