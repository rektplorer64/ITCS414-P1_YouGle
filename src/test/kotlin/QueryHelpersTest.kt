import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class QueryHelpersTest {

    private val query: String = "hello there"

    private lateinit var postingListA: List<PostingList>

    @BeforeEach
    fun setup() {
        postingListA = arrayListOf(
            PostingList(1, listOf(1, 3, 5, 7, 9, 10, 11)),
            PostingList(2, listOf(1, 3, 5, 10, 20, 30)),
            PostingList(3, listOf(1, 3, 5, 10, 20, 21, 38, 49, 52)),
            PostingList(4, listOf(1, 3, 5, 10, 50, 60, 70))
        )
    }

    @Test
    fun processQuery() {
        val queries = QueryHelpers.processQuery(query)
        assertIterableEquals(listOf("hello", "there"), queries)
    }

    @Test
    fun booleanRetrieval() {
        val query = QueryHelpers.booleanRetrieval(postingListA)
        println(query)
        assertIterableEquals(listOf(1, 3, 5, 10), query)
    }
}