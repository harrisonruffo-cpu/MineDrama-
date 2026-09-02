package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun LoginGateScreen(
    onGoogleLogin: () -> Unit,
    onFacebookLogin: () -> Unit,
    onEmailLogin: (name: String, email: String) -> Unit,
    onGuestLogin: () -> Unit
) {
    var isEmailMode by remember { mutableStateOf(false) }
    var showGoogleAccountDialog by remember { mutableStateOf(false) }
    var customGoogleName by remember { mutableStateOf("Usuário Google") }
    var customGoogleEmail by remember { mutableStateOf("usuario@gmail.com") }

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    if (showGoogleAccountDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conta Google", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Confirme suas credenciais para login com a Conta Google:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = customGoogleName,
                        onValueChange = { customGoogleName = it },
                        label = { Text("Nome da Conta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customGoogleEmail,
                        onValueChange = { customGoogleEmail = it },
                        label = { Text("E-mail Google") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEmailLogin(
                            customGoogleName.ifBlank { "Usuário Google" },
                            customGoogleEmail.ifBlank { "usuario@gmail.com" }
                        )
                        showGoogleAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Text("Entrar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleAccountDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Fundo decorativo com gradiente atmosférico
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
                        // 1. Botão Google
                        Button(
                            onClick = onGoogleLogin,
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

                        // Opção de trocar conta google
                        TextButton(
                            onClick = { showGoogleAccountDialog = true },
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text("Informar outro e-mail @gmail.com", color = DramaGold, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 2. Botão Facebook
                        Button(
                            onClick = onFacebookLogin,
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
                            Text("Entrar com E-mail", color = TextPrimary, fontWeight = FontWeight.SemiBold)
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
