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

import static org.kurron.example.rest.feedback.ExampleFeedbackContext.GENERIC_ERROR
import org.kurron.feedback.AbstractFeedbackAware
import org.kurron.example.rest.inbound.ErrorBlock
import org.kurron.example.rest.inbound.HypermediaControl
import javax.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.ErrorAttributes
import org.springframework.boot.autoconfigure.web.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.ModelAndView
import org.kurron.stereotype.InboundRestGateway

/**
 * A handler that kicks in when all the other handlers aren't triggered. See ErrorAttributes to
 * understand what attributes are populated and how.  We could substitute our own set if desired.
 */
@SuppressWarnings( ['GroovyUnusedDeclaration', 'GStringExpressionWithinString', 'DuplicateStringLiteral'] )
@InboundRestGateway
class CustomErrorController extends AbstractFeedbackAware implements ErrorController {

    /**
     * The error view path that Spring has set up.
     */
    @Value( '${error.path:/error}' )
	private String theErrorPath

    /**
     * Contains the error attributes pulled from the exception context.
     */
    private final ErrorAttributes errorAttributes

    @Autowired
    CustomErrorController( ErrorAttributes errorAttributes ) {
        this.errorAttributes = errorAttributes
    }

    @Override
    String getErrorPath() {
        theErrorPath
    }

    /**
     * This exists just to be nice to browsers.  I don't anticipate it being called often, if at all.
     * @param request the request that triggered the failure.
     * @return the model and view combination to use when rendering the page.
     */
    @RequestMapping( value = '${error.path:/error}', produces = MediaType.TEXT_HTML_VALUE )
    ModelAndView errorHtml( HttpServletRequest request ) {
            new ModelAndView( 'error', errorAttributes.getErrorAttributes( new ServletRequestAttributes( request ), false ) )
    }

    /**
     * This is the default 'view' for unhandled failures.  It converts everything into a hypermedia control.
     * @param request the HTTP request that triggered the failure.
     * @return the control containing the error details.
     */
    @RequestMapping( value = '${error.path:/error}', produces = [HypermediaControl.MIME_TYPE] )
    ResponseEntity<HypermediaControl> handleException( HttpServletRequest request ) {
        Map<String, Object> attributes = errorAttributes.getErrorAttributes( new ServletRequestAttributes( request ), false )
        def status = getStatus( request )
        def control = new HypermediaControl( status.value() )
        String path = attributes.getOrDefault( 'path', 'unknown' )
        String message = attributes.getOrDefault( 'message', 'unknown' )
        String error = attributes.getOrDefault( 'error', 'unknown' )
        feedbackProvider.sendFeedback( GENERIC_ERROR, error )
        control.errorBlock = new ErrorBlock( code: GENERIC_ERROR.code,
                                             message: error,
                                             developerMessage: "Failure for path ${path}. ${message}"  )
        new ResponseEntity<HypermediaControl>( control, status )
    }

    /**
     * Attempts to determine the HTTP status code that triggered the failure.
     * @param request the request that triggered the failure.
     * @return the obtained HTTP status code.
     */
    private static HttpStatus getStatus( HttpServletRequest request ) {
        def statusCode = request.getAttribute( 'javax.servlet.error.status_code' )
        statusCode ? HttpStatus.valueOf( statusCode as int ) : HttpStatus.INTERNAL_SERVER_ERROR
    }

}
