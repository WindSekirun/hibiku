package io.github.windsekirun.hibiku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.windsekirun.hibiku.feature.ui.immersive.ImmersivePlayerActivity
import io.github.windsekirun.hibiku.feature.R as FeatureR

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    surfaceVariant = Color(0xFF2B2B2B),
                    primary = Color(0xFF5CB3FF),
                    onPrimary = Color.Black
                )
            ) {
                MainAppScreen(
                    onOpenNotificationSettings = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    },
                    onLaunchImmersivePlayer = {
                        val intent = Intent(this, ImmersivePlayerActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    },
                    onHideIcon = {
                        // 런처 아이콘 비활성화 (PackageManager에 의해 시스템에 영속 저장됨)
                        packageManager.setComponentEnabledSetting(
                            ComponentName(this, MainActivity::class.java),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                        finish()
                    }
                )
            }
        }
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    onOpenNotificationSettings: () -> Unit,
    onLaunchImmersivePlayer: () -> Unit,
    onHideIcon: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var showHideIconDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.main_welcome_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF212121)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.main_welcome_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.main_welcome_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Permission Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPermission) {
                        Color(0xFF132B1E)
                    } else {
                        Color(0xFF331C1C)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (hasPermission) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasPermission) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(
                                if (hasPermission) R.string.main_permission_granted_title
                                else R.string.main_permission_required_title
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = stringResource(
                            if (hasPermission) R.string.main_permission_granted_desc
                            else R.string.main_permission_required_desc
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    if (!hasPermission) {
                        Button(
                            onClick = onOpenNotificationSettings,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.main_grant_permission_button),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // StandBy Immersive Player Launcher Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "스탠바이 이머시브 모드 (StandBy)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "세로/가로 센서 자동 회전, M3 모핑 셰이프 앨범아트, 앰비언트 글로우, 물결 스퀴글리 시크바가 지원되는 전체화면 플레이어입니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Button(
                        onClick = onLaunchImmersivePlayer,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(FeatureR.drawable.ic_widget_play),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.main_open_standby_button),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Widget Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.main_widget_guide_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.main_widget_guide_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 22.sp
                    )
                }
            }

            // Hide App Icon Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1A2A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.main_hide_icon_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.main_hide_icon_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 22.sp
                    )
                    Button(
                        onClick = { showHideIconDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A3F6B),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.main_hide_icon_button),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // 아이콘 숨기기 확인 다이얼로그
    if (showHideIconDialog) {
        AlertDialog(
            onDismissRequest = { showHideIconDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.main_hide_icon_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "앱 아이콘을 숨기면 런처에서 바로 열 수 없게 됩니다.\n위젯 설정은 위젯 길게 누르기로 접근할 수 있습니다.\n계속하시겠습니까?",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHideIconDialog = false
                        onHideIcon()
                    }
                ) {
                    Text(
                        text = "숨기고 종료",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideIconDialog = false }) {
                    Text(text = "취소")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// Compose UI Previews
// -------------------------------------------------------------------------

@Preview(name = "Main App Screen - Permission Granted", showBackground = true)
@Composable
fun MainAppScreenGrantedPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2B2B2B),
            primary = Color(0xFF5CB3FF),
            onPrimary = Color.Black
        )

    ) {
        MainAppScreen(
            onOpenNotificationSettings = {},
            onLaunchImmersivePlayer = {},
            onHideIcon = {}
        )
    }
}
