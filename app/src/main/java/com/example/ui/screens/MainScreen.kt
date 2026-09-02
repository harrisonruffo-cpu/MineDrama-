package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Drama
import com.example.data.model.Friend
import com.example.ui.components.AuthDialog
import com.example.ui.components.EpisodesBottomSheet
import com.example.ui.components.VerticalEpisodePlayer
import com.example.ui.theme.*
import com.example.ui.viewmodel.DramaViewModel
import com.example.ui.viewmodel.VideoUploadViewModel

@Composable
fun MainScreen(
    dramaViewModel: DramaViewModel,
    uploadViewModel: VideoUploadViewModel
) {
    val context = LocalContext.current
    var currentNavIndex by remember { mutableIntStateOf(0) }
    var selectedDramaForDetail by remember { mutableStateOf<Drama?>(null) }
    var activeChatFriend by remember { mutableStateOf<Friend?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showEpisodesSheet by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val allDramas by dramaViewModel.allDramas.collectAsState()
    val currentDrama by dramaViewModel.currentDrama.collectAsState()
    val currentEpisodeIndex by dramaViewModel.currentEpisodeIndex.collectAsState()
    val favoriteDramaIds by dramaViewModel.favoriteDramaIds.collectAsState()
    val currentUser by dramaViewModel.currentUser.collectAsState()
    val donoDoMorroManager = remember { com.example.data.util.DonoDoMorroManager(context.applicationContext) }

    val favoriteDramas = remember(allDramas, favoriteIdsMap(favoriteDramaIds)) {
        allDramas.filter { favoriteDramaIds.contains(it.id) }
    }

    // Gerenciador do Botão Voltar do Android
    BackHandler {
        when {
            showExitConfirmDialog -> {
                showExitConfirmDialog = false
            }
            showAuthDialog -> {
                showAuthDialog = false
            }
            showEpisodesSheet -> {
                showEpisodesSheet = false
            }
            activeChatFriend != null -> {
                activeChatFriend = null
            }
            selectedDramaForDetail != null -> {
                selectedDramaForDetail = null
            }
            currentNavIndex != 0 -> {
                currentNavIndex = 0
            }
            else -> {
                showExitConfirmDialog = true
            }
        }
    }

    // Modal de Confirmação para Sair do App
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.ExitToApp,
                    contentDescription = null,
                    tint = DramaCrimsonBright,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Sair do Litoral Novelas?",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tem certeza que deseja fechar o aplicativo?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sim, Sair", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitConfirmDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Continuar Assistindo", color = TextPrimary)
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = DarkSurfaceElevated
        )
    }

    if (currentUser == null) {
        LoginGateScreen(
            googleAuthHelper = dramaViewModel.googleAuthHelper,
            onGoogleAccountLogin = { dramaViewModel.loginWithGoogleAccount(it) },
            onFacebookSuccess = { name, email, avatar -> dramaViewModel.loginWithFacebook(name, email, avatar) },
            onEmailLogin = { name, email -> dramaViewModel.login(name, email) },
            onGuestLogin = { dramaViewModel.loginAsGuest() }
        )
        return
    }

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onLogin = { name, email ->
                dramaViewModel.login(name, email)
                showAuthDialog = false
            },
            onGoogleAccountLogin = { account ->
                dramaViewModel.loginWithGoogleAccount(account)
                showAuthDialog = false
            },
            onFacebookSuccess = { name, email, avatar ->
                dramaViewModel.loginWithFacebook(name, email, avatar)
                showAuthDialog = false
            },
            googleAuthHelper = dramaViewModel.googleAuthHelper
        )
    }

    if (showEpisodesSheet && currentDrama != null) {
        EpisodesBottomSheet(
            dramaTitle = currentDrama!!.title,
            episodes = currentDrama!!.episodes,
            currentEpisodeIndex = currentEpisodeIndex,
            onSelectEpisode = { idx ->
                dramaViewModel.selectEpisode(idx)
            },
            onDismiss = { showEpisodesSheet = false }
        )
    }

    Scaffold(
        bottomBar = {
            if (selectedDramaForDetail == null && activeChatFriend == null) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = DramaCrimson
                ) {
                    NavigationBarItem(
                        selected = currentNavIndex == 0,
                        onClick = { currentNavIndex = 0 },
                        icon = { Icon(Icons.Filled.Whatshot, contentDescription = "Dono Do Morro") },
                        label = { Text("Dono Do Morro", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 1,
                        onClick = { currentNavIndex = 1 },
                        icon = { Icon(Icons.Filled.PlayCircle, contentDescription = "Para Você") },
                        label = { Text("Para Você", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 2,
                        onClick = { currentNavIndex = 2 },
                        icon = { Icon(Icons.Filled.Tv, contentDescription = "Novelas") },
                        label = { Text("Novelas", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 3,
                        onClick = { currentNavIndex = 3 },
                        icon = { Icon(Icons.Filled.CloudUpload, contentDescription = "Publicar") },
                        label = { Text("Publicar", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 4,
                        onClick = { currentNavIndex = 4 },
                        icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil") },
                        label = { Text("Perfil", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (selectedDramaForDetail == null && activeChatFriend == null) innerPadding else PaddingValues())
        ) {
            when {
                activeChatFriend != null -> {
                    ChatDetailScreen(
                        friend = activeChatFriend!!,
                        socialManager = dramaViewModel.socialManager,
                        favoriteDramas = favoriteDramas,
                        onBack = { activeChatFriend = null },
                        onOpenDrama = { drama ->
                            activeChatFriend = null
                            dramaViewModel.selectDrama(drama, 0)
                            currentNavIndex = 0
                        }
                    )
                }
                selectedDramaForDetail != null -> {
                    DramaDetailScreen(
                        drama = selectedDramaForDetail!!,
                        viewModel = dramaViewModel,
                        onBack = { selectedDramaForDetail = null },
                        onPlayEpisode = { epIdx ->
                            dramaViewModel.selectDrama(selectedDramaForDetail!!, epIdx)
                            selectedDramaForDetail = null
                            currentNavIndex = 1
                        }
                    )
                }
                else -> {
                    when (currentNavIndex) {
                        0 -> {
                            // ABA EXCLUSIVA DE NOVELA PRONTA PRA ASSISTIR: DONO DO MORRO
                            DonoDoMorroScreen(
                                donoDoMorroManager = donoDoMorroManager,
                                onPlayInFeed = { epIdx ->
                                    val morroDrama = donoDoMorroManager.getDrama()
                                    dramaViewModel.selectDrama(morroDrama, epIdx)
                                    currentNavIndex = 1
                                }
                            )
                        }
                        1 -> {
                            // FEED VERTICAL (Estilo TikTok / Reels)
                            if (allDramas.isNotEmpty()) {
                                val activeDrama = currentDrama ?: allDramas.first()
                                val episodes = activeDrama.episodes
                                if (episodes.isNotEmpty()) {
                                    val currentEp = episodes.getOrNull(currentEpisodeIndex) ?: episodes.first()
                                    VerticalEpisodePlayer(
                                        drama = activeDrama,
                                        episode = currentEp,
                                        isPlaying = true,
                                        isLiked = favoriteDramaIds.contains(activeDrama.id),
                                        onToggleLike = { dramaViewModel.toggleFavorite(activeDrama.id) },
                                        onOpenEpisodesSheet = { showEpisodesSheet = true },
                                        onNextEpisode = { dramaViewModel.nextEpisode() }
                                    )
                                }
                            }
                        }
                        2 -> NovelasCatalogScreen(
                            viewModel = dramaViewModel,
                            onDramaSelected = { drama -> selectedDramaForDetail = drama }
                        )
                        3 -> PublishDramaScreen(
                            viewModel = dramaViewModel,
                            uploadViewModel = uploadViewModel
                        )
                        4 -> ProfileScreen(
                            viewModel = dramaViewModel,
                            socialManager = dramaViewModel.socialManager,
                            onDramaSelected = { drama -> selectedDramaForDetail = drama },
                            onOpenChatWithFriend = { friend -> activeChatFriend = friend }
                        )
                    }
                }
            }
        }
    }
}

private fun favoriteIdsMap(ids: Set<String>): Any = ids
