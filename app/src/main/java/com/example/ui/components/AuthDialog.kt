package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.DramaGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onLogin: (name: String, email: String) -> Unit,
    onGoogleLogin: () -> Unit = {},
    onFacebookLogin: () -> Unit = {}
) {
    var isGoogleCustom by remember { mutableStateOf(false) }
    var googleName by remember { mutableStateOf("Usuário Google") }
    var googleEmail by remember { mutableStateOf("usuario@gmail.com") }

    var isEmailMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = DramaGold, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGoogleCustom) "Login Conta Google" else "Conectar Conta",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                if (isGoogleCustom) {
                    Text(
                        text = "Confirme seu e-mail e nome da Conta Google:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = googleName,
                        onValueChange = { googleName = it },
                        label = { Text("Nome da Conta Google") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = googleEmail,
                        onValueChange = { googleEmail = it },
                        label = { Text("E-mail Google (@gmail.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onLogin(googleName.ifBlank { "Usuário Google" }, googleEmail.ifBlank { "usuario@gmail.com" })
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Confirmar e Entrar com Google", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else if (!isEmailMode) {
                    Text(
                        text = "Conecte sua conta para sincronizar favoritos, histórico e assistir novelas em qualquer aparelho.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Botão de Login com Google
                    Button(
                        onClick = {
                            onGoogleLogin()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F1F1F)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF4285F4)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continuar com o Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opção para personalizar dados Google
                    TextButton(
                        onClick = { isGoogleCustom = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Personalizar dados da Conta Google", color = DramaGold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botão de Login com Facebook
                    Button(
                        onClick = {
                            onFacebookLogin()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "f",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continuar com Facebook",
                                fontWeight = FontWeight.SemiBold,
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
                            text = " ou com e-mail ",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF333333))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { isEmailMode = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, Color(0xFF3B4354)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Usar outro E-mail", color = TextPrimary, fontSize = 13.sp)
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (email.isNotBlank()) {
                                onLogin(name.ifBlank { "Usuário Litoral" }, email)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        enabled = email.isNotBlank()
                    ) {
                        Text("Entrar com E-mail")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = DarkSurfaceElevated
    )
}
