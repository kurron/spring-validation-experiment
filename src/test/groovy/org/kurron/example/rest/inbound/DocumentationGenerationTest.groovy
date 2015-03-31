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

import static org.springframework.restdocs.RestDocumentation.document
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.kurron.example.rest.BaseOutboundIntegrationTest

/**
 * This 'test' generates code snippets used in the REST documentation.  We need to leverage the
 * integration testing facilities so we can execute calls against a running server and capture
 * actual traffic.
 */
@SuppressWarnings( 'UnnecessaryGetter' )
class DocumentationGenerationTest extends BaseOutboundIntegrationTest {

    @Autowired
    private WebApplicationContext context

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup( this.context ).apply( new RestDocumentationConfiguration() ).build()
    }

    def 'demonstrate api discovery'() {

        given: 'a valid request'
        def requestBuilder = get( '/' ).accept( HypermediaControl.MIME_TYPE )
                                       .header( CustomHttpHeaders.X_CORRELATION_ID, '155887a0-8959-4031-a30a-a8e52bc6b7d8' )

        when: 'the GET request is made'
        mockMvc.perform( requestBuilder ).andExpect( status().isOk() ).andDo( document( 'api-discovery' ) )

        then: 'examples are generated'
    }

    def 'demonstrate failure scenario'() {

        given: 'a valid request'
        def requestBuilder = get( '/{id}', randomUUID() ).accept( 'image/png;width=1024;height=768', HypermediaControl.MIME_TYPE )
                                                         .header( CustomHttpHeaders.X_CORRELATION_ID, '155887a0-8959-4031-a30a-a8e52bc6b7d8' )

        when: 'the GET request is made'
        mockMvc.perform( requestBuilder ).andExpect( status().isNotFound() ).andDo( document( 'failure-scenario' ) )

        then: 'examples are generated'
    }

    def 'demonstrate asset storage'() {

        given: 'a valid request'
        def buffer = 'some image bytes'.bytes
        def requestBuilder = post( '/' ).content( buffer )
                .contentType( 'image/png;width=1024;height=768' )
                .accept( HypermediaControl.MIME_TYPE )
                .header( 'Content-Length', buffer.size() )
                .header( CustomHttpHeaders.X_EXPIRATION_MINUTES, 10 )
                .header( CustomHttpHeaders.X_CORRELATION_ID, '155887a0-8959-4031-a30a-a8e52bc6b7d8' )

        when: 'the POST request is made'
        mockMvc.perform( requestBuilder ).andExpect( status().isCreated() ).andDo( document( 'asset-storage' ) )

        then: 'examples are generated'
    }

    def 'demonstrate asset retrieval'() {

        given: 'a previously uploaded asset'
        def buffer = 'some image bytes'.bytes
        def uploadBuilder = post( '/' ).content( buffer )
                .contentType( 'image/png;width=1024;height=768' )
                .accept( HypermediaControl.MIME_TYPE )
                .header( 'Content-Length', buffer.size() )
                .header( CustomHttpHeaders.X_EXPIRATION_MINUTES, 10 )
                .header( CustomHttpHeaders.X_CORRELATION_ID, '155887a0-8959-4031-a30a-a8e52bc6b7d8' )
        def upload = mockMvc.perform( uploadBuilder ).andExpect( status().isCreated() ).andReturn()

        when: 'the GET request is made'
        def downloadBuilder = get( upload.response.getHeaderValue( 'Location' ) as String )
                .accept( 'image/png', 'application/json' )
                .header( CustomHttpHeaders.X_CORRELATION_ID, '00588700-8959-4031-a30a-a8e52bc6b7d8' )
        mockMvc.perform( downloadBuilder ).andExpect( status().isOk(  ) ).andDo( document( 'asset-download' ) )

        then: 'examples are generated'
    }
}
