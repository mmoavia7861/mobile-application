package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PAKISTAN_CITY_COORDINATES
import com.example.ui.theme.MealVerseDeepTeal
import com.example.ui.theme.MealVerseWarmOrange
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.*

/**
 * Pure Web Mercator projection, tile math, and formatting utilities for the
 * Compose-native OpenStreetMap store location picker.
 */
object MapGeoUtils {
  const val TILE_SIZE = 256.0
  const val MIN_ZOOM = 10
  const val MAX_ZOOM = 18
  private const val MAX_LAT = 85.05112878

  fun clampLatitude(lat: Double): Double = lat.coerceIn(-MAX_LAT, MAX_LAT)

  fun wrapLongitude(lon: Double): Double {
    val wrapped = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
    return if (wrapped == -180.0 && lon > 0) 180.0 else wrapped
  }

  fun latLonToWorldPixel(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
    val clampedLat = clampLatitude(lat)
    val wrappedLon = wrapLongitude(lon)
    val scale = TILE_SIZE * (1 shl zoom)
    val x = (wrappedLon + 180.0) / 360.0 * scale
    val latRad = Math.toRadians(clampedLat)
    val sinLat = sin(latRad)
    val y = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * scale
    return x to y
  }

  fun worldPixelToLatLon(worldX: Double, worldY: Double, zoom: Int): Pair<Double, Double> {
    val scale = TILE_SIZE * (1 shl zoom)
    val wrappedX = ((worldX % scale) + scale) % scale
    val clampedY = worldY.coerceIn(0.0, scale)
    val lon = wrappedX / scale * 360.0 - 180.0
    val n = PI - 2.0 * PI * clampedY / scale
    val lat = Math.toDegrees(atan(0.5 * (exp(n) - exp(-n))))
    return clampLatitude(lat) to wrapLongitude(lon)
  }

  fun latLonToTileXY(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
    val (worldX, worldY) = latLonToWorldPixel(lat, lon, zoom)
    val maxTile = (1 shl zoom) - 1
    val tileX = floor(worldX / TILE_SIZE).toInt().coerceIn(0, maxTile)
    val tileY = floor(worldY / TILE_SIZE).toInt().coerceIn(0, maxTile)
    return tileX to tileY
  }

  fun panCenterByDelta(
    lat: Double,
    lon: Double,
    dragDxPx: Float,
    dragDyPx: Float,
    zoom: Int
  ): Pair<Double, Double> {
    val (worldX, worldY) = latLonToWorldPixel(lat, lon, zoom)
    return worldPixelToLatLon(worldX - dragDxPx, worldY - dragDyPx, zoom)
  }

  fun formatCoordinates(lat: Double, lon: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    return String.format(Locale.US, "%.5f° %s, %.5f° %s", abs(lat), latDir, abs(lon), lonDir)
  }

  fun haversineDistanceKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
  ): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
      cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
  }
}

private fun formatAddressLine(address: Address): String {
  val parts = listOfNotNull(
    address.featureName?.takeIf { it.isNotBlank() && it != address.subThoroughfare },
    address.thoroughfare?.takeIf { it.isNotBlank() },
    address.subLocality?.takeIf { it.isNotBlank() },
    address.locality?.takeIf { it.isNotBlank() }
  ).distinct()
  return if (parts.isNotEmpty()) {
    parts.joinToString(", ")
  } else {
    (0..address.maxAddressLineIndex)
      .mapNotNull { address.getAddressLine(it) }
      .joinToString(", ")
  }
}

@Suppress("DEPRECATION")
private suspend fun reverseGeocodeLocation(
  context: Context,
  lat: Double,
  lon: Double
): String? = withContext(Dispatchers.IO) {
  if (!Geocoder.isPresent()) return@withContext null
  try {
    val geocoder = Geocoder(context, Locale.getDefault())
    val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      suspendCancellableCoroutine<List<Address>> { cont ->
        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
          override fun onGeocode(addresses: MutableList<Address>) {
            if (cont.isActive) cont.resume(addresses)
          }

          override fun onError(errorMessage: String?) {
            if (cont.isActive) cont.resume(emptyList())
          }
        })
      }
    } else {
      geocoder.getFromLocation(lat, lon, 1) ?: emptyList()
    }
    list.firstOrNull()?.let { formatAddressLine(it) }?.takeIf { it.isNotBlank() }
  } catch (_: Exception) {
    null
  }
}

@Suppress("DEPRECATION")
private suspend fun searchAddressInCity(
  context: Context,
  query: String,
  city: String
): Triple<Double, Double, String?>? = withContext(Dispatchers.IO) {
  if (!Geocoder.isPresent() || query.isBlank()) return@withContext null
  try {
    val geocoder = Geocoder(context, Locale.getDefault())
    val fullQuery = if (query.contains(city, ignoreCase = true)) {
      "$query, Pakistan"
    } else {
      "$query, $city, Pakistan"
    }
    val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      suspendCancellableCoroutine<List<Address>> { cont ->
        geocoder.getFromLocationName(fullQuery, 1, object : Geocoder.GeocodeListener {
          override fun onGeocode(addresses: MutableList<Address>) {
            if (cont.isActive) cont.resume(addresses)
          }

          override fun onError(errorMessage: String?) {
            if (cont.isActive) cont.resume(emptyList())
          }
        })
      }
    } else {
      geocoder.getFromLocationName(fullQuery, 1) ?: emptyList()
    }
    val first = list.firstOrNull() ?: return@withContext null
    Triple(first.latitude, first.longitude, formatAddressLine(first).takeIf { it.isNotBlank() })
  } catch (_: Exception) {
    null
  }
}

@SuppressLint("MissingPermission")
private suspend fun fetchCurrentDeviceLocation(context: Context): Pair<Double, Double>? {
  val client = LocationServices.getFusedLocationProviderClient(context)
  return try {
    val tokenSource = CancellationTokenSource()
    val fresh = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token).await()
    if (fresh != null) {
      fresh.latitude to fresh.longitude
    } else {
      val last = client.lastLocation.await()
      if (last != null) last.latitude to last.longitude else null
    }
  } catch (_: Exception) {
    try {
      val last = client.lastLocation.await()
      if (last != null) last.latitude to last.longitude else null
    } catch (_: Exception) {
      null
    }
  }
}

private fun hasLocationPermission(context: Context): Boolean {
  val fine = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION
  ) == PackageManager.PERMISSION_GRANTED
  val coarse = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_COARSE_LOCATION
  ) == PackageManager.PERMISSION_GRANTED
  return fine || coarse
}

/**
 * Renders a smooth interactive OpenStreetMap tile grid backed by a stylized
 * vector street grid fallback so the map looks sharp even offline or during tile loading.
 */
@Composable
fun InteractiveStoreMapView(
  latitude: Double,
  longitude: Double,
  zoom: Int,
  interactive: Boolean,
  onCenterChanged: (Double, Double) -> Unit = { _, _ -> },
  onZoomChange: ((Int) -> Unit)? = null,
  showControls: Boolean = true,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  var isDragging by remember { mutableStateOf(false) }

  val currentLat by rememberUpdatedState(latitude)
  val currentLon by rememberUpdatedState(longitude)
  val currentZoom by rememberUpdatedState(zoom)

  val pinLiftOffset by animateDpAsState(
    targetValue = if (isDragging) (-10).dp else 0.dp,
    label = "pinLift"
  )

  BoxWithConstraints(
    modifier = modifier
      .clipToBounds()
      .background(Color(0xFFEBE7DF))
      .then(
        if (interactive) {
          Modifier.pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { isDragging = true },
              onDragEnd = { isDragging = false },
              onDragCancel = { isDragging = false },
              onDrag = { change, dragAmount ->
                change.consume()
                val tileRatio = with(density) { 256.dp.toPx() } / MapGeoUtils.TILE_SIZE.toFloat()
                val worldDx = dragAmount.x / tileRatio
                val worldDy = dragAmount.y / tileRatio
                val (newLat, newLon) = MapGeoUtils.panCenterByDelta(
                  lat = currentLat,
                  lon = currentLon,
                  dragDxPx = worldDx,
                  dragDyPx = worldDy,
                  zoom = currentZoom
                )
                onCenterChanged(newLat, newLon)
              }
            )
          }
        } else {
          Modifier
        }
      )
  ) {
    val widthPx = with(density) { maxWidth.toPx() }
    val heightPx = with(density) { maxHeight.toPx() }
    val tilePx = with(density) { 256.dp.toPx() }
    val tileRatio = tilePx / MapGeoUtils.TILE_SIZE.toFloat()

    val (worldX, worldY) = MapGeoUtils.latLonToWorldPixel(latitude, longitude, zoom)
    val scaledCenterX = worldX * tileRatio
    val scaledCenterY = worldY * tileRatio

    // Stylized vector map backdrop (streets, parks, blocks) visible while tiles load or offline
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(Color(0xFFF2EFE9))

      val blockSpan = tilePx * 0.45f
      val offsetX = ((-scaledCenterX % blockSpan) + blockSpan).toFloat()
      val offsetY = ((-scaledCenterY % blockSpan) + blockSpan).toFloat()

      // Subtle park patches tied to world coordinates
      var gx = -blockSpan + offsetX
      var colIdx = floor(scaledCenterX / blockSpan).toInt()
      while (gx < size.width + blockSpan) {
        var gy = -blockSpan + offsetY
        var rowIdx = floor(scaledCenterY / blockSpan).toInt()
        while (gy < size.height + blockSpan) {
          if ((colIdx + rowIdx) % 5 == 0) {
            drawRect(
              color = Color(0xFFDCECD4),
              topLeft = Offset(gx + 6f, gy + 6f),
              size = Size(blockSpan - 12f, blockSpan - 12f)
            )
          } else if ((colIdx * 3 + rowIdx) % 7 == 0) {
            drawRect(
              color = Color(0xFFE6E1D6),
              topLeft = Offset(gx + 4f, gy + 4f),
              size = Size(blockSpan - 8f, blockSpan - 8f)
            )
          }
          gy += blockSpan
          rowIdx++
        }
        gx += blockSpan
        colIdx++
      }

      // Secondary neighborhood streets
      var sx = offsetX
      while (sx < size.width) {
        drawLine(
          color = Color.White,
          start = Offset(sx, 0f),
          end = Offset(sx, size.height),
          strokeWidth = 3.5f
        )
        sx += blockSpan
      }
      var sy = offsetY
      while (sy < size.height) {
        drawLine(
          color = Color.White,
          start = Offset(0f, sy),
          end = Offset(size.width, sy),
          strokeWidth = 3.5f
        )
        sy += blockSpan
      }

      // Major arterial avenues
      val avenueSpan = blockSpan * 3f
      var ax = ((-scaledCenterX % avenueSpan) + avenueSpan).toFloat()
      while (ax < size.width) {
        drawLine(
          color = Color(0xFFFCD89D),
          start = Offset(ax, 0f),
          end = Offset(ax, size.height),
          strokeWidth = 7f
        )
        ax += avenueSpan
      }
      var ay = ((-scaledCenterY % avenueSpan) + avenueSpan).toFloat()
      while (ay < size.height) {
        drawLine(
          color = Color(0xFFFCD89D),
          start = Offset(0f, ay),
          end = Offset(size.width, ay),
          strokeWidth = 7f
        )
        ay += avenueSpan
      }
    }

    // Live OpenStreetMap Slippy Tiles overlay
    val totalTiles = 1 shl zoom
    val centerTileX = floor(worldX / MapGeoUtils.TILE_SIZE).toInt()
    val centerTileY = floor(worldY / MapGeoUtils.TILE_SIZE).toInt()

    val tilesHalfX = ceil(widthPx / (2f * tilePx)).toInt() + 1
    val tilesHalfY = ceil(heightPx / (2f * tilePx)).toInt() + 1

    val centerScreenX = widthPx / 2f
    val centerScreenY = heightPx / 2f

    for (dy in -tilesHalfY..tilesHalfY) {
      val tileY = centerTileY + dy
      if (tileY < 0 || tileY >= totalTiles) continue
      for (dx in -tilesHalfX..tilesHalfX) {
        val rawTileX = centerTileX + dx
        val tileX = ((rawTileX % totalTiles) + totalTiles) % totalTiles

        val tileOriginScreenX = centerScreenX + (rawTileX * tilePx - scaledCenterX.toFloat())
        val tileOriginScreenY = centerScreenY + (tileY * tilePx - scaledCenterY.toFloat())
        val tileUrl = "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"

        AsyncImage(
          model = ImageRequest.Builder(context)
            .data(tileUrl)
            .setHeader("User-Agent", "MealVerseAndroid/1.0 (https://mealverse.pk)")
            .crossfade(false)
            .build(),
          contentDescription = null,
          contentScale = ContentScale.FillBounds,
          modifier = Modifier
            .offset {
              IntOffset(tileOriginScreenX.roundToInt(), tileOriginScreenY.roundToInt())
            }
            .size(256.dp)
        )
      }
    }

    // Center crosshair target & animated marker pin
    Box(
      modifier = Modifier.align(Alignment.Center),
      contentAlignment = Alignment.Center
    ) {
      // Ground shadow / target pulse ring
      Box(
        modifier = Modifier
          .offset(y = 14.dp)
          .size(width = if (isDragging) 14.dp else 20.dp, height = 7.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = if (isDragging) 0.18f else 0.30f))
      )
      Box(
        modifier = Modifier
          .offset(y = 14.dp)
          .size(30.dp)
          .border(1.5.dp, MealVerseWarmOrange.copy(alpha = 0.45f), CircleShape)
      )

      // Elevated delivery pin
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(y = (-14).dp + pinLiftOffset)
      ) {
        Surface(
          shape = CircleShape,
          color = MealVerseWarmOrange,
          shadowElevation = 6.dp,
          modifier = Modifier
            .size(38.dp)
            .border(2.5.dp, Color.White, CircleShape)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Storefront,
              contentDescription = "Store Delivery Pin",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
        Box(
          modifier = Modifier
            .width(3.dp)
            .height(10.dp)
            .background(MealVerseWarmOrange, RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
        )
      }
    }

    // Zoom controls (+/-) when enabled
    if (showControls && onZoomChange != null) {
      Column(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilledIconButton(
          onClick = {
            if (zoom < MapGeoUtils.MAX_ZOOM) onZoomChange(zoom + 1)
          },
          enabled = zoom < MapGeoUtils.MAX_ZOOM,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .size(48.dp)
            .shadow(4.dp, CircleShape)
            .testTag("map_zoom_in_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Zoom in")
        }

        FilledIconButton(
          onClick = {
            if (zoom > MapGeoUtils.MIN_ZOOM) onZoomChange(zoom - 1)
          },
          enabled = zoom > MapGeoUtils.MIN_ZOOM,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .size(48.dp)
            .shadow(4.dp, CircleShape)
            .testTag("map_zoom_out_btn")
        ) {
          Icon(Icons.Default.Remove, contentDescription = "Zoom out")
        }
      }
    }

    // Coordinate pill at bottom-left
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = Color.Black.copy(alpha = 0.68f),
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(8.dp)
    ) {
      Text(
        text = MapGeoUtils.formatCoordinates(latitude, longitude),
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
      )
    }

    // Attribution badge (OSM)
    Surface(
      shape = RoundedCornerShape(6.dp),
      color = Color.White.copy(alpha = 0.8f),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(8.dp)
    ) {
      Text(
        text = "© OpenStreetMap",
        color = Color(0xFF333333),
        fontSize = 9.sp,
        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
      )
    }
  }
}

/**
 * Compact location picker card embedded inside ShopkeeperRegistrationScreen.
 */
@Composable
fun StoreLocationPickerCard(
  city: String,
  latitude: Double?,
  longitude: Double?,
  enabled: Boolean,
  isError: Boolean,
  onOpenFullMap: () -> Unit,
  onLocationCaptured: (Double, Double, String?) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isFetchingGps by remember { mutableStateOf(false) }
  var gpsError by remember { mutableStateOf<String?>(null) }

  val cityDefault = PAKISTAN_CITY_COORDINATES[city] ?: (33.6844 to 73.0479)
  val previewLat = latitude ?: cityDefault.first
  val previewLon = longitude ?: cityDefault.second
  val isPinned = latitude != null && longitude != null

  val requestLocationPermissions = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      scope.launch {
        isFetchingGps = true
        gpsError = null
        val loc = fetchCurrentDeviceLocation(context)
        if (loc != null) {
          val resolvedAddress = reverseGeocodeLocation(context, loc.first, loc.second)
          onLocationCaptured(loc.first, loc.second, resolvedAddress)
        } else {
          gpsError = "Unable to acquire GPS fix. Drag pin manually on the map."
        }
        isFetchingGps = false
      }
    } else {
      gpsError = "Location permission denied. Tap 'Pick on Map' to set coordinates manually."
    }
  }

  fun triggerGpsCapture() {
    if (hasLocationPermission(context)) {
      scope.launch {
        isFetchingGps = true
        gpsError = null
        val loc = fetchCurrentDeviceLocation(context)
        if (loc != null) {
          val resolvedAddress = reverseGeocodeLocation(context, loc.first, loc.second)
          onLocationCaptured(loc.first, loc.second, resolvedAddress)
        } else {
          gpsError = "GPS signal unavailable on this device. Use 'Pick on Map' instead."
        }
        isFetchingGps = false
      }
    } else {
      requestLocationPermissions.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
      } else {
        MaterialTheme.colorScheme.surface
      }
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = if (isError) 1.5.dp else 1.dp,
        color = when {
          isError -> MaterialTheme.colorScheme.error
          isPinned -> Color(0xFF2E7D32).copy(alpha = 0.5f)
          else -> MaterialTheme.colorScheme.outlineVariant
        },
        shape = RoundedCornerShape(16.dp)
      )
      .testTag("store_location_picker_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (isPinned) Color(0xFF2E7D32).copy(alpha = 0.14f)
                else MealVerseWarmOrange.copy(alpha = 0.14f)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isPinned) Icons.Default.WhereToVote else Icons.Default.AddLocationAlt,
              contentDescription = null,
              tint = if (isPinned) Color(0xFF2E7D32) else MealVerseWarmOrange,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Store Delivery Coordinates *",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = if (isPinned) {
                MapGeoUtils.formatCoordinates(latitude!!, longitude!!)
              } else {
                "Pin exact storefront for accurate rider pickup in $city"
              },
              style = MaterialTheme.typography.bodySmall,
              color = if (isPinned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("pinned_coordinates_text")
            )
          }
        }

        if (isPinned) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE8F5E9)
          ) {
            Text(
              text = "Pinned",
              color = Color(0xFF2E7D32),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Mini Map Preview Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(152.dp)
          .clip(RoundedCornerShape(14.dp))
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
          .clickable(enabled = enabled) { onOpenFullMap() }
      ) {
        InteractiveStoreMapView(
          latitude = previewLat,
          longitude = previewLon,
          zoom = if (isPinned) 16 else 13,
          interactive = false,
          showControls = false,
          modifier = Modifier.fillMaxSize()
        )

        // Subtle scrim & prompt chip if not yet pinned
        if (!isPinned) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.35f))
                )
              )
          )
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 10.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                tint = MealVerseWarmOrange,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Tap map to drop delivery pin in $city",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
              )
            }
          }
        }
      }

      if (gpsError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = gpsError ?: "",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.labelSmall
        )
      } else if (isError) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Please pin your exact store coordinates on the map before submitting.",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.labelSmall
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onOpenFullMap,
          enabled = enabled,
          colors = ButtonDefaults.buttonColors(containerColor = MealVerseDeepTeal),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("open_map_picker_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isPinned) "Adjust Pin" else "Pick on Map",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1
          )
        }

        OutlinedButton(
          onClick = { triggerGpsCapture() },
          enabled = enabled && !isFetchingGps,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("use_gps_location_btn")
        ) {
          if (isFetchingGps) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MealVerseWarmOrange
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Locating...", fontSize = 13.sp, maxLines = 1)
          } else {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = null,
              tint = MealVerseWarmOrange,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Use My GPS",
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
              color = MealVerseWarmOrange,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

/**
 * Full-screen interactive map picker dialog with address search, drag-to-pin,
 * zoom controls, live GPS capture, and reverse-geocoded address preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenMapLocationPickerDialog(
  city: String,
  initialLatitude: Double?,
  initialLongitude: Double?,
  onDismiss: () -> Unit,
  onConfirmLocation: (Double, Double, String?) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val cityCenter = PAKISTAN_CITY_COORDINATES[city] ?: (33.6844 to 73.0479)

  var mapLat by remember { mutableStateOf(initialLatitude ?: cityCenter.first) }
  var mapLon by remember { mutableStateOf(initialLongitude ?: cityCenter.second) }
  var mapZoom by remember { mutableStateOf(if (initialLatitude != null) 16 else 14) }
  var searchQuery by remember { mutableStateOf("") }
  var resolvedAddress by remember { mutableStateOf<String?>(null) }
  var isGeocoding by remember { mutableStateOf(false) }
  var isSearching by remember { mutableStateOf(false) }
  var isFetchingGps by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }

  // Debounced reverse geocode whenever camera center settles
  LaunchedEffect(mapLat, mapLon) {
    isGeocoding = true
    delay(450)
    val address = reverseGeocodeLocation(context, mapLat, mapLon)
    resolvedAddress = address
    isGeocoding = false
  }

  val requestLocationPermissions = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      scope.launch {
        isFetchingGps = true
        statusMessage = null
        val loc = fetchCurrentDeviceLocation(context)
        if (loc != null) {
          mapLat = loc.first
          mapLon = loc.second
          mapZoom = 17
        } else {
          statusMessage = "GPS unavailable right now. Drag the map pin to your storefront."
        }
        isFetchingGps = false
      }
    } else {
      statusMessage = "Permission denied. Drag the map manually to position your store pin."
    }
  }

  fun performSearch() {
    if (searchQuery.isBlank()) return
    scope.launch {
      isSearching = true
      statusMessage = null
      val result = searchAddressInCity(context, searchQuery.trim(), city)
      if (result != null) {
        mapLat = result.first
        mapLon = result.second
        mapZoom = 16
        if (!result.third.isNullOrBlank()) {
          resolvedAddress = result.third
        }
      } else {
        statusMessage = "No matching area found for \"$searchQuery\" in $city. Drag the pin manually."
      }
      isSearching = false
    }
  }

  fun moveToGps() {
    if (hasLocationPermission(context)) {
      scope.launch {
        isFetchingGps = true
        statusMessage = null
        val loc = fetchCurrentDeviceLocation(context)
        if (loc != null) {
          mapLat = loc.first
          mapLon = loc.second
          mapZoom = 17
        } else {
          statusMessage = "Could not retrieve GPS fix. Drag the map pin manually."
        }
        isFetchingGps = false
      }
    } else {
      requestLocationPermissions.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        // Top Header + Search Bar
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 3.dp
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = onDismiss,
                  modifier = Modifier.testTag("close_map_picker_btn")
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Close map")
                }
                Column {
                  Text(
                    text = "Pin Store Location",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                  )
                  Text(
                    text = "Drag map so the pin sits on your shop entrance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              TextButton(
                onClick = {
                  mapLat = cityCenter.first
                  mapLon = cityCenter.second
                  mapZoom = 14
                  statusMessage = null
                }
              ) {
                Text(city, fontWeight = FontWeight.Bold, color = MealVerseWarmOrange)
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Search sector, plaza, or road in $city...", fontSize = 13.sp) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
              trailingIcon = {
                if (isSearching) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                  )
                } else if (searchQuery.isNotBlank()) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { searchQuery = "" }) {
                      Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                    IconButton(
                      onClick = { performSearch() },
                      modifier = Modifier.testTag("map_search_btn")
                    ) {
                      Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Search location",
                        tint = MealVerseWarmOrange
                      )
                    }
                  }
                }
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions = KeyboardActions(onSearch = { performSearch() }),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("map_search_input")
            )
          }
        }

        // Interactive Map Canvas
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          InteractiveStoreMapView(
            latitude = mapLat,
            longitude = mapLon,
            zoom = mapZoom,
            interactive = true,
            onCenterChanged = { newLat, newLon ->
              mapLat = newLat
              mapLon = newLon
              statusMessage = null
            },
            onZoomChange = { newZoom -> mapZoom = newZoom },
            showControls = true,
            modifier = Modifier.fillMaxSize()
          )

          // GPS My Location Floating Action Button
          FloatingActionButton(
            onClick = { moveToGps() },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MealVerseDeepTeal,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(end = 14.dp, bottom = 36.dp)
              .size(50.dp)
              .testTag("map_dialog_gps_fab")
          ) {
            if (isFetchingGps) {
              CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.MyLocation, contentDescription = "Center on my GPS location")
            }
          }

          if (statusMessage != null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              shadowElevation = 4.dp,
              modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
              Text(
                text = statusMessage ?: "",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
              )
            }
          }
        }

        // Bottom Confirmation Panel
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 8.dp,
          shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp, vertical = 14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MealVerseWarmOrange,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = MapGeoUtils.formatCoordinates(mapLat, mapLon),
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = when {
                    isGeocoding -> "Looking up street address..."
                    !resolvedAddress.isNullOrBlank() -> resolvedAddress!!
                    else -> "Selected storefront delivery pin in $city"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = { onConfirmLocation(mapLat, mapLon, resolvedAddress) },
              colors = ButtonDefaults.buttonColors(containerColor = MealVerseWarmOrange),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_map_location_btn")
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Confirm Delivery Location",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }
          }
        }
      }
    }
  }
}
