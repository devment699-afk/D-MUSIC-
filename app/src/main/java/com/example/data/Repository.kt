package com.example.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class Repository(private val db: AppDatabase) {
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()

    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForChat(chatId)
    }

    suspend fun markChatAsRead(chatId: String) {
        chatDao.markChatAsRead(chatId)
    }

    /**
     * Sends a message from the user and triggers appropriate responses from AI or Group members
     */
    suspend fun sendMessage(chatId: String, text: String, onResponseTriggered: suspend () -> Unit = {}) {
        if (text.isBlank()) return

        val userMessage = MessageEntity(
            chatId = chatId,
            senderId = "me",
            senderName = "Aap",
            text = text,
            timestamp = System.currentTimeMillis()
        )

        // Insert user message and update chat preview info
        messageDao.insertMessage(userMessage)
        chatDao.updateLastMessage(chatId, text, userMessage.timestamp, 0)

        // Reset unread count for the active chat
        chatDao.markChatAsRead(chatId)

        // Trigger reactions based on chat types
        when (chatId) {
            "chat_devel_bot" -> {
                // Trigger AI Bot Typing and Reply
                handleBotResponse(chatId, text, onResponseTriggered)
            }
            "chat_dev_group" -> {
                // Trigger Simulated Group Reply
                handleGroupSimulatedResponse(chatId, text, onResponseTriggered)
            }
        }
    }

    private suspend fun handleBotResponse(chatId: String, userPrompt: String, onResponseTriggered: suspend () -> Unit) {
        // Step 1: Insert "Typing..." pending state
        val pendingMsg = MessageEntity(
            chatId = chatId,
            senderId = "bot",
            senderName = "@DevelgramAIBot",
            text = "Develgram Bot typing...",
            timestamp = System.currentTimeMillis() + 50,
            isPending = true
        )
        val pendingId = messageDao.insertMessage(pendingMsg)
        chatDao.updateLastMessage(chatId, "typing...", pendingMsg.timestamp, 0)

        // Notify UI to scroll or update states if needed
        onResponseTriggered()

        // Fetch preceding messages for conversation context (for memory)
        val history = messageDao.getMessagesForChat(chatId).first()
            .filter { !it.isPending && it.senderId != "bot" }

        // Step 2: Query Gemini API (runs with Retrofit background thread safety)
        val botReply = queryGemini(userPrompt, history)

        // Step 3: Delete the pending "typing" indicator and insert final reply
        messageDao.deleteMessageById(pendingId)
        val finalMsg = MessageEntity(
            chatId = chatId,
            senderId = "bot",
            senderName = "@DevelgramAIBot",
            text = botReply,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(finalMsg)
        chatDao.updateLastMessage(chatId, botReply.take(60), finalMsg.timestamp, 0)
        onResponseTriggered()
    }

    private suspend fun handleGroupSimulatedResponse(chatId: String, userPrompt: String, onResponseTriggered: suspend () -> Unit) {
        // Group discusses. We pick a random participant from Develgram dev guild
        val developers = listOf(
            Pair("Aaryaman", "Room database to custom theme transitions super smooth lag rhi hai bhai! Modern Jetpack Compose FTW 🚀"),
            Pair("Siddharth (Kotlin Expert)", "Bhai, kya dynamic XML styles are gone completely in Develgram? State management rocks here. @Aap great job!"),
            Pair("Apurv (UI Specialist)", "Mujhe classic Dark Blue default theme iski bahut attractive lagti hai. Telegram style background grids make it look real as hell."),
            Pair("Devel_Master", "Develgram channel me versions release posts checkout kariye aap log details ke liye.")
        )
        val responder = developers[Random.nextInt(developers.size)]

        delay(Random.nextLong(1500, 3000)) // delay to simulate actual active typing

        val devMessage = MessageEntity(
            chatId = chatId,
            senderId = "expert_${responder.first.lowercase().filter { it.isLetter() }}",
            senderName = responder.first,
            text = responder.second,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(devMessage)
        chatDao.updateLastMessage(chatId, "${responder.first}: ${responder.second.take(45)}...", devMessage.timestamp, 1)
        onResponseTriggered()
    }

    /**
     * Populates database with starter chats if empty, ensuring first boot looks professional.
     */
    suspend fun initializePrepopulatedData() {
        val count = chatDao.getAllChats().first().size
        if (count == 0) {
            val starterChats = listOf(
                ChatEntity(
                    id = "chat_devel_bot",
                    name = "✈️ @DevelgramAIBot",
                    type = "BOT",
                    avatarColor = 0xFF5D9CEC.toInt(),
                    lastMessage = "Develgram AI Active. Ask anything inside me! 🤖",
                    lastMessageTime = System.currentTimeMillis() - 5000,
                    unreadCount = 1,
                    bio = "Official AI Chatbot Companion powered by Gemini-3.5-Flash. Talk directly to prompt automated AI operations!"
                ),
                ChatEntity(
                    id = "chat_dev_group",
                    name = "👥 Develgram Dev Discussion",
                    type = "GROUP",
                    avatarColor = 0xFF4FC3F7.toInt(),
                    lastMessage = "Aaryaman: Welcome guys! Room persistence is successfully setup in current client.",
                    lastMessageTime = System.currentTimeMillis() - 15000,
                    unreadCount = 0,
                    bio = "National Kotlin specialists discussion group. Feel free to talk and suggest features!"
                ),
                ChatEntity(
                    id = "chat_dev_channel",
                    name = "🎙️ Develgram Announcements",
                    type = "CHANNEL",
                    avatarColor = 0xFFEC87C0.toInt(),
                    lastMessage = "Develgram Client v1.0.0 is released using Material 3!",
                    lastMessageTime = System.currentTimeMillis() - 60000,
                    unreadCount = 2,
                    bio = "Stay updated with weekly releases, theme design layouts, and technical specifications."
                ),
                ChatEntity(
                    id = "chat_user_ramesh",
                    name = "👤 Ramesh Kumar (Android Specialist)",
                    type = "DIRECT",
                    avatarColor = 0xFFFC6E51.toInt(),
                    lastMessage = "Bhai, Develgram app is working beautifully!",
                    lastMessageTime = System.currentTimeMillis() - 120000,
                    unreadCount = 0,
                    bio = "Freelance Android Engineer at Bangalore. Contact for app design suggestions."
                )
            )
            chatDao.insertChats(starterChats)

            // Seed initial greeting message
            messageDao.insertMessage(
                MessageEntity(
                    chatId = "chat_devel_bot",
                    senderId = "bot",
                    senderName = "@DevelgramAIBot",
                    text = "Welcome to **Develgram** ✈️!\n\nMain aapka personal AI assistant hu. Main *Gemini-3.5-Flash* AI model se powered hu. Mujhe English ya Hinglish me kuch bhi pucho, main replies dunga! \n\nAgar offline mode me test karna ho, to command likhein 'help' or explore krein features. Naye messages send kar ke local Room database flow check kijiye!",
                    timestamp = System.currentTimeMillis() - 5000,
                    isRead = false
                )
            )

            messageDao.insertMessage(
                MessageEntity(
                    chatId = "chat_dev_group",
                    senderId = "expert_aaryaman",
                    senderName = "Aaryaman",
                    text = "Aap sabhi ka swagat hai **Develgram Dev Guild** me! Aaj hum persistence test kar rhe hain. UI theme select karne settings access karein custom elements toggle karne ke liye.",
                    timestamp = System.currentTimeMillis() - 15000
                )
            )

            messageDao.insertMessage(
                MessageEntity(
                    chatId = "chat_dev_channel",
                    senderId = "admin",
                    senderName = "Develgram Channel",
                    text = "✈️ **DEVELGRAM COMPOSE VERSION 1.0 RELEASED** \n\nFeatures Included:\n1. Telegram-like Tab and Drawer Systems 🏢\n2. Real Persistence via Room SQLite 🗄️\n3. Conversational AI Chat Bot via @DevelgramAIBot 🤖\n4. Multiple Premium UI Themes (Sunset, Blue, Velvet, Hulk) 🎨",
                    timestamp = System.currentTimeMillis() - 60000
                )
            )

            messageDao.insertMessage(
                MessageEntity(
                    chatId = "chat_user_ramesh",
                    senderId = "ramesh",
                    senderName = "Ramesh Kumar",
                    text = "Kal meetup me log is client-side Room database layout ke baare me discuss karenge. Bhai, Develgram app is working beautifully!",
                    timestamp = System.currentTimeMillis() - 120000
                )
            )
        }
    }

    suspend fun addNewCustomChat(chatName: String, chatType: String, bio: String): String {
        val cleanName = chatName.trim()
        if (cleanName.isEmpty()) return ""
        val uniqueId = "chat_custom_${System.currentTimeMillis()}"
        val color = listOf(0xFF8CC152, 0xFF357EBD, 0xFFD870AD, 0xFFF6BB42, 0xFF5D9CEC, 0xFF967ADC).random().toInt()
        val defaultMsg = when (chatType) {
            "BOT" -> "Send direct message with queries!"
            "GROUP" -> "Discussion started!"
            "CHANNEL" -> "Subscribed to Broadcast postings!"
            else -> "Aap se chat start hui."
        }
        val customChat = ChatEntity(
            id = uniqueId,
            name = if (chatType == "BOT") "✈️ @$cleanName" else cleanName,
            type = chatType,
            avatarColor = color,
            lastMessage = defaultMsg,
            lastMessageTime = System.currentTimeMillis(),
            bio = bio.ifBlank { "Newly created $chatType conversation in Develgram." }
        )
        chatDao.insertChat(customChat)
        messageDao.insertMessage(
            MessageEntity(
                chatId = uniqueId,
                senderId = "system",
                senderName = "System",
                text = "Welcome to '$cleanName' ($chatType). Start sending messages locally! 🚀",
                timestamp = System.currentTimeMillis()
            )
        )
        return uniqueId
    }

    suspend fun addNewChannelPost(channelId: String, text: String) {
        val userMessage = MessageEntity(
            chatId = channelId,
            senderId = "me",
            senderName = "Admin Post",
            text = text,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userMessage)
        chatDao.updateLastMessage(channelId, text, userMessage.timestamp, 0)
    }

    suspend fun clearHistory(chatId: String) {
        messageDao.clearMessagesForChat(chatId)
        chatDao.updateLastMessage(chatId, "No messages yet.", System.currentTimeMillis(), 0)
    }

    suspend fun deleteChat(chatId: String) {
        db.withTransaction {
            chatDao.deleteChatById(chatId)
            messageDao.clearMessagesForChat(chatId)
        }
    }
}
