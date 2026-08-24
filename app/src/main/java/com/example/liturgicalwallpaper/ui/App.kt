package com.example.liturgicalwallpaper.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.liturgicalwallpaper.BuildConfig
import com.example.liturgicalwallpaper.R
import com.example.liturgicalwallpaper.domain.model.LiturgicalColor
import com.example.liturgicalwallpaper.settings.WallpaperTarget
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiturgicalApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    var pickerColor by remember { mutableStateOf<LiturgicalColor?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        val color = pickerColor
        if (uri != null && color != null) viewModel.importWallpaper(color, uri)
    }
    val updated = stringResource(R.string.wallpaper_updated)
    val failed = stringResource(R.string.wallpaper_failed)
    val serverUnavailable = stringResource(R.string.server_unavailable)
    val calendarUpdated = stringResource(R.string.calendar_updated)
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbar.showSnackbar(when (message) {
                UiMessage.WallpaperUpdated -> updated
                UiMessage.WallpaperFailed -> failed
                UiMessage.ServerUnavailable -> serverUnavailable
                UiMessage.CalendarUpdated -> calendarUpdated
                is UiMessage.WallpaperMissing -> resources.getString(R.string.wallpaper_missing, message.color)
            })
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val maxContent = if (maxWidth > 1000.dp) 1000.dp else maxWidth
            LazyColumn(
                modifier = Modifier.width(maxContent).align(Alignment.TopCenter),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { CurrentCard(state) }
                item { AutomaticRow(state.settings.automatic, viewModel::setAutomatic) }
                item { SectionTitle(R.string.wallpapers) }
                item {
                    val columns = 2
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LiturgicalColor.entries.chunked(columns).forEach { rowColors ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowColors.forEach { color -> WallpaperCard(color, state.wallpaperFiles[color], Modifier.weight(1f)) {
                                    pickerColor = color
                                    picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                                } }
                                if (rowColors.size < columns) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                item { CalendarCard(state, viewModel::refresh) }
                item { SettingsCard(state, { showTimePicker = true }, viewModel::setTarget, viewModel::applyNow) }
                if (BuildConfig.DEBUG) item { DebugCard(state.simulatedTime, viewModel::simulate, viewModel::runTransitionNow) }
            }
        }
    }
    if (showTimePicker) TimeDialog(state.settings.vespersTime, { showTimePicker = false }) {
        viewModel.setVespersTime(it); showTimePicker = false
    }
}

@Composable private fun CurrentCard(state: MainUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.today).uppercase(), style = MaterialTheme.typography.labelLarge)
            val day = state.liturgicalState?.effectiveDay
            if (day == null) Text(stringResource(R.string.no_calendar), style = MaterialTheme.typography.titleMedium)
            else {
                Text(day.celebration.names.firstOrNull() ?: day.season, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (day.celebration.rank.isBlank()) day.season
                    else stringResource(R.string.rank_and_season, day.celebration.rank, day.season),
                    style = MaterialTheme.typography.bodyMedium,
                )
                ColorLabel(day.primaryColor)
                if (state.liturgicalState.isFirstVespers) Text(stringResource(R.string.first_vespers), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            state.cache?.let { cache ->
                if (runCatching { LocalDate.parse(cache.calendar.validThrough).isBefore(LocalDate.now(cache.calendar.zoneId())) }.getOrDefault(false))
                    Text(stringResource(R.string.calendar_expired), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable private fun AutomaticRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.automatic_wallpaper), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable private fun SectionTitle(resource: Int) = Text(stringResource(resource), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

@Composable private fun WallpaperCard(color: LiturgicalColor, file: File?, modifier: Modifier, onChange: () -> Unit) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorLabel(color)
            Thumbnail(file)
            Text(if (file == null) stringResource(R.string.not_configured) else file.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            TextButton(onClick = onChange, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.change)) }
        }
    }
}

@Composable private fun Thumbnail(file: File?) {
    var bitmap by remember(file?.path, file?.lastModified()) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(file?.path, file?.lastModified()) { bitmap = withContext(Dispatchers.IO) {
        file?.let { BitmapFactory.decodeFile(it.path, BitmapFactory.Options().also { options -> options.inSampleSize = 8 }) }
    } }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable private fun CalendarCard(state: MainUiState, refresh: () -> Unit) {
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.calendar)
        Text(stringResource(R.string.colombian_ordo), style = MaterialTheme.typography.titleMedium)
        state.cache?.let {
            Text(stringResource(R.string.last_checked, it.lastSuccessfulCheck))
            Text(stringResource(R.string.available_through, it.calendar.validThrough))
        } ?: Text(stringResource(R.string.no_calendar))
        if (state.degradedMessage != null && state.cache != null) Text(stringResource(R.string.using_cache), color = MaterialTheme.colorScheme.error)
        if (state.refreshing) CircularProgressIndicator(Modifier.size(28.dp))
        else FilledTonalButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
    } }
}

@Composable private fun SettingsCard(state: MainUiState, editTime: () -> Unit, setTarget: (WallpaperTarget) -> Unit, apply: () -> Unit) {
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.settings)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(stringResource(R.string.vespers_time)); Text(state.settings.vespersTime.toString()) }
            TextButton(onClick = editTime) { Text(stringResource(R.string.change)) }
        }
        HorizontalDivider()
        Text(stringResource(R.string.apply_to), fontWeight = FontWeight.SemiBold)
        TargetOption(WallpaperTarget.HOME, state.settings.target, R.string.home_screen, setTarget)
        TargetOption(WallpaperTarget.HOME_AND_LOCK, state.settings.target, R.string.home_and_lock, setTarget)
        Button(onClick = apply) { Text(stringResource(R.string.apply_now)) }
    } }
}

@Composable private fun TargetOption(value: WallpaperTarget, selected: WallpaperTarget, label: Int, setTarget: (WallpaperTarget) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = value == selected, onClick = { setTarget(value) })
        TextButton(onClick = { setTarget(value) }) { Text(stringResource(label)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TimeDialog(initial: LocalTime, dismiss: () -> Unit, save: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, true)
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.vespers_time)) },
        text = { TimePicker(state) }, confirmButton = { TextButton(onClick = { save(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable private fun DebugCard(simulated: LocalTime?, simulate: (LocalTime?) -> Unit, run: () -> Unit) {
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.debug_tools)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(LocalTime.of(17,59), LocalTime.of(18,0), LocalTime.of(18,1), LocalTime.MIDNIGHT).forEach { time ->
                TextButton(onClick = { simulate(time) }) { Text(time.toString()) }
            }
        }
        if (simulated != null) Text(stringResource(R.string.simulated_time, simulated.toString()))
        TextButton(onClick = { simulate(null) }) { Text(stringResource(R.string.actual_time)) }
        FilledTonalButton(onClick = run) { Text(stringResource(R.string.run_transition)) }
    } }
}

@Composable private fun ColorLabel(color: LiturgicalColor) {
    val label = colorLabel(color)
    val description = stringResource(R.string.color_swatch, label)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(swatch(color)).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .semantics { contentDescription = description })
        Spacer(Modifier.width(8.dp)); Text(label)
    }
}

@Composable private fun colorLabel(color: LiturgicalColor) = stringResource(when(color) {
    LiturgicalColor.GREEN -> R.string.green; LiturgicalColor.WHITE -> R.string.white
    LiturgicalColor.RED -> R.string.red; LiturgicalColor.VIOLET -> R.string.violet
    LiturgicalColor.ROSE -> R.string.rose
    LiturgicalColor.UNKNOWN -> R.string.unknown
})
private fun swatch(color: LiturgicalColor) = when(color) {
    LiturgicalColor.GREEN -> Color(0xFF2E7D32); LiturgicalColor.WHITE -> Color.White
    LiturgicalColor.RED -> Color(0xFFC62828); LiturgicalColor.VIOLET -> Color(0xFF6A1B9A)
    LiturgicalColor.ROSE -> Color(0xFFD96C91)
    LiturgicalColor.UNKNOWN -> Color.Gray
}
