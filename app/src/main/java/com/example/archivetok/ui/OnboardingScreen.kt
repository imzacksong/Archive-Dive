package com.example.archivetok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun OnboardingScreen(
    onFinish: (Set<String>) -> Unit
) {
    val availableTags = com.example.archivetok.data.model.Tags.availableTags
    val selectedTags = remember { mutableStateListOf<String>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color(0xFF1A1A1A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding() // Ensure bottom nav bar is handled
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Welcome to Archive Dive",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tailor your vintage feed.",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp
            )
            
            // Select All / Deselect All
            val allSelected = selectedTags.size == availableTags.size
            Text(
                 text = if (allSelected) "Deselect All" else "Select All",
                 color = MaterialTheme.colorScheme.primary,
                 fontWeight = FontWeight.Bold,
                 modifier = Modifier
                     .padding(vertical = 16.dp)
                     .clickable {
                         if (allSelected) {
                             selectedTags.clear()
                         } else {
                             selectedTags.clear()
                             selectedTags.addAll(availableTags)
                         }
                     }
            )

            // Scrollable Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f) // Push button to bottom
                    .padding(bottom = 80.dp) // Space for button
            ) {
                items(availableTags) { tag ->
                    val isSelected = selectedTags.contains(tag)
                    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF333333)
                    val contentColor = if (isSelected) Color.Black else Color.White
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(backgroundColor)
                            .clickable {
                                if (isSelected) selectedTags.remove(tag)
                                else selectedTags.add(tag)
                            }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tag,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Button pinned to bottom
        Button(
            onClick = { onFinish(selectedTags.toSet()) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Start Watching",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
