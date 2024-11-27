package com.andronncollmider.argus

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI

data class DetectedObject(
    val text: String,
    val color: Color,
    var x1: Float,
    var y1: Float,
    var x2: Float,
    var y2: Float
)

data class Camera(val name: String, val uri: URI)

val COLORS = listOf(Color.Red, Color.Blue, Color.Green, Color.Blue)

class MainViewModel : ViewModel() {
    private val TAG = "ViewModel"
    private var _objects: MutableLiveData<MutableList<DetectedObject>> =
        MutableLiveData(mutableListOf())
    val objects: LiveData<MutableList<DetectedObject>> = _objects

    private val _cameras: MutableLiveData<MutableList<Camera>> = MutableLiveData(mutableListOf(
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
    ))
    val cameras: MutableLiveData<MutableList<Camera>> = _cameras

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
                _objects.value?.add(DetectedObject("Car $i", COLORS[i % 4], 0f, 0f, 0f, 0f))
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
        _objects.postValue(_objects.value)
        _objects.value = _objects.value
    }
}
