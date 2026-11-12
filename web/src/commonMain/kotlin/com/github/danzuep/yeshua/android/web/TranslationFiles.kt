/*
 * Copyright (C) 2023 Xizhi Zhu
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * The static files of the web site: `translations/index.json` lists the translations, and `translations/<shortName>.json`
 * holds the book names and the verses of one translation, keyed by "<book index>:<chapter index>" (both 0-based).
 */
object TranslationFiles {
    const val DIRECTORY = "translations"
    const val INDEX = "index.json"

    fun fileName(shortName: String): String = "$shortName.json"

    fun encodeIndex(translations: List<RemoteTranslationInfo>): String = JsonArray(translations.map { info ->
        buildJsonObject {
            put("shortName", info.shortName)
            put("name", info.name)
            put("language", info.language)
            put("size", info.size)
        }
    }).toString()

    fun decodeIndex(json: String): List<RemoteTranslationInfo> = Json.parseToJsonElement(json).jsonArray.map { element ->
        val info = element.jsonObject
        RemoteTranslationInfo(
            info.getValue("shortName").jsonPrimitive.content,
            info.getValue("name").jsonPrimitive.content,
            info.getValue("language").jsonPrimitive.content,
            info.getValue("size").jsonPrimitive.long,
        )
    }

    fun encode(translation: RemoteTranslation): String = buildJsonObject {
        put("shortName", translation.translationInfo.shortName)
        put("name", translation.translationInfo.name)
        put("language", translation.translationInfo.language)
        put("bookNames", JsonArray(translation.bookNames.map(::JsonPrimitive)))
        put("bookShortNames", JsonArray(translation.bookShortNames.map(::JsonPrimitive)))
        put("chapters", buildJsonObject {
            translation.verses.entries.sortedWith(compareBy({ it.key.first }, { it.key.second })).forEach { (key, verses) ->
                put("${key.first}:${key.second}", JsonArray(verses.map(::JsonPrimitive)))
            }
        })
    }.toString()

    /** [size] is what the translation list shows as the size of the translation. */
    fun decode(json: String, size: Long): RemoteTranslation {
        val translation = Json.parseToJsonElement(json).jsonObject
        val verses = translation.getValue("chapters").jsonObject.entries.associate { (key, verses) ->
            val (book, chapter) = key.split(':')
            Pair(book.toInt(), chapter.toInt()) to verses.jsonArray.map { it.jsonPrimitive.content }
        }
        return RemoteTranslation(
            RemoteTranslationInfo(
                translation.getValue("shortName").jsonPrimitive.content,
                translation.getValue("name").jsonPrimitive.content,
                translation.getValue("language").jsonPrimitive.content,
                size,
            ),
            translation.getValue("bookNames").jsonArray.map { it.jsonPrimitive.content },
            translation.getValue("bookShortNames").jsonArray.map { it.jsonPrimitive.content },
            verses,
        )
    }
}
