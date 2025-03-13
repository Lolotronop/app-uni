package com.andronncollmider.argus

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.Instant
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.concurrent.thread
import kotlin.math.abs

data class DetectedObject(
    val id: Int, val text: String, val color: Color, var x1: Float, var y1: Float, var x2: Float, var y2: Float
)

data class Camera(val name: String, val uri: URI)

val COLORS = listOf(Color.Red, Color.Blue, Color.Green, Color.Blue)

class MainViewModel : ViewModel() {
    private val TAG = "ViewModel"

    //    private var _objects: MutableLiveData<MutableList<DetectedObject>> =
//        MutableLiveData(mutableListOf())
//    val objects: LiveData<MutableList<DetectedObject>> = _objects
//    private var _objects = mutableStateListOf<DetectedObject>()
    var objects: MutableLiveData<MutableList<DetectedObject>> = MutableLiveData(mutableListOf())
    var observedObjects: MutableLiveData<MutableList<Int>> = MutableLiveData(mutableListOf())
    var apiBase = ""
    var login = "artmexbet"

    private val _cameras: MutableLiveData<MutableList<Camera>> = MutableLiveData(
        mutableListOf(
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
    )
    val cameras: MutableLiveData<MutableList<Camera>> = _cameras
    fun addCamera() = viewModelScope.launch(Dispatchers.Main) {
        _cameras.value?.add(Camera("New camera", URI("https://example.com")))
        _cameras.postValue(_cameras.value)
    }

    @SuppressLint("DefaultLocale")
    fun setObservedObject(index: Int)  {
        val client = OkHttpClient()
        val obj = objects.value?.get(index)
        if (obj === null) {
            return
        }
        observedObjects.value?.add(obj.id)
        observedObjects.postValue(observedObjects.value)
        if (objects.value == null || objects.value?.size == 0 || index >= objects.value?.size!!) {
            return
        }
        val car = objects.value?.get(index)
        if (car == null) {
            Log.d("MAIN", "no car found")
            return
        }
        val carId = car.id
        val headers = Headers.Builder().add("Content-Type", "application/json").build()
        val now = Instant.now().toString().split(".")[0]
        Log.d("MAIN", now)

        val tz: TimeZone = TimeZone.getDefault()
        val cal: Calendar = GregorianCalendar.getInstance(tz)
        val offsetInMillis: Int = tz.getOffset(cal.timeInMillis)

        var offset = String.format(
            "%02d:%02d",
            abs((offsetInMillis / 3600000)),
            abs(((offsetInMillis / 60000) % 60))
        )
        offset = (if (offsetInMillis >= 0) "+" else "-") + offset
        val time = now + offset

        val json = """{"car_id": $carId, "login": "$login", "time": "$time"}"""
        Log.d("MAIN", json)
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val req = Request.Builder().url("$apiBase/alarm").post(body).headers(headers).build()
        thread {
            try {
                val res = client.newCall(req).execute()
                Log.d("MAIN", res.code.toString())
            } catch (e: Exception) {
                Log.w("MAIN", "Error sending req, $e")
            }
            return@thread
        }
    }

    @SuppressLint("DefaultLocale")
    fun removeObservedObject(index: Int)  {
        val client = OkHttpClient()
        val obj = objects.value?.get(index)
        if (obj === null) {
            return
        }
        observedObjects.value?.remove(obj.id)
        observedObjects.postValue(observedObjects.value)
        if (objects.value == null || objects.value?.size == 0 || index >= objects.value?.size!!) {
            return
        }
        val car = objects.value?.get(index)
        if (car == null) {
            Log.d("MAIN", "no car found")
            return
        }
        val carId = car.id
        val headers = Headers.Builder().add("Content-Type", "application/json").build()
        val now = Instant.now().toString().split(".")[0]
        Log.d("MAIN", now)

        val tz: TimeZone = TimeZone.getDefault()
        val cal: Calendar = GregorianCalendar.getInstance(tz)
        val offsetInMillis: Int = tz.getOffset(cal.timeInMillis)

        var offset = String.format(
            "%02d:%02d",
            abs((offsetInMillis / 3600000)),
            abs(((offsetInMillis / 60000) % 60))
        )
        offset = (if (offsetInMillis >= 0) "+" else "-") + offset
        val time = now + offset

        val json = """{"car_id": $carId, "login": "$login", "time": "$time"}"""
        Log.d("MAIN", json)
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val req = Request.Builder().url("$apiBase/alarm").delete(body).headers(headers).build()
        thread {
            try {
                val res = client.newCall(req).execute()
                Log.d("MAIN", res.code.toString())
            } catch (e: Exception) {
                Log.w("MAIN", "Error sending req, $e")
            }
            return@thread
        }
    }

    fun fetchCars() {
        val req = Request.Builder().url("$apiBase/cars?login=$login").get().build()
        val client = OkHttpClient()
        thread {
            try {
                val res = client.newCall(req).execute()
                Log.d("MAIN", res.code.toString())
                val body = res.body?.string()
                if (body === null) {
                    Log.d("MAIN", "No body found")
                    return@thread
                }
                val json = JSONArray(body)
                observedObjects.value?.clear()

                for (i in 0..<json.length()) {
                    val obj = json.getJSONObject(i)
                    val id = obj.getInt("carId")
                    observedObjects.value?.add(id)
                }
                observedObjects.postValue(observedObjects.value)
            } catch (e: Exception) {
                Log.w("MAIN", "Error sending req, $e")
            }
            return@thread
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun updateObjects(text: String) = viewModelScope.launch(Dispatchers.Main) {
        val json = JSONObject(text)
        val objectsArray = json.getJSONArray("objects")
        val _objects = mutableListOf<DetectedObject>()
        for (i in 0..<objectsArray.length()) {
            val ob = objectsArray.getJSONObject(i)
            val id = ob.getInt("id")
            val ar = ob.getJSONArray("bbox")
            if (ar.length() < 4) {
                throw Error("Input positions has less than 4 sides")
            }
            if (_objects.size == i) {
//                objects.value?.add(DetectedObject("Car $i", COLORS[i % 4], 0f, 0f, 0f, 0f))
                _objects.add(DetectedObject(id, "Car $id", COLORS[i % 4], 0f, 0f, 0f, 0f))
            }
            _objects[i].x1 = ar.getDouble(0).toFloat()
            _objects[i].y1 = ar.getDouble(1).toFloat()
            _objects[i].x2 = ar.getDouble(2).toFloat()
            _objects[i].y2 = ar.getDouble(3).toFloat()
        }
        val s = _objects.size
        for (i in objectsArray.length()..<s) {
            _objects.removeAt(objectsArray.length())
            _objects.removeAt(objectsArray.length())
        }
        objects.value = _objects
        objects.postValue(_objects)
//        objects.postValue(_objects.value)
    }
}
