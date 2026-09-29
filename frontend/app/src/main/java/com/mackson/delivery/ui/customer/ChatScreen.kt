package com.mackson.delivery.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mackson.delivery.data.model.SubstitutionMessage
import com.mackson.delivery.data.model.SubstitutionSenderRole
import com.mackson.delivery.data.remote.FirebaseAuthManager
import com.mackson.delivery.data.remote.FirebaseRealtimeService
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Real-time substitution chat between customer and picker — Part 1 US-14 / US-17.
 * Backed by Firebase Realtime Database so new messages stream in over the existing WebSocket
 * connection without rewriting the whole chat document (Part 1 section 9.3.11).
 */
class ChatViewModel(
    private val orderId: String,
    private val realtimeService: FirebaseRealtimeService = FirebaseRealtimeService,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _messages = MutableStateFlow<List<SubstitutionMessage>>(emptyList())
    val messages: StateFlow<List<SubstitutionMessage>> = _messages.asStateFlow()

    fun start() {
        realtimeService.observeSubstitutionChat(orderId).onEach { _messages.value = it }.launchIn(viewModelScope)
    }

    fun respond(message: SubstitutionMessage, approved: Boolean) {
        val proposedProductId = message.proposedProductId ?: return
        viewModelScope.launch {
            realtimeService.setSubstitutionApproval(orderId, message.messageId, approved)
            firestoreRepository.respondToSubstitution(orderId, proposedProductId, approved)
        }
    }

    /** Free-form chat, per US-14 ("supporting instant text messages") — written directly to the
     * Realtime Database since it carries no business logic that needs server-side validation,
     * unlike proposeSubstitution/respondToSubstitution which do. */
    fun sendMessage(text: String) {
        val senderId = FirebaseAuthManager.getCurrentUserId() ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            realtimeService.sendLiveChatMessage(
                orderId,
                SubstitutionMessage(
                    senderId = senderId,
                    senderRole = SubstitutionSenderRole.CUSTOMER,
                    timestamp = System.currentTimeMillis(),
                    payloadText = text
                )
            )
        }
    }
}

@Composable
fun ChatScreen(orderId: String) {
    val viewModel: ChatViewModel = viewModel(factory = viewModelFactory { ChatViewModel(orderId) })
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    viewModel.start()
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Chat with your picker", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
            items(messages, key = { it.messageId }) { message -> ChatBubble(message, onRespond = viewModel::respond) }
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Message") },
                modifier = Modifier.weight(1f)
            )
            PrimaryButton("Send") {
                viewModel.sendMessage(draft)
                draft = ""
            }
        }
    }
}

@Composable
private fun ChatBubble(message: SubstitutionMessage, onRespond: (SubstitutionMessage, Boolean) -> Unit) {
    val isPicker = message.senderRole == SubstitutionSenderRole.PICKER
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                if (isPicker) "Your picker" else if (message.senderRole == SubstitutionSenderRole.CUSTOMER) "You" else "System",
                style = MaterialTheme.typography.labelLarge
            )
            Text(message.payloadText, style = MaterialTheme.typography.bodyLarge)
            if (isPicker && message.proposedProductId != null && message.isApproved == null) {
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Approve") { onRespond(message, true) }
                    PrimaryButton("Decline") { onRespond(message, false) }
                }
            }
        }
    }
}
