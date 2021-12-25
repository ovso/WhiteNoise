/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ovso.whitenoise.data.lullaby.impl

import android.content.Context
import io.github.ovso.whitenoise.data.LullabyModel
import io.github.ovso.whitenoise.data.Result
import io.github.ovso.whitenoise.data.lullaby.LullabyRepository
import io.github.ovso.whitenoise.data.lullaby.LullabySectionModel
import io.github.ovso.whitenoise.data.lullaby.SelectionModel
import io.github.ovso.whitenoise.data.response.Response
import io.github.ovso.whitenoise.utils.addOrRemove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedReader

/**
 * Implementation of InterestRepository that returns a hardcoded list of
 * topics, people and publications synchronously.
 */
class FakeLullabyRepository(private val context: Context) : LullabyRepository {

    // for now, keep the selections in memory
    private val selected = MutableStateFlow(setOf<SelectionModel>())

    // Used to make suspend functions that read and update state safe to call from any thread
    private val mutex = Mutex()

    @Suppress("BlockingMethodInNonBlockingContext")
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun getLullabies(): Result<List<LullabySectionModel>> =
        withContext(Dispatchers.Default) {
            // 로컬라이제이션 할 부분이다.
            val inputStream = context.assets.open("lullabies/lullabies.json")
            val use = inputStream.bufferedReader().use(BufferedReader::readText)
            val lullabiesResponse = Json.decodeFromString<Response>(use)
            val result = lullabiesResponse.lullabies.map {
                LullabySectionModel(
                    section = it.section,
                    items = it.items.map { item ->
                        LullabyModel(
                            name = item.name,
                            id = item.id,
                        )
                    }
                )
            }
            return@withContext Result.Success(result)
        }

    override suspend fun toggleSelection(selection: SelectionModel) =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                val set = selected.value.toMutableSet().apply {
                    removeAll {
                        selection != it
                    }
                }
                set.addOrRemove(selection)
                selected.value = set
            }
        }

    override fun observeSelected(): Flow<Set<SelectionModel>> = selected
}
