package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.GeminiFoodAssistant
import com.example.data.model.Store
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
  val text: String,
  val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFoodAssistantDialog(
  stores: List<Store>,
  currentCity: String,
  onDismiss: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val assistant = remember { GeminiFoodAssistant() }

  var messages by remember {
    mutableStateOf(
      listOf(
        ChatMessage(
          text = "Assalam-o-Alaikum! Main MealVerse AI Food Assistant hoon. $currentCity ke best khano aur budget meals ke mutabiq aap kya poochna chahte hain? (e.g., 'Mere paas Rs. 500 hain', 'Achi biryani kahan milegi?')",
          isUser = false
        )
      )
    )
  }

  var inputText by remember { mutableStateOf("") }
  var isThinking by remember { mutableStateOf(false) }

  val quickPrompts = listOf(
    "Mere paas Rs. 500 hain, kya order karun?",
    "Mujhe sasti biryani chahiye",
    "Aaj dinner ke liye kya best hai?",
    "Achi karahi kahan milegi?",
    "4 logon ke liye family meal"
  )

  fun sendMessage(prompt: String) {
    if (prompt.isBlank() || isThinking) return
    val userMsg = ChatMessage(text = prompt, isUser = true)
    messages = messages + userMsg
    inputText = ""
    isThinking = true

    scope.launch {
      val responseText = assistant.askFoodAdvice(prompt, stores, currentCity)
      isThinking = false
      messages = messages + ChatMessage(text = responseText, isUser = false)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.85f)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MealVerseDeepTeal)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("MealVerse AI Food Advisor", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
              Text("Powered by Gemini • $currentCity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Quick suggestions
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 4.dp)
        ) {
          items(quickPrompts) { p ->
            SuggestionChip(
              onClick = { sendMessage(p) },
              label = { Text(p, style = MaterialTheme.typography.bodySmall) }
            )
          }
        }

        // Messages list
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(messages) { msg ->
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
            ) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (msg.isUser) MealVerseDeepTeal else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp)
              ) {
                Text(
                  text = msg.text,
                  color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(12.dp),
                  style = MaterialTheme.typography.bodyMedium
                )
              }
            }
          }

          if (isThinking) {
            item {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
              ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MealVerseDeepTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing local menus & prices...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        // Input bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text("Ask in Urdu or English...") },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("ai_chat_input"),
            singleLine = true
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = { sendMessage(inputText) },
            colors = IconButtonDefaults.iconButtonColors(containerColor = MealVerseDeepTeal, contentColor = Color.White),
            modifier = Modifier.testTag("ai_send_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
          }
        }
      }
    }
  }
}
