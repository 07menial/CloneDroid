package com.twinndroid.data

import kotlinx.serialization.Serializable

@Serializable
enum class SourceKind {
    /** Cloned from an app installed on this device; can auto-update when the original updates. */
    INSTALLED,

    /** Cloned from an imported .apk/.apks/.xapk/.apkm file kept in app storage. */
    FILE,
}

@Serializable
enum class IconMode { ORIGINAL, BADGE, CUSTOM }

/** Colour adjustments applied to the clone's icon before any badge. */
@Serializable
data class IconAdjust(
    /** Hue rotation in degrees, -180..180. */
    val hue: Float = 0f,
    /** 0 = greyscale, 1 = unchanged, 2 = double. */
    val saturation: Float = 1f,
    /** -1..1 */
    val brightness: Float = 0f,
    /** -0.5..0.5 */
    val contrast: Float = 0f,
    /** Colour laid over the icon's shape, or null for none. */
    val tint: Long? = null,
    val tintStrength: Float = 0.5f,
    val invert: Boolean = false,
    val flip: Boolean = false,
    /** 0, 90, 180 or 270. */
    val rotation: Int = 0,
) {
    val isIdentity: Boolean
        get() = this == IconAdjust()
}

@Serializable
data class CloneConfig(
    val id: String,
    val sourceKind: SourceKind,
    val sourcePackage: String,
    val sourceLabel: String,
    val clonePackage: String,
    val label: String,
    val iconMode: IconMode = IconMode.BADGE,
    val badgeColor: Long = 0xFF7C4DFF,
    val badgeText: String = "2",
    /** Absolute path of the user's custom icon image (only for [IconMode.CUSTOM]). */
    val customIconPath: String? = null,
    val iconAdjust: IconAdjust = IconAdjust(),
    val removedPermissions: List<String> = emptyList(),
    val debuggable: Boolean = false,
    /** Version code of the source the currently installed clone was built from. 0 = never built. */
    val builtVersionCode: Long = 0,
    val builtVersionName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** [CloneFolder.id] this clone is filed under, or null when it sits at the top level. */
    val folderId: String? = null,
)
