package com.andronncollmider.argus

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
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

const val HOST = "10.0.2.2" //  192.168.31.12 -- localhost machine 10.0.2.2 -- emulator to host
const val WS_ADRESS = "ws://$HOST:3000"
const val API_URL = "http://$HOST:8080"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun App(modifier: Modifier = Modifier) {
    val TAG = "App"

    val viewModel = remember { MainViewModel() }
    viewModel.apiBase = API_URL
    val socket = remember { ObjectWebsocket(viewModel) }
    socket.connect(WS_ADRESS)

    val objects by viewModel.objects.observeAsState(mutableListOf())
    val cams by viewModel.cameras.observeAsState(mutableListOf())

    viewModel.fetchCars()

    var selectedCamera by remember { mutableIntStateOf(0) }
    var currentTab by remember { mutableIntStateOf(0) }
    var selectedObject by remember { mutableIntStateOf(0) }
    val tabs = rememberSaveable { arrayOf("Камеры", "Объекты", "События") }

    var displayNames by remember { mutableStateOf(true) }

    var showDevMenu by rememberSaveable { mutableStateOf(false) }
    var showCameraEdit by rememberSaveable { mutableStateOf(false) }
    if (showCameraEdit) {
        CameraEditScreen(
            uri = viewModel.cameras.value?.get(0)?.uri!!,
            name = viewModel.cameras.value?.get(0)?.name!!,
            onSave = { name, uri -> showCameraEdit = false },
            onCancel = { showCameraEdit = false })
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(onClick = {
//                    viewModel.addCamera()
                    showCameraEdit = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            } else if (currentTab == 1) {
                var isObserved by remember { mutableStateOf(false) }
                try {
                    val objId = viewModel.objects.value?.get(selectedObject)!!.id
                    isObserved = viewModel.observedObjects.value?.contains(objId) == true
                } catch (e: IndexOutOfBoundsException) {
                    Log.d("MAIN", "no cars?")
                }
                Log.d("MAIN", "$isObserved isObserved")
                FloatingActionButton(onClick = {
                    if (isObserved) {
                        viewModel.removeObservedObject(selectedObject)
                        isObserved = false
                    } else {
                        viewModel.setObservedObject(selectedObject)
                        isObserved = true
                    }
                }) {
                    if (isObserved) {
                        Text("D")
                    } else {
                        Text("O")
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDevMenu = true },
                title = {
                    Text(text = viewModel.cameras.value?.get(selectedCamera)?.name ?: "Argus")
                },
            )
        },
    ) { innerPadding ->

        var bewsocketUrl by rememberSaveable { mutableStateOf("ws://192.168.31.12:3000") }
        var api_url by rememberSaveable { mutableStateOf(API_URL) }
        var login by rememberSaveable { mutableStateOf("artmexbet") }
        if (showDevMenu) {
            Dialog(
                onDismissRequest = { showDevMenu = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                ),
            ) {
                Column {
                    TextField(value = bewsocketUrl,
                        onValueChange = { bewsocketUrl = it },
                        label = { Text("Bewsocket url") })
                    Button(onClick = { socket.connect(bewsocketUrl) }) {
                        Text("Reconnect")
                    }
                    TextField(value = api_url,
                        onValueChange = { api_url = it },
                        label = { Text("API url") })
                    Button(onClick = { viewModel.apiBase = api_url }) {
                        Text("Set api url")
                    }

                    TextField(value = login,
                        onValueChange = { login = it },
                        label = { Text("Login") })
                    Button(onClick = { viewModel.login = login }) {
                        Text("Set login")
                    }
                    Button(onClick = { displayNames = !displayNames }) {
                        Text("Display names")
                    }
                    Button(onClick = { viewModel.fetchCars() }) {
                        Text("Fetch observed cars")
                    }

                    Event("time", LocalDateTime.now())
                }
            }
        }

        Column(modifier = Modifier.padding(innerPadding)) {
            VideoFeed(
                uri = cams[0].uri.toString(),
                objects = objects,
                selectedObject = selectedObject,
                displayNames = displayNames
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
            Column(modifier = Modifier.zIndex(10f)) {
                when (currentTab) {
                    0 -> {
                        CameraTab(cameras = cams,
                            selectedCamera = selectedCamera,
                            onSelect = { selectedCamera = it },
                            onEdit = {id -> showCameraEdit = true}
                        )
                    }

                    1 -> {
                        var showObserved by rememberSaveable { mutableStateOf(true) }
                        Column(
                            modifier = Modifier
                                .clickable { showObserved = !showObserved }
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            val text = "На наблюдении (${viewModel.observedObjects.value?.size})"
                            val arrow = if (showObserved) "v" else ">"
                            Text(
                                "$text $arrow"
                            )
                        }
                        if (showObserved) {
                            LazyColumn {
                                itemsIndexed(objects!!.toList()) { index, obj ->
                                    if (viewModel.observedObjects.value?.contains(obj.id) == true) {
                                        ObjectListItem(obj.color,
                                            selectedObject == index,
                                            obj.text,
                                            onSelect = { selectedObject = index })

                                    }
                                }
                            }
                        }
                        Text(
                            "Машины",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
//                        LazyColumn {
//                            itemsIndexed(objects!!.toList()) { index, obj ->
//                                if (viewModel.observedObjects.value?.contains(obj.id) != true) {
//                                    ObjectListItem(obj.color,
//                                        selectedObject == index,
//                                        obj.text,
//                                        onSelect = { selectedObject = index })
//                                }
//                            }
//                        }
                        ObjectListItem(Color.Red,
                            true,
                            "Object 1",
                            onSelect = { })
                    }

                    2 -> {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Event(name = "Event", time = LocalDateTime.now())
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraEditScreen(
    modifier: Modifier = Modifier,
    onSave: (name: String, uri: String) -> Unit,
    onCancel: () -> Unit,
    name: String,
    uri: URI,
) {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(true) }
    var tempName by remember { mutableStateOf("") }
    var tempURI by remember { mutableStateOf("") }
    tempURI = uri.toString()
    tempName = name
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showBottomSheet = true
                }
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "")
            }
        }
    ) { contentPadding ->
        Image(
            painter = painterResource(id = R.drawable.map),
            contentDescription = "map",
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentScale = ContentScale.FillHeight,
        )

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState,
                modifier = modifier
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Name") })

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = tempURI,
                        onValueChange = { tempURI = it },
                        label = { Text("URI") })

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { onCancel() }) { Text("Cancel") }
                        Button(onClick = {
                            onSave(tempName, tempURI)
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