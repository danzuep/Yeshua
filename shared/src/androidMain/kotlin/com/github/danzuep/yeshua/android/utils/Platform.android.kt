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

package com.github.danzuep.yeshua.android.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

import java.util.Locale

actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

actual fun <T> synchronizedBlock(lock: Any, block: () -> T): T = synchronized(lock, block)

actual fun maxCacheBytes(): Long = Runtime.getRuntime().maxMemory()

actual fun isKnownLanguageCode(code: String): Boolean =
        Locale.forLanguageTag(code).displayLanguage.let { it.isNotEmpty() && !it.equals(code, ignoreCase = true) }

actual typealias IOException = java.io.IOException
