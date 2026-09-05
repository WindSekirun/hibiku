package io.github.windsekirun.hibiku.feature.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.windsekirun.hibiku.feature.R

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

@Preview(name = "Custom Hex Input Field Preview", showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
fun CustomHexInputFieldPreview() {
    MaterialTheme {
        CustomHexInputField(
            colorHex = "#5CB3FF",
            onColorHexChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
