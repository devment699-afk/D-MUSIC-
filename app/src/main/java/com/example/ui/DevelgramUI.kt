package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ChatEntity
import com.example.data.MessageEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Dynamic Custom Color Palette System
data class ThemeColors(
    val bg: Color,
    val surface: Color,
    val accent: Color,
    val sentBubble: Color,
    val recBubble: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val topBarBg: Color
)

@Composable
fun getThemeColors(theme: DevelgramTheme): ThemeColors {
    return when (theme) {
        DevelgramTheme.TELEGRAM_BLUE -> ThemeColors(
            bg = Color(0xFF0F1722),
            surface = Color(0xFF182230),
            accent = Color(0xFF5D9CEC),
            sentBubble = Color(0xFF2B5278),
            recBubble = Color(0xFF1E2C3D),
            textPrimary = Color(0xFFF5F7FA),
            textSecondary = Color(0xFFAAB2BD),
            topBarBg = Color(0xFF182230)
        )
        DevelgramTheme.MATRIX_GREEN -> ThemeColors(
            bg = Color(0xFF040A05),
            surface = Color(0xFF09140B),
            accent = Color(0xFF00FF41),
            sentBubble = Color(0xFF0D2D12),
            recBubble = Color(0xFF0D1C0F),
            textPrimary = Color(0xFFE0FFE0),
            textSecondary = Color(0xFF709575),
            topBarBg = Color(0xFF09140B)
        )
        DevelgramTheme.SUNSET_RED -> ThemeColors(
            bg = Color(0xFF140E10),
            surface = Color(0xFF1F1418),
            accent = Color(0xFFE95E76),
            sentBubble = Color(0xFF531F35),
            recBubble = Color(0xFF2A1C21),
            textPrimary = Color(0xFFFFF0F2),
            textSecondary = Color(0xFFB59AA0),
            topBarBg = Color(0xFF1F1418)
        )
        DevelgramTheme.CYBERPUNK_VIOLET -> ThemeColors(
            bg = Color(0xFF110B1F),
            surface = Color(0xFF1E1336),
            accent = Color(0xFFD946EF),
            sentBubble = Color(0xFF581C87),
            recBubble = Color(0xFF2E1C4E),
            textPrimary = Color(0xFFFDF4FF),
            textSecondary = Color(0xFFC084FC),
            topBarBg = Color(0xFF1E1336)
        )
        DevelgramTheme.DRACULA_DARK -> ThemeColors(
            bg = Color(0xFF191A21),
            surface = Color(0xFF282A36),
            accent = Color(0xFF8BE9FD),
            sentBubble = Color(0xFF44475A),
            recBubble = Color(0xFF282A36),
            textPrimary = Color(0xFFF8F8F2),
            textSecondary = Color(0xFF6272A4),
            topBarBg = Color(0xFF21222C)
        )
    }
}

@Composable
fun DevelgramAppContent(viewModel: DevelgramViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeTheme by viewModel.activeTheme.collectAsState()
    val themeColors = getThemeColors(activeTheme)

    // Back button handling in Compose
    BackHandler(enabled = currentScreen != ScreenType.LIST) {
        viewModel.navigateToList()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = themeColors.bg
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = spring()) + slideInHorizontally(
                    initialOffsetX = { if (targetState == ScreenType.LIST) -it else it },
                    animationSpec = spring()
                ) togetherWith fadeOut(animationSpec = spring()) + slideOutHorizontally(
                    targetOffsetX = { if (targetState == ScreenType.LIST) it else -it },
                    animationSpec = spring()
                )
            },
            label = "screen_navigation"
        ) { screen ->
            when (screen) {
                ScreenType.LIST -> ChatListScreen(viewModel, themeColors)
                ScreenType.CHAT -> ChatDetailScreen(viewModel, themeColors)
                ScreenType.SETTINGS -> SettingsScreen(viewModel, themeColors)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(viewModel: DevelgramViewModel, colors: ThemeColors) {
    val chats by viewModel.filteredChats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userBio by viewModel.userBio.collectAsState()
    val userHandle by viewModel.userHandle.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isSearchActive by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary,
                modifier = Modifier.width(310.dp)
            ) {
                // Sidebar Header Profile Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.accent.copy(alpha = 0.15f))
                        .padding(24.dp)
                ) {
                    Column {
                        // Profile Avatar Initial representation
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(colors.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = userName,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "@$userHandle",
                            color = colors.accent,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = userBio,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Items
                NavigationDrawerItem(
                    label = { Text("💬 All Conversations") },
                    selected = true,
                    onClick = {
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = colors.accent.copy(alpha = 0.1f),
                        selectedTextColor = colors.accent,
                        selectedIconColor = colors.accent,
                        unselectedTextColor = colors.textPrimary,
                        unselectedIconColor = colors.textSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("⚙️ Settings & Themes") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            viewModel.navigateToSettings()
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = colors.textPrimary,
                        unselectedIconColor = colors.textSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("🎙️ Create Group/Channel") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            showCreateDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Create Group") },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = colors.textPrimary,
                        unselectedIconColor = colors.textSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer
                Text(
                    text = "Develgram for Android v1.0.0\nSecure Room SQLite Engine ✈️",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(colors.topBarBg)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("hamburger_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Drawer", tint = colors.accent)
                        }

                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search Chats & Posts...", color = colors.textSecondary) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_input")
                            )

                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.setSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search", tint = colors.textSecondary)
                            }
                        } else {
                            Text(
                                text = "Develgram",
                                color = colors.textPrimary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )

                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.accent)
                            }

                            IconButton(
                                onClick = { viewModel.navigateToSettings() },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.accent)
                            }
                        }
                    }

                    // Telegram Tags Row (horizontal scrolling filters)
                    ScrollableTabRow(
                        selectedTabIndex = getTabIndex(activeTab),
                        containerColor = colors.topBarBg,
                        contentColor = colors.accent,
                        edgePadding = 12.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            val index = getTabIndex(activeTab)
                            if (index in tabPositions.indices) {
                                TabRowDefaults.PrimaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                                    color = colors.accent
                                )
                            }
                        }
                    ) {
                        listOf(
                            Pair("ALL", "All Chats"),
                            Pair("DIRECT", "Personal"),
                            Pair("GROUP", "Groups"),
                            Pair("CHANNEL", "Channels"),
                            Pair("BOT", "Bots")
                        ).forEach { (tabId, label) ->
                            Tab(
                                selected = activeTab == tabId,
                                onClick = { viewModel.setActiveTab(tabId) },
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (activeTab == tabId) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selectedContentColor = colors.accent,
                                unselectedContentColor = colors.textSecondary
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = colors.accent,
                    contentColor = Color.White,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("add_chat_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Chat")
                }
            },
            containerColor = colors.bg
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (chats.isEmpty()) {
                    // Empty list state illustration & tip
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = {}, modifier = Modifier.size(80.dp).background(colors.accent.copy(0.1f), CircleShape)) {
                            Icon(Icons.Default.MailOutline, contentDescription = "Empty", tint = colors.accent, modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Conversations Found",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bhai, filter badalke try kijiye ya neeche wale '+' FAB button se naya chat/channel banayein! ✈️",
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // Chats Recycler Grid view
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chats) { chat ->
                            ChatItemRow(chat, colors) {
                                viewModel.navigateToChat(chat.id)
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateChatDialog(
            colors = colors,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type, bio ->
                viewModel.createNewChat(name, type, bio)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun ChatItemRow(chat: ChatEntity, colors: ThemeColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Initial-based round Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color(chat.avatarColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initial = chat.name.replace("👥 ", "").replace("🛰️ ", "").replace("🎙️ ", "").replace("✈️ ", "").take(1)
            Text(
                text = initial.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = chat.name,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatShortTime(chat.lastMessageTime),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = chat.lastMessage,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Badge representation
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = chat.type,
                            color = colors.accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(19.dp)
                                .background(colors.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.08f), thickness = 0.5.dp, modifier = Modifier.padding(start = 74.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(viewModel: DevelgramViewModel, colors: ThemeColors) {
    val activeChat by viewModel.activeChat.collectAsState()
    val messages by viewModel.activeChatMessages.collectAsState()
    val focusManager = LocalFocusManager.current

    var messageText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Smooth scroll down to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showInfoDialog = true }
                    ) {
                        activeChat?.let { chat ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(chat.avatarColor), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = chat.name.replace("✈️ ", "").replace("👥 ", "").take(1)
                                Text(
                                    text = initial.uppercase(),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = chat.name,
                                    color = colors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when (chat.id) {
                                        "chat_devel_bot" -> "online"
                                        "chat_dev_group" -> "4 members online"
                                        "chat_dev_channel" -> "11.2k subscribers"
                                        else -> "last seen recently"
                                    },
                                    color = colors.accent,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateToList() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.accent)
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Chat Bio Info", tint = colors.accent)
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.accent)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("🧹 Clear History", color = colors.textPrimary) },
                            onClick = {
                                activeChat?.let { viewModel.clearChatHistory(it.id) }
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🗑️ Delete Chat", color = Color.Red) },
                            onClick = {
                                activeChat?.let { viewModel.deleteChat(it.id) }
                                menuExpanded = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.topBarBg)
            )
        },
        containerColor = colors.bg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Secret platform key warning representation (inserted gracefully for API safety guidance)
            if (activeChat?.id == "chat_devel_bot" && com.example.BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.accent.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "API Security", tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sandbox offline. Key missing in AI Studio Secrets tab! Fallbacks active.",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Message Board Messages List
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages) { message ->
                    MessageBubble(message = message, colors = colors)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Bottom Sending field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = {
                        // Simulated attachment alert
                    }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attachment clips", tint = colors.accent)
                    }

                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Message...", color = colors.textSecondary) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.bg,
                            unfocusedContainerColor = colors.bg,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("chat_input_field"),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(colors.accent, CircleShape)
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog && activeChat != null) {
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.2f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color(activeChat!!.avatarColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeChat!!.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = activeChat!!.name,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = activeChat!!.type + " Conversation",
                        color = colors.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "About Details / Bio:",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeChat!!.bio.ifBlank { "Develgram encryption secures this chat." },
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text("Close Details", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity, colors: ThemeColors) {
    val isMe = message.senderId == "me"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        val bubbleShape = if (isMe) {
            RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
        } else {
            RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMe) {
                // Short indicator color or bullet
                Spacer(modifier = Modifier.width(4.dp))
            }

            Card(
                shape = bubbleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isMe) colors.sentBubble else colors.recBubble
                ),
                modifier = Modifier.padding(1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Group sender handles tags
                    if (!isMe && message.senderName != "@DevelgramAIBot" && message.senderId != "system" && message.senderId != "admin") {
                        Text(
                            text = message.senderName,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Text(
                        text = message.text,
                        color = colors.textPrimary,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatShortTime(message.timestamp),
                            color = colors.textSecondary.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            if (message.isPending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.dp,
                                    color = colors.textSecondary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Read Double ticks",
                                    tint = colors.accent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: DevelgramViewModel, colors: ThemeColors) {
    val userName by viewModel.userName.collectAsState()
    val userBio by viewModel.userBio.collectAsState()
    val userHandle by viewModel.userHandle.collectAsState()
    val activeTheme by viewModel.activeTheme.collectAsState()

    var editName by remember { mutableStateOf(userName) }
    var editBio by remember { mutableStateOf(userBio) }
    var editHandle by remember { mutableStateOf(userHandle) }
    var editMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("App Settings & Styles", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateToList() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.accent)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.topBarBg)
            )
        },
        containerColor = colors.bg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Profile Card display
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Develgram Profile Details", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.textSecondary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_name_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editHandle,
                        onValueChange = { editHandle = it },
                        label = { Text("Username Handle (e.g. coder_99)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.textSecondary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_handle_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.textSecondary
                        ),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("settings_bio_field")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Choose Theme color palletes Showcase (dynamic toggles)
            Text("Select Chat UI Accent Theme", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple(DevelgramTheme.TELEGRAM_BLUE, Color(0xFF5D9CEC), "Blue"),
                    Triple(DevelgramTheme.MATRIX_GREEN, Color(0xFF00FF41), "Matrix"),
                    Triple(DevelgramTheme.SUNSET_RED, Color(0xFFE95E76), "Sunset"),
                    Triple(DevelgramTheme.CYBERPUNK_VIOLET, Color(0xFFD946EF), "Violet"),
                    Triple(DevelgramTheme.DRACULA_DARK, Color(0xFFBD93F9), "Dracula")
                ).forEach { (themeEnum, colorToken, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                viewModel.setTheme(themeEnum)
                                editMessage = "Accent updated to $label Theme! 🎨"
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(colorToken, CircleShape)
                                .clip(CircleShape)
                                .border(
                                    border = if (activeTheme == themeEnum) BorderStroke(4.dp, Color.White) else BorderStroke(0.dp, Color.Transparent),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            color = if (activeTheme == themeEnum) colors.accent else colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (activeTheme == themeEnum) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (editMessage.isNotEmpty()) {
                Text(
                    text = editMessage,
                    color = colors.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.navigateToList() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                    border = BorderStroke(1.dp, colors.accent),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        viewModel.updateProfile(editName, editHandle, editBio)
                        editMessage = "Profile updated successfully! ✅"
                        viewModel.navigateToList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    modifier = Modifier.weight(1f).padding(start = 8.dp).testTag("save_profile_button")
                ) {
                    Text("Save & Close", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CreateChatDialog(
    colors: ThemeColors,
    onDismiss: () -> Unit,
    onCreate: (name: String, type: String, bio: String) -> Unit
) {
    var chatName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("GROUP") } // "DIRECT", "GROUP", "CHANNEL", "BOT"
    var chatBio by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Create Develgram Conversation",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = chatName,
                    onValueChange = { chatName = it },
                    label = { Text("Name / Handles description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.textSecondary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_chat_name")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Conversation Structure:", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                // Selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("DIRECT", "GROUP", "CHANNEL", "BOT").forEach { type ->
                        val isSel = selectedType == type
                        Box(
                            modifier = Modifier
                                .background(if (isSel) colors.accent else colors.bg, RoundedCornerShape(6.dp))
                                .clickable { selectedType = type }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = type,
                                color = if (isSel) Color.White else colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = chatBio,
                    onValueChange = { chatBio = it },
                    label = { Text("Topic Bio description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.textSecondary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_chat_bio")
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.accent)
                    }

                    Button(
                        onClick = {
                            if (chatName.isBlank()) {
                                errorMessage = "Kripya conversation ka naam likhein!"
                            } else {
                                onCreate(chatName, selectedType, chatBio)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        modifier = Modifier.testTag("submit_new_chat")
                    ) {
                        Text("Create", color = Color.White)
                    }
                }
            }
        }
    }
}

// Helper time formatter utilities
fun formatShortTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun getTabIndex(tab: String): Int {
    return when (tab) {
        "ALL" -> 0
        "DIRECT" -> 1
        "GROUP" -> 2
        "CHANNEL" -> 3
        "BOT" -> 4
        else -> 0
    }
}
