package org.offlinemesh.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.offlinemesh.ui.theme.LocalAirLinkColors

@Composable
fun OnboardingScreen(onConfirm: (String) -> Unit) {
    val colors = LocalAirLinkColors.current
    var name by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (colors.isDark) {
                        listOf(Color(0xFF090D14), Color(0xFF0E1724), colors.background)
                    } else {
                        listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), colors.background)
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Minimalist AirLink Geometric Logo
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    colors.primary.copy(alpha = if (colors.isDark) 0.25f else 0.15f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(56.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Outer beacon loop
                        drawCircle(
                            color = colors.primary.copy(alpha = 0.2f),
                            radius = size.minDimension * 0.46f,
                            center = center,
                            style = Stroke(width = 2f)
                        )

                        // Left Emerald Arc
                        drawArc(
                            color = colors.primary,
                            startAngle = 135f,
                            sweepAngle = 220f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.12f, size.height * 0.18f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height * 0.64f),
                            style = Stroke(width = 7f, cap = StrokeCap.Round)
                        )

                        // Right Cyan Arc (interlocking link)
                        drawArc(
                            color = colors.cyan,
                            startAngle = 315f,
                            sweepAngle = 220f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.36f, size.height * 0.18f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height * 0.64f),
                            style = Stroke(width = 7f, cap = StrokeCap.Round)
                        )

                        // Central core link dot
                        drawCircle(
                            color = if (colors.isDark) Color.White else colors.textPrimary,
                            radius = 5.5f,
                            center = center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "AirLink",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Decentralized offline calls & photo sharing.\nZero internet, zero servers, zero SIM.",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Name field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "What should nearby peers call you?",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            if (it.length <= 24) {
                                name = it
                                showError = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("e.g. Tejas, Alex…", color = colors.textMuted)
                        },
                        isError = showError,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (name.trim().length >= 2) onConfirm(name.trim())
                                else showError = true
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.cardBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.primary,
                            errorBorderColor = colors.red,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface
                        )
                    )
                    if (showError) {
                        Text(
                            text = "Please enter at least 2 characters",
                            color = colors.red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (name.trim().length >= 2) onConfirm(name.trim())
                        else showError = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Stored cryptographically on this device.\nNo accounts, telemetry, or external servers.",
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
