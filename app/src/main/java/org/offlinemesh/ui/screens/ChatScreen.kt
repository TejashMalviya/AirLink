package org.offlinemesh.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.offlinemesh.chat.ChatMessage
import org.offlinemesh.mesh.MeshNeighbor
import org.offlinemesh.transport.MeshPacket
import org.offlinemesh.ui.theme.LocalAirLinkColors
import org.offlinemesh.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    peers: List<MeshNeighbor>,
    onSendMessage: (recipientDeviceId: String, text: String) -> Unit,
    onSendImage: (recipientDeviceId: String, base64Image: String) -> Unit
) {
    val colors = LocalAirLinkColors.current
    val context = LocalContext.current

    var textInput by remember { mutableStateOf("") }
    var viewingImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val defaultRecipient = if (peers.isNotEmpty()) peers.first().deviceId else MeshPacket.BROADCAST_DESTINATION
    val recipientName = if (peers.isNotEmpty()) peers.first().displayName else "AirLink Mesh Broadcast"

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImageUtils.compressUriToBase64(context, uri, maxDimension = 512)
            if (base64 != null) {
                onSendImage(defaultRecipient, base64)
            }
        }
    }

    // Full-screen image lightbox dialog
    if (viewingImageBitmap != null) {
        Dialog(onDismissRequest = { viewingImageBitmap = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { viewingImageBitmap = null },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = viewingImageBitmap!!.asImageBitmap(),
                    contentDescription = "Full photo",
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { viewingImageBitmap = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Recipient Header Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(colors.primaryDark, colors.primary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = recipientName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipientName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (peers.isEmpty()) "AirLink Mesh Broadcast" else "P2P Multi-Hop Encrypted",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted",
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "No Messages or Photos Yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "Send encrypted offline text or photos across the mesh.\nPackets hop phone-to-phone with zero internet.",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    ModernChatBubble(
                        message = msg,
                        onViewImage = { bmp -> viewingImageBitmap = bmp }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Floating Modern Message Input Bar with Photo Picker
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = colors.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo Attachment Button
                Surface(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    shape = CircleShape,
                    color = colors.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach Photo",
                            tint = colors.cyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Text Input
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Offline message…", color = colors.textMuted, fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(defaultRecipient, textInput.trim())
                                textInput = ""
                            }
                        }
                    )
                )

                // Send Button
                Surface(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(defaultRecipient, textInput.trim())
                            textInput = ""
                        }
                    },
                    shape = CircleShape,
                    color = if (textInput.isNotBlank()) colors.primary else colors.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (textInput.isNotBlank()) Color.White else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernChatBubble(
    message: ChatMessage,
    onViewImage: (Bitmap) -> Unit
) {
    val colors = LocalAirLinkColors.current
    val isMe = message.isMe
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))

    val bitmap = remember(message.imageBase64) {
        message.imageBase64?.let { ImageUtils.base64ToBitmap(it) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (!isMe) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.cyan,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isMe) 18.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isMe) Brush.linearGradient(listOf(colors.primaryDark, colors.primary))
                        else Brush.linearGradient(listOf(colors.surfaceElevated, colors.surface))
                    )
                    .border(
                        1.dp,
                        if (isMe) Color.Transparent else colors.cardBorder,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isMe) 18.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 18.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    // Photo Thumbnail (if photo message)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Attached photo",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onViewImage(bitmap) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Caption / Text
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = if (isMe) Color.White else colors.textPrimary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    // Timestamp and checkmark
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeStr,
                            fontSize = 10.sp,
                            color = if (isMe) Color.White.copy(alpha = 0.7f) else colors.textMuted
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
