package org.offlinemesh.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.offlinemesh.calls.CallState
import org.offlinemesh.ui.theme.MeshBackgroundDark
import org.offlinemesh.ui.theme.MeshBlue
import org.offlinemesh.ui.theme.MeshCyan
import org.offlinemesh.ui.theme.MeshPrimaryEmerald
import org.offlinemesh.ui.theme.MeshRed
import org.offlinemesh.ui.theme.MeshSurfaceDark
import org.offlinemesh.ui.theme.MeshSurfaceVariantDark
import org.offlinemesh.ui.theme.MeshTextMuted
import org.offlinemesh.ui.theme.MeshTextPrimary
import org.offlinemesh.ui.theme.MeshTextSecondary

@Composable
fun ActiveCallScreen(
    callState: CallState,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    val peerName = when (callState) {
        is CallState.Connected -> callState.peerName
        is CallState.OutgoingRinging -> callState.peerName
        else -> "Nearby Peer"
    }
    val isConnected = callState is CallState.Connected
    val isMuted = (callState as? CallState.Connected)?.isMuted ?: false
    val isSpeakerOn = (callState as? CallState.Connected)?.isSpeakerOn ?: false
    val duration = (callState as? CallState.Connected)?.durationSeconds ?: 0L
    val hopCount = when (callState) {
        is CallState.Connected -> callState.hopCount
        is CallState.OutgoingRinging -> callState.hopCount
        else -> 1
    }
    val isGroup = (callState as? CallState.Connected)?.isGroupCall ?: false

    // Pulsing acoustic breathing ring
    val pulseAnim = rememberInfiniteTransition(label = "pulseRing")
    val ringScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    // Animated voice activity waveform bars
    val waveAnim = rememberInfiniteTransition(label = "waveformAnim")
    val wave1 by waveAnim.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "w1")
    val wave2 by waveAnim.animateFloat(0.5f, 0.9f, infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse), label = "w2")
    val wave3 by waveAnim.animateFloat(0.2f, 1f, infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "w3")
    val wave4 by waveAnim.animateFloat(0.4f, 0.85f, infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse), label = "w4")
    val wave5 by waveAnim.animateFloat(0.3f, 0.95f, infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "w5")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090E17),
                        Color(0xFF0D1826),
                        Color(0xFF070B12)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Call Info Area
            Column(
                modifier = Modifier.padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Call status tag
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isConnected) MeshPrimaryEmerald.copy(alpha = 0.15f) else MeshCyan.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) MeshPrimaryEmerald else MeshCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                !isConnected -> "CALLING PEER…"
                                isGroup -> "GROUP MESH CALL"
                                else -> "OFFLINE VOICE CALL"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) MeshPrimaryEmerald else MeshCyan,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Caller Display Name
                Text(
                    text = peerName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Duration or Ringing Status
                Text(
                    text = if (isConnected) formatCallDuration(duration) else "Ringing nearby phone…",
                    fontSize = 16.sp,
                    color = if (isConnected) MeshTextPrimary else MeshTextSecondary,
                    fontWeight = if (isConnected) FontWeight.SemiBold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Encryption & Hop Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MeshSurfaceVariantDark.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MeshPrimaryEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "AES-256 E2EE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MeshPrimaryEmerald
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MeshSurfaceVariantDark.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = MeshCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (hopCount <= 1) "Direct 1-Hop" else "$hopCount Hops Relay",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MeshCyan
                            )
                        }
                    }
                }
            }

            // Center: Breathing Avatar Circle with Voice Waveform
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft Outer Breathing Ring
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MeshPrimaryEmerald.copy(alpha = 0.18f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Core Avatar Surface
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF065F46), Color(0xFF10B981))
                                )
                            )
                            .border(2.dp, MeshPrimaryEmerald.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = peerName.take(1).uppercase(),
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Live Audio Waveform Bars (visible when connected)
                if (isConnected && !isMuted) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(32.dp)
                    ) {
                        WaveformBar(wave1, MeshPrimaryEmerald)
                        WaveformBar(wave2, MeshCyan)
                        WaveformBar(wave3, MeshPrimaryEmerald)
                        WaveformBar(wave4, MeshCyan)
                        WaveformBar(wave5, MeshPrimaryEmerald)
                    }
                } else if (isMuted) {
                    Text(
                        text = "Microphone Muted",
                        fontSize = 13.sp,
                        color = MeshRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bottom Controls Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Toggle Button
                    CallDockButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Muted" else "Mute",
                        isActive = isMuted,
                        activeColor = MeshRed,
                        onClick = onToggleMute,
                        enabled = isConnected
                    )

                    // End Call Button (Large Vibrant Red Circle)
                    Surface(
                        onClick = onEndCall,
                        shape = CircleShape,
                        color = MeshRed,
                        modifier = Modifier.size(76.dp),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // Speakerphone Toggle Button
                    CallDockButton(
                        icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                        label = if (isSpeakerOn) "Speaker" else "Earpiece",
                        isActive = isSpeakerOn,
                        activeColor = MeshCyan,
                        onClick = onToggleSpeaker,
                        enabled = isConnected
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveformBar(amplitude: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(5.dp)
            .height((amplitude * 28).dp.coerceAtLeast(6.dp))
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}

@Composable
private fun CallDockButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = if (isActive) activeColor.copy(alpha = 0.2f) else MeshSurfaceVariantDark,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isActive) activeColor else MeshSurfaceVariantDark
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) activeColor else MeshTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive) activeColor else MeshTextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatCallDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
