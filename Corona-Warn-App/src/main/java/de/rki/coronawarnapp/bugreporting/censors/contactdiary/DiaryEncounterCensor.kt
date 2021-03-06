package de.rki.coronawarnapp.bugreporting.censors.contactdiary

import dagger.Reusable
import de.rki.coronawarnapp.bugreporting.censors.BugCensor
import de.rki.coronawarnapp.bugreporting.censors.BugCensor.Companion.toNewLogLineIfDifferent
import de.rki.coronawarnapp.bugreporting.censors.BugCensor.Companion.withValidComment
import de.rki.coronawarnapp.bugreporting.debuglog.LogLine
import de.rki.coronawarnapp.bugreporting.debuglog.internal.DebuggerScope
import de.rki.coronawarnapp.contactdiary.storage.repo.ContactDiaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Reusable
class DiaryEncounterCensor @Inject constructor(
    @DebuggerScope debugScope: CoroutineScope,
    diary: ContactDiaryRepository
) : BugCensor {

    private val encounters by lazy {
        diary.personEncounters.stateIn(
            scope = debugScope,
            started = SharingStarted.Lazily,
            initialValue = null
        ).filterNotNull()
    }

    override suspend fun checkLog(entry: LogLine): LogLine? {
        val encountersNow = encounters.first().filter { !it.circumstances.isNullOrBlank() }

        if (encountersNow.isEmpty()) return null

        val newMessage = encountersNow.fold(entry.message) { orig, encounter ->
            var wip = orig

            withValidComment(encounter.circumstances) {
                wip = wip.replace(it, "Encounter#${encounter.id}/Circumstances")
            }

            wip
        }

        return entry.toNewLogLineIfDifferent(newMessage)
    }
}
