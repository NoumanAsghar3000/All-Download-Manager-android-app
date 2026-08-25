package com.alldownloadmanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import com.alldownloadmanager.database.*
import com.alldownloadmanager.download.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); val shared = intent.getStringExtra(Intent.EXTRA_TEXT); setContent { App(shared) } }
}
class DownloadsViewModel : ViewModel() {
    private lateinit var repo: DownloadRepository
    fun init(context: android.content.Context) { if (!::repo.isInitialized) repo = DownloadRepository(context) }
    fun items() = repo.observe()
    fun add(url: String, context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope) = scope.launch { val item = repo.add(url); WorkManager.getInstance(context).enqueueUniqueWork(item.id, androidx.work.ExistingWorkPolicy.KEEP, DownloadWorker.request(item.id)) }
    fun pause(item: DownloadEntity, context: android.content.Context) = viewModelScope.launch { WorkManager.getInstance(context).cancelUniqueWork(item.id); repo.update(item.copy(status = DownloadStatus.PAUSED)) }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(shared: String?) {
    val context = androidx.compose.ui.platform.LocalContext.current; val vm: DownloadsViewModel = viewModel(); vm.init(context)
    val items by vm.items().collectAsStateWithLifecycle(emptyList()); val scope = rememberCoroutineScope(); var url by remember { mutableStateOf(shared ?: "") }; var tab by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF315F90))) {
        Scaffold(topBar = { TopAppBar(title = { Text("All Download Manager") }, actions = { IconButton(onClick = {}) { Icon(Icons.Default.Settings, "Settings") } }) },
            bottomBar = { NavigationBar { listOf(Icons.Default.Home to "Home", Icons.Default.Download to "Downloads", Icons.Default.Public to "Browser", Icons.Default.Folder to "Files").forEachIndexed { i, pair -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(pair.first, pair.second) }, label = { Text(pair.second) }) } } }) { padding ->
            Column(Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {
                if (tab == 0 || tab == 1) {
                    Spacer(Modifier.height(16.dp)); Text(if (tab == 0) "Ready when you are" else "Downloads", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp)); OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Paste a file URL") }, singleLine = true, trailingIcon = { IconButton(onClick = { if (url.isNotBlank()) vm.add(url, context, scope) }) { Icon(Icons.Default.ArrowDownward, "Download") } })
                    Spacer(Modifier.height(20.dp)); if (items.isEmpty()) Text("No downloads yet. Paste an HTTP or HTTPS file URL to begin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(items, key = { it.id }) { DownloadRow(it, context, vm) } }
                } else if (tab == 2) BrowserScreen(url) { url = it; if (it.startsWith("http")) vm.add(it, context, scope) } else FilesScreen()
            }
        }
    }
}
@Composable private fun DownloadRow(item: DownloadEntity, context: android.content.Context, vm: DownloadsViewModel) {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(item.fileName, style = MaterialTheme.typography.titleMedium); Text(item.status.name.lowercase().replace('_', ' '), color = MaterialTheme.colorScheme.primary); if (item.totalBytes > 0) LinearProgressIndicator(progress = { (item.downloadedBytes.toFloat() / item.totalBytes).coerceIn(0f, 1f) }, Modifier.fillMaxWidth()); Row { if (item.status == DownloadStatus.DOWNLOADING) TextButton({ vm.pause(item, context) }) { Text("Pause") }; TextButton({ WorkManager.getInstance(context).enqueueUniqueWork(item.id, androidx.work.ExistingWorkPolicy.REPLACE, DownloadWorker.request(item.id)) }) { Text("Retry") } } } }
}
@Composable private fun BrowserScreen(initialUrl: String, onDownload: (String) -> Unit) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    Column {
        Text("Browser", style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(12.dp))
        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Address") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        AndroidView(factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: android.webkit.WebView, request: android.webkit.WebResourceRequest): Boolean {
                        val target = request.url.toString()
                        if (target.startsWith("http")) { url = target }
                        return false
                    }
                    override fun shouldInterceptRequest(view: android.webkit.WebView, request: android.webkit.WebResourceRequest): android.webkit.WebResourceResponse? {
                        val target = request.url.toString()
                        if (target.matches(Regex(".*\\.(zip|apk|pdf|mp4|mp3|mkv|png|jpg|jpeg)(\\?.*)?$", RegexOption.IGNORE_CASE))) onDownload(target)
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                if (url.startsWith("http")) loadUrl(url)
            }
        }, Modifier.fillMaxWidth().height(360.dp), update = { if (url.startsWith("http") && it.url != url) it.loadUrl(url) })
        Spacer(Modifier.height(12.dp))
        Text("Direct HTTP(S) media and document links are sent to the queue. Protected streams remain in the browser.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button({ if (url.isNotBlank()) onDownload(url) }) { Text("Download this URL") }
    }
}
@Composable private fun FilesScreen() { Column { Text("Files", style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(12.dp)); Text("Videos   Music   Images   Documents   Archives   APK   Other", style = MaterialTheme.typography.bodyLarge); Text("Files are stored in the app-scoped Downloads folder. Use Android's share/open actions to work with them safely.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }