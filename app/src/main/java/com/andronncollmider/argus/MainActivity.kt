package com.andronncollmider.argus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.andronncollmider.argus.ui.theme.MyApplicationTheme
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

const val EMUL_WS = "ws://10.0.2.2:3000"
const val LOCAL_WS = "ws://192.168.31.12:3000"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun App(modifier: Modifier = Modifier) {
    val viewModel = remember { MainViewModel() }
    val socket = remember { ObjectWebsocket(viewModel) }
    val objects = viewModel.objects.observeAsState()
    val cams by viewModel.cameras.observeAsState(mutableListOf())
    socket.connect(LOCAL_WS)

    var selectedCamera by remember { mutableIntStateOf(0) }
    var currentTab by remember { mutableIntStateOf(0) }
    var selectedObject by remember { mutableIntStateOf(0) }
    val tabs = rememberSaveable { arrayOf("Cameras", "Objects", "Events") }

    var flipflop by remember { mutableStateOf(false) }
    remember {
        viewModel.objects.observeForever {
            flipflop = !flipflop
        }
        0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(onClick = {
                    cams.add(Camera("New camera", URI("https://example.com")))
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
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
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            VideoFeed(
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
                    CameraTab(cameras = cams,
                        selectedCamera = selectedCamera,
                        onSelect = { selectedCamera = it })
                }

                1 -> {
                    if (objects.value != null) {
                        LazyColumn {
                            itemsIndexed(objects.value!!.toList()) { index, obj ->
                                ObjectListItem(obj.color,
                                    selectedObject == index,
                                    "Car $index",
                                    onSelect = { selectedObject = index })
                            }
                        }
                    }
                }

                2 -> {
                    Column {
                        var bewsocketUrl by rememberSaveable { mutableStateOf("ws://192.168.31.12:3000") }
                        TextField(value = bewsocketUrl,
                            onValueChange = { bewsocketUrl = it },
                            label = { Text("Bewsocket url") })
                        Button(onClick = { socket.connect(bewsocketUrl) }) {
                            Text("Reconnect")
                        }
                        Event("time", LocalDateTime.now())
                    }
                }
            }
        }
    }
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
fun ObjectListItem(
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