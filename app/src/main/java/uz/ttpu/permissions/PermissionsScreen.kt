package uz.ttpu.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


@Composable
fun DemoScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        CameraGate { PhotoSection() }

        LocationSection()
    }
}

@Composable
fun CameraGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity

    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        asked = true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = context.hasCameraPermission()
    }

    val rationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.CAMERA
    )
    val blocked = asked && !granted && !rationale

    when {
        granted -> content()

        blocked -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Доступ к камере запрещён. Включите его в настройках.")
            Button(onClick = { context.openAppSettings() }) {
                Text("Открыть настройки")
            }
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rationale) Text("Камера нужна, чтобы делать фото и видео.")
            else Text("Камера: разрешение не выдано.")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Разрешить камеру")
            }
        }
    }
}

fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    )
    startActivity(intent)
}
fun Context.createMediaUri(video: Boolean): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "demo_${System.currentTimeMillis()}")
        put(MediaStore.MediaColumns.MIME_TYPE, if (video) "video/mp4" else "image/jpeg")
    }
    val collection = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    return contentResolver.insert(collection, values)
}

@Composable
fun PhotoSection() {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        message = if (ok) "Фото сохранено в галерею" else "Фото не сделано"
    }

    val takeVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { ok ->
        message = if (ok) "Видео сохранено в галерею" else "Видео не записано"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Камера: разрешение получено")

        Button(onClick = {
            context.createMediaUri(video = false)?.let { takePhoto.launch(it) }
        }) { Text("Сделать фото") }

        Button(onClick = {
            context.createMediaUri(video = true)?.let { takeVideo.launch(it) }
        }) { Text("Записать видео") }

        Text(message)
    }
}

@SuppressLint("MissingPermission")
@Composable
fun LocationSection() {
    val context = LocalContext.current
    var text by remember { mutableStateOf("Координаты: —") }

    fun loadLocation() {
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                text = if (loc != null) {
                    "Координаты: ${loc.latitude}, ${loc.longitude}"
                } else {
                    "Не удалось получить координаты (задайте Location в эмуляторе)"
                }
            }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) loadLocation() else text = "Доступ к геолокации запрещён"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text)
        Button(onClick = {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }) { Text("Определить местоположение") }

        Button(onClick = { context.openAppSettings() }) {
            Text("Открыть настройки приложения")
        }
    }
}