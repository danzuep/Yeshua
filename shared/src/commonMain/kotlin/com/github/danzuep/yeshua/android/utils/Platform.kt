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

package com.github.danzuep.yeshua.android.utils

import kotlinx.coroutines.CoroutineDispatcher

/** Runs [block] while holding [lock] on platforms with threads; browsers are single threaded so they just run it. */
expect fun <T> synchronizedBlock(lock: Any, block: () -> T): T

/** The dispatcher for blocking work; browsers have no thread pool for it, so they use the default dispatcher. */
expect fun ioDispatcher(): CoroutineDispatcher

/** The most memory the reading caches may be sized from. */
expect fun maxCacheBytes(): Long

/** Whether the platform can name the language with the ISO 639-3 [code] (three lowercase letters). */
expect fun isKnownLanguageCode(code: String): Boolean

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect open class IOException(message: String?) : Exception
