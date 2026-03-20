package plus.vplan.lib.sp24.source

fun removeLeadingZeros(input: String): String {
    return input.dropWhile { it == '0' }
}