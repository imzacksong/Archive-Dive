package com.example.archivetok.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.archivetok.data.local.ExhibitEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToExhibitSheet(
    exhibits: List<ExhibitEntity>,
    onAddToExhibit: (ExhibitEntity) -> Unit,
    onCreateExhibit: (String) -> Unit,
    brandColor: Color,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showCreateInput by remember { mutableStateOf(false) }
    var newExhibitName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Black.copy(alpha = 0.9f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .padding(16.dp)
        ) {
            Text(
                text = "Add to Museum",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = brandColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (showCreateInput) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = newExhibitName,
                        onValueChange = { newExhibitName = it },
                        label = { Text("Exhibit Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = brandColor,
                            unfocusedIndicatorColor = Color.Gray,
                            cursorColor = brandColor,
                            focusedLabelColor = brandColor,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showCreateInput = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                if (newExhibitName.isNotBlank()) {
                                    onCreateExhibit(newExhibitName)
                                    showCreateInput = false
                                    newExhibitName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            enabled = newExhibitName.isNotBlank()
                        ) {
                            Text("Create", color = Color.Black)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showCreateInput = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Exhibit", color = Color.White)
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(exhibits) { exhibit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddToExhibit(exhibit) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exhibit.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Divider(color = Color.Gray.copy(alpha = 0.3f))
                }
                
                if (exhibits.isEmpty() && !showCreateInput) {
                    item {
                        Text(
                            text = "No exhibits yet. Create one to get started!",
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}


