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

import com.github.danzuep.yeshua.android.core.repository.remote.parseTranslationCsv
import com.github.danzuep.yeshua.android.core.repository.remote.readUsfmBooks
import com.github.danzuep.yeshua.android.core.repository.remote.toRemoteTranslation
import com.github.danzuep.yeshua.android.core.repository.remote.toRemoteTranslationInfo
import com.github.danzuep.yeshua.android.core.repository.remote.toShortName
import com.github.danzuep.yeshua.android.core.repository.remote.usfmZipUrl
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

private const val ORIGIN = "https://ebible.org"

/**
 * Usage: `<translations.csv> <output directory> <comma separated eBible ids>`. An id can be followed by `=<zip url>` to read that zip
 * instead of the one on eBible.org, e.g. a zip with ruby text. Downloads the USFM zip of each
 * translation, parses it with the same code as the app and writes `<shortName>.json` and `index.json`.
 */
fun main(args: Array<String>) {
    require(args.size == 3) { "Usage: <translations.csv> <output directory> <comma separated eBible ids>" }
    val catalog = parseTranslationCsv(File(args[0]).readText(), "$ORIGIN/").associateBy { it.id }
    val output = File(args[1]).apply { mkdirs() }

    val infos = args[2].split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { entry ->
        val id = entry.substringBefore('=')
        val row = requireNotNull(catalog[id]) { "$id is not a redistributable translation in ${args[0]}" }
        val info = row.toRemoteTranslationInfo()
        val books = URI(entry.substringAfter('=', usfmZipUrl(ORIGIN, row.toShortName()))).toURL().openStream().use { stream ->
            ZipInputStream(stream).use { zip ->
                generateSequence { zip.nextEntry }
                    .filter { !it.isDirectory }
                    .map { it.name to zip.readBytes() }
                    .let { readUsfmBooks(it.toList().asSequence()) }
            }
        }
        val json = TranslationFiles.encode(toRemoteTranslation(info, books))
        File(output, TranslationFiles.fileName(info.shortName)).writeText(json)
        println("${info.shortName}: ${books.size} books, ${json.length} characters")
        info.copy(size = json.length.toLong())
    }
    File(output, TranslationFiles.INDEX).writeText(TranslationFiles.encodeIndex(infos))
}
