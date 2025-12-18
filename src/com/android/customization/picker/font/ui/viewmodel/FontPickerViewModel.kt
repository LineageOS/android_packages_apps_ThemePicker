/*
 * Copyright (C) The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.customization.picker.font.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.customization.model.font.FontManager
import com.android.customization.model.font.FontOption
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

class FontPickerViewModel(private val fontManager: FontManager) : ViewModel() {

    private val _fontOptions = MutableStateFlow<List<FontOption>>(emptyList())
    val fontOptions: StateFlow<List<FontOption>> = _fontOptions.asStateFlow()

    private val _selectedOption = MutableStateFlow<FontOption?>(null)
    val selectedOption: StateFlow<FontOption?> = _selectedOption.asStateFlow()

    private val _activeOption = MutableStateFlow<FontOption?>(null)
    val activeOption: StateFlow<FontOption?> = _activeOption.asStateFlow()

    private val _appliedOption = MutableStateFlow<FontOption?>(null)

    private val _applyEvent = Channel<Unit>(Channel.BUFFERED)
    val applyEvent = _applyEvent.receiveAsFlow()

    val isApplyVisible: StateFlow<Boolean> =
        combine(_selectedOption, _appliedOption) { selected, applied ->
                if (selected == null || applied == null) false
                else selected.packageName != applied.packageName
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val onApply: Flow<(suspend () -> Unit)?> =
        isApplyVisible.map { visible ->
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
        fontManager.fetchOptions(
            { options ->
                _fontOptions.value = options
                val active =
                    options.firstOrNull { fontManager.isActive(it) } ?: options.firstOrNull()
                _selectedOption.value = active
                _appliedOption.value = active
                _activeOption.value = active
            },
            true,
        )
    }

    fun selectFont(option: FontOption) {
        _selectedOption.value = option
    }

    class Factory @Inject constructor(private val fontManager: FontManager) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FontPickerViewModel(fontManager) as T
        }
    }
}
