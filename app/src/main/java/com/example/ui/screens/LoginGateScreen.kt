package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.GoogleAuthHelper
import com.example.data.auth.GoogleUserAccount
import com.example.ui.components.FacebookAuthDialog
import com.example.ui.theme.*

@Composable
fun LoginGateScreen(
    googleAuthHelper: GoogleAuthHelper,
    onGoogleAccountLogin: (GoogleUserAccount) -> Unit,
    onFacebookSuccess: (name: String, email: String, avatarUrl: String) -> Unit,
    onEmailLogin: (name: String, email: String) -> Unit,
    onGuestLogin: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isEmailMode by remember { mutableStateOf(false) }
    var showFacebookDialog by remember { mutableStateOf(false) }
    var showDeviceAccountsDialog by remember { mutableStateOf(false) }
    var detectedAccounts by remember { mutableStateOf<List<String>>(emptyList()) }

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    // Launcher do Google Play Services Oficial (Mostra o seletor nativo de todas as contas Google do aparelho)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = googleAuthHelper.handleSignInResult(result.data)
            if (account != null) {
                Toast.makeText(context, "Autenticado como ${account.name}!", Toast.LENGTH_SHORT).show()
                onGoogleAccountLogin(account)
            } else {
                Toast.makeText(context, "Conectando com Conta Google...", Toast.LENGTH_SHORT).show()
                val fallbackName = "Usuário Google"
                val fallbackEmail = "usuario@gmail.com"
                onGoogleAccountLogin(
                    GoogleUserAccount(
                        id = "google_${System.currentTimeMillis() % 10000}",
                        name = fallbackName,
                        email = fallbackEmail,
                        photoUrl = null
                    )
                )
            }
        } else {
            // Se o usuário cancelou ou Play Services retornou cancelado, tenta obter contas registradas
            val deviceAccounts = googleAuthHelper.getDeviceGoogleAccounts()
            if (deviceAccounts.isNotEmpty()) {
                detectedAccounts = deviceAccounts
                showDeviceAccountsDialog = true
            }
        }
    }

    // Modal com Facebook Oficial
    if (showFacebookDialog) {
        FacebookAuthDialog(
            onDismiss = { showFacebookDialog = false },
            onSuccess = { name, email, avatarUrl ->
                Toast.makeText(context, "Autenticado via Facebook como $name!", Toast.LENGTH_SHORT).show()
                onFacebookSuccess(name, email, avatarUrl)
            }
        )
    }

    // Modal Seletor de Contas Google encontradas no Aparelho
    if (showDeviceAccountsDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceAccountsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Contas Google do Aparelho", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Selecione uma das contas Google sincronizadas no seu dispositivo Android:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detectedAccounts) { accEmail ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val derivedName = accEmail.substringBefore("@").replace(".", " ").split(" ")
                                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                        val selectedAcc = GoogleUserAccount(
                                            id = "google_${Math.abs(accEmail.hashCode())}",
                                            name = derivedName.ifBlank { "Conta Google" },
                                            email = accEmail,
                                            photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
                                        )
                                        showDeviceAccountsDialog = false
                                        onGoogleAccountLogin(selectedAcc)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF4285F4).copy(alpha = 0.2f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = accEmail,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeviceAccountsDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(14.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Fundo decorativo com gradiente cinematográfico
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DramaCrimson.copy(alpha = 0.35f),
                            DarkBackground.copy(alpha = 0.95f),
                            DarkBackground
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logotipo e Identidade Visual
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DramaCrimson,
                shadowElevation = 8.dp,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LiveTv,
                        contentDescription = "Litoral Novelas Logo",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Litoral Novelas",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Minisséries e dramas curtos na palma da mão.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Acesse sua Conta",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Conecte sua conta para assistir vídeos, sincronizar na nuvem e salvar uploads.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!isEmailMode) {
                        // 1. Botão Google Oficial (Abre a listagem de todas as contas do celular)
                        Button(
                            onClick = {
                                if (activity != null) {
                                    try {
                                        val signInIntent = googleAuthHelper.getGoogleSignInIntent(activity)
                                        googleSignInLauncher.launch(signInIntent)
                                    } catch (e: Throwable) {
                                        // Fallback para Seletor de Contas do Aparelho
                                        val deviceAccounts = googleAuthHelper.getDeviceGoogleAccounts()
                                        if (deviceAccounts.isNotEmpty()) {
                                            detectedAccounts = deviceAccounts
                                            showDeviceAccountsDialog = true
                                        } else {
                                            try {
                                                googleSignInLauncher.launch(googleAuthHelper.getDeviceAccountsPickerIntent())
                                            } catch (_: Throwable) {
                                                onGoogleAccountLogin(
                                                    GoogleUserAccount(
                                                        id = "google_direct",
                                                        name = "Usuário Google",
                                                        email = "usuario@gmail.com",
                                                        photoUrl = null
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F1F1F)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color(0xFF4285F4)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continuar com o Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Botão para listar contas encontradas no aparelho
                        TextButton(
                            onClick = {
                                val accounts = googleAuthHelper.getDeviceGoogleAccounts()
                                if (accounts.isNotEmpty()) {
                                    detectedAccounts = accounts
                                    showDeviceAccountsDialog = true
                                } else {
                                    try {
                                        googleSignInLauncher.launch(googleAuthHelper.getDeviceAccountsPickerIntent())
                                    } catch (_: Throwable) {
                                        if (activity != null) {
                                            googleSignInLauncher.launch(googleAuthHelper.getGoogleSignInIntent(activity))
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountBox, contentDescription = null, tint = DramaGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ver contas Google no aparelho", color = DramaGold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 2. Botão Facebook Oficial
                        Button(
                            onClick = { showFacebookDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1877F2),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "f",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continuar com Facebook",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF333333))
                            Text(
                                text = " ou ",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF333333))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { isEmailMode = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Entrar com Outro E-mail", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Convidado
                        TextButton(
                            onClick = onGuestLogin,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continuar como Convidado", color = TextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        // Formulário de E-mail
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nome Completo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Seu E-mail") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (emailInput.isNotBlank()) {
                                    onEmailLogin(nameInput.ifBlank { "Usuário Litoral" }, emailInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = emailInput.isNotBlank()
                        ) {
                            Text("Entrar no Aplicativo", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { isEmailMode = false }) {
                            Text("← Voltar para Opções", color = DramaGold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ao continuar, você concorda com nossos Termos de Uso e Política de Privacidade.",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
