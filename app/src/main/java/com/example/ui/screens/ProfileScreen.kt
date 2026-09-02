package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.auth.AuthManager
import com.example.data.model.Drama
import com.example.data.model.Friend
import com.example.data.social.SocialManager
import com.example.data.util.YouTubeHelper
import com.example.ui.components.DramaCard
import com.example.ui.components.ImageCropperDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.DramaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: DramaViewModel,
    socialManager: SocialManager,
    onDramaSelected: (Drama) -> Unit,
    onOpenChatWithFriend: (Friend) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allDramas by viewModel.allDramas.collectAsState()
    val favoriteIds by viewModel.favoriteDramaIds.collectAsState()
    val friends by socialManager.friends.collectAsState()
    val messagesMap by socialManager.messagesByFriend.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showImageCropperDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    var editedName by remember { mutableStateOf(currentUser?.name ?: "Usuário") }
    var userAvatarUrl by remember { mutableStateOf(currentUser?.avatarUrl ?: "") }

    val favoriteDramas = remember(allDramas, favoriteIds) {
        allDramas.filter { favoriteIds.contains(it.id) }
    }

    val myPublishedDramas = remember(allDramas) {
        allDramas.filter { it.isPublishedLocally }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            editedName = currentUser!!.name
            userAvatarUrl = currentUser!!.avatarUrl
        }
    }

    // Modal de Recorte e Edição de Foto de Perfil
    if (showImageCropperDialog) {
        ImageCropperDialog(
            currentAvatarUrl = userAvatarUrl,
            onDismiss = { showImageCropperDialog = false },
            onAvatarCropped = { newAvatar ->
                userAvatarUrl = newAvatar
                viewModel.updateUserProfile(editedName, newAvatar)
            }
        )
    }

    // Modal de Edição de Nome e Perfil
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(editedName) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text("Editar Perfil", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Altere seu nome de exibição no Litoral Novelas:", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Nome Completo") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DramaCrimson,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            editedName = tempName
                            viewModel.updateUserProfile(tempName, userAvatarUrl)
                        }
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Modal Adicionar Amigo
    if (showAddFriendDialog) {
        var friendName by remember { mutableStateOf("") }
        var friendHandle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = {
                Text("Adicionar Novo Amigo", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Digite o nome ou @usuário do seu amigo:", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = friendName,
                        onValueChange = { friendName = it },
                        label = { Text("Nome do Amigo") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DramaCrimson
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = friendHandle,
                        onValueChange = { friendHandle = it },
                        label = { Text("@usuario (ex: @carol_novelas)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DramaCrimson
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (friendName.isNotBlank()) {
                            socialManager.addFriend(friendName, friendHandle.ifBlank { "@amigo" })
                            showAddFriendDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Adicionar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val isAdmin = currentUser?.email?.equals(AuthManager.ADMIN_EMAIL, ignoreCase = true) == true || currentUser?.isAdmin == true
        val effectiveAvatarUrl = if (isAdmin && (userAvatarUrl.isBlank() || userAvatarUrl.contains("unsplash"))) {
            AuthManager.ADMIN_AVATAR
        } else {
            YouTubeHelper.normalizeAvatarUrl(userAvatarUrl.ifBlank { if (isAdmin) AuthManager.ADMIN_AVATAR else "" })
        }
        val totalFollowers = currentUser?.followersCount ?: 28450

        // CABEÇALHO DO PERFIL
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Foto com Moldura Especial e Botão de Edição/Recorte
                    Box(
                        modifier = Modifier.clickable { showImageCropperDialog = true }
                    ) {
                        if (effectiveAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = effectiveAvatarUrl,
                                contentDescription = editedName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isAdmin) 3.dp else 2.5.dp,
                                        brush = if (isAdmin) {
                                            Brush.linearGradient(listOf(DramaGold, DramaCrimsonBright, DramaGold))
                                        } else {
                                            Brush.linearGradient(listOf(DramaCrimsonBright, DramaCrimson))
                                        },
                                        shape = CircleShape
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(DramaCrimson),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = editedName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Badge de Coroa para ADM Oficial
                        if (isAdmin) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(DramaGold)
                                    .border(1.5.dp, DarkSurfaceElevated, CircleShape)
                                    .align(Alignment.TopStart),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👑", fontSize = 14.sp)
                            }
                        }

                        // Badge de Câmera / Recorte
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isAdmin) DramaGold else DramaCrimson)
                                .border(1.5.dp, DarkSurfaceElevated, CircleShape)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "Trocar Foto",
                                tint = if (isAdmin) Color.Black else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = editedName,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            if (isAdmin) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verificado",
                                    tint = DramaGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    color = DramaGold.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DramaGold),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ADM",
                                        color = DramaGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar Nome", tint = DramaCrimsonBright, modifier = Modifier.size(15.dp))
                            }
                        }

                        Text(
                            text = currentUser?.email ?: AuthManager.ADMIN_EMAIL,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Badges Oficiais
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isAdmin) {
                                Surface(
                                    color = Color(0xFFB71C1C),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "👑 ADM Oficial",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFF0D47A1),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "💻 Dev",
                                        color = Color(0xFF90CAF9),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFF57F17),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "🪙 9.999 Moedas",
                                        color = Color(0xFFFFF9C4),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = Color(0xFF1B5E20),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "VIP Nuvem",
                                        color = Color(0xFFA5D6A7),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFF3E2723),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "🪙 300 Moedas",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sair", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CARDS DE ESTATÍSTICAS (SEGUIDORES AUTOMÁTICOS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${totalFollowers / 1000}.${(totalFollowers % 1000) / 100}K",
                                color = DramaGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Seguidores",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isAdmin) "Todos do App ✅" else "Seguindo ADM ✅",
                                color = if (isAdmin) Color(0xFF81C784) else DramaCrimsonBright,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "1",
                                color = DramaCrimsonBright,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Seguindo",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Comunidade",
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "1",
                                color = Color(0xFF64B5F6),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Novelas",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Dono Do Morro",
                                color = Color(0xFF90CAF9),
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Banner Informativo de Administrador / Seguidor
                Surface(
                    color = if (isAdmin) DramaGold.copy(alpha = 0.12f) else DramaCrimson.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAdmin) DramaGold.copy(alpha = 0.4f) else DramaCrimson.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Filled.Security else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isAdmin) DramaGold else DramaCrimsonBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isAdmin) {
                                    "Perfil Principal • Desenvolvedor & ADM Oficial"
                                } else {
                                    "Seguidor Oficial de Harrison Ruffo (ADM Oficial)"
                                },
                                color = if (isAdmin) DramaGold else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAdmin) {
                                    "Todos que entram no aplicativo viram automaticamente seus seguidores."
                                } else {
                                    "Você segue o criador oficial e tem acesso a conteúdos e novidades exclusivas."
                                },
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // TAB BAR DO PERFIL
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = DarkSurface,
            contentColor = DramaCrimsonBright,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = DramaCrimsonBright
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Favoritas (${favoriteDramas.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Amigos (${friends.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Mensagens", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 3,
                onClick = { selectedTabIndex = 3 },
                text = { Text("Publicadas (${myPublishedDramas.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // CONTEÚDO DA ABA SELECIONADA
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (selectedTabIndex) {
                // 1. NOVELAS FAVORITAS
                0 -> {
                    if (favoriteDramas.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Você ainda não favoritou nenhuma novela.", color = TextSecondary, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Toque no coração ao assistir para salvar aqui!", color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(favoriteDramas) { drama ->
                                DramaCard(
                                    drama = drama,
                                    onClick = { onDramaSelected(drama) }
                                )
                            }
                        }
                    }
                }

                // 2. AMIGOS
                1 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Amigos na Comunidade",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showAddFriendDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adicionar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(friends) { friend ->
                                val isFriendAdmin = friend.id == "admin_harrison_ruffo" || friend.handle.contains("harrison", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isFriendAdmin) Color(0xFF1E1B15) else DarkSurfaceElevated,
                                    border = if (isFriendAdmin) androidx.compose.foundation.BorderStroke(1.5.dp, DramaGold) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenChatWithFriend(friend) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box {
                                            AsyncImage(
                                                model = if (isFriendAdmin) AuthManager.ADMIN_AVATAR else friend.avatarUrl,
                                                contentDescription = friend.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        width = if (isFriendAdmin) 2.dp else 0.dp,
                                                        color = if (isFriendAdmin) DramaGold else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                            )
                                            if (isFriendAdmin) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(DramaGold)
                                                        .align(Alignment.TopStart),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("👑", fontSize = 9.sp)
                                                }
                                            }
                                            if (friend.isOnline) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF25D366))
                                                        .border(1.5.dp, DarkSurfaceElevated, CircleShape)
                                                        .align(Alignment.BottomEnd)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(friend.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                if (isFriendAdmin) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Filled.Verified, contentDescription = "Oficial", tint = DramaGold, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(color = Color(0xFFB71C1C), shape = RoundedCornerShape(3.dp)) {
                                                        Text("ADM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            Text(friend.handle, color = if (isFriendAdmin) DramaGold else DramaCrimsonBright, fontSize = 11.sp)
                                            if (isFriendAdmin) {
                                                Text("💻 Desenvolvedor Oficial • Você o segue ✅", color = Color(0xFFA5D6A7), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            } else if (friend.currentWatching != null) {
                                                Text("Assistindo: ${friend.currentWatching}", color = Color(0xFFA5D6A7), fontSize = 11.sp)
                                            } else {
                                                Text(friend.status, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                                            }
                                        }

                                        IconButton(
                                            onClick = { onOpenChatWithFriend(friend) },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = if (isFriendAdmin) DramaGold else Color(0xFF005C4B))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Chat,
                                                contentDescription = "Conversar",
                                                tint = if (isFriendAdmin) Color.Black else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. MENSAGENS (ESTILO WHATSAPP)
                2 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Conversas Recentes",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(friends) { friend ->
                                val messages = messagesMap[friend.id].orEmpty()
                                val lastMsg = messages.lastOrNull()?.text ?: "Inicie uma conversa..."

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1F2C34), // Estilo WhatsApp Dark Card
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenChatWithFriend(friend) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box {
                                            AsyncImage(
                                                model = friend.avatarUrl,
                                                contentDescription = friend.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                            )
                                            if (friend.isOnline) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF25D366))
                                                        .align(Alignment.BottomEnd)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(friend.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    text = if (friend.isOnline) "online" else "há pouco",
                                                    color = if (friend.isOnline) Color(0xFF25D366) else Color(0xFF8696A0),
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = lastMsg,
                                                color = Color(0xFF8696A0),
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. MINHAS PUBLICAÇÕES
                3 -> {
                    if (myPublishedDramas.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Você ainda não publicou nenhuma novela.", color = TextSecondary, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Vá na aba 'Publicar' para adicionar seus vídeos e novelas!", color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(myPublishedDramas) { drama ->
                                DramaCard(
                                    drama = drama,
                                    onClick = { onDramaSelected(drama) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
