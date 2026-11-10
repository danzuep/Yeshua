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

import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.asExecutor
import com.github.danzuep.yeshua.android.Navigator
import com.github.danzuep.yeshua.android.R
import com.github.danzuep.yeshua.android.annotated.AnnotatedVerseAdapter
import com.github.danzuep.yeshua.android.core.ReadingTimeline
import com.github.danzuep.yeshua.android.core.provider.CoroutineDispatcherProvider
import com.github.danzuep.yeshua.android.databinding.ActivityTimelineBinding
import com.github.danzuep.yeshua.android.infra.BaseActivity
import com.github.danzuep.yeshua.android.ui.dialog
import com.github.danzuep.yeshua.android.ui.fadeIn
import com.github.danzuep.yeshua.android.utils.localized
import javax.inject.Inject

@AndroidEntryPoint
class TimelineActivity : BaseActivity<ActivityTimelineBinding, TimelineViewModel.ViewAction, TimelineViewModel.ViewState, TimelineViewModel>() {
    @Inject
    lateinit var coroutineDispatcherProvider: CoroutineDispatcherProvider

    private lateinit var adapter: AnnotatedVerseAdapter
    private var limit = ReadingTimeline.DEFAULT_LIMIT

    override val viewModel: TimelineViewModel by viewModels()

    override val viewBinding: ActivityTimelineBinding by lazy(LazyThreadSafetyMode.NONE) { ActivityTimelineBinding.inflate(layoutInflater) }

    override fun initializeView() = with(viewBinding) {
        toolbar.setNavigationOnClickListener { navigator.goBack(this@TimelineActivity) }
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_timeline_limit -> {
                    showLimitDialog()
                    true
                }
                else -> false
            }
        }

        adapter = AnnotatedVerseAdapter(
            inflater = layoutInflater,
            executor = coroutineDispatcherProvider.default.asExecutor()
        ) { viewEvent ->
            when (viewEvent) {
                is AnnotatedVerseAdapter.ViewEvent.OpenVerse -> viewModel.openVerse(viewEvent.verseToOpen)
                is AnnotatedVerseAdapter.ViewEvent.ShowPreview -> viewModel.openVerse(viewEvent.verseToPreview)
            }
        }
        verseList.adapter = adapter
    }

    override fun onViewActionEmitted(viewAction: TimelineViewModel.ViewAction) = when (viewAction) {
        TimelineViewModel.ViewAction.OpenReadingScreen -> navigator.navigate(this, Navigator.SCREEN_READING)
    }

    override fun onViewStateUpdated(viewState: TimelineViewModel.ViewState): Unit = with(viewBinding) {
        limit = viewState.limit
        toolbar.menu.findItem(R.id.action_timeline_limit).title = viewState.limit.localized()

        if (viewState.loading) {
            loadingSpinner.fadeIn()
            verseList.isVisible = false
        } else {
            loadingSpinner.isVisible = false
            verseList.fadeIn()
        }

        adapter.submitList(viewState.items)

        viewState.error?.handle()
    }

    private fun showLimitDialog() {
        if (isDestroyed) return

        val options = (ReadingTimeline.LIMIT_OPTIONS + limit).distinct().sorted()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_timeline_limit)
            .setSingleChoiceItems(options.map { it.localized() }.toTypedArray(), options.indexOf(limit)) { dialog, which ->
                dialog.dismiss()
                if (options[which] != limit) viewModel.saveLimit(options[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun TimelineViewModel.ViewState.Error.handle() = when (this) {
        is TimelineViewModel.ViewState.Error.TimelineLoadingError -> {
            dialog(
                cancelable = false,
                title = R.string.dialog_title_error,
                message = R.string.dialog_message_failed_to_load_annotated_verses,
                onPositive = { _, _ -> viewModel.loadTimeline() },
                onNegative = { _, _ -> navigator.goBack(this@TimelineActivity) },
                onDismiss = { viewModel.markErrorAsShown(this) }
            )
        }
        is TimelineViewModel.ViewState.Error.LimitSavingError -> {
            dialog(
                cancelable = true,
                title = R.string.dialog_title_error,
                message = R.string.dialog_message_failed_to_save_sort_order,
                onPositive = { _, _ -> viewModel.saveLimit(limit) },
                onDismiss = { viewModel.markErrorAsShown(this) }
            )
        }
        is TimelineViewModel.ViewState.Error.VerseOpeningError -> {
            dialog(
                cancelable = true,
                title = R.string.dialog_title_error,
                message = R.string.dialog_message_failed_to_select_verse,
                onPositive = { _, _ -> viewModel.openVerse(verseToOpen) },
                onDismiss = { viewModel.markErrorAsShown(this) }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadTimeline()
    }
}
