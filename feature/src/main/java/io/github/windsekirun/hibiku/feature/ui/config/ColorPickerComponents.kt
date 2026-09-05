package io.github.windsekirun.hibiku.feature.ui.config

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.windsekirun.hibiku.feature.R

data class ColorPreset(
    val name: String,
    val hex: String,
    val color: Color
)

val PRESET_COLORS: List<ColorPreset> = listOf(
    ColorPreset("White", "#FFFFFF", Color(0xFFFFFFFF)),
    ColorPreset("Shirakami Fubuki Cosmic Blue", "#5CB3FF", Color(0xFF5CB3FF)),
    ColorPreset("Star Gold", "#FAD02C", Color(0xFFFAD02C)),
    ColorPreset("Neon Purple", "#A855F7", Color(0xFFA855F7)),
    ColorPreset("Mint", "#10B981", Color(0xFF10B981)),
    ColorPreset("Coral", "#F43F5E", Color(0xFFF43F5E)),
    ColorPreset("Amber", "#F59E0B", Color(0xFFF59E0B))
)

fun normalizeHex(hex: String): String {
    val trimmed = hex.trim()
    return if (trimmed.startsWith("#")) trimmed.uppercase() else "#${trimmed.uppercase()}"
}

fun isValidHexColor(hex: String): Boolean {
    val normalized = normalizeHex(hex)
    return Regex("^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$").matches(normalized)
}

fun parseColorIntOrNull(hex: String): Int? {
    if (!isValidHexColor(hex)) return null
    val rawHex = normalizeHex(hex).removePrefix("#")
    return runCatching {
        when (rawHex.length) {
            6 -> ((0xFFL shl 24) or rawHex.toLong(16)).toInt()
            8 -> rawHex.toLong(16).toInt()
            else -> null
        }
    }.getOrNull()
}

fun parseColorOrNull(hex: String): Color? {
    return parseColorIntOrNull(hex)?.let { Color(it) }
}

@Composable
fun ColorPickerComponents(
    useDynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    selectedColorHex: String,
    onColorHexChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dynamic Color / Material You Switch Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDynamicColorChange(!useDynamicColor) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.config_dynamic_color_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.config_dynamic_color_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useDynamicColor,
                    onCheckedChange = onDynamicColorChange
                )
            }
        }

        // Preset Colors and Custom Hex Input
        AnimatedVisibility(
            visible = !useDynamicColor,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preset Color Swatches
                Text(
                    text = stringResource(R.string.config_preset_colors),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val normalizedSelected = normalizeHex(selectedColorHex)

                    PRESET_COLORS.forEach { preset ->
                        val isSelected = normalizeHex(preset.hex).equals(normalizedSelected, ignoreCase = true)
                        ColorSwatchChip(
                            preset = preset,
                            isSelected = isSelected,
                            onClick = {
                                onColorHexChange(preset.hex)
                            }
                        )
                    }
                }

                // Custom HEX Input
                CustomHexInputField(
                    colorHex = selectedColorHex,
                    onColorHexChange = onColorHexChange
                )
            }
        }
    }
}

@Composable
fun ColorSwatchChip(
    preset: ColorPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val borderWidth = if (isSelected) 3.dp else 1.dp
    val checkmarkTint = if (preset.hex.equals("#FFFFFF", ignoreCase = true) ||
        preset.hex.equals("#FAD02C", ignoreCase = true)) {
        Color.Black
    } else {
        Color.White
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(preset.color)
                .border(borderWidth, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkmarkTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = preset.name.split(" ").last(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CustomHexInputField(
    colorHex: String,
    onColorHexChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember(colorHex) {
        mutableStateOf(if (colorHex.startsWith("#")) colorHex else "#$colorHex")
    }

    val parsedColor = remember(textInput) { parseColorOrNull(textInput) }
    val isValid = isValidHexColor(textInput)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.config_custom_hex),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = textInput,
            onValueChange = { input ->
                val filtered = input.filter { it == '#' || it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                    .take(9)
                textInput = filtered
                if (isValidHexColor(filtered)) {
                    onColorHexChange(normalizeHex(filtered))
                }
            },
            singleLine = true,
            isError = !isValid && textInput.isNotBlank(),
            label = { Text(stringResource(R.string.config_custom_hex)) },
            placeholder = { Text(stringResource(R.string.config_custom_hex_hint)) },
            supportingText = {
                if (!isValid && textInput.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.config_custom_hex_error),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(stringResource(R.string.config_custom_hex_helper))
                }
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(parsedColor ?: Color.Transparent)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// -------------------------------------------------------------------------
// Compose UI Previews
// -------------------------------------------------------------------------

@Preview(name = "Color Picker - Dynamic Color Enabled", showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
fun ColorPickerComponentsDynamicPreview() {
    MaterialTheme {
        ColorPickerComponents(
            useDynamicColor = true,
            onDynamicColorChange = {},
            selectedColorHex = "#5CB3FF",
            onColorHexChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Color Picker - Custom Hex / Presets", showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
fun ColorPickerComponentsCustomPreview() {
    MaterialTheme {
        ColorPickerComponents(
            useDynamicColor = false,
            onDynamicColorChange = {},
            selectedColorHex = "#5CB3FF",
            onColorHexChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

