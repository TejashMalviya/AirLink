package org.offlinemesh.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.offlinemesh.identity.DeviceIdentity
import org.offlinemesh.mesh.MeshDiagnostics
import org.offlinemesh.ui.theme.LocalAirLinkColors

@Composable
fun DiagnosticsScreen(
    identity: DeviceIdentity,
    diagnostics: MeshDiagnostics,
    isRelayEnabled: Boolean,
    isDarkMode: Boolean,
    onToggleRelay: (Boolean) -> Unit,
    onToggleDarkMode: () -> Unit,
    onUpdateDisplayName: (String) -> Unit
) {
    val colors = LocalAirLinkColors.current
    val context = LocalContext.current
    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(identity.displayName) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. User Profile Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.primaryDark, colors.primary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = identity.displayName.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    if (isEditingName) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.cardBorder,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )
                        IconButton(onClick = {
                            if (nameInput.isNotBlank()) {
                                onUpdateDisplayName(nameInput.trim())
                            }
                            isEditingName = false
                        }) {
                            Icon(Icons.Default.Check, "Save", tint = colors.primary)
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = identity.displayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "AirLink Offline Identity",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        Surface(
                            onClick = { isEditingName = true },
                            shape = CircleShape,
                            color = colors.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, "Edit", tint = colors.cyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 2. Section: Theme & Appearance (Light / Dark Mode)
        SectionHeader("APPEARANCE", colors.textMuted)

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isDarkMode) "Dark Theme" else "Light Theme",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDarkMode) "OLED obsidian dark styling" else "Clean paper light styling",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = colors.surfaceVariant
                    )
                )
            }
        }

        // 3. Section: Mesh Relay Participation
        SectionHeader("MESH NETWORKING", colors.textMuted)

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.cyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Hub, null, tint = colors.cyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Multi-Hop Packet Relay",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Forward voice & photos for distant peers",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isRelayEnabled,
                        onCheckedChange = onToggleRelay,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceElevated)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Encrypted voice packets and photos are forwarded hop-to-phone. Relaying phones cannot decrypt or access private payloads.",
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 4. Section: Real-time Telemetry
        SectionHeader("NETWORK TELEMETRY", colors.textMuted)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryTile(
                title = "Packets Sent",
                value = diagnostics.packetsSent.toString(),
                accent = colors.primary,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                title = "Packets Received",
                value = diagnostics.packetsReceived.toString(),
                accent = colors.cyan,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryTile(
                title = "Packets Relayed",
                value = diagnostics.packetsRelayed.toString(),
                accent = Color(0xFFA78BFA),
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                title = "Duplicates Filtered",
                value = diagnostics.duplicatesDropped.toString(),
                accent = colors.amber,
                modifier = Modifier.weight(1f)
            )
        }

        // 5. Section: Security & Identity
        SectionHeader("CRYPTOGRAPHIC SECURITY", colors.textMuted)

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Node ID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Node Identifier", fontSize = 11.sp, color = colors.textMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            identity.deviceId,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
                        )
                    }

                    IconButton(
                        onClick = { copyToClipboard("Node ID", identity.deviceId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                // Public Key Fingerprint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Public Key Fingerprint (SHA-256)", fontSize = 11.sp, color = colors.textMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            identity.fingerprint,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.cyan
                        )
                    }

                    IconButton(
                        onClick = { copyToClipboard("Fingerprint", identity.fingerprint) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                // Architecture Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = colors.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ECDH secp256r1 + AES-256-GCM authenticated cipher",
                        fontSize = 11.sp,
                        color = colors.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp)
    )
}

@Composable
private fun TelemetryTile(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalAirLinkColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 11.sp, color = colors.textMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}
