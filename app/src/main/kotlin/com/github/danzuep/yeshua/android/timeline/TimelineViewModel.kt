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

package com.github.danzuep.yeshua.android.timeline

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.github.danzuep.yeshua.android.R
import com.github.danzuep.yeshua.android.annotated.AnnotatedVerseItem
import com.github.danzuep.yeshua.android.core.BibleReadingManager
import com.github.danzuep.yeshua.android.core.Constants
import com.github.danzuep.yeshua.android.core.ReadingTimeline
import com.github.danzuep.yeshua.android.core.SettingsManager
import com.github.danzuep.yeshua.android.core.VerseIndex
import com.github.danzuep.yeshua.android.core.provider.CoroutineDispatcherProvider
import com.github.danzuep.yeshua.android.infra.BaseViewModel
import com.github.danzuep.yeshua.android.utils.firstNotEmpty
import co.touchlab.kermit.Logger
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val bibleReadingManager: BibleReadingManager,
    private val settingsManager: SettingsManager,
    private val coroutineDispatcherProvider: CoroutineDispatcherProvider,
    private val application: Application
) : BaseViewModel<TimelineViewModel.ViewAction, TimelineViewModel.ViewState>(
    initialViewState = ViewState(
        loading = false,
        limit = ReadingTimeline.DEFAULT_LIMIT,
        items = emptyList(),
        error = null,
    )
) {
    sealed class ViewAction {
        object OpenReadingScreen : ViewAction()
    }

    data class ViewState(
        val loading: Boolean,
        val limit: Int,
        val items: List<AnnotatedVerseItem>,
        val error: Error?,
    ) {
        sealed class Error {
            object TimelineLoadingError : Error()
            data class LimitSavingError(val limit: Int) : Error()
            data class VerseOpeningError(val verseToOpen: VerseIndex) : Error()
        }
    }

    fun loadTimeline() {
        viewModelScope.launch(coroutineDispatcherProvider.default) {
            runCatching {
                updateViewState { it.copy(loading = true, items = emptyList()) }

                val settings = settingsManager.settings().first()
                val limit = bibleReadingManager.timelineLimit()
                val timeline = bibleReadingManager.readTimeline()
                val translation = bibleReadingManager.currentTranslation().firstNotEmpty()
                val verses = bibleReadingManager.readVerses(translation, timeline)
                val bookNames = bibleReadingManager.readBookNames(translation)
                val bookShortNames = bibleReadingManager.readBookShortNames(translation)

                val items = timeline.mapNotNull { verseIndex ->
                    verses[verseIndex]?.let {
                        AnnotatedVerseItem.Bookmark(
                            settings = settings,
                            verseIndex = verseIndex,
                            bookName = bookNames[verseIndex.bookIndex],
                            bookShortName = bookShortNames[verseIndex.bookIndex],
                            verseText = it.text.text,
                            sortOrder = Constants.SORT_BY_DATE
                        )
                    }
                }.ifEmpty {
                    listOf(AnnotatedVerseItem.Header(settings = settings, text = application.getString(R.string.text_no_timeline), hideDivider = true))
                }
                updateViewState { it.copy(loading = false, limit = limit, items = items) }
            }.onFailure { e ->
                Logger.withTag(tag).e(e) { "Failed to load the reading timeline" }
                updateViewState { it.copy(loading = false, error = ViewState.Error.TimelineLoadingError) }
            }
        }
    }

    fun saveLimit(limit: Int) {
        viewModelScope.launch(coroutineDispatcherProvider.default) {
            runCatching {
                bibleReadingManager.saveTimelineLimit(limit)
                loadTimeline()
            }.onFailure { e ->
                Logger.withTag(tag).e(e) { "Failed to save the reading timeline limit" }
                updateViewState { it.copy(error = ViewState.Error.LimitSavingError(limit)) }
            }
        }
    }

    fun openVerse(verseToOpen: VerseIndex) {
        viewModelScope.launch(coroutineDispatcherProvider.default) {
            runCatching {
                bibleReadingManager.saveCurrentVerseIndex(verseToOpen)
                emitViewAction(ViewAction.OpenReadingScreen)
            }.onFailure { e ->
                Logger.withTag(tag).e(e) { "Failed to save current verse" }
                updateViewState { it.copy(error = ViewState.Error.VerseOpeningError(verseToOpen)) }
            }
        }
    }

    fun markErrorAsShown(error: ViewState.Error) {
        updateViewState { current -> if (current.error == error) current.copy(error = null) else null }
    }
}
