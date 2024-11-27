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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import androidx.media3.common.AudioAttributes
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

data class OutlinedObject(
    val text: String,
    val color: Color,
    var x1: Float,
    var y1: Float,
    var x2: Float,
    var y2: Float
)

data class Camera(val name: String, val uri: URI)

class MainViewModel : ViewModel() {
    private var _objects: MutableLiveData<MutableList<OutlinedObject>> =
        MutableLiveData(mutableListOf())
    val objects: LiveData<MutableList<OutlinedObject>> = _objects

    private val _cameras: MutableLiveData<List<Camera>> = MutableLiveData(listOf())
    val cameras: LiveData<List<Camera>> = _cameras

    private val COLORS = listOf(Color.Red, Color.Blue, Color.Green, Color.Blue)

    @androidx.annotation.OptIn(UnstableApi::class)
    fun updateObjects(text: String) = viewModelScope.launch(Dispatchers.Main) {
        val json = JSONObject(text)
        val objectsArray = json.getJSONArray("objects")
        for (i in 0..<objectsArray.length()) {
            val ob = objectsArray.getJSONObject(i)
            val ar = ob.getJSONArray("bbox")
            if (ar.length() < 4) {
                throw Error("Input positions has less than 4 sides")
            }
            if (_objects.value!!.size == i) {
                Log.d("Test3", "Creating new object...")
                _objects.value?.add(OutlinedObject("Car $i", COLORS[i % 4], 0f, 0f, 0f, 0f))
            }
            _objects.value!![i].x1 = ar.getDouble(0).toFloat()
            _objects.value!![i].y1 = ar.getDouble(1).toFloat()
            _objects.value!![i].x2 = ar.getDouble(2).toFloat()
            _objects.value!![i].y2 = ar.getDouble(3).toFloat()
        }
        val s = _objects.value!!.size
        for (i in objectsArray.length()..<s) {
            objects.value!!.removeAt(objectsArray.length())
        }
        _objects.value = _objects.value
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
open class WS(
    private val viewModel: MainViewModel
) : WebSocketListener() {

    private val TAG = "Test"

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        webSocket.send("Android Device Connected")
        Log.d(TAG, "onOpen:")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        viewModel.updateObjects(text)
        Log.d(TAG, "onMessage: $text")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        Log.d(TAG, "onClosing: $code $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        Log.d(TAG, "onClosed: $code $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(TAG, "onFailure: ${t.message} $response")
        super.onFailure(webSocket, t, response)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun CameraDisplay(
    modifier: Modifier = Modifier,
    uri: String,
    objectsState: State<MutableList<OutlinedObject>?>,
    f: Boolean,
    selectedObject: Int
) {
    // FIXME: this is the biggest hack of all time
    if (f) {
        Log.d("H", "H")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        //                .background(MaterialTheme.colorScheme.error),
    ) {
        PlayerScreen(uri)

        val textMeasurer = rememberTextMeasurer()
        val objects = objectsState.value

        val intervals = FloatArray(2)
        intervals[0] = 10f
        intervals[1] = 4f
        var phase by remember { mutableFloatStateOf(0f) }
        Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
            if (objects == null) {
                Log.d("Test3", "Is null")
            } else {
                objects.forEachIndexed({ index, obj ->
                    val x1 = obj.x1 * size.width
                    val y1 = obj.y1 * size.height
                    val x2 = obj.x2 * size.width
                    val y2 = obj.y2 * size.height

                    val path = Path()
                    path.moveTo(x1, y1)
                    path.lineTo(x2, y1)
                    path.lineTo(x2, y2)
                    path.lineTo(x1, y2)
                    path.lineTo(x1, y1)
                    path.close()

                    if (index == selectedObject) {
                        val effect = PathEffect.dashPathEffect(intervals = intervals, phase = phase)
                        drawPath(path, obj.color, style = Stroke(width = 5f, pathEffect = effect))
                        drawRect(obj.color.copy(0.2f), Offset(x1, y1), Size(x2 - x1, y2 - y1))
                        phase = (phase + 0.3f) % 14
                    } else {
                        drawPath(path, obj.color, style = Stroke(width = 3f))
                    }
                    val measuredText = textMeasurer.measure(
                        AnnotatedString(obj.text), style = TextStyle(fontSize = 14.sp)
                    )
                    val height = y2 - y1
                    var offset = Offset(x1, y1 - measuredText.size.height)
                    if (y1 - measuredText.size.height < 14) {
                        offset = Offset(x1, y1 + height)
                    }
                    drawRect(
                        Color.White, size = measuredText.size.toSize(), topLeft = offset
                    )
                    drawText(measuredText, topLeft = offset)
                })
            }
        })
    }

}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun App(modifier: Modifier = Modifier) {
    val ddd = remember { MainViewModel() }
    val sss = remember { WS(ddd) }
    val objects = ddd.objects.observeAsState()
    val client = remember { OkHttpClient() }
    val emulUrl = "ws://10.0.2.2:3000"
    val localUrl = "ws://192.168.31.12:3000"
    var socket = client.newWebSocket(Request.Builder().url(localUrl).build(), sss)
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
                "Camera 3",
                URI("rtsp://admin:Video2023@109.195.69.236:3393/cam/realmonitor?channel=1&subtype=0")
            ),
        )
    }
    var selectedCamera by remember { mutableIntStateOf(0) }
    var currentTab by remember { mutableIntStateOf(0) }
    var selectedObject by remember { mutableIntStateOf(0) }
    val tabs = arrayOf("Cameras", "Objects", "Events")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(onClick = {
                    cams.add(
                        Camera(
                            "New camera", URI("https://example.com")
                        )
                    )
                }) {
                    Icon(
                        Icons.Default.Add, contentDescription = "Add"
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
        Column(modifier = Modifier.padding(innerPadding)) {
            var flipflop by remember { mutableStateOf(false) }
            ddd.objects.observeForever {
                flipflop = !flipflop
            }
            CameraDisplay(
                uri = cams[0].uri.toString(),
                objectsState = objects,
                f = flipflop,
                selectedObject = selectedObject
            )

            TabRow(currentTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = currentTab == index, onClick = { currentTab = index }, text = {
                        Text(
                            text = title, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    })
                }
            }

            when (currentTab) {
                0 -> {
                    Column {
                        cams.forEachIndexed { i, camera ->
                            CameraListItem(name = camera.name,
                                uri = camera.uri,
                                selected = selectedCamera == i,
                                updateName = { cams[i] = cams[i].copy(name = it) },
                                deleteCamera = { cams.remove(camera) },
                                updateURI = { cams[i] = cams[i].copy(uri = URI(it)) },
                                onSelect = { selectedCamera = i })
                        }
                    }
                }

                1 -> {
                    if (objects.value != null) {
                        LazyColumn {
                            itemsIndexed(objects.value!!.toList()) { index, obj ->
                                Obj(
                                    obj.color,
                                    selectedObject == index,
                                    "Car $index",
                                    onSelect = { selectedObject = index })
                            }
                        }
                    }
                }

                2 -> {
                    Column {
                        var bewsocketUrl by remember { mutableStateOf("ws://192.168.31.12:3000") }
                        TextField(
                            value = bewsocketUrl,
                            onValueChange = { bewsocketUrl = it },
                            label = { Text("Bewsocket url") }
                        )
                        Button(onClick = {
                            socket.close(1000, "No")
                            socket = client.newWebSocket(
                                Request.Builder().url(bewsocketUrl).build(),
                                sss
                            )
                        }, content = { Text("Reconnect") })
                        Event("time", LocalDateTime.now())
                    }
                }
            }
        }
    }
}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(uri: String) {
    val context = LocalContext.current
    val source: MediaSource =
        RtspMediaSource.Factory().setForceUseRtpTcp(true).createMediaSource(MediaItem.fromUri(uri))
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            setMediaSource(source)
            prepare()
        }
    }
    exoPlayer.volume = 0f
    Log.i("HHH", "----Video player created")
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
    AndroidExternalSurface(modifier = modifier, onInit = {
        onSurface { surface, _, _ ->
            exoPlayer.setVideoSurface(surface)
            exoPlayer.play()
            Log.i("HHH", "------CREATED SURFACE")
            surface.onDestroyed {
                exoPlayer.setVideoSurface(null)
                Log.i("HHH", "------Destroyed")
            }
        }
    })
}


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
                verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(8.dp)
            ) {
                Column {
                    TextField(value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Name") })

                    TextField(value = tempURI,
                        onValueChange = { tempURI = it },
                        label = { Text("URI") })
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { deleteCamera() }, colors = ButtonColors(
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
                        TextButton(onClick = {
                            updateName(tempName)
                            updateURI(tempURI)
                            onDismissRequest()
                        }) {
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
    name: String, time: LocalDateTime, modifier: Modifier = Modifier
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