package com.adnanfoisal.play2pdf.core.designsystem.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bell
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrowserUpdated
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Custom icon set for Play2PDF.
 *
 * Per Asset F (design plan §2): 24 icons, 24×24 viewBox, 2px stroke,
 * rounded caps, no fills, single color (currentColor → tinted at runtime).
 *
 * STATUS: This file currently exposes Material Icons as **temporary
 * placeholders** per §3.2 of the design plan. When the Design Agent
 * delivers the SVG source for Asset F, replace each entry with a real
 * `ImageVector` built from the SVG path data using the
 * `compose-icons.svg-to-compose` converter (or hand-rolled `materialIcon`
 * builder calls — see Compose Material Icons source for examples).
 *
 * Each entry below is tagged with `// TODO: replace with AppIcons.*` to
 * make find-and-replace easy when the asset lands.
 *
 * The list of icons matches Asset F's spec exactly:
 *  1. playlist      — 3 stacked bars, decreasing width.
 *  2. topic         — bookmark with center dot.
 *  3. book          — open book, 2 lines per page.
 *  4. compile       — document + corner sparkle.
 *  5. history       — clock at 10:10.
 *  6. settings      — 6-tooth gear.
 *  7. search        — magnifier, 2px stroke.
 *  8. filter        — 3 lines, decreasing width.
 *  9. bell          — bell + indicator dot.
 * 10. pdf           — document + corner fold.
 * 11. more          — 3 vertical dots.
 * 12. delete        — trash can.
 * 13. download      — down arrow + tray.
 * 14. open_external — box + up-right arrow.
 * 15. key           — key.
 * 16. wifi          — wifi arcs.
 * 17. cloud         — cloud.
 * 18. user          — person silhouette.
 * 19. close         — X, rounded caps.
 * 20. check         — checkmark.
 * 21. error         — X in circle.
 * 22. plus          — + rounded caps.
 * 23. play          — triangle play.
 * 24. sparkle       — 4-point star.
 */
object AppIcons {
    val Playlist: ImageVector = Icons.Filled.PlaylistPlay            // TODO: replace with custom Asset F SVG
    val Topic: ImageVector = Icons.Outlined.Bookmark                // TODO: replace with custom Asset F SVG
    val Book: ImageVector = Icons.Filled.Book                       // TODO: replace with custom Asset F SVG
    val Compile: ImageVector = Icons.Filled.BrowserUpdated          // TODO: replace with custom Asset F SVG
    val History: ImageVector = Icons.Filled.History                 // TODO: replace with custom Asset F SVG
    val Settings: ImageVector = Icons.Filled.Settings               // TODO: replace with custom Asset F SVG
    val Search: ImageVector = Icons.Filled.Search                   // TODO: replace with custom Asset F SVG
    val Filter: ImageVector = Icons.Filled.FilterList               // TODO: replace with custom Asset F SVG
    val Bell: ImageVector = Icons.Filled.Bell                       // TODO: replace with custom Asset F SVG
    val Pdf: ImageVector = Icons.Filled.PictureAsPdf                // TODO: replace with custom Asset F SVG
    val More: ImageVector = Icons.Filled.MoreVert                   // TODO: replace with custom Asset F SVG
    val Delete: ImageVector = Icons.Filled.Delete                   // TODO: replace with custom Asset F SVG
    val Download: ImageVector = Icons.Filled.Download               // TODO: replace with custom Asset F SVG
    val OpenExternal: ImageVector = Icons.Filled.OpenInNew          // TODO: replace with custom Asset F SVG
    val Key: ImageVector = Icons.Filled.Key                         // TODO: replace with custom Asset F SVG
    val Wifi: ImageVector = Icons.Filled.Wifi                       // TODO: replace with custom Asset F SVG
    val Cloud: ImageVector = Icons.Filled.Cloud                     // TODO: replace with custom Asset F SVG
    val User: ImageVector = Icons.Filled.Spa                        // TODO: replace with custom Asset F SVG (person silhouette)
    val Close: ImageVector = Icons.Filled.Close                     // TODO: replace with custom Asset F SVG
    val Check: ImageVector = Icons.Filled.Check                     // TODO: replace with custom Asset F SVG
    val Error: ImageVector = Icons.Filled.Error                     // TODO: replace with custom Asset F SVG
    val Plus: ImageVector = Icons.Filled.Add                        // TODO: replace with custom Asset F SVG
    val Play: ImageVector = Icons.AutoMirrored.Filled.PlayArrow     // TODO: replace with custom Asset F SVG
    val Sparkle: ImageVector = Icons.Filled.Spa                     // TODO: replace with custom Asset F SVG
    val Inbox: ImageVector = Icons.Filled.Inbox                     // TODO: replace with custom Asset F SVG (empty-state fallback)
    val ArrowForward: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
    val ArrowBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
}
