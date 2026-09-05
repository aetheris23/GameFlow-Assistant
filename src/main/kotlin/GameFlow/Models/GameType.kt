package GameFlow.Models

/**
 * Games supported by GameFlow Assistant. The user picks one on startup; the
 * matching profile, templates, key bindings and automation profiles load after.
 */
enum class GameType {
    NONE,
    UMA_MUSUME,
    BLUE_ARCHIVE;

    /** Match by enum name or the human name ("Uma Musume"). */
    fun fromName(name: String?): GameType {
        if (name == null || name.isEmpty()) return NONE
        val n = name.trim()
        for (g in values()) {
            if (g.name.lowercase() == n.lowercase() || g.displayName().lowercase() == n.lowercase()) return g
        }
        return NONE
    }

    /** Directory key used for the game's resource folder / DB profile key. */
    fun profileKey(): String {
        return if (this == UMA_MUSUME) "UmaMusume"
                else if (this == BLUE_ARCHIVE) "BlueArchive"
                else "None"
    }

    fun displayName(): String {
        return if (this == UMA_MUSUME) "Uma Musume"
                else if (this == BLUE_ARCHIVE) "Blue Archive"
                else "None"
    }
}