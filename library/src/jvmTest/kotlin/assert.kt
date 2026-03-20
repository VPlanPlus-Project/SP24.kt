inline fun assertTrue(condition: Boolean, lazyMessage: () -> Any) {
    if (!condition) {
        throw AssertionError(lazyMessage())
    }
}