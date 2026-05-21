package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.model.Prompt
import com.example.data.model.Review
import com.example.ui.theme.*
import com.example.ui.viewmodel.GenerateUiState
import com.example.ui.viewmodel.PromptViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: PromptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        PromptAppNavHost(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PromptAppNavHost(viewModel: PromptViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                onNavigateToGenerate = { navController.navigate("generate") },
                onNavigateToCreateManual = { navController.navigate("create_manual") }
            )
        }
        composable(
            route = "detail/{promptId}",
            arguments = listOf(navArgument("promptId") { type = NavType.IntType })
        ) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getInt("promptId") ?: 0
            DetailScreen(
                promptId = promptId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("generate") {
            AILabScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> 
                    navController.navigate("detail/$id") {
                        popUpTo("home")
                    }
                }
            )
        }
        composable("create_manual") {
            CreateManualScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PromptViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToGenerate: () -> Unit,
    onNavigateToCreateManual: () -> Unit
) {
    val prompts by viewModel.filteredPrompts.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showFabMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant Cosmic Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkBackground, DarkSurface)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Prompt Studio",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "Unleash state-of-the-art AI generation",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                        IconButton(
                            onClick = onNavigateToGenerate,
                            modifier = Modifier
                                .background(PrimaryTeal.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, PrimaryTeal.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Prompt Generator",
                                tint = SecondaryAqua
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Clean Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        placeholder = { Text("Search image, video, text prompts...", color = textSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = textSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // Main categories slider
            Text(
                text = "Explore Categories",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CategoryChip(
                        name = "All Prompts",
                        isSelected = selectedCategory == null,
                        onClick = { viewModel.selectedCategory.value = null }
                    )
                }
                items(viewModel.mainCategories) { category ->
                    CategoryChip(
                        name = category,
                        isSelected = selectedCategory == category,
                        onClick = { viewModel.selectedCategory.value = category }
                    )
                }
            }

            // Subcategories horizontal list if a major category is selected
            if (selectedCategory != null) {
                val subcats = viewModel.subCategoriesMap[selectedCategory] ?: emptyList()
                if (subcats.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subcats) { subCategory ->
                            AssistChip(
                                onClick = { viewModel.searchQuery.value = subCategory },
                                label = { Text(subCategory, color = textSecondary, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = DarkSurface.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body List of Prompts
            if (prompts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No prompts found",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Try clearing queries or start creating a new prompt!",
                            color = textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(prompts) { prompt ->
                        PromptItemCard(
                            prompt = prompt,
                            viewModel = viewModel,
                            onClick = { onNavigateToDetail(prompt.id) }
                        )
                    }
                }
            }
        }

        // Expanded Floating Options FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    // Manual Prompt option
                    FloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            onNavigateToCreateManual()
                        },
                        containerColor = DarkSurface,
                        contentColor = textPrimary,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .border(1.dp, BorderColor, CircleShape)
                            .testTag("fab_option_manual"),
                        shape = CircleShape
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Manual", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // AI Prompt generator option
                    FloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            onNavigateToGenerate()
                        },
                        containerColor = PrimaryTeal,
                        contentColor = DarkBackground,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("fab_option_ai"),
                        shape = CircleShape
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate with AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = PrimaryTeal,
                    contentColor = DarkBackground,
                    modifier = Modifier.testTag("fab_trigger")
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "New Prompt Option Menu"
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(
                if (isSelected) PrimaryTeal else DarkSurface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else BorderColor,
                shape = RoundedCornerShape(30.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val textColor = if (isSelected) DarkBackground else textPrimary
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun PromptItemCard(
    prompt: Prompt,
    viewModel: PromptViewModel,
    onClick: () -> Unit
) {
    val reviews by viewModel.getReviewsForPrompt(prompt.id).collectAsState(initial = emptyList())
    val avgScore = if (reviews.isEmpty()) 0f else reviews.map { it.rating }.average().toFloat()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prompt_card_${prompt.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Platform Tag
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                prompt.targetPlatform.contains("Midjourney", true) -> Color(0xFF5865F2).copy(alpha = 0.15f)
                                prompt.targetPlatform.contains("Stable", true) -> Color(0xFF6B21A8).copy(alpha = 0.15f)
                                prompt.targetPlatform.contains("Sora", true) -> Color(0xFF0F766E).copy(alpha = 0.15f)
                                prompt.targetPlatform.contains("ChatGPT", true) -> Color(0xFF15803D).copy(alpha = 0.15f)
                                prompt.targetPlatform.contains("Claude", true) -> Color(0xFFB45309).copy(alpha = 0.15f)
                                else -> PrimaryTeal.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                prompt.targetPlatform.contains("Midjourney", true) -> Color(0xFF5865F2)
                                prompt.targetPlatform.contains("Stable", true) -> Color(0xFF9333EA)
                                prompt.targetPlatform.contains("Sora", true) -> Color(0xFF14B8A6)
                                prompt.targetPlatform.contains("ChatGPT", true) -> Color(0xFF22C55E)
                                prompt.targetPlatform.contains("Claude", true) -> Color(0xFFF59E0B)
                                else -> PrimaryTeal
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = prompt.targetPlatform,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            prompt.targetPlatform.contains("Midjourney", true) -> Color(0xFFCBCFFF)
                            prompt.targetPlatform.contains("Stable", true) -> Color(0xFFE9D5FF)
                            prompt.targetPlatform.contains("Sora", true) -> Color(0xFFCCFBF1)
                            prompt.targetPlatform.contains("ChatGPT", true) -> Color(0xFFDCFCE7)
                            prompt.targetPlatform.contains("Claude", true) -> Color(0xFFFEF3C7)
                            else -> textPrimary
                        }
                    )
                }

                // Average Rating with Star Logo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = RatingGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (reviews.isEmpty()) "Unrated" else String.format(Locale.US, "%.1f", avgScore),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    if (reviews.isNotEmpty()) {
                        Text(
                            text = " (${reviews.size})",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prompt Title
            Text(
                text = prompt.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Body content summary string
            Text(
                text = prompt.content,
                fontSize = 13.sp,
                color = textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Multi Categories Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category list badges
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    prompt.categories.take(3).forEach { cat ->
                        Box(
                            modifier = Modifier
                                .background(DarkBackground, RoundedCornerShape(20.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(cat, fontSize = 9.sp, color = textSecondary)
                        }
                    }
                }

                // Inline quick Copy Action
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(prompt.content))
                        Toast.makeText(context, "Prompt copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(DarkBackground, CircleShape)
                        .border(1.dp, BorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Quick copy prompt",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// FlowRow simulator for layout rows
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    promptId: Int,
    viewModel: PromptViewModel,
    onBack: () -> Unit
) {
    val promptFlow = remember(promptId) { viewModel.getPromptById(promptId) }
    val prompt by promptFlow.collectAsState(initial = null)
    val reviews by viewModel.getReviewsForPrompt(promptId).collectAsState(initial = emptyList())
    val avgScore = if (reviews.isEmpty()) 0f else reviews.map { it.rating }.average().toFloat()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Write review inputs
    var ratingChosen by remember { mutableStateOf(5) }
    var reviewerName by remember { mutableStateOf("") }
    var reviewText by remember { mutableStateOf("") }

    if (prompt == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryTeal)
        }
    } else {
        val p = prompt!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Top Nav bar details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Prompt Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (!p.isPreloaded) {
                        IconButton(onClick = {
                            viewModel.deletePrompt(p)
                            Toast.makeText(context, "Prompt deleted from library", Toast.LENGTH_SHORT).show()
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete prompt", tint = Color.Red)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Header card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Target Platform Highlight tag
                            Box(
                                modifier = Modifier
                                    .background(PrimaryTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, PrimaryTeal, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(p.targetPlatform, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Star indicator
                            Icon(Icons.Default.Star, contentDescription = null, tint = RatingGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (reviews.isEmpty()) "Unrated" else String.format(Locale.US, "%.1f ★", avgScore),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(p.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Tags list representation
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            p.categories.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(DarkBackground, RoundedCornerShape(30.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(30.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(tag, fontSize = 11.sp, color = textSecondary)
                                }
                            }
                        }
                    }
                }

                // Prompt content block
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "THE PROMPT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Prompt core textual content
                            Text(
                                text = p.content,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(p.content))
                                    Toast.makeText(context, "Full prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("copy_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy Full Prompt", fontWeight = FontWeight.Bold, color = DarkBackground)
                                }
                            }
                        }
                    }
                }

                // How to use / instruction directions
                if (p.instruction.isNotBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SYSTEM INSTRUCTIONS & TIPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryAqua,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = p.instruction,
                                fontSize = 13.sp,
                                color = textSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // Submit rating and review container
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "SUBMIT A RATING & REVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        // Rating star click selection
                        Text("Your score:", fontSize = 12.sp, color = textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = if (i <= ratingChosen) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Rate $i star",
                                    tint = RatingGold,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { ratingChosen = i }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Reviewer Name field
                        OutlinedTextField(
                            value = reviewerName,
                            onValueChange = { reviewerName = it },
                            placeholder = { Text("Your name (optional)", color = textSecondary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reviewer_username"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryTeal,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Review Text field
                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            placeholder = { Text("Write your review on how this prompt performed...", color = textSecondary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("reviewer_comment_box"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryTeal,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.submitReview(promptId, ratingChosen, reviewerName, reviewText)
                                Toast.makeText(context, "Review submitted! Thank you.", Toast.LENGTH_SHORT).show()
                                reviewerName = ""
                                reviewText = ""
                                ratingChosen = 5
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_review_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Submit Review", fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    }
                }

                // Recent reviews display list
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "RECENT REVIEWS (${reviews.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (reviews.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No reviews yet. Be the first to leave a review!", color = textSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(reviews) { r ->
                        ReviewItemCard(r)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItemCard(review: Review) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val dateStr = remember(review.timestamp) { formatter.format(Date(review.timestamp)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(review.reviewerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textPrimary)
            Text(dateStr, color = textSecondary, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stars layout inside reviews
        Row {
            for (i in 1..5) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (i <= review.rating) RatingGold else BorderColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(review.reviewText, fontSize = 13.sp, color = textSecondary, lineHeight = 18.sp)
    }
}

// AI Gen Lab Composable Screen
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AILabScreen(
    viewModel: PromptViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val uiState by viewModel.generateUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Lab select values
    var selectedMainCategory by remember { mutableStateOf(viewModel.mainCategories[0]) }
    val subsList = remember(selectedMainCategory) { viewModel.subCategoriesMap[selectedMainCategory] ?: emptyList() }
    var selectedSubCategory by remember { mutableStateOf(subsList.getOrElse(0) { "" }) }

    // Synchronize subcategory selection when main changes
    LaunchedEffect(selectedMainCategory) {
        if (subsList.isNotEmpty()) {
            selectedSubCategory = subsList[0]
        }
    }

    var userIdea by remember { mutableStateOf("") }
    var targetPlatform by remember { mutableStateOf("Midjourney v6") }
    var promptTitleGenerated by remember { mutableStateOf("") }

    val platformList = listOf(
        "Midjourney v6",
        "Stable Diffusion XL",
        "DALL-E 3",
        "OpenAI Sora",
        "Runway Gen-3",
        "Luma Dream Machine",
        "ChatGPT",
        "Claude 3.5 Sonnet",
        "Gemini 1.5 Pro"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Lab top bar header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    viewModel.resetGenerateState()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI Prompt Laboratory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecondaryAqua)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "CRAFT A POWERFUL PROMPT USING AI",
                color = PrimaryTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Choose Category Selectors
            Text("1. Select Category & Focus", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            // Main Category chips
            Text("Main Focus Category:", fontSize = 12.sp, color = textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.mainCategories.forEach { cat ->
                    val isSelected = cat == selectedMainCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryTeal else DarkSurface)
                            .border(1.dp, if (isSelected) Color.Transparent else BorderColor, RoundedCornerShape(20.dp))
                            .clickable { selectedMainCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) DarkBackground else textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub category dropdown chips
            if (subsList.isNotEmpty()) {
                Text("Sub-category Specialization:", fontSize = 12.sp, color = textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    subsList.forEach { sub ->
                        val isSelected = sub == selectedSubCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) SecondaryAqua.copy(alpha = 0.25f) else DarkSurface)
                                .border(1.dp, if (isSelected) SecondaryAqua else BorderColor, RoundedCornerShape(20.dp))
                                .clickable { selectedSubCategory = sub }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = sub,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) SecondaryAqua else textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Select Target platform
            Text("2. Destination AI Engine", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                platformList.forEach { plat ->
                    val isSelected = plat == targetPlatform
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentPurple.copy(alpha = 0.3f) else DarkSurface)
                            .border(1.dp, if (isSelected) AccentPurple else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { targetPlatform = plat }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(text = plat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Describe initial idea text input
            Text("3. Describe your idea", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = userIdea,
                onValueChange = { userIdea = it },
                placeholder = { Text("E.g., A futuristic cyberpunk hacker in an glowing alleyway eating ramen at sunset...", color = textSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("ai_generator_idea_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Generate Button call
            Button(
                onClick = {
                    val fallbackTitle = userIdea.trim().take(20).ifEmpty { "AI Generated" }
                    promptTitleGenerated = "$targetPlatform: $fallbackTitle..."
                    viewModel.generatePromptWithAI(
                        userIdea = userIdea,
                        targetPlatform = targetPlatform,
                        category = selectedMainCategory,
                        subCategory = selectedSubCategory
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("generate_prompt_ai_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Optimize with Gemini AI", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Handler output UI states
            when (val state = uiState) {
                is GenerateUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = SecondaryAqua)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("AI Architect is designing your prompt...", color = SecondaryAqua, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                is GenerateUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is GenerateUiState.Success -> {
                    Text(
                        text = "AI OPTIMIZED RESULTS",
                        color = SecondaryAqua,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text("Generated prompt:", color = textSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        SelectableTextCodeBox(text = state.optimizedPrompt)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Usage hints / Tips:", color = textSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = state.usageInstructions,
                            fontSize = 13.sp,
                            color = textPrimary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Copy button action
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.optimizedPrompt))
                                    Toast.makeText(context, "Optimized prompt copied!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize), tint = PrimaryTeal)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy", color = PrimaryTeal, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Save Prompt Button Persistence
                            Button(
                                onClick = {
                                    viewModel.savePrompt(
                                        title = promptTitleGenerated,
                                        content = state.optimizedPrompt,
                                        instruction = state.usageInstructions,
                                        targetPlatform = targetPlatform,
                                        categories = listOf(selectedMainCategory, selectedSubCategory)
                                    )
                                    Toast.makeText(context, "Optimized prompt saved directly to library!", Toast.LENGTH_SHORT).show()
                                    viewModel.resetGenerateState()
                                    onBack()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize), tint = DarkBackground)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save to Library", color = DarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                else -> { /* Idle */ }
            }
        }
    }
}

@Composable
fun SelectableTextCodeBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground, RoundedCornerShape(8.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = textPrimary
        )
    }
}

// Create Manual Prompt Composable view
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateManualScreen(
    viewModel: PromptViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("") }
    var targetPlatform by remember { mutableStateOf("ChatGPT") }

    // Multi category tags selections (A prompt can be tagged with multiple categories/sub-categories!)
    val selectedTags = remember { mutableStateListOf<String>() }

    val platformList = listOf("Midjourney v6", "DALL-E 3", "Stable Diffusion", "OpenAI Sora", "Runway Gen-3", "ChatGPT", "Claude 3.5 Sonnet", "Gemini")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Nav bar details
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Create Custom Prompt",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "MANUALLY PERSIST CORE PROMPT",
                color = PrimaryTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Title Box field
            Text("Prompt Title:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("E.g., Photorealistic Studio Ghibli Landscape", color = textSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_prompt_title"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Direct content
            Text("Core Prompt Text:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Write the full detailed prompt instructions here...", color = textSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag("manual_prompt_content"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rules instructions
            Text("Instructions / Support tips:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = instruction,
                onValueChange = { instruction = it },
                placeholder = { Text("E.g., Use prompt parameters '--ar 16:9' or specify specific steps...", color = textSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .testTag("manual_prompt_instruction"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Platform tag Selectors
            Text("Platform Engine Tag:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                platformList.forEach { pt ->
                    val isSelected = pt == targetPlatform
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentPurple.copy(alpha = 0.3f) else DarkSurface)
                            .border(1.dp, if (isSelected) AccentPurple else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { targetPlatform = pt }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(text = pt, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Multi categories tagged selector
            Text("Tag categories (Select multiple):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Spacer(modifier = Modifier.height(4.dp))

            viewModel.mainCategories.forEach { mainCategory ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Checkbox for major focus tag
                    val hasMain = selectedTags.contains(mainCategory)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (hasMain) selectedTags.remove(mainCategory) else selectedTags.add(mainCategory)
                            }
                    ) {
                        Checkbox(
                            checked = hasMain,
                            onCheckedChange = { checked ->
                                if (checked == true) selectedTags.add(mainCategory) else selectedTags.remove(mainCategory)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryTeal,
                                uncheckedColor = BorderColor,
                                checkmarkColor = DarkBackground
                            )
                        )
                        Text(mainCategory, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    }

                    // Display sublevel horizontal items
                    val subcats = viewModel.subCategoriesMap[mainCategory] ?: emptyList()
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.padding(start = 32.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subcats.forEach { sub ->
                            val hasSub = selectedTags.contains(sub)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (hasSub) SecondaryAqua.copy(alpha = 0.25f) else DarkSurface)
                                    .border(1.dp, if (hasSub) SecondaryAqua else BorderColor, RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (hasSub) selectedTags.remove(sub) else selectedTags.add(sub)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = sub,
                                    fontSize = 11.sp,
                                    color = if (hasSub) SecondaryAqua else textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank() || content.isBlank()) {
                        Toast.makeText(context, "Please write a title and prompt text first.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (selectedTags.isEmpty()) {
                        // Default to text category tag
                        selectedTags.add("Text Generation")
                    }

                    viewModel.savePrompt(
                        title = title,
                        content = content,
                        instruction = instruction,
                        targetPlatform = targetPlatform,
                        categories = selectedTags.toList()
                    )

                    Toast.makeText(context, "Custom prompt saved to library!", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_manual_prompt_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save to Library", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
            }
        }
    }
}
