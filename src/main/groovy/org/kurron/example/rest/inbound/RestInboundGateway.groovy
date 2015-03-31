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

import static org.kurron.example.rest.feedback.ExampleFeedbackContext.CONTENT_LENGTH_REQUIRED
import static org.kurron.example.rest.feedback.ExampleFeedbackContext.PAYLOAD_TOO_LARGE
import static org.kurron.example.rest.feedback.ExampleFeedbackContext.PRECONDITION_FAILED
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo
import static org.springframework.web.bind.annotation.RequestMethod.GET
import static org.springframework.web.bind.annotation.RequestMethod.POST
import org.kurron.feedback.AbstractFeedbackAware
import org.kurron.feedback.exceptions.LengthRequiredError
import org.kurron.feedback.exceptions.PayloadTooLargeError
import org.kurron.feedback.exceptions.PreconditionFailedError
import org.kurron.example.rest.ApplicationProperties
import javax.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.boot.actuate.metrics.GaugeService
import org.springframework.hateoas.Link
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.kurron.example.rest.outbound.PersistenceOutboundGateway
import org.kurron.example.rest.outbound.RedisResource
import org.kurron.stereotype.InboundRestGateway

/**
 * Handles inbound REST requests.
 */
@InboundRestGateway
@RequestMapping( value = '/' )
class RestInboundGateway extends AbstractFeedbackAware {

    /**
     * Constant for the 'api documentation' relation.
     */
    public static final String API_DOCUMENTATION = 'api-documentation'

    /**
     * Constant for the 'api discovery' relation.
     */
    public static final String API_DISCOVERY_RELATION = 'api-discovery'

    /**
     * Constant for the 'asset upload' relation.
     */
    public static final String UPLOAD_RELATION = 'upload'

    /**
     * Handles interactions with the outbound persistence layer.
     */
    private final PersistenceOutboundGateway outboundGateway

    /**
     * Provides currently active property values.
     */
    private final ApplicationProperties configuration

    /**
     * The number of seconds in a minute.
     */
    private static final int SECONDS_IN_A_MINUTE = 60

    /**
     * Number of Bytes in a Megabyte.
     */
    @SuppressWarnings( 'DuplicateNumberLiteral' )
    private static final long BYTES_IN_A_MEGABYTE = 1024 * 1024

    /**
     * Used to track counts.
     */
    private final CounterService counterService

    /**
     * Used to track values.
     */
    private final GaugeService gaugeService

    @Autowired
    RestInboundGateway( final PersistenceOutboundGateway anOutboundGateway,
                        final ApplicationProperties aConfiguration,
                        final CounterService aCounterService,
                        final GaugeService aGaugeService ) {
        outboundGateway = anOutboundGateway
        configuration = aConfiguration
        counterService = aCounterService
        gaugeService = aGaugeService

    }

    /**
     * Store an asset temporarily. The asset is expired after the number of minutes specified in the X-Expiration-Minutes
     * header. The location to the stored asset is returned in the body and the location header.
     * @param payload the binary asset to store.
     * @param requestHeaders the HTTP request headers.
     * @param request the servlet request being serviced.
     * @return the response entity.
     */
    @RequestMapping( method = POST, produces = HypermediaControl.MIME_TYPE )
    ResponseEntity<HypermediaControl> store( @RequestBody final byte[] payload,
                                             @RequestHeader HttpHeaders requestHeaders,
                                             HttpServletRequest request ) {
        validateContentLengthHeader( requestHeaders )
        validatePayloadSize( payload )
        def contentType = extractContentType( requestHeaders )
        def expirationMinutes = extractExpirationMinutes( requestHeaders )
        def id = outboundGateway.store( new RedisResource( contentType: contentType.toString(), payload: payload ), minutesToSeconds( expirationMinutes ) )
        counterService.increment( 'example.upload' )
        gaugeService.submit( 'example.upload.payload.size', payload.length )
        gaugeService.submit( 'example.upload.expiration', expirationMinutes )
        toResponseEntity( id, contentType.toString(), payload.length, request )
    }

    /**
     * Validate the content length, ensuring that it is set to a non-zero (positive) value. Note that we don't actually use the length
     * and instead calculate it based on the payload. However, it still must be set to ensure chunked encoding is not being used.
     * @param headers the headers to extract the content length from.
     */
    @SuppressWarnings( 'UnnecessaryGetter' )
    private void validateContentLengthHeader( HttpHeaders headers ) {
        if ( headers.getContentLength() <= 0 ) {
            feedbackProvider.sendFeedback( CONTENT_LENGTH_REQUIRED )
            throw new LengthRequiredError( CONTENT_LENGTH_REQUIRED )
        }
    }

    /**
     * Validate the payload size of the request, ensuring that it is equal to or below the maximum allowed size.
     * @param payload the payload to validate.
     */
    private void validatePayloadSize( byte[] payload ) {
        if ( configuration.maxPayloadSize * BYTES_IN_A_MEGABYTE < payload.length ) {
            feedbackProvider.sendFeedback( PAYLOAD_TOO_LARGE, payload.length, configuration.maxPayloadSize )
            throw new PayloadTooLargeError( PAYLOAD_TOO_LARGE, payload.length, configuration.maxPayloadSize  )
        }
    }

    /**
     * Extracts the expiration minutes from the HTTP headers. If not set, an error is thrown.
     * @param headers the headers to extract from.
     * @return the extracted expiration minutes.
     */
    private int extractExpirationMinutes( HttpHeaders headers ) {
        def expirationMinutes = headers.getFirst( CustomHttpHeaders.X_EXPIRATION_MINUTES )
        if ( !expirationMinutes ) { throwPreconditionFailedError( CustomHttpHeaders.X_EXPIRATION_MINUTES ) }
        expirationMinutes as int
    }

    /**
     * Extracts the content type from the HTTP headers. If not set, an error is thrown.
     * @param headers the headers to extract from.
     * @return the extracted content type.
     */
    @SuppressWarnings( 'UnnecessaryGetter' )
    private MediaType extractContentType( HttpHeaders headers ) {
        def contentType = headers.getContentType()
        if ( !contentType ) { throwPreconditionFailedError( 'Content-Type' ) }
        contentType
    }

    /**
     * Logs a message and throws a precondition failed error.
     * @param missingHeaderName the name of the missing header to use in the logs and error context.
     */
    private void throwPreconditionFailedError( String missingHeaderName ) {
        feedbackProvider.sendFeedback( PRECONDITION_FAILED, missingHeaderName )
        throw new PreconditionFailedError( PRECONDITION_FAILED, missingHeaderName )
    }

    private static long minutesToSeconds( int minutes ) {
        minutes * SECONDS_IN_A_MINUTE
    }

    /**
     * Wraps the id in a 201 (created) HTTP response entity to be returned to the client.
     * @param id the id to embed in the response.
     * @param mimeType the MIME type of the uploaded asset.
     * @param contentLength the length, in bytes, of the uploaded asset.
     * @param request the servlet request being serviced.
     */
    private static ResponseEntity<HypermediaControl> toResponseEntity( UUID id,
                                                                  String mimeType,
                                                                  int contentLength,
                                                                  HttpServletRequest request ) {
        def control = newControl( HttpStatus.CREATED, request )
        def location = linkTo( RestInboundGateway, RestInboundGateway.getMethod( 'retrieve', UUID ), id )
        control.add( location.withSelfRel() )
        control.metaDataBlock = new MetaDataBlock( mimeType: mimeType, contentLength: contentLength )
        def headers = new HttpHeaders( location: location.toUri() )
        new ResponseEntity( control, headers, HttpStatus.CREATED )
    }

    /**
     * Retrieves an asset based on the provided id. If an error occurs, the global error handler kicks in and our json
     * fault document will be returned.
     * @param id the id of the asset to retrieve.
     * @return the response entity containing the requested asset.
     */
    @RequestMapping( value = '/{id}', method = GET )
    ResponseEntity<byte[]> retrieve( @PathVariable( 'id' ) final UUID id ) {
        def resource = outboundGateway.retrieve( id )
        def headers = new HttpHeaders( contentType: MediaType.parseMediaType( resource.contentType ) )
        counterService.increment( 'example.download' )
        new ResponseEntity( resource.payload, headers, HttpStatus.OK )
    }

    /**
     * This supports the GET of the 'api discovery' resource where the client can decode what the API currently supports.
     * @param request the servlet request being serviced.
     * @return hypermedia control describing the API.
     */
    @RequestMapping( method = GET, produces = [HypermediaControl.MIME_TYPE] )
    ResponseEntity<HypermediaControl> apiDiscovery( HttpServletRequest request ) {
        def control = newControl( HttpStatus.OK, request )
        new ResponseEntity( control, HttpStatus.OK )
    }

    /**
     * Builds out a new instance of the control with values that make sense for most contexts. Further
     * customization is expected after this call.
     * @param status the HTTP status to save in the control.
     * @param request the servlet request being serviced.
     * @return partially populated control.
     */
    @SuppressWarnings( 'DuplicateStringLiteral' )
    private static HypermediaControl newControl( HttpStatus status, HttpServletRequest request ) {
        def control = new HypermediaControl()
        control.httpCode = status.value()

        // currently, these links are always valid
        control.add( linkTo( RestInboundGateway, RestInboundGateway.getMethod( 'apiDiscovery', HttpServletRequest ) ).withRel( API_DISCOVERY_RELATION ) )
        control.add( linkTo( RestInboundGateway, RestInboundGateway.getMethod( 'store', byte[], HttpHeaders, HttpServletRequest ) ).withRel( UPLOAD_RELATION ) )
        def apiDocLocation = ServletUriComponentsBuilder.fromContextPath( request ).path( 'api-docs/api-guide.html' ).build().toString()
        control.add( new Link( apiDocLocation, API_DOCUMENTATION ) )
        control
    }
}
