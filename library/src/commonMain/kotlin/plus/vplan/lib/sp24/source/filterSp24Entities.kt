package plus.vplan.lib.sp24.source

fun Set<String>.filterBadSp24Entities(type: SchoolEntityType): Set<String> = this.toList().filterBadSp24Entities(type).toSet()

fun List<String>.filterBadSp24Entities(type: SchoolEntityType): List<String> {
    return this
        .filterNot { it.isBadSp24Entity() }
        .map {
            it
                .dropWhile { c -> type != SchoolEntityType.Room && c == '-' }
                .dropLastWhile { c -> c == '-' }
                .trim()
        }
        .distinct()
}

fun String.isBadSp24Entity(): Boolean {
    return this.isBlank() || this == "_" || this.matches(Regex("-+")) || this == "&nbsp;"
}