import Query.CollectionUtil.*
import QueryUtil.booleanRetrieval
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

internal class CollectionUtilTest {

    private lateinit var postingListA: List<PostingList>

    @BeforeEach
    fun setup() {
        postingListA = arrayListOf(
            PostingList(1, listOf(1, 3, 5, 7, 9, 11)),
            PostingList(2, listOf(1, 3, 5, 10, 10, 20, 30)),
            PostingList(3, listOf(1, 3, 5, 10, 20, 21, 38, 52, 49)),
            PostingList(4, listOf(1, 3, 5, 10, 10, 50, 60, 70)),
            PostingList(5, listOf(1, 3, 5, 7, 9, 10, 11))
        )
    }

    @Test
    fun `Basic Array Intersection Test`() {
        val intersection: List<Int> = intersect<Int>(postingListA[0].list, postingListA[1].list, COMPARATOR_INTEGER)
        assertIterableEquals(listOf(1, 3, 5), intersection)
    }

    @Test
    fun `Basic Array Intersection Test With Duplications`() {
        val intersection: List<Int> = intersect<Int>(postingListA[2].list, postingListA[3].list, COMPARATOR_INTEGER)
        assertIterableEquals(listOf(1, 3, 5, 10), intersection)
    }

    @Test
    fun `Basic Array Intersection Test With Un-consecutive Elements`() {
        val a = postingListA[1].list
        val b = postingListA[4].list
        val intersection: List<Int> = intersect<Int>(a, b, COMPARATOR_INTEGER)
        assertIterableEquals(listOf(1, 3, 5, 10), intersection)
    }

    @Test
    fun `Basic Array Intersection Test With Multi Duplications`() {
        val a = postingListA[1].list
        val b = postingListA[3].list
        val intersection: List<Int> = intersect<Int>(a, b, COMPARATOR_INTEGER)
        assertIterableEquals(listOf(1, 3, 5, 10, 10), intersection)
    }

    @Test
    fun `Basic Symmetric Differentiation Test`(){
        val symmetric = symmetricDifferentiate<Int>(postingListA[0].list, postingListA[1].list, COMPARATOR_INTEGER)
        assertIterableEquals(listOf(7, 9, 11, 10, 10, 20 ,30), symmetric)
    }

    @Test
    fun `Boolean Retrieval on two PostingList`(){
        val booleanIntersect = booleanRetrieval(postingListA.subList(0, 2))
        assertIterableEquals(listOf(1, 3, 5), booleanIntersect)
    }

    @Test
    fun `Boolean Retrieval on All PostingList`(){
        val booleanIntersect = booleanRetrieval(postingListA)
        assertIterableEquals(listOf(1, 3, 5), booleanIntersect)
    }
}