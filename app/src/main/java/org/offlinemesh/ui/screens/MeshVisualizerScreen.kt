package org.offlinemesh.ui.screens

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.offlinemesh.identity.DeviceIdentity
import org.offlinemesh.mesh.MeshDiagnostics
import org.offlinemesh.mesh.MeshNeighbor
import org.offlinemesh.ui.theme.LocalAirLinkColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeshVisualizerScreen(
    identity: DeviceIdentity,
    peers: List<MeshNeighbor>,
    diagnostics: MeshDiagnostics
) {
    val colors = LocalAirLinkColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "meshPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Top Status Capsule
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AirLink Radar Topology",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Local Node: ${identity.deviceId} · ${peers.size} Reachable",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Radar, null, tint = colors.cyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Live", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.cyan)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Tactical Topology Map
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = (size.minDimension / 2f) * 0.78f

                val gridColor = if (colors.isDark) Color(0x1A64748B) else Color(0x2694A3B8)
                val ringFractions = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                ringFractions.forEach { frac ->
                    drawCircle(
                        color = gridColor,
                        radius = maxRadius * frac,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                }

                drawLine(
                    color = gridColor,
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1f
                )

                // Central Node Radiating Pulse Rings
                drawCircle(
                    color = colors.primary.copy(alpha = (1f - pulseProgress) * 0.3f),
                    radius = 28f + (pulseProgress * 55f),
                    center = center
                )

                // Central Node Core (User)
                drawCircle(
                    color = colors.primary,
                    radius = 26f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = center
                )

                // Orbiting Peer Nodes
                val peerCount = peers.size
                if (peerCount > 0) {
                    val angleStep = (2 * Math.PI) / peerCount
                    peers.forEachIndexed { index, peer ->
                        val angle = index * angleStep - (Math.PI / 2)
                        val peerRadius = if (peer.hopCount <= 1) maxRadius * 0.65f else maxRadius * 0.95f
                        val peerPos = Offset(
                            x = center.x + (peerRadius * cos(angle)).toFloat(),
                            y = center.y + (peerRadius * sin(angle)).toFloat()
                        )

                        val isDirect = peer.hopCount <= 1
                        val linkColor = if (isDirect) colors.primary else colors.cyan

                        // Connecting Link Line
                        drawLine(
                            color = linkColor.copy(alpha = 0.45f),
                            start = center,
                            end = peerPos,
                            strokeWidth = 2.5f
                        )

                        // Traveling Packet Particle on the Link
                        val packetPos = Offset(
                            x = center.x + (peerPos.x - center.x) * pulseProgress,
                            y = center.y + (peerPos.y - center.y) * pulseProgress
                        )
                        drawCircle(
                            color = if (colors.isDark) Color.White else colors.textPrimary,
                            radius = 4f,
                            center = packetPos
                        )

                        // Peer Node Circle
                        drawCircle(
                            color = linkColor,
                            radius = 20f,
                            center = peerPos
                        )
                        drawCircle(
                            color = colors.surface,
                            radius = 14f,
                            center = peerPos
                        )
                        drawCircle(
                            color = linkColor,
                            radius = 6f,
                            center = peerPos
                        )

                        // Native Paint Labels
                        val textPaint = Paint().apply {
                            color = colors.textPrimary.toArgb()
                            textSize = 28f
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                        val subTextPaint = Paint().apply {
                            color = if (isDirect) colors.primary.toArgb() else colors.cyan.toArgb()
                            textSize = 22f
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            peer.displayName.take(8),
                            peerPos.x,
                            peerPos.y + 36f,
                            textPaint
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            if (isDirect) "1-Hop" else "${peer.hopCount}-Hops",
                            peerPos.x,
                            peerPos.y + 58f,
                            subTextPaint
                        )
                    }
                }
            }

            // Central "You" label overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 68.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceElevated.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "You (${identity.displayName.take(8)})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Telemetry Footer
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Packets Relayed", fontSize = 11.sp, color = colors.textMuted)
                    Text(
                        "${diagnostics.packetsRelayed}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.cyan
                    )
                }
                Column {
                    Text("Active Mesh Routes", fontSize = 11.sp, color = colors.textMuted)
                    Text(
                        "${diagnostics.activeRoutesCount}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Column {
                    Text("Duplicates Filtered", fontSize = 11.sp, color = colors.textMuted)
                    Text(
                        "${diagnostics.duplicatesDropped}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
