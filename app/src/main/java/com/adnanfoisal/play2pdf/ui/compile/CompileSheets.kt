package com.adnanfoisal.play2pdf.ui.compile

// Extracted from CompileScreen.kt (A3): the four bottom sheets.
// Pure move - no behaviour changes.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumTextField
import com.adnanfoisal.play2pdf.core.designsystem.components.PrimaryButton
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.ui.compile.components.PdfThemePreviewRow

// --- Bottom Sheets ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistUrlDialog(
    initialValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandColors.Surface1,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {
            Text("Add Playlist", color = BrandColors.TextPrimary, style = AppType.title3)
            Spacer(Modifier.height(Spacing.md))
            PremiumTextField(
                value = initialValue,
                onValueChange = onValueChange,
                label = "YouTube playlist URL",
                placeholder = "https://youtube.com/playlist?list=\u2026"
            )
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(text = "Add Playlist", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopicInputDialog(
    initialValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandColors.Surface1,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {
            Text("Add Topics (Batch Import)", color = BrandColors.TextPrimary, style = AppType.title3)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Paste multiple topics separated by commas or newlines. They will be added all at once.",
                color = BrandColors.TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(Spacing.md))
            androidx.compose.material3.OutlinedTextField(
                value = initialValue,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("e.g. Photosynthesis, Cell division\nGenetics", color = BrandColors.TextSecondary) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandColors.Brand,
                    unfocusedBorderColor = BrandColors.SurfaceBorder,
                    focusedTextColor = BrandColors.TextPrimary,
                    unfocusedTextColor = BrandColors.TextPrimary
                )
            )
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(text = "Add Topics", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemePickerDialog(
    themes: List<PdfTheme>,
    selected: PdfTheme,
    onSelect: (PdfTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandColors.Surface1,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {
            Text("PDF Theme", color = BrandColors.TextPrimary, style = AppType.title3)
            Spacer(Modifier.height(Spacing.md))
            PdfThemePreviewRow(
                themes = themes,
                selected = selected,
                onSelect = onSelect,
                compact = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdvancedDialog(
    subject: String,
    author: String,
    onSubjectChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandColors.Surface1,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text("Book Details", color = BrandColors.TextPrimary, style = AppType.title3)
            PremiumTextField(value = subject, onValueChange = onSubjectChange, label = "Subject")
            PremiumTextField(value = author, onValueChange = onAuthorChange, label = "Author")
            PrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = Spacing.sm)
            .size(40.dp, 4.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .background(BrandColors.SurfaceBorderStrong)
    )
}
