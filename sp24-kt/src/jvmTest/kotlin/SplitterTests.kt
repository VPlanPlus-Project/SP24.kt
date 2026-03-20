import plus.vplan.lib.sp24.source.SchoolEntityType
import plus.vplan.lib.sp24.source.sp24EntityListSplitter
import kotlin.test.Test
import kotlin.test.assertEquals

class SplitterTests {

    companion object {
        private val classes = setOf(
            "5a",
            "5b",
            "5c",
            "6a",
            "6b",
            "6c",
            "7a",
            "7b",
            "7c",
            "8a",
            "8b",
            "8c",
            "9a",
            "9b",
            "9c",
            "10a",
            "10b",
            "10c",
            "11",
            "12",
        )

        private val rooms = setOf(
            "008",
            "101",
            "102",
            "103",
            "104",
            "TH 1",
            "TH 2",
        )
    }

    @Test
    fun `Existing element`() {
        val element = "7b"
        val result = sp24EntityListSplitter(
            knownEntities = classes,
            input = element,
            entityType = SchoolEntityType.Class,
            throwIfIsComposed = false
        )
        assertTrue(result.size == 1) { "Result has too many/few elements" }
        assertTrue(result.first() == element) { "Result does not match" }
    }

    @Test
    fun `Comma-separated`() {
        val element = "7b, 8a,9c"
        val result = sp24EntityListSplitter(
            knownEntities = classes,
            input = element,
            entityType = SchoolEntityType.Class,
            throwIfIsComposed = false
        )
        assertEquals(setOf("7b", "8a", "9c"), result)
    }

    @Test
    fun `Space-separated`() {
        val element = "7b 8a 9c"
        val result = sp24EntityListSplitter(
            knownEntities = classes,
            input = element,
            entityType = SchoolEntityType.Class,
            throwIfIsComposed = false
        )
        assertEquals(setOf("7b", "8a", "9c"), result)
    }

    @Test
    fun `Range with hyphen`() {
        val element = "7a-7c"
        val result = sp24EntityListSplitter(
            knownEntities = classes,
            input = element,
            entityType = SchoolEntityType.Class,
            throwIfIsComposed = false
        )
        assertEquals(setOf("7a", "7b", "7c"), result)
    }

    @Test
    fun `Slash-separated`() {
        val element = "7a/7c"
        val result = sp24EntityListSplitter(
            knownEntities = classes,
            input = element,
            entityType = SchoolEntityType.Class,
            throwIfIsComposed = false
        )
        assertEquals(setOf("7a", "7c"), result)
    }

    @Test
    fun `Comma-separated ranges 1`() {
        val element = "7a-7c, 8a-8c"
        val result = sp24EntityListSplitter(
            knownEntities = classes,
            input = element,
            entityType = SchoolEntityType.Class,
            throwIfIsComposed = false
        )
        assertEquals(setOf("7a", "7b", "7c", "8a", "8b", "8c"), result)
    }

    @Test
    fun `Comma-separated ranges 2`() {
        val element = "5a-5c,6a-6c,7a-7c,8a-8c"
        val result = sp24EntityListSplitter(
            knownEntities = classes,
            input = element,
            entityType = SchoolEntityType.Class,
            throwIfIsComposed = false
        )
        assertEquals(
            setOf("5a", "5b", "5c", "6a", "6b", "6c", "7a", "7b", "7c", "8a", "8b", "8c"),
            result
        )
    }

    @Test
    fun `Mixed formats`() {
        val element = "7a-7c, 8a/8c, 9b 10a-10c"
        val result = sp24EntityListSplitter(
            knownEntities = classes,
            input = element,
            entityType = SchoolEntityType.Class,
            throwIfIsComposed = false
        )
        assertEquals(setOf("7a", "7b", "7c", "8a", "8c", "9b", "10a", "10b", "10c"), result)
    }

    @Test
    fun `Space-separated with space in name`() {
        val element = "TH 1 TH 2"
        val result = sp24EntityListSplitter(
            knownEntities = rooms,
            input = element,
            entityType = SchoolEntityType.Room,
            throwIfIsComposed = false
        )
        assertEquals(setOf("TH 1", "TH 2"), result)
    }
}