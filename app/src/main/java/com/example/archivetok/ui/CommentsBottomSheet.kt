package com.example.archivetok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.archivetok.data.model.ArchiveReview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    reviews: List<ArchiveReview>,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Black.copy(alpha = 0.9f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillWrapperMsgHeight(0.7f) // Occupy 70% of screen height
                .padding(bottom = 16.dp)
        ) {
            // Header
            Text(
                text = "Comments (${reviews.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Divider(color = Color.Gray.copy(alpha = 0.5f))

            if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(reviews) { review ->
                        CommentItem(review = review)
                        Divider(color = Color.DarkGray.copy(alpha = 0.5f))
                    }
                }
            }
            
            // Disclaimer Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray.copy(alpha = 0.3f))
                    .padding(12.dp)
            ) {
                 Text(
                    text = "Comments are read-only in app to protect your privacy.\nTo post a review/comment, click the web button to go directly to the page.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Extension to fix height issue if fillMaxHeight doesn't work well in Sheet
fun Modifier.fillWrapperMsgHeight(fraction: Float = 1f): Modifier = this.then(Modifier.fillMaxHeight(fraction))

@Composable
fun CommentItem(review: ArchiveReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = review.reviewer ?: "Anonymous",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = review.reviewdate?.take(10) ?: "", // Show YYYY-MM-DD
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = review.reviewbody ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray
        )
    }
}
