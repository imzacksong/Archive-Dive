package com.example.archivetok.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.archivetok.data.local.ExhibitEntity
import com.example.archivetok.ui.theme.NeonGreen
import com.example.archivetok.ui.theme.ArchiveYellow

@Composable
fun MuseumScreen(
    exhibits: List<ExhibitEntity>,
    onExhibitSelected: (ExhibitEntity) -> Unit,
    onCreateExhibit: () -> Unit,
    onUpdateExhibit: (Long, String, Int?, Boolean) -> Unit,
    onUpdateAllThemes: (Int?) -> Unit,
    isAudioMode: Boolean = false,
    brandColor: Color = Color.Yellow,
    modifier: Modifier = Modifier
) {
    var exhibitToEdit by remember { mutableStateOf<ExhibitEntity?>(null) }

    if (exhibitToEdit != null) {
        EditExhibitDialog(
            exhibit = exhibitToEdit!!,
            onDismissRequest = { exhibitToEdit = null },
            onConfirm = { name, theme, useCover ->
                onUpdateExhibit(exhibitToEdit!!.id, name, theme, useCover)
                exhibitToEdit = null
            },
            onApplyToAll = { theme ->
                onUpdateAllThemes(theme)
                exhibitToEdit = null
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(Color.Black),
        containerColor = Color.Black,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateExhibit,
                containerColor = brandColor,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Exhibit")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text(
                text = "Museum",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = brandColor,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            )

            if (exhibits.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No exhibits yet",
                            color = Color.Gray,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Create one to start curating",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(exhibits) { exhibit ->
                        ExhibitItem(
                            exhibit = exhibit,
                            isAudioMode = isAudioMode,
                            onClick = { onExhibitSelected(exhibit) },
                            onEdit = { exhibitToEdit = exhibit }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExhibitItem(
    exhibit: ExhibitEntity,
    isAudioMode: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onEdit
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        ) {
            if (exhibit.coverImage != null && exhibit.useCoverImage) {
                coil.compose.AsyncImage(
                    model = exhibit.coverImage,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Gradient Overlay for Text Readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                startY = 0.4f
                            )
                        )
                )
            } else {
                // Generated Gradient Placeholder
                val colors = remember(exhibit.id, exhibit.colorTheme, isAudioMode) { 
                    getExhibitColors(exhibit.id, exhibit.colorTheme, isAudioMode) 
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = colors,
                                start = androidx.compose.ui.geometry.Offset.Zero,
                                end = androidx.compose.ui.geometry.Offset.Infinite
                            )
                        )
                )
                
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }
            
            // Edit Icon
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = exhibit.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = "Exhibit", 
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

// Deterministic Gradient Generator
object MuseumPalettes {
    val palettes = listOf(
        listOf(Color(0xFFEF5350), Color(0xFFC62828)), // Red
        listOf(Color(0xFFEC407A), Color(0xFFAD1457)), // Pink
        listOf(Color(0xFFAB47BC), Color(0xFF6A1B9A)), // Purple
        listOf(Color(0xFF7E57C2), Color(0xFF4527A0)), // Deep Purple
        listOf(Color(0xFF5C6BC0), Color(0xFF283593)), // Indigo
        listOf(Color(0xFF42A5F5), Color(0xFF1565C0)), // Blue
        listOf(Color(0xFF26C6DA), Color(0xFF00838F)), // Cyan
        listOf(Color(0xFF26A69A), Color(0xFF00695C)), // Teal
        listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)), // Green
        listOf(Color(0xFFFFCA28), Color(0xFFFF8F00)), // Amber
        listOf(Color(0xFFFF7043), Color(0xFFD84315)), // Deep Orange
        listOf(Color(0xFF8D6E63), Color(0xFF5D4037)), // Brown
        listOf(Color(0xFF78909C), Color(0xFF455A64))  // Blue Grey
    )
    
    fun getPaletteResult(id: Long, themeId: Int?, isAudioMode: Boolean = false): List<Color> {
         if (themeId != null && themeId >= 0 && themeId < palettes.size) {
             return palettes[themeId]
         }
         // User requested DEFAULT
         if (isAudioMode) {
             return listOf(NeonGreen, Color(0xFF006400)) // Neon Green to Dark Green
         }
         // Default Yellow
         return listOf(ArchiveYellow, Color(0xFFFBC02D))
    }
}

// Helper for backward compatibility or direct usage
fun getExhibitColors(id: Long, themeId: Int? = null, isAudioMode: Boolean = false): List<Color> {
    return MuseumPalettes.getPaletteResult(id, themeId, isAudioMode)
}

