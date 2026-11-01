package com.ilya.safarshots.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ilya.safarshots.R
import com.ilya.safarshots.feature.camera.CameraScreen
import com.ilya.safarshots.feature.collage.CollageEditorScreen
import com.ilya.safarshots.feature.collage.CollagePickerScreen
import com.ilya.safarshots.feature.gallery.GalleryScreen
import com.ilya.safarshots.feature.map.MapScreen
import com.ilya.safarshots.feature.photoDetail.PhotoDetailScreen
import com.ilya.safarshots.feature.settings.SettingsScreen

object Routes {
    const val MAP = "map"
    const val CAMERA = "camera"
    const val GALLERY = "gallery"
    const val COLLAGE = "collage"
    const val SETTINGS = "settings"
    const val PHOTO_DETAIL = "photoDetail/{photoId}"
    const val COLLAGE_PICKER = "collagePicker?photoIds={photoIds}"
    const val COLLAGE_EDITOR = "collageEditor?photoIds={photoIds}&template={template}"

    fun photoDetail(photoId: String) = "photoDetail/$photoId"
    fun collagePicker(photoIds: List<String>) = "collagePicker?photoIds=${Uri.encode(photoIds.joinToString(","))}"
    fun collageEditor(photoIds: List<String>, template: String) =
        "collageEditor?photoIds=${Uri.encode(photoIds.joinToString(","))}&template=$template"
}

private data class MainDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val mainDestinations = listOf(
    MainDestination(Routes.MAP, R.string.nav_map, Icons.Rounded.Map, Icons.Outlined.Map),
    MainDestination(Routes.CAMERA, R.string.nav_camera, Icons.Rounded.AddAPhoto, Icons.Outlined.AddAPhoto),
    MainDestination(Routes.GALLERY, R.string.nav_gallery, Icons.Rounded.Image, Icons.Outlined.Image),
    MainDestination(Routes.COLLAGE, R.string.nav_collage, Icons.Rounded.Collections, Icons.Outlined.Collections),
    MainDestination(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings),
)

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute != Routes.CAMERA && currentRoute != Routes.PHOTO_DETAIL

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    mainDestinations.forEach { destination ->
                        val selected = currentDestination.isSelected(destination.route)
                        val label = stringResource(destination.labelRes)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Routes.MAP) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = label,
                                )
                            },
                            label = { Text(label) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAP,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.MAP) {
                MapScreen(
                    onOpenCamera = { navController.navigate(Routes.CAMERA) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenPhoto = { navController.navigate(Routes.photoDetail(it)) },
                    onCreateCollage = { navController.navigate(Routes.collagePicker(it)) },
                )
            }
            composable(Routes.CAMERA) {
                CameraScreen(
                    onClose = { navController.popBackStack() },
                    onPhotoSaved = { photoId ->
                        navController.navigate(Routes.collagePicker(listOf(photoId))) {
                            popUpTo(Routes.CAMERA) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.GALLERY) {
                GalleryScreen(
                    onOpenCamera = { navController.navigate(Routes.CAMERA) },
                    onOpenPhoto = { navController.navigate(Routes.photoDetail(it)) },
                    onCreateCollage = { navController.navigate(Routes.collagePicker(it)) },
                )
            }
            composable(Routes.COLLAGE) {
                CollagePickerScreen(
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onEdit = { ids, template -> navController.navigate(Routes.collageEditor(ids, template.name)) },
                )
            }
            composable(
                route = Routes.PHOTO_DETAIL,
                arguments = listOf(navArgument("photoId") { type = NavType.StringType }),
            ) {
                PhotoDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMap = { navController.navigate(Routes.MAP) },
                    onAddToCollage = { navController.navigate(Routes.collagePicker(listOf(it))) },
                )
            }
            composable(
                route = Routes.COLLAGE_PICKER,
                arguments = listOf(navArgument("photoIds") { defaultValue = "" }),
            ) {
                CollagePickerScreen(
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onEdit = { ids, template -> navController.navigate(Routes.collageEditor(ids, template.name)) },
                )
            }
            composable(
                route = Routes.COLLAGE_EDITOR,
                arguments = listOf(
                    navArgument("photoIds") { defaultValue = "" },
                    navArgument("template") { defaultValue = "PHOTO_WITH_MAP" },
                ),
            ) {
                CollageEditorScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}

private fun NavDestination?.isSelected(route: String): Boolean =
    this?.hierarchy?.any { it.route == route || (route == Routes.COLLAGE && it.route?.startsWith("collage") == true) } == true
