package com.example.ui.shopkeeper

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.Store
import com.example.data.repository.MealVerseRepository
import com.example.ui.components.Animated3DFoodThumbnail
import com.example.ui.customer.get3DFoodGraphic
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
  store: Store,
  repository: MealVerseRepository,
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val products by repository.observeProducts(store.id).collectAsState(initial = emptyList())

  var showAddDialog by remember { mutableStateOf(false) }
  var newName by remember { mutableStateOf("") }
  var newDesc by remember { mutableStateOf("") }
  var newPrice by remember { mutableStateOf("") }
  var newCategory by remember { mutableStateOf("Main Course") }
  var isSaving by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Menu Management • ${store.name}", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MealVerseDeepTeal,
        contentColor = Color.White
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add New Item")
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.TopCenter
    ) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 720.dp)
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Text(
            text = "Active Menu Items (${products.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }

        if (products.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
            ) {
              Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No products in menu yet", fontWeight = FontWeight.Bold)
                Text("Tap the + button below to add your first dish or grocery product.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        } else {
          itemsIndexed(products, key = { _, p -> p.id.ifBlank { p.name } }) { index, product ->
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Animated3DFoodThumbnail(
                  imageRes = get3DFoodGraphic(product.name, product.category),
                  contentDescription = product.name,
                  size = 56.dp,
                  cornerRadius = 14.dp,
                  phaseOffset = index * 0.5f
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                  if (product.description.isNotBlank()) {
                    Text(product.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("Rs. ${product.price.toInt()} • ${product.category}", fontWeight = FontWeight.SemiBold, color = MealVerseDeepTeal)
                }

                // Availability Switch
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    if (product.isAvailable) "In Stock" else "Sold Out",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
                  )
                  Switch(
                    checked = product.isAvailable,
                    onCheckedChange = { isChecked ->
                      scope.launch {
                        repository.toggleProductAvailability(store.id, product.id, isChecked)
                      }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = MealVerseDeepTeal)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Add Product Dialog
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { if (!isSaving) showAddDialog = false },
      title = { Text("Add Dish / Product", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            enabled = !isSaving,
            label = { Text("Dish / Item Name") },
            placeholder = { Text("e.g. Chicken Biryani Single") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = newDesc,
            onValueChange = { newDesc = it },
            enabled = !isSaving,
            label = { Text("Description") },
            placeholder = { Text("Ingredients, portion size...") },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = newPrice,
            onValueChange = { newPrice = it },
            enabled = !isSaving,
            label = { Text("Price in PKR (Rs.)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = newCategory,
            onValueChange = { newCategory = it },
            enabled = !isSaving,
            label = { Text("Category") },
            placeholder = { Text("Main Course, Karahi, Dessert...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val priceVal = newPrice.toDoubleOrNull() ?: 0.0
            if (newName.isBlank() || priceVal <= 0.0) return@Button

            scope.launch {
              isSaving = true
              val newProduct = Product(
                storeId = store.id,
                name = newName,
                description = newDesc,
                price = priceVal,
                category = newCategory.ifBlank { "Main Course" },
                isAvailable = true
              )
              repository.addProduct(store.id, newProduct)
              isSaving = false
              showAddDialog = false
              newName = ""
              newDesc = ""
              newPrice = ""
            }
          },
          enabled = !isSaving,
          colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal)
        ) {
          if (isSaving) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
          } else {
            Text("Save to Menu")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }, enabled = !isSaving) {
          Text("Cancel")
        }
      }
    )
  }
}
