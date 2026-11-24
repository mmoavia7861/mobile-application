package com.example

import com.example.data.model.*
import com.example.ui.components.MapGeoUtils
import org.junit.Assert.*
import org.junit.Test

class LocationPickerUtilsTest {

  @Test
  fun `all Pakistan cities have valid default map coordinates`() {
    for (city in PAKISTAN_CITIES) {
      val coords = PAKISTAN_CITY_COORDINATES[city]
      assertNotNull("Missing coordinates for $city", coords)
      val (lat, lon) = coords!!
      assertTrue("Invalid latitude for $city", lat in 23.0..37.5)
      assertTrue("Invalid longitude for $city", lon in 60.0..78.0)
    }
  }

  @Test
  fun `web mercator round trip preserves Islamabad coordinates`() {
    val (lat, lon) = 33.6844 to 73.0479
    val zoom = 15
    val (worldX, worldY) = MapGeoUtils.latLonToWorldPixel(lat, lon, zoom)
    val (decodedLat, decodedLon) = MapGeoUtils.worldPixelToLatLon(worldX, worldY, zoom)

    assertEquals(lat, decodedLat, 0.00001)
    assertEquals(lon, decodedLon, 0.00001)
  }

  @Test
  fun `panCenterByDelta shifts center coordinates accurately`() {
    val startLat = 31.5204
    val startLon = 74.3587
    // Dragging pointer right (+dx) moves map eastbound tiles right, so center moves west (-lon)
    val (movedLat, movedLon) = MapGeoUtils.panCenterByDelta(
      lat = startLat,
      lon = startLon,
      dragDxPx = 128f,
      dragDyPx = -128f,
      zoom = 14
    )
    assertTrue("Dragging down/negative Y should move center south", movedLat < startLat)
    assertTrue("Dragging right/positive X should move center west", movedLon < startLon)
  }

  @Test
  fun `formatCoordinates outputs readable N E format`() {
    val formatted = MapGeoUtils.formatCoordinates(33.6844, 73.0479)
    assertEquals("33.68440° N, 73.04790° E", formatted)
  }

  @Test
  fun `tile XY indices stay within zoom bounds`() {
    val zoom = 14
    val (tx, ty) = MapGeoUtils.latLonToTileXY(24.8607, 67.0011, zoom)
    val maxTile = (1 shl zoom) - 1
    assertTrue(tx in 0..maxTile)
    assertTrue(ty in 0..maxTile)
  }

  @Test
  fun `store and merchant models preserve delivery coordinates`() {
    val store = Store(
      name = "Savour Foods",
      city = "Islamabad",
      latitude = 33.7077,
      longitude = 73.0501,
      status = "Pending"
    )
    assertEquals(33.7077, store.latitude!!, 0.00001)
    assertEquals(73.0501, store.longitude!!, 0.00001)

    val merchant = Merchant(
      businessName = "Savour Foods",
      ownerName = "Tariq",
      latitude = 33.7077,
      longitude = 73.0501,
      status = "Pending"
    )
    assertEquals(33.7077, merchant.latitude!!, 0.00001)
    assertEquals(73.0501, merchant.longitude!!, 0.00001)
  }
}
