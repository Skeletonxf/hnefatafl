package io.github.skeletonxf.ffi

import io.github.skeletonxf.ui.RoleType

data class Configuration(
    val attackers: RoleType,
    val defenders: RoleType,
)