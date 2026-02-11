package io.github.skeletonxf.data

import io.github.skeletonxf.ui.RoleType
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val attackers: RoleType,
    val defenders: RoleType,
)