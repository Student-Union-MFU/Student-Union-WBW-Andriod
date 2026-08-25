package th.ac.mfu.su.wbw.ui.theme

/**
 * The avatars a participant may choose from.
 *
 * **The keys are the contract, the emoji are not.** What travels to the server and back —
 * and what an older or newer build of the app will be handed — is the key. The glyph beside
 * it is this build's idea of what "deer" looks like, and it can be re-drawn, restyled or
 * replaced with artwork later without touching a single stored row. That is the whole
 * reason the server stores `"deer"` rather than the character itself.
 *
 * This list must stay in step with `avatarKeys` in the server's `wbw_admin_service.go`,
 * which is where a key is accepted or refused. They are deliberately two lists rather than
 * one downloaded from the server: the picker has to draw something the moment Settings
 * opens, on a phone that may be on a hill with no signal, and a set of twelve glyphs is not
 * worth a request.
 *
 * Order is the order they are shown in, and it is grouped rather than alphabetical — trees
 * and flowers, then creatures, then ground and sky — so the grid reads as a small world
 * rather than as a list somebody sorted.
 */
object WbwAvatars {

    /** Key to glyph, in display order. */
    val all: List<Pair<String, String>> = listOf(
        "pine" to "🌲",
        "blossom" to "🌸",
        "fern" to "🌿",
        "leaf" to "🍃",
        "grass" to "🌾",
        "mushroom" to "🍄",
        "deer" to "🦌",
        "bird" to "🐦",
        "butterfly" to "🦋",
        "feather" to "🪶",
        "peak" to "⛰️",
        "sun" to "🌞",
    )

    private val byKey = all.toMap()

    /**
     * The glyph for a key, or null.
     *
     * Null for a key this build does not know, which is a real case rather than a defensive
     * one: the set can grow server-side while somebody is still carrying an older APK, and
     * the honest answer for them is the colour disc they had before — not a blank square,
     * and not a wrong guess at what the new key meant.
     */
    fun glyph(key: String?): String? = key?.let { byKey[it] }
}
