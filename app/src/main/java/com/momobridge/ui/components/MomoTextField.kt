package com.momobridge.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoTypography

@Composable
fun MomoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MomoTypography.BodySmall,
            color = MomoColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))

        val fieldModifier = if (focusRequester != null) {
            Modifier.fillMaxWidth().focusRequester(focusRequester)
        } else {
            Modifier.fillMaxWidth()
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, style = MomoTypography.BodyMedium, color = MomoColors.TextTertiary) }
            } else null,
            singleLine = singleLine,
            isError = error != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            shape = MomoShapes.InputShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) MomoColors.StatusFailed else MomoColors.BorderAccent,
                unfocusedBorderColor = if (error != null) MomoColors.StatusFailed else MomoColors.BorderSubtle,
                cursorColor = MomoColors.Gold,
                focusedTextColor = MomoColors.TextPrimary,
                unfocusedTextColor = MomoColors.TextPrimary,
                focusedContainerColor = MomoColors.GroundLight,
                unfocusedContainerColor = MomoColors.GroundLight,
                focusedLabelColor = MomoColors.TextSecondary,
                unfocusedLabelColor = MomoColors.TextTertiary,
                errorTextColor = MomoColors.TextPrimary,
                errorContainerColor = MomoColors.GroundLight
            ),
            modifier = fieldModifier
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                style = MomoTypography.BodySmall,
                color = MomoColors.StatusFailed
            )
        }
    }
}
