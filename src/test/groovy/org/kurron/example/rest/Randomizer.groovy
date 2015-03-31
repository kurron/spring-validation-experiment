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

import java.security.SecureRandom

/**
 * Generates pseudo-random data for testing purposes.
 */
class Randomizer {

    def random = new SecureRandom()

    /**
     * Legal hex characters.
     */
    private final char[] hexCharacters = [ 'A', 'B', 'C', 'D', 'E', 'F', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' ]

    /**
     * Generates a random integer from 0 up to, but not inclusive, of the specified max value.
     * @param maxValue top of the number range.
     * @return random number.
     */
    int randomNumberExclusive( int maxValue ) {
        random.nextInt( maxValue )
    }

    /**
     * Generates a random hex string of the specified length. A default length of 8 characters is used.
     * @param length how long to make the string.
     * @return the string.
     */
    String randomHexString( int length = 8 ) {
        (1..length).collect { hexCharacters[randomNumberExclusive( hexCharacters.length )] }.join( '' )
    }

    byte[] randomByteArray( int size ) {
        def buffer = new byte[size]
        random.nextBytes( buffer )
        buffer
    }

    UUID randomUUID() {
        UUID.randomUUID()
    }

    /**
     * Selects a pseudo-random enum value from the provided enum class.
     * @param clazz the enum class to select a value from.
     * @return the randomly selected enum value.
     */
    def <T extends Enum<T>> T randomEnum(Class<T> clazz) {
        clazz.enumConstants[randomNumberExclusive( clazz.enumConstants.length )]
    }

    /**
     * Generates a random number between the two ranges. The ceiling must be greater than the floor.
     * @param floor the smallest value to randomize (inclusive).
     * @param ceiling the largest value to randomize (inclusive).
     * @return random integer within the range.
     */
    int randomInteger( int floor = 1, int ceiling = Integer.MAX_VALUE ) {
        assert floor < ceiling
        random.nextInt( ceiling - floor + 1 ) + floor
    }

    boolean randomBoolean() {
        random.nextBoolean()
    }
}
