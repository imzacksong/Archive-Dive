package com.example.archivetok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentFilter: String?,
    onFilterSelected: (String?) -> Unit,
    currentLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1E1E), // Dark theme
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Time Travel (Decade)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val decades = listOf(
                "All Time", 
                "1800s", "1900s", "1910s", "1920s", "1930s", "1940s", "1950s",
                "1960s", "1970s", "1980s", "1990s", "2000s", "2010s", "2020s"
            )
            
            // FlowRow for chips
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                decades.forEach { decade ->
                    val isSelected = if (decade == "All Time") currentFilter == null else currentFilter == decade
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newValue = if (decade == "All Time") null else decade
                            onFilterSelected(newValue)
                            onDismissRequest()
                        },
                        label = { Text(decade) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Yellow,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.DarkGray,
                            labelColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Language",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            val languages = listOf(
                "All Languages",
                "English", "Spanish", "French", "German", 
                "Japanese", "Chinese", "Russian", "Italian", 
                "Portuguese", "Hindi", "Arabic"
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { language ->
                    val isSelected = if (language == "All Languages") currentLanguage == null else currentLanguage == language
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newValue = if (language == "All Languages") null else language
                            onLanguageSelected(newValue)
                            // Don't dismiss immediately for multi-filter feel, or do? 
                            // Current behavior is dismiss on decade, so let's dismiss here too for consistency, 
                            // OR keep open to allow mixing. Let's keep open for mixing? 
                            // Actually user might expect valid state immediately. 
                            // Let's NOT dismiss automatically here to allow setting both Decade + Language.
                            // But wait, the previous implementation dismissed on Decade. 
                            // Let's change behavior: ONLY dismiss when clicking outside or a "Done" button? 
                            // For now, let's keep it simple: Select -> Trigger Update -> Remain Open?
                            // Or Select -> Trigger Update -> Dismiss.
                            // The Decade implementation dismisses. I should probably stick to that pattern for now 
                            // OR change the UI to have an "Apply" button.
                            // Given the "TikTok" flow, immediate feedback is nice. 
                            // Let's dismiss for now to match consistency, or maybe I should add an Apply button in a future polish.
                            // Re-reading code: The previous one dismisses. 
                            onDismissRequest() 
                        },
                        label = { Text(language) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Yellow,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.DarkGray,
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}
