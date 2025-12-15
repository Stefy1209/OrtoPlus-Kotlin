package com.example.ortoplus.ui.pages

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.ortoplus.clinic.models.Clinic
import com.example.ortoplus.clinic.service.ClinicRepository
import com.example.ortoplus.notification.SignalRService
import com.example.ortoplus.review.models.Review
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val NOTIFICATION_CHANNEL_ID = "signalr_messages"
private const val NOTIFICATION_CHANNEL_NAME = "SignalR Messages"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    clinicId: String,
    clinicRepository: ClinicRepository,
    signalRService: SignalRService,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val snackBarHostState = remember { SnackbarHostState() }
    val notificationManager = remember {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    var clinic by remember { mutableStateOf<Clinic?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showReviewDialog by remember { mutableStateOf(false) }

    // Request notification permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            scope.launch {
                snackBarHostState.showSnackbar("Notification permission denied")
            }
        }
    }

    // Create notification channel and request permission
    LaunchedEffect(Unit) {
        createNotificationChannel(notificationManager)

        when (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )) {
            PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(clinicId) {
        clinicRepository.getClinicById(clinicId)
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
        println("DetailScreen: Setting up SignalR connection")
        val job = scope.launch(Dispatchers.IO) {
            try {
                signalRService.startConnection { message ->
                    println("DetailScreen: Received SignalR message: $message")

                    // Show notification
                    try {
                        showNotification(context, notificationManager, message)
                    } catch (e: Exception) {
                        println("DetailScreen: Failed to show notification: ${e.message}")
                        e.printStackTrace()
                    }

                    // Show snackbar
                    scope.launch(Dispatchers.Main) {
                        println("DetailScreen: Showing snackbar")
                        snackBarHostState.showSnackbar(message)
                    }
                }
            } catch (e: Exception) {
                println("DetailScreen: Error in SignalR connection: ${e.message}")
                e.printStackTrace()
            }
        }

        onDispose {
            println("DetailScreen: Cleaning up SignalR connection")
            signalRService.stopConnection()
            job.cancel()
        }
    }

    val onSubmitReview = { rating: Int, comment: String ->
        scope.launch {
            println("DetailScreen: Submitting review...")
            clinicRepository.addReview(clinicId, comment, rating)
                .onSuccess { review ->
                    println("DetailScreen: Review created successfully")
                    val updatedReviews = clinic!!.reviews + review
                    clinic = clinic!!.copy(reviews = updatedReviews.toMutableList())
                    showReviewDialog = false
                }
                .onFailure { e ->
//                    println("DetailScreen: Failed to create review: ${e.message}")
//                    scope.launch {
//                        snackBarHostState.showSnackbar(
//                            message = "Failed to add review: ${e.message ?: "Unknown error"}"
//                        )
//                    }


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

private fun createNotificationChannel(
    notificationManager: NotificationManager
) {
    val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Notifications from SignalR service"
        enableVibration(true)
    }
    notificationManager.createNotificationChannel(channel)
}

private fun showNotification(
    context: Context,
    notificationManager: NotificationManager,
    message: String
) {
    println("showNotification called with message: $message")

    // Check permission
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    println("Notification permission granted: $hasPermission")

    if (!hasPermission) {
        println("Notification permission not granted, skipping notification")
        return
    }

    try {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Review")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        println("Showing notification with ID: $notificationId")
        notificationManager.notify(notificationId, notification)
        println("Notification posted successfully")
    } catch (e: Exception) {
        println("Error showing notification: ${e.message}")
        e.printStackTrace()
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
            item {
                ClinicHeader(clinic)
            }

            item {
                AddressCard(
                    addressText = clinic.address.toString(),
                    onGetDirections = openMap
                )
            }

            item {
                Text(
                    text = "Patient Reviews (${clinic.reviews.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

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