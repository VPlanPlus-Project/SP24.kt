package plus.vplan.lib.sp24.source

/**
 * @param throwIfIsComposed If the input is a combination of known entities, throw a BadSp24EntityException or return its parts.
 * @param knownEntities A set of known entity names (e.g. class names, teacher names, room names). These MUST BE indexed values as it allows to ignore certain splitting rules.
 * @throws BadSp24EntityException if the input cannot be split into known entities.
 */
fun sp24EntityListSplitter(
    knownEntities: Set<String>,
    input: String,
    entityType: SchoolEntityType,
    throwIfIsComposed: Boolean
): Set<String> {
    if (input in knownEntities) return setOf(input)
    val result = mutableSetOf<String>()
    val knownEntities = knownEntities
        .filterBadSp24Entities(entityType)

        /**
         * Some schools have a room called "Aula" which is used in combination with other rooms but
         * not explicitly listed in the room list. This workaround adds "Aula" to the list of known
         * entities if the input contains "Aula" in any form, but not as a standalone entity.
         */
        .let { if (entityType == SchoolEntityType.Room && it.map { " $it" }.any { "Aula" in it } && "Aula" !in it) (it + "Aula").also { result.add("Aula")} else it }
        .let { if (entityType == SchoolEntityType.Room && it.map { " $it" }.any { "AULA" in it } && "AULA" !in it) (it + "AULA").also { result.add("AULA")} else it }
    if (',' in input) return result + input.split(',').map { it.trim() }.flatMap { sp24EntityListSplitter(knownEntities, it, entityType, throwIfIsComposed) }.distinct()
    if ('/' in input) {
        if (entityType == SchoolEntityType.Class) {
            if (Regex("""^\d+/\d+$""").matches(input)) return result + listOf(input)
        }
        return result + input.split('/').map { it.trim() }.flatMap { sp24EntityListSplitter(knownEntities, it, entityType, throwIfIsComposed) }
    }
    if ('-' in input) {
        if (' ' in input) {
            val items = input.split(' ').map { it.trim() }
            return result + items.flatMap { sp24EntityListSplitter(knownEntities, it, entityType, throwIfIsComposed) }
        }

        /**
         * This can interpolate ranges of entities if they share a common prefix and differ only in the last character(s).
         * Example: "101-105" -> "101", "102", "103", "104", "105"
         * Example: "A1-A5" -> "A1", "A2", "A3", "A4", "A5"
         */

        val parts = input.split('-').map { it.trim() }
        if (parts.size != 2) return result + listOf(input)

        val (differentSuffixA, differentSuffixB, prefix) = removeCommonPrefix(parts[0], parts[1])

        if (differentSuffixA.isBlank() || differentSuffixB.isBlank() || prefix.isBlank()) return result + listOf(input)

        val range = (differentSuffixA.first()..differentSuffixB.first())
        val rangeEntities = range.toList()
        return result + rangeEntities.map { "${parts[0].substringBefore(differentSuffixA)}$it" }
    }

    if (' ' in input) {

        /**
         * If the input consists of multiple numbers separated by spaces, just return the numbers as a list.
         */

        if (Regex("""(\d )+\d""").matches(input)) {
            return result + input.split(' ').map { it.trim() }
        }

        val splitItems = input.split(' ').map { it.trim() }
        if (entityType == SchoolEntityType.Room && splitItems.all {
                val roomNumber = Regex("""^\d+[a-zA-Z]?$""") // e.g. 101, 102a, 103b
                val roomWithPrefix = Regex("""^(Raum( )?)?([a-zA-Z]( )?)?\d+(( )?[a-zA-Z]?)?$""") // e.g. Raum 101, R102, S103a
                val roomName = Regex("""^(Raum )?( [0-9]+)?$""") // e.g. Aula, Raum Paris, Hörsaal 2
                val roomNameDotNotation = Regex("""^(R(aum)?( )?)?(\d+\.)*(\d+)[a-zA-Z]*$""") // e.g. R1.2, Raum 3.4.5, 1.2a
                roomNumber.matches(it) || roomWithPrefix.matches(it) || roomName.matches(it) || roomNameDotNotation.matches(it)
            }) {
            return result + splitItems
        }

        /*
            * This is a backtracking algorithm to find if the input can be split into known entities.
            * Example: If a room list contains "TH 1", "TH 2" and "TH 1 TH 2" with the latter being the input,
            * the algorithm will try to build this string using the known entities.
            * If it can be built, it throws a BadSp24EntityException since it is not a valid entity
            * but rather a combination of known entities.
            * If it cannot be built, it returns the input as a single entity as it might be a valid entity.
         */
        val entitiesWithoutCurrent = knownEntities
            .filter { it != input }

        fun backtrack(remaining: String, used: MutableSet<String>, solution: MutableList<String>): List<String>? {
            val remaining = remaining.trim()
            if (remaining.isEmpty()) {
                return solution.toList()
            }

            for (word in entitiesWithoutCurrent) {
                if (word in used) {
                    continue
                }
                if (remaining.startsWith(word)) {
                    used.add(word)
                    solution.add(word)
                    val result = backtrack(remaining.substring(word.length), used, solution)
                    if (result != null) {
                        return result
                    }
                    used.remove(word)
                    solution.removeAt(solution.size - 1)
                }
            }
            return null
        }

        val result = backtrack(input, mutableSetOf(), mutableListOf())
        if (result != null) {
            if (throwIfIsComposed) throw BadSp24EntityException(
                "The input '$input' is not a valid SP24 entity and cannot be split. " +
                        "Known entities: ${knownEntities.joinToString(", ")}"
            ) else return result.toSet()
        }
    }

    return result + listOf(input)
}

class BadSp24EntityException(message: String) : Exception(message)

/**
 * @return part a, part b, prefix
 */
internal fun removeCommonPrefix(a: String, b: String): Triple<String, String, String> {
    val minLength = minOf(a.length, b.length)
    var index = 0
    while (index < minLength && a[index] == b[index]) {
        index++
    }
    return Triple(a.substring(index), b.substring(index), a.substring(0, index))
}
