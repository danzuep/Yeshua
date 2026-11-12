/*
 * Copyright (C) 2026 Daniel Collingwood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.danzuep.yeshua.android.web

import com.github.danzuep.yeshua.android.core.repository.remote.RemoteTranslation
import com.github.danzuep.yeshua.android.core.repository.remote.RemoteTranslationInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class TranslationFilesTest {
    private val info = RemoteTranslationInfo("eng_web", "World English Bible", "eng", 12L)

    @Test
    fun `test encode() and decode() keep the structure of the verses`() {
        val translation = RemoteTranslation(
            info,
            listOf("Genesis", "Exodus"),
            listOf("GEN", "EXO"),
            mapOf(
                Pair(1, 0) to listOf("\u2028Heading\u2028\u2029In \"quotes\" and\nlines", ""),
                Pair(0, 10) to listOf("In the beginning\u2027f1:1 note\u2027"),
                Pair(0, 2) to listOf("\u2029a"),
            )
        )

        assertEquals(translation, TranslationFiles.decode(TranslationFiles.encode(translation), info.size))
    }

    @Test
    fun `test encodeIndex() and decodeIndex()`() {
        val translations = listOf(info, RemoteTranslationInfo("eng_kjv2006", "King James Version", "eng", 34L))

        assertEquals(translations, TranslationFiles.decodeIndex(TranslationFiles.encodeIndex(translations)))
    }
}
