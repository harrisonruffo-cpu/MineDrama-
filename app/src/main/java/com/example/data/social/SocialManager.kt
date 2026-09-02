package com.example.data.social

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.Friend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SocialManager(private val context: Context) {
    private val TAG = "SocialManager"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("litoral_social_prefs", Context.MODE_PRIVATE)

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _messagesByFriend = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messagesByFriend: StateFlow<Map<String, List<ChatMessage>>> = _messagesByFriend.asStateFlow()

    init {
        loadFriends()
        loadMessages()
    }

    private fun loadFriends() {
        val savedJson = prefs.getString("saved_friends", null)
        if (!savedJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<Friend>()
                val arr = JSONArray(savedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Friend(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            handle = obj.optString("handle", "@amigo"),
                            avatarUrl = obj.getString("avatarUrl"),
                            status = obj.optString("status", "Amante de novelas"),
                            isOnline = obj.optBoolean("isOnline", true),
                            lastSeen = obj.optString("lastSeen", "Online"),
                            currentWatching = obj.optString("currentWatching").takeIf { it.isNotBlank() }
                        )
                    )
                }
                _friends.value = list
                return
            } catch (e: Exception) {
                Log.w(TAG, "Erro parse amigos salvos: ${e.message}")
            }
        }

        // Amigos Padrão da Comunidade Litoral Novelas
        val defaultFriends = listOf(
            Friend(
                id = "friend_carol",
                name = "Carolina Lima",
                handle = "@carol_dramas",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80",
                status = "Maratonando O Segredo da Baía! 🍿🌊",
                isOnline = true,
                lastSeen = "Online agora",
                currentWatching = "O Segredo da Baía"
            ),
            Friend(
                id = "friend_lucas",
                name = "Lucas Mendonça",
                handle = "@lucas_m",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80",
                status = "Procurando novelas curtas de romance ✨",
                isOnline = true,
                lastSeen = "Visto por último há 10 min",
                currentWatching = "Amor em Mar Aberto"
            ),
            Friend(
                id = "friend_mariana",
                name = "Mariana Silva",
                handle = "@mari_novelas",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80",
                status = "Criadora de conteúdo no Litoral Novelas 🎬",
                isOnline = false,
                lastSeen = "Hoje às 09:30",
                currentWatching = null
            ),
            Friend(
                id = "friend_gabriel",
                name = "Gabriel Santos",
                handle = "@gabriel_s",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80",
                status = "Sempre pronto para o próximo episódio!",
                isOnline = true,
                lastSeen = "Online agora",
                currentWatching = "O Segredo da Baía"
            )
        )
        _friends.value = defaultFriends
        saveFriends(defaultFriends)
    }

    private fun loadMessages() {
        val savedJson = prefs.getString("saved_messages_map", null)
        if (!savedJson.isNullOrBlank()) {
            try {
                val map = mutableMapOf<String, List<ChatMessage>>()
                val rootObj = JSONObject(savedJson)
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = rootObj.getJSONArray(key)
                    val list = mutableListOf<ChatMessage>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            ChatMessage(
                                id = obj.getString("id"),
                                senderId = obj.getString("senderId"),
                                text = obj.getString("text"),
                                timestamp = obj.getLong("timestamp"),
                                isFromMe = obj.getBoolean("isFromMe"),
                                isRead = obj.optBoolean("isRead", true),
                                sharedDramaId = obj.optString("sharedDramaId").takeIf { it.isNotBlank() },
                                sharedDramaTitle = obj.optString("sharedDramaTitle").takeIf { it.isNotBlank() },
                                sharedDramaCover = obj.optString("sharedDramaCover").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                    map[key] = list
                }
                _messagesByFriend.value = map
                return
            } catch (e: Exception) {
                Log.w(TAG, "Erro parse mensagens salvas: ${e.message}")
            }
        }

        // Conversas Iniciais Padrão
        val defaultMap = mutableMapOf<String, List<ChatMessage>>()
        defaultMap["friend_carol"] = listOf(
            ChatMessage(
                id = "msg_1",
                senderId = "friend_carol",
                text = "Oi! Você já assistiu ao episódio 2 de 'O Segredo da Baía'? Aquele final foi incrível!",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                isFromMe = false,
                isRead = true
            ),
            ChatMessage(
                id = "msg_2",
                senderId = "me",
                text = "Sim!! Fiquei chocado com a revelação no cais!",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 30,
                isFromMe = true,
                isRead = true
            )
        )

        defaultMap["friend_lucas"] = listOf(
            ChatMessage(
                id = "msg_3",
                senderId = "friend_lucas",
                text = "Me recomenda alguma novela boa de suspense por aqui?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 120,
                isFromMe = false,
                isRead = true
            )
        )

        _messagesByFriend.value = defaultMap
        saveMessagesMap(defaultMap)
    }

    fun addFriend(name: String, handle: String) {
        val newFriend = Friend(
            id = "friend_${System.currentTimeMillis()}",
            name = name,
            handle = if (handle.startsWith("@")) handle else "@$handle",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80",
            status = "Novo amigo no Litoral Novelas 👋",
            isOnline = true,
            lastSeen = "Online agora"
        )
        val updated = _friends.value + newFriend
        _friends.value = updated
        saveFriends(updated)
    }

    fun sendMessage(
        friendId: String,
        text: String,
        sharedDramaId: String? = null,
        sharedDramaTitle: String? = null,
        sharedDramaCover: String? = null
    ) {
        if (text.isBlank() && sharedDramaId == null) return

        val myMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = "me",
            text = text,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            isRead = true,
            sharedDramaId = sharedDramaId,
            sharedDramaTitle = sharedDramaTitle,
            sharedDramaCover = sharedDramaCover
        )

        val currentList = _messagesByFriend.value[friendId].orEmpty()
        val updatedList = currentList + myMsg
        val updatedMap = _messagesByFriend.value.toMutableMap().apply {
            put(friendId, updatedList)
        }
        _messagesByFriend.value = updatedMap
        saveMessagesMap(updatedMap)

        // Resposta interativa e amigável simulada após alguns segundos (estilo WhatsApp)
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500)
            val friend = _friends.value.find { it.id == friendId } ?: return@launch
            val responseText = when {
                sharedDramaId != null -> "Adorei a recomendação de '${sharedDramaTitle ?: "essa novela"}'! Vou assistir agora mesmo 😍🎬"
                text.contains("oi", ignoreCase = true) || text.contains("olá", ignoreCase = true) -> "Oi! Tudo bem? O que você está achando dos episódios novos?"
                text.contains("bom dia", ignoreCase = true) -> "Bom dia! Já separou a pipoca para as novelas de hoje? 🍿"
                text.contains("boa tarde", ignoreCase = true) -> "Boa tarde! Bora maratonar mais tarde!"
                text.contains("boa noite", ignoreCase = true) -> "Boa noite! Assistindo o último episódio antes de dormir 🌙"
                text.contains("recomenda", ignoreCase = true) -> "Super recomendo 'O Segredo da Baía' e 'Amor em Mar Aberto'!"
                else -> "Legal demais! Adoro acompanhar as novidades com você por aqui ✨"
            }

            val friendMsg = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                senderId = friendId,
                text = responseText,
                timestamp = System.currentTimeMillis(),
                isFromMe = false,
                isRead = true
            )

            val afterResponseList = _messagesByFriend.value[friendId].orEmpty() + friendMsg
            val afterResponseMap = _messagesByFriend.value.toMutableMap().apply {
                put(friendId, afterResponseList)
            }
            _messagesByFriend.value = afterResponseMap
            saveMessagesMap(afterResponseMap)
        }
    }

    private fun saveFriends(list: List<Friend>) {
        try {
            val arr = JSONArray()
            list.forEach { f ->
                arr.put(
                    JSONObject().apply {
                        put("id", f.id)
                        put("name", f.name)
                        put("handle", f.handle)
                        put("avatarUrl", f.avatarUrl)
                        put("status", f.status)
                        put("isOnline", f.isOnline)
                        put("lastSeen", f.lastSeen)
                        put("currentWatching", f.currentWatching ?: "")
                    }
                )
            }
            prefs.edit().putString("saved_friends", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar amigos: ${e.message}")
        }
    }

    private fun saveMessagesMap(map: Map<String, List<ChatMessage>>) {
        try {
            val root = JSONObject()
            map.forEach { (friendId, messages) ->
                val arr = JSONArray()
                messages.forEach { msg ->
                    arr.put(
                        JSONObject().apply {
                            put("id", msg.id)
                            put("senderId", msg.senderId)
                            put("text", msg.text)
                            put("timestamp", msg.timestamp)
                            put("isFromMe", msg.isFromMe)
                            put("isRead", msg.isRead)
                            put("sharedDramaId", msg.sharedDramaId ?: "")
                            put("sharedDramaTitle", msg.sharedDramaTitle ?: "")
                            put("sharedDramaCover", msg.sharedDramaCover ?: "")
                        }
                    )
                }
                root.put(friendId, arr)
            }
            prefs.edit().putString("saved_messages_map", root.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar mensagens: ${e.message}")
        }
    }
}
