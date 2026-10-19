package org.offlinemesh.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.offlinemesh.calls.CallState
import org.offlinemesh.ui.theme.MeshBlue
import org.offlinemesh.ui.theme.MeshCyan
import org.offlinemesh.ui.theme.MeshPrimaryEmerald
import org.offlinemesh.ui.theme.MeshRed
import org.offlinemesh.ui.theme.MeshSurfaceDark
import org.offlinemesh.ui.theme.MeshTextMuted
import org.offlinemesh.ui.theme.MeshTextPrimary
import org.offlinemesh.ui.theme.MeshTextSecondary

@Composable
fun IncomingCallDialog(
    callState: CallState,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val isGroup = callState is CallState.GroupCallRinging
    val callerName = when (callState) {
        is CallState.IncomingRinging -> callState.peerName
        is CallState.GroupCallRinging -> callState.initiatorName
        else -> "Nearby Peer"
    }
    val subtitle = when (callState) {
        is CallState.IncomingRinging -> "${callState.hopCount} hop(s) via offline mesh"
        is CallState.GroupCallRinging -> "Group Call (${callState.participants.size} participants)"
        else -> ""
    }

    val pulseAnim = rememberInfiniteTransition(label = "ringPulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = onReject,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isGroup) MeshBlue else MeshCyan),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated avatar / ring
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                if (isGroup) MeshBlue.copy(alpha = 0.18f)
                                else MeshPrimaryEmerald.copy(alpha = 0.18f)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (isGroup) MeshBlue.copy(alpha = 0.4f) else MeshPrimaryEmerald.copy(alpha = 0.4f),
                                        MeshSurfaceDark
                                    )
                                )
                            )
                            .border(2.dp, if (isGroup) MeshBlue else MeshPrimaryEmerald, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGroup) Icons.Default.Groups else Icons.Default.PhoneInTalk,
                            contentDescription = "Call",
                            tint = if (isGroup) MeshBlue else MeshPrimaryEmerald,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (isGroup) "Incoming Group Call" else "Incoming Voice Call",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isGroup) MeshBlue else MeshCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = callerName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MeshTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MeshTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Action buttons: Decline & Accept
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onReject,
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MeshRed,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Decline",
                            style = MaterialTheme.typography.labelSmall.copy(color = MeshTextSecondary)
                        )
                    }

                    // Accept
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onAccept,
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MeshPrimaryEmerald,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accept",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Accept",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MeshPrimaryEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
