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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical
import org.springframework.hateoas.ResourceSupport
import org.springframework.http.MediaType

import javax.validation.constraints.AssertFalse
import javax.validation.constraints.Digits
import javax.validation.constraints.Max
import javax.validation.constraints.Min

/**
 * This is just to test the different validation annotations.
 */
@Canonical
@JsonInclude( JsonInclude.Include.NON_NULL )
class ValidationExampleControl extends ResourceSupport {

    /**
     * The expected MIME type for the control.
     */
    public static final String MIME_TYPE = 'application/json;type=validation;version=1.0.0'

    /**
     * Convenience form of the MIME-TYPE for Spring MVC APIs.
     **/
    public static final MediaType MEDIA_TYPE = MediaType.parseMediaType( MIME_TYPE )

    @JsonProperty( 'some-boolean' )
    @AssertFalse
    boolean someBoolean = Boolean.TRUE

    @JsonProperty( 'some-integer' )
    @Max( 100L )
    @Min( 1L )
    int someInteger = 0
}
