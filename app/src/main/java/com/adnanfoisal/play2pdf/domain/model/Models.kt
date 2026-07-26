package com.adnanfoisal.play2pdf.domain.model

/**
 * Domain models for Play2PDF.
 *
 * These are pure Kotlin data classes — no Room annotations, no Moshi
 * annotations, no Android imports. The data layer (Room entities, Moshi
 * DTOs) maps to/from these.
 */

/** A YouTube playlist the user added to a compile job. */
data class Playlist(
    val url: String,
    val title: String? = null,
    val channel: String? = null,
    val videoCount: Int? = null,
    val thumbnailUrl: String? = null
)

/** A syllabus topic the user entered or auto-extracted. */
data class Topic(
    val text: String,
    val source: TopicSource = TopicSource.Manual
)

enum class TopicSource { Manual, AutoExtracted }

/** One PDF compile history entry, persisted across app restarts via Room. */
data class PdfHistory(
    val id: Long = 0L,
    val subject: String,
    val author: String,
    val playlistUrls: List<String>,
    val topics: List<String>,
    val theme: PdfTheme,
    val createdAtEpochMs: Long,
    val pdfUri: String? = null,      // content:// URI to the cached PDF
    val pdfSizeBytes: Long? = null,
    val videoCount: Int? = null,
    val topicCount: Int? = null
)

enum class PdfTheme(val apiName: String, val displayName: String) {
    // Light Themes
    NordicFrost("nordic_frost", "Nordic Frost"),
    VelvetDawn("velvet_dawn", "Velvet Dawn"),
    MintBlueprint("mint_blueprint", "Mint Blueprint"),
    GoldenEra("golden_era", "Golden Era"),
    // Dark Themes
    MidnightPurple("midnight_purple", "Midnight Purple"),
    Cyberpunk2077("cyberpunk_2077", "Cyberpunk 2077"),
    ObsidianCrimson("obsidian_crimson", "Obsidian Crimson"),
    OceanicAbyss("oceanic_abyss", "Oceanic Abyss");

    companion object {
        fun fromApiName(name: String): PdfTheme =
            entries.find { it.apiName == name } ?: NordicFrost
    }
}

/** Backend connection state — shown live in Settings. */
enum class ConnectionStatus { Online, Offline, Checking }

/** Steps the compile job walks through — shown as a checklist in CompilingScreen. */
enum class CompileStep(val label: String) {
    Connecting("Connecting to backend"),
    FetchingVideos("Fetching playlist videos"),
    ExtractingTopics("Extracting topics"),
    MatchingTopics("AI matching topics to videos"),
    RenderingPdf("Rendering PDF"),
    Done("Done");

    val next: CompileStep? get() = entries.getOrNull(ordinal + 1)
}

/** All persisted user settings — backed by DataStore. */
data class UserSettings(
    val youtubeApiKey: String = "",
    val geminiApiKey: String = "",
    val backendUrl: String = "https://adnanfoisal-play2pdf.hf.space",
    val userName: String = "",
    val onboardingComplete: Boolean = false,
    val selectedTheme: PdfTheme = PdfTheme.NordicFrost,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)
