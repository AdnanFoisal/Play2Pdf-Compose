package com.adnanfoisal.play2pdf.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumCard
import com.adnanfoisal.play2pdf.core.designsystem.components.PremiumTextField
import com.adnanfoisal.play2pdf.core.designsystem.components.PrimaryButton
import com.adnanfoisal.play2pdf.core.designsystem.icons.AppIcons
import com.adnanfoisal.play2pdf.core.effects.pressScaleClickable
import com.adnanfoisal.play2pdf.core.effects.settingsAtmosphere
import com.adnanfoisal.play2pdf.domain.model.ConnectionStatus
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.tokens.Spacing
import com.adnanfoisal.play2pdf.theme.AppShape
import com.adnanfoisal.play2pdf.theme.AppType
import com.adnanfoisal.play2pdf.theme.BrandColors
import com.adnanfoisal.play2pdf.ui.compile.components.PdfThemePreviewRow
import com.adnanfoisal.play2pdf.ui.settings.components.ConnectionStatusIndicator

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.settings

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.Bg)
            .settingsAtmosphere()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Spacing.lg)
        ) {
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.settings_title),
                color = BrandColors.TextPrimary,
                style = AppType.title1
            )
            Spacer(Modifier.height(Spacing.lg))

            // Section 1: API Keys
            SectionHeader(stringResource(R.string.settings_section_api_keys))
            Spacer(Modifier.height(Spacing.sm))
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                var ytKeyVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                var geminiKeyVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PremiumTextField(
                        value = s.youtubeApiKey,
                        onValueChange = viewModel::setYoutubeKey,
                        label = stringResource(R.string.settings_yt_key_label),
                        visualTransformation = if (ytKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { ytKeyVisible = !ytKeyVisible }) {
                                Icon(
                                    imageVector = if (ytKeyVisible) androidx.compose.material.icons.Icons.Filled.Visibility else androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = BrandColors.TextSecondary
                                )
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ConnectionStatusIndicator(status = state.youtubeStatus)
                    }
                    StatusDetail(state.youtubeDetail, state.youtubeStatus)
                    PremiumTextField(
                        value = s.geminiApiKey,
                        onValueChange = viewModel::setGeminiKey,
                        label = stringResource(R.string.settings_gemini_key_label),
                        visualTransformation = if (geminiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { geminiKeyVisible = !geminiKeyVisible }) {
                                Icon(
                                    imageVector = if (geminiKeyVisible) androidx.compose.material.icons.Icons.Filled.Visibility else androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = BrandColors.TextSecondary
                                )
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ConnectionStatusIndicator(status = state.geminiStatus)
                    }
                    StatusDetail(state.geminiDetail, state.geminiStatus)
                }
            }
            Spacer(Modifier.height(Spacing.lg))

            // Section 2: Backend
            SectionHeader(stringResource(R.string.settings_section_backend))
            Spacer(Modifier.height(Spacing.sm))
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PremiumTextField(
                        value = s.backendUrl,
                        onValueChange = viewModel::setBackendUrl,
                        label = stringResource(R.string.settings_backend_url_label)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ConnectionStatusIndicator(status = state.connectionStatus)
                    }
                    StatusDetail(state.connectionDetail, state.connectionStatus)
                }
            }
            Spacer(Modifier.height(Spacing.lg))

            // Section 3: User
            SectionHeader(stringResource(R.string.settings_section_user))
            Spacer(Modifier.height(Spacing.sm))
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    PremiumTextField(
                        value = s.userName,
                        onValueChange = viewModel::setUserName,
                        label = stringResource(R.string.settings_user_name_label)
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))

            // Section 4: PDF Theme — live in-app page previews
            SectionHeader(stringResource(R.string.settings_section_pdf_theme))
            Spacer(Modifier.height(Spacing.sm))
            PdfThemePreviewRow(
                themes = PdfTheme.entries.toList(),
                selected = s.selectedTheme,
                onSelect = viewModel::setTheme,
                compact = false
            )
            Spacer(Modifier.height(Spacing.lg))

            // Section 5: Preferences
            SectionHeader(stringResource(R.string.settings_section_preferences))
            Spacer(Modifier.height(Spacing.sm))
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    ToggleRow(
                        label = stringResource(R.string.settings_sound_toggle),
                        checked = s.soundEnabled,
                        onChange = viewModel::setSoundEnabled
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    ToggleRow(
                        label = stringResource(R.string.settings_haptics_toggle),
                        checked = s.hapticsEnabled,
                        onChange = viewModel::setHapticsEnabled
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Spacer(Modifier.height(80.dp)) // Extra padding for the floating button
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }

        // Floating action button.
        // B: the label used to say "Save Settings" but the handler re-tests
        // the connection (every field already auto-saves on edit) — honest
        // label now, matching what it does.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            PrimaryButton(
                text = stringResource(R.string.settings_test_connection),
                onClick = { viewModel.testConnection() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * One-line explanation under a status indicator: green-ish confirmation
 * when Online, the actual failure reason (HTTP code + Google's message)
 * when Offline. Previously a failed check just said "Offline" with no way
 * to tell an invalid key from a disabled API from no network.
 */
@Composable
private fun StatusDetail(detail: String?, status: ConnectionStatus) {
    if (detail.isNullOrBlank()) return
    Text(
        text = detail,
        color = when (status) {
            ConnectionStatus.Online -> BrandColors.TextTertiary
            ConnectionStatus.Checking -> BrandColors.TextTertiary
            ConnectionStatus.Offline -> BrandColors.Error
        },
        style = AppType.caption,
        modifier = Modifier.padding(bottom = Spacing.sm)
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = BrandColors.TextTertiary,
        style = AppType.label
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = BrandColors.TextPrimary, style = AppType.body)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrandColors.PureWhite,
                checkedTrackColor = BrandColors.Brand,
                uncheckedThumbColor = BrandColors.TextSecondary,
                uncheckedTrackColor = BrandColors.Surface3
            )
        )
    }
}
