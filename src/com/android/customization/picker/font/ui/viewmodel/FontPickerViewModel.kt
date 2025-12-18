package com.android.customization.picker.font.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.customization.model.font.FontManager
import com.android.customization.model.font.FontOption
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class FontPickerViewModel(
    private val fontManager: FontManager
) : ViewModel() {

    private val _fontOptions = MutableStateFlow<List<FontOption>>(emptyList())
    val fontOptions: StateFlow<List<FontOption>> = _fontOptions.asStateFlow()

    private val _selectedOption = MutableStateFlow<FontOption?>(null)
    val selectedOption: StateFlow<FontOption?> = _selectedOption.asStateFlow()

    private val _activeOption = MutableStateFlow<FontOption?>(null)
    val activeOption: StateFlow<FontOption?> = _activeOption.asStateFlow()

    private val _appliedOption = MutableStateFlow<FontOption?>(null)

    private val _applyEvent = Channel<Unit>(Channel.BUFFERED)
    val applyEvent = _applyEvent.receiveAsFlow()

    val isApplyVisible: StateFlow<Boolean> = combine(_selectedOption, _appliedOption) { selected, applied ->
        if (selected == null || applied == null) false
        else selected.packageName != applied.packageName
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val onApply: Flow<(suspend () -> Unit)?> = isApplyVisible.map { visible ->
        if (visible) {
            suspend {
                val option = _selectedOption.value
                if (option != null) {
                    fontManager.apply(option, null)
                    _appliedOption.value = option
                    _activeOption.value = option
                    _applyEvent.trySend(Unit)
                }
            }
        } else null
    }

    init {
        fontManager.fetchOptions({ options ->
            _fontOptions.value = options
            val active = options.firstOrNull { fontManager.isActive(it) } ?: options.firstOrNull()
            _selectedOption.value = active
            _appliedOption.value = active
            _activeOption.value = active
        }, true)
    }

    fun selectFont(option: FontOption) {
        _selectedOption.value = option
    }

    class Factory @Inject constructor(
        private val fontManager: FontManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FontPickerViewModel(fontManager) as T
        }
    }
}
