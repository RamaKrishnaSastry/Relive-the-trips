package com.example.ui.home.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TerracottaPrimary

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#C2410C") }
    val colors = listOf("#C2410C", "#D97706", "#047857", "#1D4ED8", "#7E22CE", "#B91C1C", "#8B5CF6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name *") },
                    placeholder = { Text("e.g. Hill Temples, Ancient Chola") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Select Category Color:", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    if (selectedColor == hex) 3.dp else 1.dp,
                                    if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
            ) {
                Text("Save Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
