package com.ilya.safarshots.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilya.safarshots.R
import com.ilya.safarshots.core.dunta.DuntaBatterySettingsRequester
import com.ilya.safarshots.core.dunta.DuntaSdkController
import com.ilya.safarshots.core.settings.AppLanguage
import com.ilya.safarshots.design.SectionDivider
import com.ilya.safarshots.design.SettingsRow
import com.ilya.safarshots.design.SettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val resources = LocalResources.current
    val context = LocalContext.current

    LaunchedEffect(resources) {
        viewModel.messages.collect { resId -> snackbarHostState.showSnackbar(resources.getString(resId)) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsSection(
                title = stringResource(R.string.settings_section_privacy),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SettingsRow(
                    title = stringResource(R.string.settings_exif_title),
                    subtitle = stringResource(R.string.settings_exif_subtitle),
                    leadingIcon = Icons.Outlined.PrivacyTip,
                    trailing = {
                        Switch(
                            checked = state.settings.writeGeoDataToExif,
                            onCheckedChange = viewModel::setWriteExif,
                        )
                    },
                )
                SectionDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_precise_title),
                    subtitle = stringResource(R.string.settings_precise_subtitle),
                    leadingIcon = Icons.Outlined.Place,
                    trailing = {
                        Switch(
                            checked = state.settings.showPreciseCoordinates,
                            onCheckedChange = viewModel::setPreciseCoordinates,
                        )
                    },
                )
                SectionDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_dunta_title),
                    subtitle = stringResource(
                        if (state.duntaSdk.isConfigured) {
                            R.string.settings_dunta_subtitle
                        } else {
                            R.string.settings_dunta_unavailable
                        },
                    ),
                    leadingIcon = Icons.Outlined.PrivacyTip,
                    trailing = {
                        Switch(
                            checked = state.duntaSdk.isEnabled,
                            enabled = state.duntaSdk.isConfigured,
                            onCheckedChange = { checked ->
                                viewModel.setDuntaDataSharing(context as? AppCompatActivity, checked)
                            },
                        )
                    },
                )
                if (state.duntaSdk.isEnabled) {
                    SectionDivider()
                    SettingsRow(
                        title = stringResource(R.string.settings_dunta_battery_title),
                        subtitle = stringResource(
                            if (state.duntaSdk.batteryOptimizationExempt) R.string.settings_dunta_battery_unrestricted
                            else R.string.settings_dunta_battery_restricted,
                        ),
                        leadingIcon = Icons.Outlined.Info,
                        onClick = {
                            (context as? DuntaBatterySettingsRequester)?.openDuntaBatterySettings()
                        },
                    )
                }
                SectionDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_dunta_more_info_title),
                    subtitle = stringResource(R.string.settings_dunta_more_info_subtitle),
                    leadingIcon = Icons.Outlined.Info,
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(DuntaSdkController.PRIVACY_POLICY_URL),
                            ),
                        )
                    },
                )
            }
            SettingsSection(
                title = stringResource(R.string.settings_section_appearance),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SettingsRow(
                    title = stringResource(R.string.settings_material_you_title),
                    subtitle = stringResource(R.string.settings_material_you_subtitle),
                    leadingIcon = Icons.Outlined.Palette,
                    trailing = {
                        Switch(
                            checked = state.settings.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                        )
                    },
                )
            }
            SettingsSection(
                title = stringResource(R.string.settings_section_language),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SettingsRow(
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(state.settings.language.labelRes()),
                    leadingIcon = Icons.Outlined.Language,
                    onClick = { viewModel.setLanguageDialog(true) },
                )
            }
            SettingsSection(
                title = stringResource(R.string.settings_section_data),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SettingsRow(
                    title = stringResource(R.string.settings_delete_title),
                    subtitle = stringResource(R.string.settings_delete_subtitle),
                    leadingIcon = Icons.Outlined.DeleteForever,
                    onClick = { viewModel.setDeleteDialog(true) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (state.showLanguageDialog) {
        LanguageChooserDialog(
            current = state.settings.language,
            onPick = { language ->
                viewModel.setLanguage(language)
                (context as? AppCompatActivity)?.recreate()
            },
            onDismiss = { viewModel.setLanguageDialog(false) },
        )
    }

    if (state.showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setDeleteDialog(false) },
            title = { Text(stringResource(R.string.settings_delete_dialog_title)) },
            text = { Text(stringResource(R.string.settings_delete_dialog_text)) },
            confirmButton = { TextButton(onClick = viewModel::deleteAll) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { viewModel.setDeleteDialog(false) }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun LanguageChooserDialog(
    current: AppLanguage,
    onPick: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_dialog_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEach { language ->
                    val selected = language == current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onPick(language) },
                            )
                            .padding(vertical = 10.dp),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Spacer(Modifier.padding(horizontal = 6.dp))
                        Text(
                            stringResource(language.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.ENGLISH -> R.string.settings_language_english
    AppLanguage.INDONESIAN -> R.string.settings_language_indonesian
    AppLanguage.HINDI -> R.string.settings_language_hindi
    AppLanguage.URDU -> R.string.settings_language_urdu
}
