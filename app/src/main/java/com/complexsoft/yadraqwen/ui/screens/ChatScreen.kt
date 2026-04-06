package com.complexsoft.yadraqwen.ui.screens

import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.complexsoft.yadraqwen.data.ChatMessage
import com.complexsoft.yadraqwen.data.MessageRole
import com.complexsoft.yadraqwen.ui.ChatViewModel
import com.complexsoft.yadraqwen.ui.LoadState
import com.complexsoft.yadraqwen.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatRootScreen(vm: ChatViewModel = viewModel()) {
    val context   = LocalContext.current
    val loadState by vm.loadState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.initialize(context) }

    AnimatedContent(
        targetState = loadState,
        transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(400)) },
        label = "root_transition"
    ) { state ->
        when (state) {
            is LoadState.Ready -> ChatScreen(vm)
            is LoadState.Error -> ErrorScreen(state.message) { vm.initialize(context) }
            else               -> LoadingScreen(
                message = when (state) {
                    is LoadState.Loading -> state.message
                    else                 -> "Iniciando…"
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOADING SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoadingScreen(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseOut), RepeatMode.Restart),
        label = "ring1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseOut), RepeatMode.Restart),
        label = "alpha1"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = EaseOut, delayMillis = 700), RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = EaseOut, delayMillis = 700), RepeatMode.Restart
        ),
        label = "alpha2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(listOf(VulkanRedGlow, QwenPurpleGlow, Color.Transparent)),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(ring1Scale)
                .alpha(ring1Alpha)
                .drawBehind {
                    drawCircle(
                        color = VulkanRed,
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(ring2Scale)
                .alpha(ring2Alpha)
                .drawBehind {
                    drawCircle(
                        color = QwenPurple,
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(VulkanRed, QwenPurple),
                            start = Offset.Zero,
                            end = Offset(200f, 200f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Y",
                    style = TextStyle(
                        color      = Color.White,
                        fontSize   = 34.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = (-1).sp
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text  = "YADRA",
                style = TextStyle(
                    color         = TextPrimary,
                    fontSize      = 20.sp,
                    fontWeight    = FontWeight.W700,
                    letterSpacing = 6.sp
                )
            )
            Text(
                text  = "Qwen3 0.6B · Vulkan",
                style = TextStyle(
                    color         = TextSecondary,
                    fontSize      = 11.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(Modifier.height(40.dp))

            AnimatedContent(
                targetState = message,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "progress_text"
            ) { msg ->
                println(msg)
                Text(
                    text  = "Loading Weights on GPU…",
                    style = TextStyle(
                        color      = VulkanRed.copy(alpha = 0.8f),
                        fontSize   = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Spacer(Modifier.height(12.dp))
            DotsLoader()
        }
    }
}

@Composable
fun DotsLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..2).forEach { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(600, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 200)
                ),
                label = "dot_$i"
            )
            val dotColor = if (i % 2 == 0) VulkanRed else QwenPurple
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .alpha(alpha)
                    .background(dotColor, CircleShape)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ERROR SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("⚠", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text       = "Error al cargar el modelo",
                color      = TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.W600
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = message,
                color      = TextSecondary,
                fontSize   = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors  = ButtonDefaults.buttonColors(containerColor = VulkanRed)
            ) {
                Text("Reintentar", color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHAT SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val messages     by vm.messages.collectAsStateWithLifecycle()
    val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
    val thinkingOn   by vm.thinkingEnabled.collectAsStateWithLifecycle()
    val liveTps      by vm.liveTps.collectAsStateWithLifecycle()        // ← NUEVO
    val tokenCount   by vm.tokenCount.collectAsStateWithLifecycle()     // ← NUEVO
    val lastStats    by vm.lastStats.collectAsStateWithLifecycle()      // ← NUEVO
    val listState    = rememberLazyListState()
    val scope        = rememberCoroutineScope()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty())
            listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
            .statusBarsPadding()
    ) {
        TopBar(
            isGenerating = isGenerating,
            onClear      = { vm.clearConversation() }
        )

        // ── Área de mensajes ─────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                WelcomePlaceholder()
            } else {
                LazyColumn(
                    state               = listState,
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val visible = remember {
                            MutableTransitionState(false).apply { targetState = true }
                        }
                        Column {
                            AnimatedVisibility(
                                visibleState = visible,
                                enter        = slideInVertically(
                                    initialOffsetY = { it / 3 },
                                    animationSpec  = spring(dampingRatio = 0.75f)
                                ) + fadeIn(tween(300)),
                            ) {
                                MessageBubble(msg)
                            }
                        }
                    }
                }
            }
        }

        // ── Stats bar — vive entre los mensajes y el teclado ─────────────────
        InferenceStatsBar(                                              // ← NUEVO
            isGenerating = isGenerating,
            liveTps      = liveTps,
            tokenCount   = tokenCount,
            finalStats   = lastStats,
            modifier     = Modifier.fillMaxWidth()
        )

        // ── Input ────────────────────────────────────────────────────────────
        InputBar(
            isGenerating  = isGenerating,
            thinkingOn    = thinkingOn,
            onToggleThink = { vm.toggleThinking() },
            onSend        = { text ->
                vm.sendMessage(text)
                scope.launch {
                    delay(100)
                    if (messages.isNotEmpty())
                        listState.animateScrollToItem(messages.size - 1)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TopBar(
    isGenerating: Boolean,
    onClear:      () -> Unit
) {
    val statusColor = if (isGenerating) ThinkAmber else StatusReady
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val statusAlpha by infiniteTransition.animateFloat(
        initialValue  = if (isGenerating) 0.3f else 1f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (isGenerating) 800 else 1, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(listOf(VulkanRed, QwenPurple))
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text  = "YADRA INFERENCE",
                style = TextStyle(
                    color         = Color.White,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.W800,
                    letterSpacing = 2.sp
                )
            )
        }

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(statusAlpha)
                .background(statusColor, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text  = if (isGenerating) "Generating…" else "Qwen3 0.6B",
            style = TextStyle(
                color         = TextSecondary,
                fontSize      = 11.sp,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Elevated)
                .clickable(onClick = onClear),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WELCOME PLACEHOLDER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WelcomePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(VulkanRed, QwenPurple),
                        start = Offset.Zero, end = Offset(200f, 200f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("Y", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.W800)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text       = "How can I help you?",
            color      = TextPrimary,
            fontSize   = 20.sp,
            fontWeight = FontWeight.W600
        )
        Spacer(Modifier.height(6.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MESSAGE BUBBLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp, top = 2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(VulkanRed, QwenPurple),
                            start = Offset.Zero, end = Offset(80f, 80f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Y", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.W700)
            }
        }

        Column(
            modifier            = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            val showThinkingBlock = message.isThinkingEnabled &&
                    (message.thinkingContent != null ||
                            (message.isStreaming && !message.isThinkingComplete))

            if (showThinkingBlock) {
                ThinkingBlock(
                    content            = message.thinkingContent,
                    isComplete         = message.isThinkingComplete,
                    durationSec        = message.thinkingDurationSec,
                    isMessageStreaming = message.isStreaming
                )
                Spacer(Modifier.height(6.dp))
            }

            if (message.content.isNotBlank() ||
                message.isThinkingComplete ||
                (message.isStreaming && !message.isThinkingEnabled) ||
                (!message.isStreaming && message.thinkingContent == null)) {
                if (isUser) UserBubble(content = message.content)
                else AssistantBubble(content = message.content, isStreaming = message.isStreaming)
            }
        }
    }
}

@Composable
fun UserBubble(content: String) {
    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = 18.dp, topEnd = 4.dp,
                    bottomStart = 18.dp, bottomEnd = 18.dp
                )
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2A0A1A), QwenPurpleDim),
                    start = Offset.Zero, end = Offset(0f, 200f)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(QwenPurple.copy(alpha = 0.5f), VulkanRed.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 4.dp,
                    bottomStart = 18.dp, bottomEnd = 18.dp
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text  = content,
            style = TextStyle(color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTENT PARSING
// ─────────────────────────────────────────────────────────────────────────────
sealed class ContentSegment {
    data class Text(val content: String)                          : ContentSegment()
    data class Code(val language: String, val content: String)   : ContentSegment()
    data class Latex(val content: String, val isBlock: Boolean)  : ContentSegment()
}

fun parseContent(text: String): List<ContentSegment> {
    val segments  = mutableListOf<ContentSegment>()
    // Matches: ```code```, $$block latex$$, $inline latex$
    val blockRegex = Regex(
        """```(\w*)\n([\s\S]*?)```|\$\$([\s\S]*?)\$\$|\$((?!\s)[\s\S]*?(?<!\s))\$"""
    )
    var lastEnd = 0

    for (match in blockRegex.findAll(text)) {
        if (match.range.first > lastEnd)
            segments.add(ContentSegment.Text(text.substring(lastEnd, match.range.first)))

        when {
            // ```code``` — group 1 = lang, group 2 = body
            match.groupValues[2].isNotEmpty() || match.groupValues[1].isNotEmpty() -> {
                segments.add(
                    ContentSegment.Code(
                        language = match.groupValues[1].ifBlank { "text" },
                        content  = match.groupValues[2].trimEnd()
                    )
                )
            }
            // $$block latex$$
            match.groupValues[3].isNotEmpty() -> {
                segments.add(ContentSegment.Latex(match.groupValues[3].trim(), isBlock = true))
            }
            // $inline$
            match.groupValues[4].isNotEmpty() -> {
                segments.add(ContentSegment.Latex(match.groupValues[4].trim(), isBlock = false))
            }
        }
        lastEnd = match.range.last + 1
    }
    if (lastEnd < text.length)
        segments.add(ContentSegment.Text(text.substring(lastEnd)))

    return segments
}

// ─────────────────────────────────────────────────────────────────────────────
// INLINE BOLD PARSER  (*word* or *phrase*)
// ─────────────────────────────────────────────────────────────────────────────
private val BoldHighlight = Color(0xFFFFD580)   // amber — contrasts on dark bg

fun parseInlineBold(text: String): AnnotatedString = buildAnnotatedString {
    val regex  = Regex("""\*([^*\n]+)\*""")
    var cursor = 0
    for (match in regex.findAll(text)) {
        if (match.range.first > cursor)
            withStyle(SpanStyle(color = TextPrimary)) {
                append(text.substring(cursor, match.range.first))
            }
        withStyle(
            SpanStyle(
                color      = BoldHighlight,
                fontWeight = FontWeight.W700,
                background = BoldHighlight.copy(alpha = 0.08f)
            )
        ) { append(match.groupValues[1]) }
        cursor = match.range.last + 1
    }
    if (cursor < text.length)
        withStyle(SpanStyle(color = TextPrimary)) { append(text.substring(cursor)) }
}

// ─────────────────────────────────────────────────────────────────────────────
// LATEX VIEW  (KaTeX via WebView)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LatexView(latex: String, isBlock: Boolean) {
    // Escape chars that would break the JS template literal
    val escaped = latex
        .replace("\\", "\\\\")
        .replace("`",  "\\`")
        .replace("$",  "\\$")

    val html = """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <link rel="stylesheet"
              href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css">
        <script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>
        <style>
          html,body { margin:0; padding:0; background:transparent; }
          #math {
            color: #E6EDF3;
            font-size: ${if (isBlock) "15" else "14"}px;
            padding: ${if (isBlock) "10px 0" else "0"};
            text-align: ${if (isBlock) "center" else "left"};
            display: ${if (isBlock) "block" else "inline"};
          }
          .katex-html { overflow-x: auto; }
        </style></head><body>
        <div id="math"></div>
        <script>
          katex.render(`$escaped`, document.getElementById('math'), {
            displayMode: $isBlock,
            throwOnError: false,
            output: 'html'
          });
        </script></body></html>
    """.trimIndent()

    // Fixed height needed for LazyColumn — bump isBlock if you render tall matrices
    val height = if (isBlock) 80.dp else 28.dp

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        factory  = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
            }
        },
        update = { wv ->
            wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SYNTAX HIGHLIGHTING
// ─────────────────────────────────────────────────────────────────────────────
private val CodeBackground = Color(0xFF0D1117)
private val CodeKeyword    = Color(0xFFFF7B72)
private val CodeFunction   = Color(0xFFD2A8FF)
private val CodeString     = Color(0xFFA5D6FF)
private val CodeComment    = Color(0xFF8B949E)
private val CodeNumber     = Color(0xFFF2CC60)
private val CodeDefault    = Color(0xFFE6EDF3)

@Composable
fun SyntaxHighlightedCode(code: String, language: String) {
    val pythonKeywords = setOf(
        "def", "return", "import", "from", "class", "if", "else", "elif",
        "for", "while", "in", "not", "and", "or", "True", "False", "None",
        "try", "except", "with", "as", "pass", "break", "continue", "lambda",
        "print", "len", "range", "self", "yield", "raise", "del", "global"
    )

    val annotated = buildAnnotatedString {
        val lines = code.lines()
        lines.forEachIndexed { lineIdx, line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("#")) {
                withStyle(SpanStyle(color = CodeComment)) { append(line) }
            } else {
                var i = 0
                while (i < line.length) {
                    when {
                        line[i] == '"' || line[i] == '\'' -> {
                            val quote = line[i]
                            var j = i + 1
                            while (j < line.length && line[j] != quote) j++
                            val str = line.substring(i, minOf(j + 1, line.length))
                            withStyle(SpanStyle(color = CodeString)) { append(str) }
                            i = minOf(j + 1, line.length)
                        }
                        line[i].isDigit() -> {
                            var j = i
                            while (j < line.length && (line[j].isDigit() || line[j] == '.')) j++
                            withStyle(SpanStyle(color = CodeNumber)) { append(line.substring(i, j)) }
                            i = j
                        }
                        line[i].isLetter() || line[i] == '_' -> {
                            var j = i
                            while (j < line.length && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                            val word   = line.substring(i, j)
                            val isFunc = j < line.length && line[j] == '('
                            val color  = when {
                                word in pythonKeywords -> CodeKeyword
                                isFunc                 -> CodeFunction
                                else                   -> CodeDefault
                            }
                            withStyle(SpanStyle(color = color)) { append(word) }
                            i = j
                        }
                        line[i] == '#' -> {
                            withStyle(SpanStyle(color = CodeComment)) { append(line.substring(i)) }
                            i = line.length
                        }
                        else -> {
                            withStyle(SpanStyle(color = CodeDefault)) { append(line[i]) }
                            i++
                        }
                    }
                }
            }
            if (lineIdx < lines.size - 1) append("\n")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CodeBackground)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = language.ifBlank { "code" },
                style = TextStyle(
                    color         = TextSecondary,
                    fontSize      = 11.sp,
                    fontFamily    = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Text(
            text     = annotated,
            style    = TextStyle(
                fontSize   = 13.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ASSISTANT BUBBLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AssistantBubble(content: String, isStreaming: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = if (isStreaming) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    if (isStreaming && content.isEmpty()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(Surface)
                .border(1.dp, Border,
                    RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) { DotsLoader() }
        return
    }

    val segments = remember(content) { parseContent(content) }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(Surface)
            .border(1.dp, Border,
                RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
    ) {
        Row {
            // Gradient side bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(VulkanRed, QwenPurple.copy(alpha = 0.4f))))
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                segments.forEachIndexed { idx, segment ->
                    when (segment) {
                        is ContentSegment.Code -> {
                            SyntaxHighlightedCode(
                                code     = segment.content,
                                language = segment.language
                            )
                        }
                        is ContentSegment.Latex -> {
                            LatexView(
                                latex   = segment.content,
                                isBlock = segment.isBlock
                            )
                        }
                        is ContentSegment.Text -> {
                            // Strip markdown headings
                            val cleaned = segment.content
                                .lines()
                                .joinToString("\n") { line ->
                                    line.trimStart('#', ' ').let { stripped ->
                                        if (line.trimStart().startsWith("#")) stripped.trim()
                                        else line
                                    }
                                }
                                .trim()

                            val displayText = if (isStreaming && idx == segments.size - 1)
                                cleaned + "▋"
                            else cleaned

                            if (displayText.isNotBlank()) {
                                // *word* → amber bold highlight
                                val annotated = remember(displayText) { parseInlineBold(displayText) }
                                Text(
                                    text  = annotated,
                                    style = TextStyle(fontSize = 14.sp, lineHeight = 22.sp)
                                )
                            }
                        }
                    }
                    if (idx < segments.size - 1) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THINKING BLOCK
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ThinkingBlock(
    content:            String?,
    isComplete:         Boolean,
    durationSec:        Int,
    isMessageStreaming: Boolean
) {
    var expanded by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "think_dots")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ThinkAmberDim)
            .border(1.dp, ThinkAmber.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isComplete && isMessageStreaming) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (0..2).forEach { i ->
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue  = 1f,
                            animationSpec = infiniteRepeatable(
                                animation  = tween(500, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse,
                                initialStartOffset = StartOffset(i * 160)
                            ),
                            label = "think_dot_$i"
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .alpha(alpha)
                                .background(ThinkAmber, CircleShape)
                        )
                    }
                }
            } else {
                Text("◈", color = ThinkAmber, fontSize = 11.sp)
            }

            Text(
                text = if (!isComplete && isMessageStreaming)
                    "Thinking…"
                else if (durationSec > 0)
                    "Thought for $durationSec second${if (durationSec != 1) "s" else ""}"
                else
                    "Thought",
                style = TextStyle(
                    color      = ThinkAmber,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.W500
                )
            )

            Spacer(Modifier.weight(1f))

            AnimatedContent(
                targetState = expanded,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "chevron"
            ) { exp ->
                Text(
                    text     = if (exp) "∧" else "∨",
                    color    = ThinkAmber.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        Column {
            AnimatedVisibility(
                visible = expanded && (content != null || (!isComplete && isMessageStreaming)),
                enter   = expandVertically(spring(dampingRatio = 0.8f)) + fadeIn(tween(200)),
                exit    = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column {
                    HorizontalDivider(
                        color     = ThinkAmber.copy(alpha = 0.15f),
                        thickness = 0.5.dp,
                        modifier  = Modifier.padding(horizontal = 14.dp)
                    )
                    if (content.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) { DotsLoader() }
                    } else {
                        Text(
                            text     = content,
                            style    = TextStyle(
                                color      = ThinkAmber.copy(alpha = 0.75f),
                                fontSize   = 12.sp,
                                lineHeight = 19.sp,
                                fontFamily = FontFamily.Monospace,
                                fontStyle  = FontStyle.Italic
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INPUT BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InputBar(
    isGenerating:  Boolean,
    thinkingOn:    Boolean,
    onToggleThink: () -> Unit,
    onSend:        (String) -> Unit
) {
    var text    by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue   = if (focused) VulkanRed.copy(alpha = 0.6f) else Border,
        animationSpec = tween(200),
        label         = "input_border"
    )
    val sendScale by animateFloatAsState(
        targetValue   = if (text.isNotBlank() && !isGenerating) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.5f),
        label         = "send_scale"
    )

    Surface(color = Surface, tonalElevation = 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Elevated)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                BasicTextField(
                    value         = text,
                    onValueChange = { text = it },
                    textStyle     = TextStyle(
                        color      = TextPrimary,
                        fontSize   = 15.sp,
                        lineHeight = 22.sp
                    ),
                    cursorBrush   = SolidColor(VulkanRed),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(
                                text  = if (isGenerating) "Generating…" else "Type a message",
                                style = TextStyle(color = TextSecondary, fontSize = 15.sp)
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThinkChip(enabled = thinkingOn, onToggle = onToggleThink)

                val canSend = text.isNotBlank() && !isGenerating
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .scale(sendScale)
                        .clip(CircleShape)
                        .background(
                            if (isGenerating)
                                Brush.radialGradient(listOf(ThinkAmber, ThinkAmber.copy(0.6f)))
                            else
                                Brush.linearGradient(
                                    listOf(VulkanRed, QwenPurple),
                                    start = Offset.Zero, end = Offset(80f, 80f)
                                )
                        )
                        .clickable(enabled = canSend) {
                            if (canSend) { onSend(text.trim()); text = "" }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (isGenerating) "◼" else "↑",
                        color      = Color.White,
                        fontSize   = if (isGenerating) 14.sp else 18.sp,
                        fontWeight = FontWeight.W700
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THINK CHIP
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ThinkChip(enabled: Boolean, onToggle: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue   = if (enabled) ThinkAmberDim else Color.Transparent,
        animationSpec = tween(250),
        label         = "think_bg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (enabled) ThinkAmber.copy(alpha = 0.7f) else Border,
        animationSpec = tween(250),
        label         = "think_border"
    )
    val textColor by animateColorAsState(
        targetValue   = if (enabled) ThinkAmber else TextSecondary,
        animationSpec = tween(250),
        label         = "think_text"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text     = if (enabled) "⊗" else "⊙",
            color    = textColor,
            fontSize = 13.sp
        )
        Text(
            text  = "Think",
            style = TextStyle(
                color         = textColor,
                fontSize      = 13.sp,
                fontWeight    = FontWeight.W500,
                letterSpacing = 0.2.sp
            )
        )
    }
}