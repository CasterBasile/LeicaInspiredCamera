package com.leicacamera.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.leicacamera.app.ui.CameraScreen
import com.leicacamera.app.ui.CameraViewModel
import com.leicacamera.app.ui.theme.LeicaAnthracite
import com.leicacamera.app.ui.theme.LeicaBlack
import com.leicacamera.app.ui.theme.LeicaCameraTheme
import com.leicacamera.app.ui.theme.LeicaRed
import com.leicacamera.app.ui.theme.LeicaSilver
import com.leicacamera.app.ui.theme.LeicaWhite

class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            LeicaCameraTheme {
                val context = LocalContext.current
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasCameraPermission = granted
                }

                LaunchedEffect(Unit) {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                if (hasCameraPermission) {
                    CameraScreen(viewModel = cameraViewModel)
                } else {
                    MinimalPermissionGate(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onOpenAppSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null)
                            )
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalPermissionGate(
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LeicaBlack)
            .padding(36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(LeicaRed, CircleShape)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "ACCESSO ALL'OTTICA RICHIESTO",
            color = LeicaWhite,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Per mostrare il mirino ottico in tempo reale e acquisire fotografie è necessario autorizzare l'uso della fotocamera.",
            color = LeicaSilver,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(LeicaWhite)
                .clickable { onRequestPermission() }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "ABILITA FOTOCAMERA",
                color = LeicaBlack,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(LeicaAnthracite)
                .border(1.dp, LeicaSilver.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .clickable { onOpenAppSettings() }
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "APRI IMPOSTAZIONI DI SISTEMA",
                color = LeicaSilver,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}