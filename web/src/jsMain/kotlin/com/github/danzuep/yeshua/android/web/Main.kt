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

import com.github.danzuep.yeshua.android.core.PARAGRAPH
import com.github.danzuep.yeshua.android.core.extractHeadings
import com.github.danzuep.yeshua.android.core.hasRuby
import com.github.danzuep.yeshua.android.core.repository.remote.RemoteTranslation
import com.github.danzuep.yeshua.android.core.repository.remote.RemoteTranslationInfo
import com.github.danzuep.yeshua.android.core.toDisplayedText
import com.github.danzuep.yeshua.android.core.toRubyParts
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement

private const val KEY_TRANSLATION = "translation"
private const val KEY_BOOK = "book"
private const val KEY_CHAPTER = "chapter"
private const val KEY_RUBY = "ruby"
private const val KEY_THEME = "theme"
private const val KEY_FONT = "font"

/** Pixels from an edge at which the next chapter is loaded. */
private const val LOAD_DISTANCE = 1200

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
    private val rubyToggle = element<HTMLInputElement>("ruby")
    private val rubyLabel = element<HTMLElement>("ruby-label")
    private val themeButton = element<HTMLButtonElement>("theme")
    private val text = element<HTMLDivElement>("text")
    private val status = element<HTMLElement>("status")
    private val loaded = HashMap<String, RemoteTranslation>()

    private lateinit var translation: RemoteTranslation
    private var book = 0
    private var chapter = 0
    private var fontSize = 100
    private var scrollQueued = false

    /** Chapters (0-based) that have verses, by book. */
    private val chapters get() = translation.verses.keys.groupBy({ it.first }, { it.second }).mapValues { it.value.sorted() }

    suspend fun start() {
        val saved = localStorage.getItem(KEY_TRANSLATION)
        val info = translations.firstOrNull { it.shortName == saved } ?: translations.first()
        translationSelect.append(*translations.map { option(it.shortName, it.name) }.toTypedArray())
        translationSelect.value = info.shortName
        book = localStorage.getItem(KEY_BOOK)?.toIntOrNull() ?: JOHN
        chapter = localStorage.getItem(KEY_CHAPTER)?.toIntOrNull() ?: 0
        rubyToggle.checked = localStorage.getItem(KEY_RUBY) != "false"
        applyTheme(localStorage.getItem(KEY_THEME) ?: "auto")
        applyFontSize(localStorage.getItem(KEY_FONT)?.toIntOrNull() ?: 100)
        showTranslation(info.shortName)

        translationSelect.onchange = { launch { showTranslation(translationSelect.value) } }
        bookSelect.onchange = { select(bookSelect.value.toInt(), 0) }
        chapterSelect.onchange = { select(book, chapterSelect.value.toInt()) }
        rubyToggle.onchange = {
            localStorage.setItem(KEY_RUBY, rubyToggle.checked.toString())
            select(book, chapter)
        }
        themeButton.onclick = {
            applyTheme(when (localStorage.getItem(KEY_THEME) ?: "auto") { "auto" -> "dark"; "dark" -> "light"; else -> "auto" })
        }
        element<HTMLButtonElement>("font-smaller").onclick = { applyFontSize(fontSize - 10) }
        element<HTMLButtonElement>("font-larger").onclick = { applyFontSize(fontSize + 10) }
        window.addEventListener("scroll", {
            if (!scrollQueued) {
                scrollQueued = true
                window.requestAnimationFrame { scrollQueued = false; fill() }
            }
        })
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
        rubyLabel.hidden = translation.verses.values.none { chapter -> chapter.any { it.hasRuby() } }

        bookSelect.innerHTML = ""
        bookSelect.append(*chapters.keys.sorted().map { option(it.toString(), translation.bookNames[it]) }.toTypedArray())
        if (book !in chapters) book = chapters.keys.min().also { chapter = 0 }
        select(book, chapter)
    }

    /** Every chapter with verses, in reading order. */
    private val order get() = translation.verses.keys.sortedWith(compareBy({ it.first }, { it.second }))

    /** Jumps to a chapter, replacing everything shown. */
    private fun select(newBook: Int, newChapter: Int) {
        val bookChapters = chapters.getValue(newBook)
        val target = Pair(newBook, newChapter.takeIf { it in bookChapters } ?: bookChapters.first())
        text.innerHTML = ""
        chapterSelect.innerHTML = ""
        text.appendChild(renderChapter(target.first, target.second))
        setCurrent(target.first, target.second)
        window.scrollTo(0.0, 0.0)
        fill()
    }

    private fun setCurrent(newBook: Int, newChapter: Int) {
        val bookChanged = newBook != book || chapterSelect.options.length == 0
        book = newBook
        chapter = newChapter
        localStorage.setItem(KEY_BOOK, book.toString())
        localStorage.setItem(KEY_CHAPTER, chapter.toString())
        document.title = "${translation.bookNames[book]} ${chapter + 1} · Yeshua"
        bookSelect.value = book.toString()
        if (bookChanged) {
            chapterSelect.innerHTML = ""
            chapterSelect.append(*chapters.getValue(book).map { option(it.toString(), (it + 1).toString()) }.toTypedArray())
        }
        chapterSelect.value = chapter.toString()
    }

    private fun sections() = (0 until text.children.length).map { text.children.item(it).unsafeCast<HTMLElement>() }

    private fun key(section: HTMLElement) =
        Pair(section.getAttribute("data-book")!!.toInt(), section.getAttribute("data-chapter")!!.toInt())

    /** Loads chapters above and below as the reader nears the edges (infinite scroll). */
    private fun fill() {
        val chapters = order
        val root = document.documentElement!!
        while (true) {
            val last = key(sections().last())
            val index = chapters.indexOf(last)
            if (index == chapters.lastIndex || window.scrollY + window.innerHeight < root.scrollHeight - LOAD_DISTANCE) break
            val (b, c) = chapters[index + 1]
            text.appendChild(renderChapter(b, c))
        }
        while (window.scrollY < LOAD_DISTANCE) {
            val index = chapters.indexOf(key(sections().first()))
            if (index <= 0) break
            val (b, c) = chapters[index - 1]
            val before = root.scrollHeight
            text.insertBefore(renderChapter(b, c), text.firstChild)
            window.scrollBy(0.0, (root.scrollHeight - before).toDouble())
        }
        updateCurrent()
    }

    /** The chapter under the sticky header is the current one. */
    private fun updateCurrent() {
        val line = element<HTMLElement>("toolbar").getBoundingClientRect().bottom + 8
        val visible = sections().lastOrNull { it.getBoundingClientRect().top <= line } ?: sections().firstOrNull() ?: return
        val (b, c) = key(visible)
        if (b != book || c != chapter) setCurrent(b, c)
    }

    private fun renderChapter(chapterBook: Int, chapterIndex: Int): HTMLElement {
        val section = document.createElement("section").unsafeCast<HTMLElement>()
        section.setAttribute("data-book", chapterBook.toString())
        section.setAttribute("data-chapter", chapterIndex.toString())
        section.appendChild(document.createElement("h2").apply {
            textContent = "${translation.bookNames[chapterBook]} ${chapterIndex + 1}"
        })
        var paragraph = newParagraph(section)
        translation.verses.getValue(Pair(chapterBook, chapterIndex)).forEachIndexed { index, verse ->
            if (verse.isEmpty()) return@forEachIndexed
            verse.extractHeadings().forEach { heading ->
                section.appendChild(document.createElement("h3").apply { textContent = heading })
                paragraph = newParagraph(section)
            }
            if (verse.trimStart().startsWith(PARAGRAPH) && paragraph.hasChildNodes()) paragraph = newParagraph(section)
            val lines = verse.toDisplayedText(rubyToggle.checked).split('\n')
            lines.forEachIndexed { line, content ->
                if (line > 0) paragraph = newParagraph(section)
                if (line == 0) paragraph.appendChild(document.createElement("sup").apply {
                    className = "verse-number"
                    textContent = (index + 1).toString()
                })
                paragraph.appendChild(document.createElement("span").apply {
                    className = "verse"
                    content.toRubyParts().forEach { part ->
                        if (part.reading.isNullOrEmpty()) {
                            appendChild(document.createTextNode(part.text))
                        } else {
                            appendChild(document.createElement("ruby").apply {
                                appendChild(document.createTextNode(part.text))
                                appendChild(document.createElement("rt").apply { textContent = part.reading })
                            })
                        }
                    }
                    appendChild(document.createTextNode(" "))
                })
            }
        }
        return section
    }

    private fun applyTheme(theme: String) {
        if (theme == "auto") document.documentElement!!.removeAttribute("data-theme")
        else document.documentElement!!.setAttribute("data-theme", theme)
        themeButton.textContent = when (theme) { "dark" -> "🌙"; "light" -> "☀️"; else -> "🌓" }
        themeButton.title = "Theme: $theme"
        localStorage.setItem(KEY_THEME, theme)
    }

    private fun applyFontSize(percent: Int) {
        fontSize = percent.coerceIn(70, 200)
        document.documentElement.unsafeCast<HTMLElement>().style.setProperty("font-size", "$fontSize%")
        localStorage.setItem(KEY_FONT, fontSize.toString())
    }

    private fun newParagraph(parent: HTMLElement): HTMLElement =
        document.createElement("p").unsafeCast<HTMLElement>().also { parent.appendChild(it) }

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
