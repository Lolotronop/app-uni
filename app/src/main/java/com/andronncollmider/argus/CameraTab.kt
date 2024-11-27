package com.andronncollmider.argus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.net.URI


@Composable
fun CameraTab(
    modifier: Modifier = Modifier,
    cameras: MutableList<Camera>,
    selectedCamera: Int,
    onSelect: (Int) -> Unit
) {
    LazyColumn(modifier) {
        itemsIndexed(cameras) { i, camera ->
            CameraListItem(
                name = camera.name,
                uri = camera.uri,
                selected = selectedCamera == i,
                updateName = { cameras[i] = cameras[i].copy(name = it) },
                deleteCamera = { cameras.remove(camera) },
                updateURI = { cameras[i] = cameras[i].copy(uri = URI(it)) },
                onSelect = {onSelect(i)}
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
