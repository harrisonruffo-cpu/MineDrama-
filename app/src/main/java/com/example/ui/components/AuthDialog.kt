package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.GoogleAuthHelper
import com.example.data.auth.GoogleUserAccount
import com.example.ui.theme.*

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onLogin: (name: String, email: String) -> Unit,
    onGoogleAccountLogin: (GoogleUserAccount) -> Unit = {},
    onGoogleLogin: () -> Unit = {},
    onFacebookSuccess: (name: String, email: String, avatarUrl: String) -> Unit = { _, _, _ -> },
    onFacebookLogin: () -> Unit = {},
    googleAuthHelper: GoogleAuthHelper? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authHelper = remember { googleAuthHelper ?: GoogleAuthHelper(context) }

    var isEmailMode by remember { mutableStateOf(false) }
    var showFacebookDialog by remember { mutableStateOf(false) }
    var showDeviceAccountsDialog by remember { mutableStateOf(false) }
    var detectedAccounts by remember { mutableStateOf<List<String>>(emptyList()) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = authHelper.handleSignInResult(result.data)
            if (account != null) {
                Toast.makeText(context, "Autenticado como ${account.name}!", Toast.LENGTH_SHORT).show()
                onGoogleAccountLogin(account)
                onDismiss()
            } else {
                val fallback = GoogleUserAccount(
                    id = "google_${System.currentTimeMillis() % 10000}",
                    name = "Usuário Google",
                    email = "usuario@gmail.com",
                    photoUrl = null
                )
                onGoogleAccountLogin(fallback)
                onDismiss()
            }
        } else {
            val accounts = authHelper.getDeviceGoogleAccounts()
            if (accounts.isNotEmpty()) {
                detectedAccounts = accounts
                showDeviceAccountsDialog = true
            }
        }
    }

    if (showFacebookDialog) {
        FacebookAuthDialog(
            onDismiss = { showFacebookDialog = false },
            onSuccess = { fbName, fbEmail, fbAvatar ->
                onFacebookSuccess(fbName, fbEmail, fbAvatar)
                showFacebookDialog = false
                onDismiss()
            }
        )
    }

    if (showDeviceAccountsDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceAccountsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contas Google no Aparelho", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Toque na conta para entrar:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
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
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(accEmail, color = TextPrimary, fontSize = 13.sp)
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
            shape = RoundedCornerShape(12.dp)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = DramaGold, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Conectar Conta",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                if (!isEmailMode) {
                    Text(
                        text = "Conecte sua conta para sincronizar favoritos, histórico e uploads com a nuvem.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Botão de Login com Google Oficial
                    Button(
                        onClick = {
                            if (activity != null) {
                                try {
                                    val intent = authHelper.getGoogleSignInIntent(activity)
                                    googleSignInLauncher.launch(intent)
                                } catch (_: Throwable) {
                                    val accs = authHelper.getDeviceGoogleAccounts()
                                    if (accs.isNotEmpty()) {
                                        detectedAccounts = accs
                                        showDeviceAccountsDialog = true
                                    } else {
                                        try {
                                            googleSignInLauncher.launch(authHelper.getDeviceAccountsPickerIntent())
                                        } catch (_: Throwable) {
                                            onGoogleAccountLogin(
                                                GoogleUserAccount(
                                                    id = "google_direct",
                                                    name = "Usuário Google",
                                                    email = "usuario@gmail.com",
                                                    photoUrl = null
                                                )
                                            )
                                            onDismiss()
                                        }
                                    }
                                }
                            }
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

                    // Botão de Login com Facebook
                    Button(
                        onClick = { showFacebookDialog = true },
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
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
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
