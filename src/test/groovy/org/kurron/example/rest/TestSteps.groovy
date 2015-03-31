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

import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import groovy.util.logging.Slf4j
import org.kurron.example.rest.feedback.ExampleFeedbackContext
import org.kurron.example.rest.inbound.CustomHttpHeaders
import org.kurron.example.rest.inbound.HypermediaControl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.util.UriComponentsBuilder

/**
 * Step definitions geared towards the application's acceptance test but remember, all steps are used
 * by Cucumber unless special care is taken. If you word your features in a consistent manner, then
 * steps will automatically get reused and you won't have to keep writing the same test code.
 **/
@ContextConfiguration( classes = [AcceptanceTestConfiguration], loader = SpringApplicationContextLoader )
@Slf4j
@SuppressWarnings( ['GrMethodMayBeStatic', 'GStringExpressionWithinString'] )
class TestSteps {

    /**
     * The application's configuration settings.
     **/
    @Autowired
    private ApplicationProperties configuration

    /**
     * Constant for unknown media type.
     **/
    public static final String ALL = '*'

    /**
     * Default size of uploaded payload.
     **/
    public static final int BUFFER_SIZE = 256

    /**
     * Number of Bytes in a Megabyte.
     */
    @SuppressWarnings( 'DuplicateNumberLiteral' )
    public static final int BYTES_IN_A_MEGABYTE = 1024 * 1024

    /**
     * Generates random data.
     **/
    @SuppressWarnings( 'UnusedPrivateField' )
    @Delegate
    private final Randomizer randomizer = new Randomizer()

    /**
     * This is state shared between steps and can be setup and torn down by the hooks.
     **/
    static class MyWorld {
        ResponseEntity<HypermediaControl> uploadEntity
        ResponseEntity<byte[]> downloadEntity
        byte[] bytes = new byte[0]
        def headers = new HttpHeaders()
        def mediaType  = new MediaType( ALL, ALL )
        URI uri
        URI location
        HttpStatus statusCode = HttpStatus.I_AM_A_TEAPOT
        def internet = BaseInboundIntegrationTest.restOperations
        def transformer = BaseInboundIntegrationTest.mapper
    }

    /**
     * Shared between hooks and steps.
     **/
    MyWorld sharedState

    @Before
    void assembleSharedState() {
        log.info( 'Creating shared state' )
        sharedState = new MyWorld()
        sharedState.bytes = randomByteArray( BUFFER_SIZE )
        sharedState.uri = BaseInboundIntegrationTest.serverUri
    }

    @After
    void destroySharedState() {
        log.info( 'Destroying shared state' )
        sharedState = null
    }

    @Given( '^each request contains an X-Correlation-Id header filled in with a unique value$' )
    void 'each request contains an X-Correlation-Id header filled in with a unique value'() {
        specifyCorrelationID()
    }

    private specifyCorrelationID( String id = randomHexString() )  {
        sharedState.headers.set( CustomHttpHeaders.X_CORRELATION_ID, id )
    }

    @Given( '^a Content-Type header filled in with media-type of the uploaded asset$' )
    void 'a Content-Type header filled in with media-type of the uploaded asset'() {
        sharedState.mediaType = generateMediaType()
        specifyContentType()
    }

    private MediaType generateMediaType() {
        def type = randomHexString()
        def subtype = randomHexString()
        Map<String, String> options = [:]
        options[randomHexString()] = randomHexString()
        new MediaType( type, subtype, options )
    }

    private specifyContentType() {
        sharedState.headers.setContentType( sharedState.mediaType )
    }

    @Given( '^an X-Expiration-Minutes header filled in with the number of minutes the asset should be available$' )
    void 'an X-Expiration-Minutes header filled in with the number of minutes the asset should be available'() {
        specifyExpiration()
    }

    private specifyExpiration( int minutes = 1 ) {
        sharedState.headers.set( CustomHttpHeaders.X_EXPIRATION_MINUTES, minutes.toString() )
    }

    @Given( '^a Content-Length header filled in with the size, in bytes, of the asset being uploaded$' )
    @SuppressWarnings( 'EmptyMethod' )
    void 'a Content-Length header filled in with the size, in bytes, of the asset being uploaded'() {
        // nothing to do because the Spring Template always sets the header with the correct value
    }

    @Given( '^an Accept header filled in with the desired media-type of the returned hypermedia control$' )
    void 'an Accept header filled in with the desired media-type of the returned hypermedia control'() {
        // there is a ticket to deal with versions so, presumably, we will also be required to send
        // some options, eg type=custom-control;version=1.2.3
        specifyAcceptType()
    }

    private specifyAcceptType( MediaType mediaType = MediaType.APPLICATION_JSON ) {
        List<MediaType> acceptable = [mediaType]
        acceptable.add( HypermediaControl.MEDIA_TYPE )
        sharedState.headers.setAccept( acceptable )
    }

    @Given( '^an asset to be uploaded$' )
    void 'an asset to be uploaded'() {
        sharedState.bytes = sharedState.bytes ?: randomByteArray( BUFFER_SIZE )
    }

    @Given( '^an asset that is too large$' )
    void 'an asset that is too large'() {
        int tooBig = (configuration.maxPayloadSize * BYTES_IN_A_MEGABYTE) + 1
        sharedState.bytes = randomByteArray( tooBig )
    }

    @Given( '^an Accept header filled in with the desired media-type of the bits to be downloaded$' )
    void 'an Accept header filled in with the desired media-type of the bits to be downloaded'() {
        specifyAcceptType( sharedState.mediaType )
    }

    @Given( '^an asset has previously been uploaded$' )
    void 'an asset has previously been uploaded'() {
        // reusing steps to upload bytes is too difficult to properly manage so we invoke the steps here
        sharedState.headers = new HttpHeaders() // clear out any prior steps' headers
        specifyCorrelationID()
        specifyExpiration()
        sharedState.mediaType = generateMediaType()
        specifyContentType()
        specifyAcceptType()
        def requestEntity = new HttpEntity( sharedState.bytes, sharedState.headers )
        sharedState.location = sharedState.internet.postForLocation( sharedState.uri, requestEntity )
        sharedState.headers = new HttpHeaders() // reset for the remaining steps
    }

    @Given( '^Content-Length header with a value larger than what the server will accept$' )
    void 'Content-Length header with a value larger than what the server will accept'() {
        // Spring is smart and sets the Content-Length based on the uploaded payload
        int tooBig = (configuration.maxPayloadSize * BYTES_IN_A_MEGABYTE) + 1
        sharedState.bytes = randomByteArray( tooBig )
    }

    @Given( '^a URI that does not match anything in the system$' )
    void 'a URI that does not match anything in the system'() {
        def builder = UriComponentsBuilder.newInstance()
        sharedState.location.with {
            builder.scheme( it.scheme ).host( it.host ).port( it.port )
        }
        sharedState.location = builder.path( '/' ).path( randomUUID().toString() ).build().toUri()
    }

    @Given( '^a URI of an expired asset$' )
    void 'a URI of an expired asset'() {
        // we will wait long enough to allow the previously uploaded asset to expire
        Thread.sleep( 1000 * 65 )
    }

    @When( '^a POST request is made with the asset in the body$' )
    void 'a POST request is made with the asset in the body'() {
        def requestEntity = new HttpEntity( sharedState.bytes, sharedState.headers )
        sharedState.uploadEntity = sharedState.internet.postForEntity( sharedState.uri, requestEntity, HypermediaControl )
        sharedState.statusCode = sharedState.uploadEntity.statusCode
    }

    @When( '^a GET request is made to the URI$' )
    void '^a GET request is made to the URI$'() {
        def requestEntity = new HttpEntity( new byte[0], sharedState.headers )

        sharedState.downloadEntity = sharedState.internet.exchange( sharedState.location, HttpMethod.GET, requestEntity, byte[] )
        sharedState.statusCode = sharedState.downloadEntity.statusCode
    }

    @Then( '^a response with a (\\d+) HTTP status code is returned$' )
    void 'a response with an HTTP status code is returned'( int statusCode ) {
        assert statusCode == sharedState.statusCode.value()
    }

    @Then( '^the Content-Type header matches the Accept header$' )
    @SuppressWarnings( 'UnnecessaryGetter' )
    void 'the Content-Type header matches the Accept header'() {
        String acceptType = sharedState.headers.accept.first()
        def types = MediaType.parseMediaTypes( acceptType )
        assert types.any { it.isCompatibleWith( sharedState.downloadEntity.headers.getContentType() ) }
    }

    @Then( '^the body contains the asset$' )
    void 'the body contains the asset'() {
        assert sharedState.downloadEntity.body == sharedState.bytes
    }

    @Then( '^the Location header contains the URI of the uploaded asset$' )
    void 'the Location header contains the URI of the uploaded asset'() {
        assert sharedState.uploadEntity.headers.location
    }

    @Then( '^the hypermedia control contains the URI of the uploaded asset$' )
    void 'the hypermedia control contains the URI of the uploaded asset'() {
        assert sharedState.uploadEntity.body.links.find { it.rel == 'self' }
    }

    @Then( '^the hypermedia control contains the meta-data of the uploaded asset$' )
    void 'the hypermedia control contains the meta-data of the uploaded asset'() {
        assert sharedState.uploadEntity.body.metaDataBlock.mimeType == sharedState.mediaType.toString()
        assert sharedState.uploadEntity.body.metaDataBlock.contentLength == sharedState.bytes.length
    }

    @Then( '^the hypermedia control describing the size problem is returned$' )
    void 'the hypermedia control describing the size problem is returned'() {
        assert sharedState.uploadEntity.body.httpCode == HttpStatus.PAYLOAD_TOO_LARGE.value()
        assert sharedState.uploadEntity.body.errorBlock.code == ExampleFeedbackContext.PAYLOAD_TOO_LARGE.code
        assert sharedState.uploadEntity.body.errorBlock.message
        assert sharedState.uploadEntity.body.errorBlock.developerMessage
    }

    @Then( '^the hypermedia control describing the unknown asset is returned$' )
    @SuppressWarnings( 'UnnecessaryGetter' )
    void 'the hypermedia control describing the unknown asset is returned'() {
        assert sharedState.downloadEntity.headers.getContentType().isCompatibleWith( HypermediaControl.MEDIA_TYPE )
        HypermediaControl control = sharedState.transformer.readValue( sharedState.downloadEntity.body, HypermediaControl )
        assert control.errorBlock.code
        assert control.errorBlock.message
        assert control.errorBlock.developerMessage
    }
}
