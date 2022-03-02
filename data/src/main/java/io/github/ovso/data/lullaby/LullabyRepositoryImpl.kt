package io.github.ovso.data.lullaby

import io.github.ovso.domain.LullabyEntity
import io.github.ovso.domain.repository.LullabyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LullabyRepositoryImpl @Inject constructor(
  private val resProvider: ResProvider,
) : LullabyRepository {

  private val selected = MutableStateFlow(setOf<LullabyEntity>())

  // Used to make suspend functions that read and update state safe to call from any thread
  private val mutex = Mutex()


  override suspend fun getLullabies(): List<LullabyEntity> {
    return withContext(Dispatchers.Default) {
      resProvider.getLullabies("lullabies", "data.json")
    }
  }

  override suspend fun toggleSelection(model: LullabyEntity) =
    withContext(Dispatchers.Default) {
      mutex.withLock {
        val set = selected.value.toMutableSet().apply {
          removeAll {
            model != it
          }
        }
        set.addOrRemove(model)
        selected.value = set
      }
    }

  override fun observeSelected(): Flow<Set<LullabyEntity>> = selected
}
