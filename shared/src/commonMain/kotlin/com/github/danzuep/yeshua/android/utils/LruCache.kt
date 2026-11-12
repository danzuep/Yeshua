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

/** A least recently used cache holding at most [maxSize] units, as measured by [sizeOf]. Safe to share between threads. */
open class LruCache<K : Any, V : Any>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()
    private var size = 0

    protected open fun sizeOf(key: K, value: V): Int = 1

    fun get(key: K): V? = synchronizedBlock(map) {
        val value = map.remove(key) ?: return@synchronizedBlock null
        map[key] = value
        value
    }

    fun put(key: K, value: V) {
        synchronizedBlock(map) {
            map.remove(key)?.let { size -= sizeOf(key, it) }
            map[key] = value
            size += sizeOf(key, value)
            while (size > maxSize && map.size > 1) {
                val eldest = map.entries.first()
                map.remove(eldest.key)
                size -= sizeOf(eldest.key, eldest.value)
            }
        }
    }
}
