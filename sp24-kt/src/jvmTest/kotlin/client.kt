import plus.vplan.lib.sp24.source.Authentication

fun getWPlanSchool(): Authentication {
    return authenticationForSp24Id("10063764")!!
}

fun getSPlanSchool(): Authentication {
    return authenticationForSp24Id("20299165")!!
}
