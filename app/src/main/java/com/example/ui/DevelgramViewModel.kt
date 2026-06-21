package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenType {
    LIST, CHAT, SETTINGS
}

enum class DevelgramTheme {
    TELEGRAM_BLUE,    // Standard cyan-blue
    MATRIX_GREEN,     // Nerd cyberpunk green
    SUNSET_RED,       // Elegant dark rose/sunset red
    CYBERPUNK_VIOLET, // Pink accent violet theme
    DRACULA_DARK      // Dark charcoal Dracula mode
}

class DevelgramViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db)

    // User specifications
    private val _userName = MutableStateFlow("Android Developer")
    val userName = _userName.asStateFlow()

    private val _userBio = MutableStateFlow("Hi! Building Develgram with Jetpack Compose & persistence. ✈️🚀")
    val userBio = _userBio.asStateFlow()

    private val _userHandle = MutableStateFlow("devel_composer")
    val userHandle = _userHandle.asStateFlow()

    // Navigation and Routing
    private val _currentScreen = MutableStateFlow(ScreenType.LIST)
    val currentScreen = _currentScreen.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId = _activeChatId.asStateFlow()

    // Filtering controls
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _activeTab = MutableStateFlow("ALL") // "ALL", "DIRECT", "GROUP", "CHANNEL", "BOT"
    val activeTab = _activeTab.asStateFlow()

    // Active Theme Token
    private val _activeTheme = MutableStateFlow(DevelgramTheme.TELEGRAM_BLUE)
    val activeTheme = _activeTheme.asStateFlow()

    // Observe active chat entity details in real time
    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat = _activeChat.asStateFlow()

    // Reactive list of chats combining tabs and search filters
    val filteredChats: StateFlow<List<ChatEntity>> = combine(
        repository.allChats,
        _searchQuery,
        _activeTab
    ) { chats, query, tab ->
        var list = chats
        if (query.isNotBlank()) {
            list = list.filter { chat ->
                chat.name.contains(query, ignoreCase = true) || 
                chat.lastMessage.contains(query, ignoreCase = true)
            }
        }
        if (tab != "ALL") {
            list = list.filter { it.type == tab }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive message streams switching depending on active conversation
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatMessages: StateFlow<List<MessageEntity>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForChat(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate Database instantly of setup
        viewModelScope.launch {
            repository.initializePrepopulatedData()
        }
    }

    fun navigateToChat(chatId: String) {
        viewModelScope.launch {
            _activeChatId.value = chatId
            _activeChat.value = db.chatDao().getChatById(chatId)
            repository.markChatAsRead(chatId)
            _currentScreen.value = ScreenType.CHAT
        }
    }

    fun navigateToList() {
        _activeChatId.value = null
        _activeChat.value = null
        _currentScreen.value = ScreenType.LIST
    }

    fun navigateToSettings() {
        _currentScreen.value = ScreenType.SETTINGS
    }

    fun updateProfile(name: String, handle: String, bio: String) {
        if (name.isNotBlank()) _userName.value = name
        if (handle.isNotBlank()) _userHandle.value = handle.filter { !it.isWhitespace() }
        _userBio.value = bio
    }

    fun setTheme(theme: DevelgramTheme) {
        _activeTheme.value = theme
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun sendMessage(text: String) {
        val chatId = _activeChatId.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(chatId, text) {
                // Instantly re-fetch active details to show typing preview updates
                _activeChat.value = db.chatDao().getChatById(chatId)
            }
        }
    }

    fun postChannelBroadcast(channelId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addNewChannelPost(channelId, text)
        }
    }

    fun clearChatHistory(chatId: String) {
        viewModelScope.launch {
            repository.clearHistory(chatId)
            _activeChat.value = db.chatDao().getChatById(chatId)
        }
    }

    fun createNewChat(name: String, type: String, bio: String) {
        viewModelScope.launch {
            val newlyCreatedId = repository.addNewCustomChat(name, type, bio)
            if (newlyCreatedId.isNotBlank()) {
                navigateToChat(newlyCreatedId)
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (_activeChatId.value == chatId) {
                navigateToList()
            }
        }
    }
}
