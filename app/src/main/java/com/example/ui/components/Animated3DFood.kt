package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.customer.FEATURED_3D_DISHES
import com.example.ui.customer.Featured3DDish
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive 3D Animated Food Stage with perspective tilt, levitation physics,
 * dynamic 3D drop shadow, rising steam wisps, and glowing spice sparkles.
 */
@Composable
fun Interactive3DFoodStage(
  modifier: Modifier = Modifier,
  dishes: List<Featured3DDish> = FEATURED_3D_DISHES,
  compact: Boolean = false,
  onDishSelected: (Featured3DDish) -> Unit = {}
) {
  var currentIndex by remember { mutableIntStateOf(0) }
  var isUserInteracting by remember { mutableStateOf(false) }

  // Drag-based interactive 3D rotation
  var dragRotationX by remember { mutableFloatStateOf(0f) }
  var dragRotationY by remember { mutableFloatStateOf(0f) }

  val animatedDragRotX by animateFloatAsState(
    targetValue = dragRotationX,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "dragRotX"
  )
  val animatedDragRotY by animateFloatAsState(
    targetValue = dragRotationY,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "dragRotY"
  )

  // Auto-cycle dishes gently unless user is dragging
  LaunchedEffect(currentIndex, isUserInteracting) {
    if (!isUserInteracting && dishes.size > 1) {
      delay(4200L)
      currentIndex = (currentIndex + 1) % dishes.size
    }
  }

  // Continuous infinite 3D levitation & gimbal motion
  val infiniteTransition = rememberInfiniteTransition(label = "3d_food_motion")

  val levitationPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "levitationPhase"
  )

  val steamPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "steamPhase"
  )

  val orbitGlowAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 9000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "orbitGlow"
  )

  val activeDish = dishes.getOrElse(currentIndex) { dishes.first() }
  val accentColor = Color(activeDish.accentColorHex)
  val density = LocalDensity.current.density

  // Derived smooth sinusoidal motion
  val floatOffsetY = sin(levitationPhase) * (if (compact) 6f else 9f)
  val idleRotY = sin(levitationPhase * 0.5f) * 10f
  val idleRotX = cos(levitationPhase * 0.5f) * 5f
  val breathScale = 1f + sin(levitationPhase) * 0.025f
  val shadowScale = 1f - (sin(levitationPhase) * 0.10f)

  Card(
    shape = RoundedCornerShape(26.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("animated_3d_food_stage")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(
              Color(0xFF062E2A),
              MealVerseDeepTeal,
              Color(0xFF1A3A35),
              Color(0xFF3A1D0A)
            )
          )
        )
        .padding(horizontal = 16.dp, vertical = if (compact) 14.dp else 18.dp)
    ) {
      // Ambient glowing radial orbs in background
      Canvas(modifier = Modifier.matchParentSize()) {
        val orbitRad = (orbitGlowAngle * PI / 180.0).toFloat()
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(size.width * 0.72f + cos(orbitRad) * 18f, size.height * 0.45f + sin(orbitRad) * 14f),
            radius = size.minDimension * 0.65f
          ),
          radius = size.minDimension * 0.65f,
          center = Offset(size.width * 0.72f + cos(orbitRad) * 18f, size.height * 0.45f + sin(orbitRad) * 14f)
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(MealVerseSaffronYellow.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * 0.2f, size.height * 0.2f),
            radius = size.minDimension * 0.5f
          ),
          radius = size.minDimension * 0.5f,
          center = Offset(size.width * 0.2f, size.height * 0.2f)
        )
      }

      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left Column: Editorial Copy & CTA
          Column(
            modifier = Modifier
              .weight(1.05f)
              .padding(end = 10.dp)
          ) {
            Surface(
              color = accentColor.copy(alpha = 0.22f),
              shape = RoundedCornerShape(50),
              modifier = Modifier.border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(50))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.LocalFireDepartment,
                  contentDescription = null,
                  tint = MealVerseSaffronYellow,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "3D FRESH MENU",
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
              targetState = activeDish,
              transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(initialScale = 0.94f)).togetherWith(fadeOut(tween(200)))
              },
              label = "dishTextTransition"
            ) { dish ->
              Column {
                Text(
                  text = dish.title,
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = if (compact) 19.sp else 21.sp
                  ),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = dish.subtitle,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.82f)
                  ),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Surface(
                    color = MealVerseSaffronYellow,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { onDishSelected(dish) }
                  ) {
                    Text(
                      text = dish.priceTag,
                      color = Color(0xFF1A1C1E),
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 11.sp,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtle interactive hint
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Drag dish to tilt 3D",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Color.White.copy(alpha = 0.65f),
                  fontSize = 10.sp
                )
              )
            }
          }

          // Right Column: 3D Floating & Interactive Dish Sphere
          val dishSize = if (compact) 128.dp else 148.dp
          Box(
            modifier = Modifier
              .size(width = dishSize + 18.dp, height = dishSize + 24.dp)
              .pointerInput(Unit) {
                detectDragGestures(
                  onDragStart = { isUserInteracting = true },
                  onDragEnd = {
                    isUserInteracting = false
                    dragRotationX = 0f
                    dragRotationY = 0f
                  },
                  onDragCancel = {
                    isUserInteracting = false
                    dragRotationX = 0f
                    dragRotationY = 0f
                  },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    dragRotationY = (dragRotationY + dragAmount.x * 0.45f).coerceIn(-32f, 32f)
                    dragRotationX = (dragRotationX - dragAmount.y * 0.45f).coerceIn(-28f, 28f)
                  }
                )
              },
            contentAlignment = Alignment.Center
          ) {
            // 1. Dynamic 3D Elliptical Floor Shadow (shrinks as dish floats up)
            Canvas(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .size(width = dishSize * 0.78f, height = 18.dp)
                .graphicsLayer {
                  scaleX = shadowScale
                  scaleY = shadowScale
                }
            ) {
              drawOval(
                brush = Brush.radialGradient(
                  colors = listOf(
                    Color.Black.copy(alpha = 0.55f),
                    Color.Black.copy(alpha = 0.18f),
                    Color.Transparent
                  )
                )
              )
            }

            // 2. Animated Steam Wisps & Spice Sparks rising behind/above the 3D food
            SteamAndSpiceCanvas(
              steamPhase = steamPhase,
              accentColor = accentColor,
              modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = dishSize, height = 56.dp)
            )

            // 3. The 3D Levitating & Tilting Dish Medallion
            Box(
              modifier = Modifier
                .size(dishSize)
                .graphicsLayer {
                  cameraDistance = 14f * density
                  translationY = floatOffsetY * density
                  rotationX = idleRotX + animatedDragRotX
                  rotationY = idleRotY + animatedDragRotY
                  scaleX = breathScale
                  scaleY = breathScale
                }
                .clip(RoundedCornerShape(32.dp))
                .border(
                  width = 2.5.dp,
                  brush = Brush.linearGradient(
                    colors = listOf(
                      MealVerseSaffronYellow.copy(alpha = 0.9f),
                      accentColor,
                      Color.White.copy(alpha = 0.45f)
                    )
                  ),
                  shape = RoundedCornerShape(32.dp)
                )
                .clickable { onDishSelected(activeDish) },
              contentAlignment = Alignment.Center
            ) {
              AnimatedContent(
                targetState = activeDish.imageRes,
                transitionSpec = {
                  (fadeIn(tween(320)) + scaleIn(initialScale = 0.85f))
                    .togetherWith(fadeOut(tween(240)) + scaleOut(targetScale = 1.08f))
                },
                label = "3dDishImage"
              ) { resId ->
                Image(
                  painter = painterResource(id = resId),
                  contentDescription = activeDish.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              }

              // Subtle top-left 3D specular rim shine
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.linearGradient(
                      colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.15f)
                      )
                    )
                  )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Carousel Controls & Dish Pills
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            dishes.forEachIndexed { idx, dish ->
              val isSelected = idx == currentIndex
              val pillWidth by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "dotWidth"
              )
              Box(
                modifier = Modifier
                  .height(7.dp)
                  .width(pillWidth)
                  .clip(CircleShape)
                  .background(
                    if (isSelected) MealVerseSaffronYellow else Color.White.copy(alpha = 0.35f)
                  )
                  .clickable { currentIndex = idx }
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallStageIconBtn(
              icon = Icons.Default.ChevronLeft,
              contentDescription = "Previous 3D Dish",
              onClick = {
                currentIndex = if (currentIndex == 0) dishes.lastIndex else currentIndex - 1
              }
            )
            SmallStageIconBtn(
              icon = Icons.Default.ChevronRight,
              contentDescription = "Next 3D Dish",
              onClick = {
                currentIndex = (currentIndex + 1) % dishes.size
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SmallStageIconBtn(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Surface(
    shape = CircleShape,
    color = Color.White.copy(alpha = 0.16f),
    modifier = Modifier
      .size(30.dp)
      .clip(CircleShape)
      .clickable(onClick = onClick)
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = Color.White,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
private fun SteamAndSpiceCanvas(
  steamPhase: Float,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Draw 3 rising sinusoidal steam ribbons
    val strandPositions = listOf(0.28f, 0.50f, 0.72f)
    strandPositions.forEachIndexed { index, xRatio ->
      val localPhase = (steamPhase + index * 0.33f) % 1f
      val alpha = sin(localPhase * PI).toFloat().coerceIn(0f, 1f) * 0.48f
      val baseX = w * xRatio
      val startY = h * 0.92f - localPhase * (h * 0.35f)
      val topY = startY - h * 0.45f

      val path = Path().apply {
        moveTo(baseX, startY)
        cubicTo(
          baseX - 10f * sin(localPhase * 4f + index),
          (startY + topY) * 0.65f,
          baseX + 10f * cos(localPhase * 4f + index),
          (startY + topY) * 0.35f,
          baseX + 4f * sin(localPhase * 6f),
          topY
        )
      }
      drawPath(
        path = path,
        color = Color.White.copy(alpha = alpha),
        style = Stroke(width = 3.2f, cap = StrokeCap.Round)
      )
    }

    // Floating warm golden spice embers
    for (i in 0 until 5) {
      val p = (steamPhase + i * 0.21f) % 1f
      val alpha = sin(p * PI).toFloat().coerceIn(0f, 1f) * 0.75f
      val x = w * (0.18f + 0.16f * i) + sin(p * 5f + i) * 8f
      val y = h * (0.95f - p * 0.85f)
      drawCircle(
        color = if (i % 2 == 0) MealVerseSaffronYellow.copy(alpha = alpha) else accentColor.copy(alpha = alpha),
        radius = 3.5f - p * 1.5f,
        center = Offset(x, y)
      )
    }
  }
}

/**
 * Compact 3D Animated Thumbnail with subtle hover tilt & levitation for cards & menu items.
 */
@Composable
fun Animated3DFoodThumbnail(
  @DrawableRes imageRes: Int,
  contentDescription: String,
  size: Dp = 68.dp,
  cornerRadius: Dp = 18.dp,
  phaseOffset: Float = 0f,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "thumb_3d")
  val angle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "thumbAngle"
  )
  val density = LocalDensity.current.density
  val adjusted = angle + phaseOffset
  val bobY = sin(adjusted) * 2.5f
  val rotY = cos(adjusted) * 8f
  val rotX = sin(adjusted) * 4f

  Box(
    modifier = modifier
      .size(size)
      .graphicsLayer {
        cameraDistance = 10f * density
        translationY = bobY * density
        rotationY = rotY
        rotationX = rotX
      }
      .clip(RoundedCornerShape(cornerRadius))
      .border(
        width = 1.5.dp,
        color = MealVerseSaffronYellow.copy(alpha = 0.55f),
        shape = RoundedCornerShape(cornerRadius)
      )
  ) {
    Image(
      painter = painterResource(id = imageRes),
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)
          )
        )
    )
  }
}
