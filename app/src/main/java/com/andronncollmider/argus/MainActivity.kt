package com.andronncollmider.argus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.MediaSource
import com.andronncollmider.argus.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                App()
            }
        }
    }
}

//class DrawableViewModel : ViewModel {
//    val positions = MutableLiveData<List<Float>>()
//    init {
//
//    }
//}

class MainViewModel : ViewModel() {
    private val _positions: MutableLiveData<List<List<Float>>> = MutableLiveData()
    val positions: LiveData<List<List<Float>>> = _positions

    private val _socketStatus = MutableLiveData(false)
    val socketStatus: LiveData<Boolean> = _socketStatus

    private val _messages = MutableLiveData<Pair<Boolean, String>>()
    val messages: LiveData<Pair<Boolean, String>> = _messages

    @androidx.annotation.OptIn(UnstableApi::class)
    fun addMessage(message: Pair<Boolean, String>) = viewModelScope.launch(Dispatchers.Main) {
        if (_socketStatus.value == true) {
            val json = JSONObject(message.second)
            val positionsObject = json.getJSONArray("positions")
            val positions = mutableListOf<List<Float>>()
            for (i in 0..<positionsObject.length()) {
                val ar = positionsObject.getJSONArray(i)
                val l = mutableListOf<Float>()
                for (j in 0..<ar.length()) {
                    l.add(ar.getDouble(j).toFloat())
                }
                positions.add(l)
            }
            Log.d("Test2", "$positions")

            _messages.value = message
            _positions.value = positions
        }
    }

    fun setStatus(status: Boolean) = viewModelScope.launch(Dispatchers.Main) {
        _socketStatus.value = status
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
open class WS(
    private val viewModel: MainViewModel
): WebSocketListener() {

    private val TAG = "Test"

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        viewModel.setStatus(true)
        webSocket.send("Android Device Connected")
        Log.d(TAG, "onOpen:")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        viewModel.addMessage(Pair(false, text))
        Log.d(TAG, "onMessage: $text")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        Log.d(TAG, "onClosing: $code $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        viewModel.setStatus(false)
        Log.d(TAG, "onClosed: $code $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(TAG, "onFailure: ${t.message} $response")
        super.onFailure(webSocket, t, response)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun App(modifier: Modifier = Modifier) {
    val ddd = MainViewModel()
    val sss = WS(ddd)
    val client = OkHttpClient()
    client.newWebSocket(Request.Builder().url("ws://10.0.2.2:3003").build(), sss)
    val cams = remember {
        mutableStateListOf(
            Camera(
                "Camera 1",
                URI("rtsp://admin:Video2023@109.195.69.236:3393/cam/realmonitor?channel=1&subtype=0")
            ),
            Camera(
                "Camera 2",
                URI("rtsp://admin:Video2023@109.195.69.236:3393/cam/realmonitor?channel=1&subtype=0")
            ),
            Camera(
                "Camera 3", URI("rtsp://admin:Video2023@109.195.69.236:3393/cam/realmonitor?channel=1&subtype=0")
            ),
        )
    }
    var selectedCamera by remember { mutableIntStateOf(1) }
    var currentTab by remember { mutableIntStateOf(0) }
    val tabs = arrayOf("Cameras", "Objects", "Events")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(onClick = {
                    cams.add(
                        Camera(
                            "New camera",
                            URI("https://example.com")
                        )
                    )
                }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add"
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                title = { Text("Argus") },
            )
        },
//                    bottomBar = {Text(text="Ok now")}
    ) { innerPadding ->
        val textMeasurer = rememberTextMeasurer()

        Column(modifier = Modifier.padding(innerPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
    //                .background(MaterialTheme.colorScheme.error),
            ) {
                PlayerScreen(cams[0].uri.toString())
                val drawables = listOf(
                    OutlinedObject("Car_1", Color.Red, listOf(0.5f, 0.5f, 0.2f, 0.3f)),
                    OutlinedObject("Car_2", Color.Green, listOf(0.1f, 0.1f, 0.3f, 0.2f)),
                    OutlinedObject("Car_3", Color.Blue, listOf(0.7f, 0.3f, 0.2f, 0.3f)),
                )
                val positions = ddd.positions.observeAsState()
                val colors = listOf(Color.Red, Color.Green, Color.Blue)
                Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
                    if (positions.value == null) {
                        Log.d("Test2", "Is null")
                    } else{
                        for (i in 0..<positions.value!!.size) {
                            val obj = OutlinedObject("Car_$i", colors[i], positions.value!!.get(i))
                            val path = Path()
//                        val x = size.width / 2 - size.width / 100
//                        val y = size.height / 5 - size.width / 100
//                        val width = size.width / 7
//                        val heigth = size.height / 6
                            val x = obj.points[0] * size.width
                            val y = obj.points[1] * size.height
                            val width = obj.points[2] * size.width
                            val heigth = obj.points[3] * size.height
                            path.moveTo(x, y)
                            path.lineTo(x + width, y)
                            path.lineTo(x + width, y + heigth)
                            path.lineTo(x, y + heigth)
                            path.lineTo(x, y)
                            path.close()
                            drawPath(path, obj.color, style = Stroke(width = 5f))
                            val measuredText =
                                textMeasurer.measure(
                                    AnnotatedString(obj.text),
                                    style = TextStyle(fontSize = 14.sp)
                                )
                            var offset = Offset(x, y - measuredText.size.height)
                            if (y - measuredText.size.height < 14) {
                                offset = Offset(x, y + heigth)
                            }
                            drawRect(Color.White, size = measuredText.size.toSize(), topLeft = offset)
                            drawText(measuredText, topLeft = offset)

                        }

                    }
                })
            }


            TabRow(currentTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        text = {
                            Text(
                                text = title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            when (currentTab) {
                0 -> {
                    Column {
                        cams.forEachIndexed { i, camera ->
                            CameraListItem(
                                name = camera.name,
                                uri = camera.uri,
                                selected = selectedCamera == i,
                                updateName = { cams[i] = cams[i].copy(name = it) },
                                deleteCamera = { cams.remove(camera) },
                                updateURI = { cams[i] = cams[i].copy(uri = URI(it)) },
                                onSelect = { selectedCamera = i }
                            )
                        }
                    }
                }

                1 -> {
                    val colors = arrayOf(Color.Blue, Color.Red, Color.Green)
                    var selectedObject by remember { mutableIntStateOf(0) }
                    Column {
                        colors.forEachIndexed { index, color ->
                            Obj(
                                color,
                                selectedObject == index,
                                "Obj $index",
                                onSelect = { selectedObject = index })
                        }
                    }
                }

                2 -> {
                    Column {
                        Event("time", LocalDateTime.now())
                    }
                }
            }
        }
    }
}

data class OutlinedObject(val text: String, val color: Color, val points: List<Float>)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(uri: String) {
    val context = LocalContext.current
    val source: MediaSource =
        RtspMediaSource.Factory().setForceUseRtpTcp(true).createMediaSource(MediaItem.fromUri(uri))
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build().apply {
            playWhenReady = true
            setMediaSource(source)
            prepare()
        }
    }
    Log.i("HHH","----Video player created")
    VideoSurface(modifier = Modifier.fillMaxSize(), exoPlayer = exoPlayer)
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoSurface(modifier: Modifier = Modifier, exoPlayer: ExoPlayer) {
    // TODO: test if this actually works
//    AndroidView(factory = { context ->
//        PlayerView(context).apply {
//            player = exoPlayer
//        }
//    })
    AndroidExternalSurface(
        modifier = modifier,
        onInit = {
            onSurface { surface, _, _ ->
                exoPlayer.setVideoSurface(surface)
                Log.i("HHH", "------CREATED SURFACE")
                surface.onDestroyed {
                    exoPlayer.setVideoSurface(null)
                    exoPlayer.release()
                    Log.i( "HHH", "------Destroyed" )
                }
            }
        }
    )
}

data class Camera(val name: String, val uri: URI)

@Composable
fun ListItem(modifier: Modifier = Modifier, content: @Composable (RowScope.() -> Unit)) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        content()
    }
}

@Composable
fun Obj(
    color: Color,
    selected: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    ListItem(modifier.clickable { onSelect() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = { onSelect() })
            Text(text)
        }

        Canvas(modifier = Modifier.size(25.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawCircle(
                color,
                center = Offset(x = canvasWidth / 2, y = canvasHeight / 2),
                radius = size.minDimension / 2
            )
        }
    }
}

@Composable
fun CameraListItem(
    name: String,
    uri: URI,
    selected: Boolean,
    modifier: Modifier = Modifier,
    updateName: (String) -> Unit,
    updateURI: (String) -> Unit,
    deleteCamera: () -> Unit,
    onSelect: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        CameraDialog(
            name = name,
            uri = uri,
            onDismissRequest = { showDialog = false },
            updateName = { updateName(it) },
            updateURI = { updateURI(it) },
            deleteCamera = { deleteCamera() },
        )
    }

    ListItem(modifier.clickable { onSelect() }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected, onClick = { onSelect() })
            Text(name)
        }
        IconButton(onClick = { showDialog = true }) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Open camera settings",
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
fun CameraDialog(
    name: String,
    uri: URI,
    onDismissRequest: () -> Unit,
    updateName: (String) -> Unit,
    updateURI: (String) -> Unit,
    deleteCamera: () -> Unit
) {
    var tempName by remember { mutableStateOf("") }
    var tempURI by remember { mutableStateOf("") }
    tempURI = uri.toString()
    tempName = name
    Dialog(
        onDismissRequest = { onDismissRequest() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Column {
                    TextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Name") }
                    )

                    TextField(
                        value = tempURI,
                        onValueChange = { tempURI = it },
                        label = { Text("URI") }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { deleteCamera() },
                        colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete camera",
                        )
                    }
                    Row {
                        TextButton(onClick = { onDismissRequest() }) { Text("Close") }
                        TextButton(
                            onClick = {
                                updateName(tempName)
                                updateURI(tempURI)
                                onDismissRequest()
                            }
                        ) {
                            Text(
                                "Save"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Event(
    name: String,
    time: LocalDateTime,
    modifier: Modifier = Modifier
) {
    ListItem(modifier) {
        Column {
            Text(
                time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)),
                // TODO: make it smaller
                modifier = Modifier
            )
            Text(name)
        }
    }
}