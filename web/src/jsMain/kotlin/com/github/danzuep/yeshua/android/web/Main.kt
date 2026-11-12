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

import com.github.danzuep.yeshua.android.core.PARAGRAPH
import com.github.danzuep.yeshua.android.core.extractHeadings
import com.github.danzuep.yeshua.android.core.repository.remote.RemoteTranslation
import com.github.danzuep.yeshua.android.core.repository.remote.RemoteTranslationInfo
import com.github.danzuep.yeshua.android.core.toDisplayedText
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement

private const val KEY_TRANSLATION = "translation"
private const val KEY_BOOK = "book"
private const val KEY_CHAPTER = "chapter"

private fun <T> element(id: String): T = document.getElementById(id).unsafeCast<T>()

private suspend fun fetchText(path: String): String {
    val response = window.fetch("${TranslationFiles.DIRECTORY}/$path").await()
    check(response.ok) { "${response.status} ${path}" }
    return response.text().await()
}

private class Reader(private val translations: List<RemoteTranslationInfo>) {
    private val translationSelect = element<HTMLSelectElement>("translation")
    private val bookSelect = element<HTMLSelectElement>("book")
    private val chapterSelect = element<HTMLSelectElement>("chapter")
    private val previous = element<HTMLButtonElement>("previous")
    private val next = element<HTMLButtonElement>("next")
    private val text = element<HTMLDivElement>("text")
    private val status = element<HTMLElement>("status")
    private val loaded = HashMap<String, RemoteTranslation>()

    private lateinit var translation: RemoteTranslation
    private var book = 0
    private var chapter = 0

    /** Chapters (0-based) that have verses, by book. */
    private val chapters get() = translation.verses.keys.groupBy({ it.first }, { it.second }).mapValues { it.value.sorted() }

    suspend fun start() {
        val saved = localStorage.getItem(KEY_TRANSLATION)
        val info = translations.firstOrNull { it.shortName == saved } ?: translations.first()
        translationSelect.append(*translations.map { option(it.shortName, it.name) }.toTypedArray())
        translationSelect.value = info.shortName
        book = localStorage.getItem(KEY_BOOK)?.toIntOrNull() ?: JOHN
        chapter = localStorage.getItem(KEY_CHAPTER)?.toIntOrNull() ?: 0
        showTranslation(info.shortName)

        translationSelect.onchange = { launch { showTranslation(translationSelect.value) } }
        bookSelect.onchange = { select(bookSelect.value.toInt(), 0) }
        chapterSelect.onchange = { select(book, chapterSelect.value.toInt()) }
        previous.onclick = { step(-1) }
        next.onclick = { step(1) }
    }

    private fun launch(block: suspend () -> Unit) = MainScope().launch {
        try {
            block()
        } catch (e: Throwable) {
            status.textContent = "Error: ${e.message}"
        }
    }

    private suspend fun showTranslation(shortName: String) {
        status.textContent = "Loading…"
        val info = translations.first { it.shortName == shortName }
        translation = loaded.getOrPut(shortName) {
            TranslationFiles.decode(fetchText(TranslationFiles.fileName(shortName)), info.size)
        }
        status.textContent = ""
        localStorage.setItem(KEY_TRANSLATION, shortName)

        bookSelect.innerHTML = ""
        bookSelect.append(*chapters.keys.sorted().map { option(it.toString(), translation.bookNames[it]) }.toTypedArray())
        if (book !in chapters) book = chapters.keys.min().also { chapter = 0 }
        select(book, chapter)
    }

    private fun select(newBook: Int, newChapter: Int) {
        book = newBook
        val bookChapters = chapters.getValue(book)
        chapter = newChapter.takeIf { it in bookChapters } ?: bookChapters.first()
        localStorage.setItem(KEY_BOOK, book.toString())
        localStorage.setItem(KEY_CHAPTER, chapter.toString())

        bookSelect.value = book.toString()
        chapterSelect.innerHTML = ""
        chapterSelect.append(*bookChapters.map { option(it.toString(), (it + 1).toString()) }.toTypedArray())
        chapterSelect.value = chapter.toString()
        render()
    }

    private fun step(direction: Int) {
        val books = chapters.keys.sorted()
        val bookChapters = chapters.getValue(book)
        val index = bookChapters.indexOf(chapter) + direction
        when {
            index in bookChapters.indices -> select(book, bookChapters[index])
            direction < 0 && book != books.first() -> books[books.indexOf(book) - 1].let { select(it, chapters.getValue(it).last()) }
            direction > 0 && book != books.last() -> select(books[books.indexOf(book) + 1], 0)
        }
        window.scrollTo(0.0, 0.0)
    }

    private fun render() {
        text.innerHTML = ""
        document.title = "${translation.bookNames[book]} ${chapter + 1} · Yeshua"
        var paragraph = newParagraph()
        translation.verses.getValue(Pair(book, chapter)).forEachIndexed { index, verse ->
            if (verse.isEmpty()) return@forEachIndexed
            verse.extractHeadings().forEach { heading ->
                text.appendChild(document.createElement("h3").apply { textContent = heading })
                paragraph = newParagraph()
            }
            if (verse.trimStart().startsWith(PARAGRAPH) && paragraph.hasChildNodes()) paragraph = newParagraph()
            val lines = verse.toDisplayedText().split('\n')
            lines.forEachIndexed { line, content ->
                if (line > 0) paragraph = newParagraph()
                if (line == 0) paragraph.appendChild(document.createElement("sup").apply {
                    className = "verse-number"
                    textContent = (index + 1).toString()
                })
                paragraph.appendChild(document.createElement("span").apply {
                    className = "verse"
                    textContent = "$content "
                })
            }
        }
    }

    private fun newParagraph(): HTMLElement =
        document.createElement("p").unsafeCast<HTMLElement>().also { text.appendChild(it) }

    private fun option(value: String, label: String) = document.createElement("option").apply {
        setAttribute("value", value)
        textContent = label
    }
}

/** John, the book a first-time visitor starts with. */
private const val JOHN = 42

fun main() {
    MainScope().launch {
        val status = element<HTMLElement>("status")
        try {
            Reader(TranslationFiles.decodeIndex(fetchText(TranslationFiles.INDEX))).start()
        } catch (e: Throwable) {
            status.textContent = "Error: ${e.message}"
        }
    }
}
