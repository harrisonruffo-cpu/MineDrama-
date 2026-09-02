package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.Drama
import com.example.data.model.Friend
import com.example.data.social.SocialManager
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    friend: Friend,
    socialManager: SocialManager,
    favoriteDramas: List<Drama>,
    onBack: () -> Unit,
    onOpenDrama: (Drama) -> Unit
) {
    val messagesMap by socialManager.messagesByFriend.collectAsState()
    val messages = messagesMap[friend.id].orEmpty()
    var inputText by remember { mutableStateOf("") }
    var showShareDramaDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showShareDramaDialog) {
        AlertDialog(
            onDismissRequest = { showShareDramaDialog = false },
            title = {
                Text("Recomendar Novela para ${friend.name}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                if (favoriteDramas.isEmpty()) {
                    Text("Você ainda não favoritou nenhuma novela para recomendar.", color = TextSecondary, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        items(favoriteDramas) { drama ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        socialManager.sendMessage(
                                            friendId = friend.id,
                                            text = "Recomendo muito assistir essa novela!",
                                            sharedDramaId = drama.id,
                                            sharedDramaTitle = drama.title,
                                            sharedDramaCover = drama.coverUrl
                                        )
                                        showShareDramaDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = drama.coverUrl,
                                        contentDescription = drama.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(drama.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${drama.genre} • ${drama.totalEpisodes} eps", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareDramaDialog = false }) {
                    Text("Fechar", color = DramaCrimsonBright)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    Scaffold(
        topBar = {
            // Header Estilo WhatsApp com Foto, Nome e Status
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            AsyncImage(
                                model = friend.avatarUrl,
                                contentDescription = friend.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                            )
                            if (friend.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366))
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = friend.name,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = if (friend.isOnline) "online" else friend.lastSeen,
                                color = if (friend.isOnline) Color(0xFF25D366) else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDramaDialog = true }) {
                        Icon(Icons.Filled.Tv, contentDescription = "Recomendar Novela", tint = DramaCrimsonBright)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1F2C34))
            )
        },
        bottomBar = {
            // Barra Inferior Estilo WhatsApp para Envio de Mensagem
            Surface(
                color = Color(0xFF1F2C34),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showShareDramaDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Anexar Novela", tint = Color(0xFF8696A0))
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Mensagem", color = Color(0xFF8696A0), fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A3942),
                            unfocusedContainerColor = Color(0xFF2A3942),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 100.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val text = inputText
                                inputText = ""
                                socialManager.sendMessage(friend.id, text)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) Color(0xFF00A884) else Color(0xFF005C4B))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0B141A) // Cor de fundo clássica WhatsApp Dark
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                // Notificação de Criptografia e Privacidade Estilo WhatsApp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xFF182229),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFFD279), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "As mensagens são protegidas na comunidade Litoral Novelas.",
                                color = Color(0xFFFFD279),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            items(messages) { message ->
                ChatBubble(
                    message = message,
                    timeFormatter = timeFormatter,
                    onOpenDrama = { dramaId ->
                        val found = favoriteDramas.find { it.id == dramaId }
                        if (found != null) {
                            onOpenDrama(found)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    timeFormatter: SimpleDateFormat,
    onOpenDrama: (String) -> Unit
) {
    val isMe = message.isFromMe

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isMe) 12.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 12.dp
            ),
            color = if (isMe) Color(0xFF005C4B) else Color(0xFF202C33),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                // Card de Novela Compartilhada
                if (message.sharedDramaId != null && message.sharedDramaTitle != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable { onOpenDrama(message.sharedDramaId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!message.sharedDramaCover.isNullOrBlank()) {
                                AsyncImage(
                                    model = message.sharedDramaCover,
                                    contentDescription = message.sharedDramaTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎬 ${message.sharedDramaTitle}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Toque para Assistir Junto",
                                    color = DramaCrimsonBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = message.text,
                    color = Color(0xFFE9EDEF),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatter.format(Date(message.timestamp)),
                        color = Color(0xFF8696A0),
                        fontSize = 10.sp
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Lido",
                            tint = Color(0xFF53BDEB),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
