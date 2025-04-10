package com.andronncollmider.argus

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val id: Int,
    val text: String,
    val color: Color,
    var x1: Float,
    var y1: Float,
    var x2: Float,
    var y2: Float
)

@Entity
data class Camera(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "uri") val uri: URI
)

@Dao
interface CameraDao {
    @Query("select * from camera")
    fun getAll(): List<Camera>

    @Insert
    fun insertAll(vararg cameras: Camera)

    @Delete
    fun delete(user: Camera)

    @Update
    fun update(camera: Camera)
}
class UriConverters {
    @TypeConverter
    fun fromUriToString(uri: URI): String {
        return uri.toString() // Uri to String
    }

    @TypeConverter
    fun fromStringToUri(string: String): URI {
        return URI.create(string)
    }
}


@Database(entities = [Camera::class], version = 1)
@TypeConverters(UriConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
}

val COLORS = listOf(Color.Red, Color.Blue, Color.Green, Color.Blue)

class MainViewModel(context: Context) : ViewModel() {
    private val TAG = "ViewModel"

    //    private var _objects: MutableLiveData<MutableList<DetectedObject>> =
//        MutableLiveData(mutableListOf())
//    val objects: LiveData<MutableList<DetectedObject>> = _objects
//    private var _objects = mutableStateListOf<DetectedObject>()
    var objects: MutableLiveData<MutableList<DetectedObject>> = MutableLiveData(mutableListOf())
    var observedObjects: MutableLiveData<MutableList<Int>> = MutableLiveData(mutableListOf())
    var apiBase = ""
    var login = "artmexbet"

    val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "database-name"
    ).build()

    private val _cameras: MutableLiveData<MutableList<Camera>> = MutableLiveData(mutableListOf())

    init {
        viewModelScope.launch {
            val cams = withContext(Dispatchers.IO) {
                db.cameraDao().getAll().toMutableList()
            }
            _cameras.value = cams
            _cameras.postValue(cams)
        }
    }

    val cameras: MutableLiveData<MutableList<Camera>> = _cameras
    fun addCamera() {
        val cam = Camera(name = "New camera", uri = URI("https://example.com"))
        _cameras.value?.add(cam)
        _cameras.postValue(_cameras.value)
        viewModelScope.launch(Dispatchers.IO) {
            db.cameraDao().insertAll(cam)
        }
    }

    fun updateCamera(cam: Camera) = viewModelScope.launch(Dispatchers.IO) {
        db.cameraDao().update(cam)
    }

    fun deleteCamera(cam: Camera) = viewModelScope.launch(Dispatchers.IO) {
        _cameras.value?.remove(cam)
        db.cameraDao().delete(cam)
    }


    @SuppressLint("DefaultLocale")
    fun setObservedObject(index: Int) {
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
    fun removeObservedObject(index: Int) {
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
