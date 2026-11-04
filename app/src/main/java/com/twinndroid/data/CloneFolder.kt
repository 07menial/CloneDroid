package com.twinndroid.data

import kotlinx.serialization.Serializable

/** A user-made group of clones on the home screen. Clones point to it through [CloneConfig.folderId]. */
@Serializable
data class CloneFolder(
    val id: String,
    val name: String,
    /** Whether the folder is shown folded on the home screen. Persisted so it survives restarts. */
    val collapsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
