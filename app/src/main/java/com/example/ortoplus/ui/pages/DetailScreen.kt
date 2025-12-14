package com.example.ortoplus.ui.pages

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.ortoplus.clinic.models.Clinic
import com.example.ortoplus.clinic.service.ClinicService
import com.example.ortoplus.notification.SignalRService
import com.example.ortoplus.review.models.Review
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    clinicId: String,
    clinicService: ClinicService,
    signalRService: SignalRService,
    onNavigateBack: () -> Unit
) {
    LocalContext.current
    val scope = rememberCoroutineScope()

    val snackBarHostState = remember { SnackbarHostState() }

    var clinic by remember { mutableStateOf<Clinic?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(clinicId) {
        clinicService.getClinicById(clinicId)
            .onSuccess {
                clinic = it
                isLoading = false
            }
            .onFailure {
                isLoading = false
                // Handle error
            }
    }

    DisposableEffect(Unit) {
        val job = scope.launch(Dispatchers.IO) {
            signalRService.startConnection { message ->
                scope.launch(Dispatchers.Main) {
                    snackBarHostState.showSnackbar(message)
                }
            }
        }

        onDispose {
            signalRService.stopConnection()
            job.cancel()
        }
    }

    val onSubmitReview = { rating: Int, comment: String ->
        scope.launch {
            clinicService.addReview(clinicId, comment, rating)
                .onSuccess { review ->
                    val updatedReviews = clinic!!.reviews + review
                    clinic = clinic!!.copy(reviews = updatedReviews.toMutableList())
                    showReviewDialog = false
                }
                .onFailure { e ->
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = "Failed to add review: ${e.message ?: "Unknown error"}"
                        )
                    }
                }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (clinic != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            DetailContent(
                clinic = clinic!!,
                onNavigateBack = onNavigateBack,
                onAddReviewClick = { showReviewDialog = true },
                snackBarHostState = snackBarHostState
            )

            if (showReviewDialog) {
                AddReviewDialog(
                    onDismiss = { showReviewDialog = false },
                    onSubmit = { rating, comment -> onSubmitReview(rating, comment) }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error loading clinic details")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    clinic: Clinic,
    onNavigateBack: () -> Unit,
    onAddReviewClick: () -> Unit,
    snackBarHostState: SnackbarHostState
    ) {
    val context = LocalContext.current

    val openMap = remember {
        {
            val gmmIntentUri = "geo:${clinic.latitude},${clinic.longitude}?q=${clinic.latitude},${clinic.longitude}(${clinic.name})".toUri()
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            // Check if maps is installed, otherwise let system handle it
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinic Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddReviewClick,
                icon = { Icon(Icons.Default.Add, "Review") },
                text = { Text("Add Review") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section (Name & Rating)
            item {
                ClinicHeader(clinic)
            }

            // Address & Map Action
            item {
                AddressCard(
                    addressText = clinic.address.toString(),
                    onGetDirections = openMap
                )
            }

            // Reviews Section Title
            item {
                Text(
                    text = "Patient Reviews (${clinic.reviews.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // List of Reviews
            if (clinic.reviews.isEmpty()) {
                item {
                    Text(
                        text = "No reviews yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(clinic.reviews) { review ->
                    ReviewItem(review)
                }
            }

            // Spacer for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ClinicHeader(clinic: Clinic) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = clinic.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Rating",
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${clinic.rating} / 5",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun AddressCard(
    addressText: String,
    onGetDirections: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGetDirections,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Directions")
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Patient Rating: ${review.rating}/5",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AddReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write a Review") },
        text = {
            Column {
                Text("Rate your experience:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Star Rating Row
                Row {
                    for (i in 1..5) {
                        val isSelected = i <= rating
                        val icon = Icons.Default.Star
                        val iconTintColor = if (isSelected) Color(0xFFFFB300) else Color.LightGray

                        Icon(
                            imageVector = icon,
                            contentDescription = "Star $i",
                            tint = iconTintColor,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { rating = i }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    onSubmit(rating, comment)
                },
                enabled = comment.isNotBlank() && !isSubmitting
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}