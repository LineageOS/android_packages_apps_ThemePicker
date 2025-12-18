package com.android.customization.picker.font.ui.view

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.customization.model.font.FontOption
import com.android.customization.picker.font.ui.viewmodel.FontPickerViewModel

@Composable
fun FontSectionScreen(
    viewModel: FontPickerViewModel,
    isDark: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val options by viewModel.fontOptions.collectAsState()
    val selectedOption by viewModel.selectedOption.collectAsState()
    val context = LocalContext.current

    val colorScheme = remember(isDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (isDark) darkColorScheme() else lightColorScheme()
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ABC/123 preview
            val previewFont = remember(selectedOption) {
                selectedOption?.let {
                    FontFamily(Typeface(it.headlineFont))
                } ?: FontFamily.Default
            }

            // Preview Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "ABC • abc • 123",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = previewFont,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(options) { option ->
                    val isSelected = option == selectedOption
                    FontOptionItem(
                        option = option,
                        isSelected = isSelected,
                        onClick = {
                            viewModel.selectFont(option)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FontOptionItem(
    option: FontOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    val borderWidth = if (isSelected) 2.dp else 1.dp

    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.surface
    else
        Color.Transparent

    val textColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurface

    val fontFamily = remember(option) {
        FontFamily(Typeface(option.headlineFont))
    }

    // Aa
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .size(80.dp)
                .border(borderWidth, borderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = fontFamily),
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scroll on longer strings
        Text(
            text = option.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
        )
    }
}
