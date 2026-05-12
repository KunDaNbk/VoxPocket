package com.voxpocket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voxpocket.data.repository.ChatRepository
import com.voxpocket.service.LlamaServerManager
import com.voxpocket.util.PreferencesHelper

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val serverManager: LlamaServerManager,
    private val preferencesHelper: PreferencesHelper
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, serverManager, preferencesHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
