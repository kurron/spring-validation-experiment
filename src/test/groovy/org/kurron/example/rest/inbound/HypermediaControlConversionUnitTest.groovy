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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.kurron.example.rest.BaseUnitTest
import org.springframework.hateoas.Link
import org.springframework.http.HttpStatus
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * Unit-level tests validating the various conversions of the Hypermedia Control.
 */
@SuppressWarnings( 'UnnecessaryGetter' )
class HypermediaControlConversionUnitTest extends BaseUnitTest {

    ObjectMapper sut = new Jackson2ObjectMapperBuilder().featuresToEnable( SerializationFeature.INDENT_OUTPUT ).build()

    def 'exercise empty control'() {

        given: 'an empty control'
        def control = new HypermediaControl()

        when: 'the object is transformed into JSON'
        def json = sut.writeValueAsString( control )

        then: 'JSON is mostly empty'
        json.contains( 'links' ) // links are always added because of ResourceSupport
    }

    def 'exercise error block'() {

        given: 'a control with only error section in it'
        def control = new HypermediaControl().with {
            httpCode = HttpStatus.FORBIDDEN.value()
            errorBlock = new ErrorBlock( code: randomInteger(),
                                         message: randomHexString(),
                                         developerMessage: randomHexString() )
            it
        }

        when: 'the object is transformed into JSON'
        String json = sut.writeValueAsString( control )

        then: 'JSON has error elements'
        json.contains( 'error' )
        json.contains( 'code' )
        json.contains( 'message' )
    }

    def 'exercise meta-data block'() {

        given: 'a control with only meta-data section in it'
        def control = new HypermediaControl().with {
            metaDataBlock = new MetaDataBlock( mimeType: 'type/subtype;parameter=one', contentLength: randomInteger() )
            it
        }

        when: 'the object is transformed into JSON'
        String json = sut.writeValueAsString( control )

        then: 'JSON has header elements'
        json.contains( 'meta-data' )
    }

    def 'exercise HAL block'() {

        given: 'a control with only headers section in it'
        def control = new HypermediaControl().with {
            add( new Link( 'http://api.example.com/', 'create' ) )
            add( new Link( 'http://api.example.com/123', 'read' ) )
            add( new Link( 'http://api.example.com/123', 'self' ) )
            it
        }

        when: 'the object is transformed into JSON'
        String json = sut.writeValueAsString( control )

        then: 'JSON has HAL elements'
        json.contains( 'links' )
        json.contains( 'self' )
        json.contains( 'create' )
        json.contains( 'read' )
    }

    @SuppressWarnings( 'Println' )
    def 'printout entire structure'() {

        given: 'a control with everything in it'
        def control = new HypermediaControl().with {
            httpCode = HttpStatus.OK.value()
            errorBlock = new ErrorBlock( code: randomInteger(),
                                         message: randomHexString(),
                                         developerMessage: randomHexString() )
            metaDataBlock = new MetaDataBlock( mimeType: 'type/subtype;parameter=one', contentLength: randomInteger() )
            add( new Link( 'http://api.example.com/', 'create' ) )
            add( new Link( 'http://api.example.com/123', 'read' ) )
            add( new Link( 'http://api.example.com/123', 'self' ) )
            it
        }

        when: 'the object is transformed into JSON'
        String json = sut.writeValueAsString( control )

        then: 'JSON is printed'
        println json
    }
}
