package com.example.data.auth

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

data class GoogleUserAccount(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?
)

class GoogleAuthHelper(private val context: Context) {
    private val TAG = "GoogleAuthHelper"

    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun getGoogleSignInIntent(activity: Activity): Intent {
        val client = getGoogleSignInClient(activity)
        // Limpa cache de login anterior para sempre mostrar o seletor de contas se solicitado
        client.signOut()
        return client.signInIntent
    }

    fun getDeviceAccountsPickerIntent(): Intent {
        return AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            null,
            null,
            null,
            null
        )
    }

    fun getDeviceGoogleAccounts(): List<String> {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            accounts.map { it.name }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permissão GET_ACCOUNTS não concedida: ${e.message}")
            emptyList()
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao listar contas do dispositivo: ${e.message}")
            emptyList()
        }
    }

    fun handleSignInResult(data: Intent?): GoogleUserAccount? {
        if (data == null) return null
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email ?: "usuario@gmail.com"
            val name = account.displayName ?: email.substringBefore("@").replace(".", " ").capitalize()
            val photoUrl = account.photoUrl?.toString() ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
            val id = account.id ?: "google_${Math.abs(email.hashCode())}"

            Log.d(TAG, "Google Sign-In sucesso oficial: $email, $name, $photoUrl")
            GoogleUserAccount(id = id, name = name, email = email, photoUrl = photoUrl)
        } catch (e: ApiException) {
            Log.w(TAG, "GoogleSignIn ApiException statusCode: ${e.statusCode}, mensagem: ${e.message}")
            // Tenta verificar se o Intent continha retorno do AccountManager
            val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val email = accountName
                val name = email.substringBefore("@").replace(".", " ").split(" ")
                    .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                val photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
                GoogleUserAccount(id = "google_${Math.abs(email.hashCode())}", name = name, email = email, photoUrl = photoUrl)
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Erro inesperado no resultado do Google Sign-In: ${e.message}")
            val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val email = accountName
                val name = email.substringBefore("@").replace(".", " ").split(" ")
                    .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                GoogleUserAccount(id = "google_${Math.abs(email.hashCode())}", name = name, email = email, photoUrl = null)
            } else {
                null
            }
        }
    }
}
