package com.ilya.safarshots.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
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
                title = "Privacy",
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SettingsRow(
                    title = "Записывать геоданные в EXIF",
                    subtitle = "Выключено по умолчанию. Координаты хранятся в локальной базе.",
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
                    title = "Показывать точные координаты",
                    subtitle = "В деталях фото и коллажах.",
                    leadingIcon = Icons.Outlined.Place,
                    trailing = {
                        Switch(
                            checked = state.settings.showPreciseCoordinates,
                            onCheckedChange = viewModel::setPreciseCoordinates,
                        )
                    },
                )
            }
            SettingsSection(
                title = "Appearance",
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SettingsRow(
                    title = "Material You",
                    subtitle = "Использовать системные цвета Android 12+.",
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
                title = "Data",
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SettingsRow(
                    title = "Удалить все данные",
                    subtitle = "Локальные записи Room, фото и thumbnails.",
                    leadingIcon = Icons.Outlined.DeleteForever,
                    onClick = { viewModel.setDeleteDialog(true) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (state.showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setDeleteDialog(false) },
            title = { Text("Удалить все данные?") },
            text = { Text("Это удалит локальные записи и файлы GeoMapShots. Действие нельзя отменить.") },
            confirmButton = { TextButton(onClick = viewModel::deleteAll) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { viewModel.setDeleteDialog(false) }) { Text("Отмена") } },
        )
    }
}
