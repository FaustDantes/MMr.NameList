package com.michalmarcin.namelist.data.repo

import com.michalmarcin.namelist.data.db.LearnRound
import com.michalmarcin.namelist.data.db.LearnRoundDao
import com.michalmarcin.namelist.data.db.Person
import com.michalmarcin.namelist.data.db.PersonDao
import com.michalmarcin.namelist.data.db.PersonTab
import com.michalmarcin.namelist.data.db.Tab
import com.michalmarcin.namelist.data.db.TabDao
import kotlin.random.Random

/**
 * One flash card (F35). The front shows [tags], [spark], [place] and [notes]; the back shows [name].
 * The order on the front is the owner's priority: conversation spark first, then the place, notes at the bottom.
 */
data class LearnCard(
    val personId: Long,
    val name: String,
    val tags: List<String>,
    val spark: String?,
    val place: String?,
    val notes: String?,
)

/** Learning with flash cards and the saved results (F35, ADR-042). */
class LearnRepository(
    private val people: PersonDao,
    private val tabs: TabDao,
    private val rounds: LearnRoundDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** Up to [size] random cards. Each person is in a round only once. */
    suspend fun newDeck(size: Int = ROUND_SIZE, random: Random = Random.Default): List<LearnCard> =
        buildDeck(people.getAll(), people.getAllTabLinks(), tabs.getAll(), size, random)

    /** Saves a finished round and returns all rounds, the oldest first. */
    suspend fun saveRound(known: Int, total: Int): List<LearnRound> {
        rounds.insert(LearnRound(finishedAt = clock(), known = known, total = total))
        return rounds.getAll()
    }

    suspend fun history(): List<LearnRound> = rounds.getAll()

    companion object {
        const val ROUND_SIZE = 10

        /**
         * Only people who can be guessed: not "Me", not archived, and with at least one thing for the front
         * of the card (spark, place or notes). A tag alone does not count; many people share a tag.
         */
        fun buildDeck(
            people: List<Person>,
            links: List<PersonTab>,
            tabs: List<Tab>,
            size: Int,
            random: Random,
        ): List<LearnCard> {
            val order = tabs.sortedWith(compareBy({ it.sortIndex }, { it.id }))
            val tagsOf = links.groupBy({ it.personId }, { it.tabId })
            return people
                .filter { !it.isSelf && it.archivedAt == null }
                .mapNotNull { person ->
                    val ids = tagsOf[person.id].orEmpty().toSet()
                    LearnCard(
                        personId = person.id,
                        name = person.name,
                        tags = order.filter { it.id in ids }.map { it.name },
                        spark = person.askAbout.clean(),
                        place = person.metCity.clean(),
                        notes = person.notes.clean(),
                    ).takeIf { it.spark != null || it.place != null || it.notes != null }
                }
                .shuffled(random)
                .take(size)
        }

        private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
