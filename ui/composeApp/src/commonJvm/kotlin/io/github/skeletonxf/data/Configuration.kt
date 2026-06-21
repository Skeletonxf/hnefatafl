package io.github.skeletonxf.data

import io.github.skeletonxf.ui.game.RoleType
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val attackers: RoleType,
    val defenders: RoleType,
)