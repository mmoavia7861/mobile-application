package com.example.ui.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.Store
import com.example.data.repository.MealVerseRepository
import com.example.data.reviews.ReviewValidator
import com.example.ui.components.Animated3DFoodThumbnail
import com.example.ui.components.StoreReviewCard
import com.example.ui.components.StoreReviewsSectionHeader
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
  store: Store,
  repository: MealVerseRepository,
  cartItems: List<CartItem>,
  onAddToCart: (Product, Int, String) -> Unit,
  onBack: () -> Unit,
  onViewCart: () -> Unit
) {
  val products by remember(store.id) {
    repository.observeProducts(store.id)
  }.collectAsStateWithLifecycle(initialValue = emptyList())
  val storeReviews by remember(store.id) {
    repository.observeStoreReviews(store.id)
  }.collectAsStateWithLifecycle(initialValue = emptyList())
  val ratingSummary = remember(storeReviews, store.rating, store.reviewCount) {
    ReviewValidator.computeStoreRatingSummary(
      reviews = storeReviews,
      baseRating = store.rating,
      baseCount = store.reviewCount
    )
  }
  var selectedProductForModal by remember { mutableStateOf<Product?>(null) }
  var modalQty by remember { mutableIntStateOf(1) }
  var modalInstructions by remember { mutableStateOf("") }

  val cartTotal = cartItems.sumOf { it.product.price * it.quantity }
  val cartCount = cartItems.sumOf { it.quantity }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(store.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${store.category} • ${store.city}", style = MaterialTheme.typography.bodySmall)
          }
        },
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
    bottomBar = {
      if (cartCount > 0) {
        Surface(
          shadowElevation = 10.dp,
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "$cartCount Items in Cart",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Rs. ${cartTotal.toInt()}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = MealVerseDeepTeal)
              )
            }
            Button(
              onClick = onViewCart,
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseWarmOrange),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .height(48.dp)
                .testTag("view_cart_button")
            ) {
              Text("View Cart & Checkout", fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.width(6.dp))
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
          }
        }
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
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
      ) {
        // Store summary card with photographic header + 3D dish medallion
        item {
          val bannerRes = getStoreGraphic(store)
          val dish3DRes = get3DFoodGraphic(store.name, store.category)

          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 18.dp)
          ) {
            Column {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(168.dp)
              ) {
                Image(
                  painter = painterResource(id = bannerRes),
                  contentDescription = store.name,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )

                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.verticalGradient(
                        listOf(
                          Color.Black.copy(alpha = 0.16f),
                          Color.Black.copy(alpha = 0.88f)
                        )
                      )
                    )
                )

                Row(
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalAlignment = Alignment.Bottom,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.ExtraBold)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = MealVerseSaffronYellow,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                    Text(
                      text = "${store.category} • ${store.city}",
                      style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f))
                    )
                  }

                  Animated3DFoodThumbnail(
                    imageRes = dish3DRes,
                    contentDescription = store.name,
                    size = 64.dp,
                    cornerRadius = 18.dp
                  )
                }
              }

              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = store.description.ifBlank { "Authentic quality food prepared fresh." },
                  style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  InfoBadgePill(icon = Icons.Default.Timer, text = "${store.prepTimeMinutes} mins", tint = MealVerseWarmOrange)
                  InfoBadgePill(icon = Icons.Default.DeliveryDining, text = "Rs. ${store.deliveryFee.toInt()}", tint = MealVerseDeepTeal)
                  InfoBadgePill(
                    icon = Icons.Default.Star,
                    text = "${ratingSummary.averageRating} (${ratingSummary.totalReviewCount})",
                    tint = Color(0xFFF57F17)
                  )
                }
              }
            }
          }
        }

        item {
          Text(
            text = "Menu Items (${products.size})",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }

        if (products.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MealVerseDeepTeal)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = "Loading fresh menu items...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else {
          itemsIndexed(products, key = { _, p -> p.id.ifBlank { p.name } }) { index, product ->
            val inCartQty = cartItems.find { it.product.id == product.id }?.quantity ?: 0
            ProductItemRow(
              product = product,
              inCartQty = inCartQty,
              phaseOffset = index * 0.55f,
              onAddClick = {
                selectedProductForModal = product
                modalQty = 1
                modalInstructions = ""
              }
            )
            Spacer(modifier = Modifier.height(10.dp))
          }
        }

        // Verified Customer Reviews Section
        item {
          Spacer(modifier = Modifier.height(14.dp))
          StoreReviewsSectionHeader(
            summary = ratingSummary,
            customerReviewCount = storeReviews.size
          )
          Spacer(modifier = Modifier.height(10.dp))
        }

        if (storeReviews.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("store_reviews_empty")
            ) {
              Text(
                text = "No written customer reviews yet. Complete an order to leave the first star rating and comment!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(18.dp)
              )
            }
          }
        } else {
          items(storeReviews, key = { it.id.ifBlank { it.orderId } }) { review ->
            StoreReviewCard(review = review)
            Spacer(modifier = Modifier.height(10.dp))
          }
        }
      }
    }
  }

  // Add Item Dialog / Modal
  if (selectedProductForModal != null) {
    val prod = selectedProductForModal!!
    val dishImageRes = get3DFoodGraphic(prod.name, prod.category)

    AlertDialog(
      onDismissRequest = { selectedProductForModal = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Animated3DFoodThumbnail(
            imageRes = dishImageRes,
            contentDescription = prod.name,
            size = 48.dp,
            cornerRadius = 12.dp
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(prod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
      },
      text = {
        Column {
          if (prod.description.isNotBlank()) {
            Text(prod.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
          }
          Text("Price: Rs. ${prod.price.toInt()}", fontWeight = FontWeight.Bold, color = MealVerseDeepTeal)

          Spacer(modifier = Modifier.height(16.dp))

          // Quantity controls
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
          ) {
            FilledIconButton(
              onClick = { if (modalQty > 1) modalQty-- },
              colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(
              text = "$modalQty",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 24.dp)
            )
            FilledIconButton(
              onClick = { modalQty++ },
              colors = IconButtonDefaults.filledIconButtonColors(containerColor = MealVerseDeepTeal)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = modalInstructions,
            onValueChange = { modalInstructions = it },
            label = { Text("Special cooking instructions (optional)") },
            placeholder = { Text("e.g. Less spicy, extra salad...") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onAddToCart(prod, modalQty, modalInstructions)
            selectedProductForModal = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal)
        ) {
          Text("Add to Cart • Rs. ${(prod.price * modalQty).toInt()}")
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedProductForModal = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun InfoBadgePill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
  tint: Color
) {
  Surface(
    color = tint.copy(alpha = 0.1f),
    shape = RoundedCornerShape(10.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = tint))
    }
  }
}

@Composable
fun ProductItemRow(
  product: Product,
  inCartQty: Int = 0,
  phaseOffset: Float = 0f,
  onAddClick: () -> Unit
) {
  val dishRes = get3DFoodGraphic(product.name, product.category)

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 3D Animated Dish Graphic
      Animated3DFoodThumbnail(
        imageRes = dishRes,
        contentDescription = product.name,
        size = 72.dp,
        cornerRadius = 16.dp,
        phaseOffset = phaseOffset
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )
        }
        if (product.description.isNotBlank()) {
          Text(
            text = product.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Rs. ${product.price.toInt()}",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              color = MealVerseDeepTeal
            )
          )
          if (inCartQty > 0) {
            Surface(
              color = MealVerseWarmOrange.copy(alpha = 0.14f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "$inCartQty in cart",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MealVerseWarmOrange,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = onAddClick,
        enabled = product.isAvailable,
        colors = ButtonDefaults.buttonColors(
          containerColor = MealVerseDeepTeal.copy(alpha = 0.14f),
          contentColor = MealVerseDeepTeal
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier.height(48.dp)
      ) {
        Text(
          text = if (product.isAvailable) "ADD +" else "Sold Out",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp
        )
      }
    }
  }
}
