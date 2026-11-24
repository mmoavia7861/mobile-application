package com.example.ui.customer

import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.model.Store

fun getStoreGraphic(name: String, category: String): Int {
  return when {
    name.contains("Biryani", ignoreCase = true) ||
      category.contains("Biryani", ignoreCase = true) ||
      name.contains("Savour", ignoreCase = true) ||
      name.contains("Pulao", ignoreCase = true) -> R.drawable.img_biryani_special

    category.contains("Sweets", ignoreCase = true) ||
      category.contains("Bakery", ignoreCase = true) ||
      category.contains("Dessert", ignoreCase = true) ||
      name.contains("Sweets", ignoreCase = true) ||
      name.contains("Halwa", ignoreCase = true) ||
      name.contains("Mithai", ignoreCase = true) ||
      name.contains("Ice Cream", ignoreCase = true) ||
      name.contains("Rewari", ignoreCase = true) -> R.drawable.img_mithai_sweets

    category.contains("Nashta", ignoreCase = true) ||
      category.contains("Breakfast", ignoreCase = true) ||
      name.contains("Chai", ignoreCase = true) ||
      name.contains("Paratha", ignoreCase = true) ||
      name.contains("Nashta", ignoreCase = true) ||
      name.contains("Lassi", ignoreCase = true) -> R.drawable.img_chai_nashta

    category.contains("Fast Food", ignoreCase = true) ||
      category.contains("Pizza", ignoreCase = true) ||
      category.contains("Burger", ignoreCase = true) ||
      name.contains("Cheezious", ignoreCase = true) ||
      name.contains("Zinger", ignoreCase = true) ||
      name.contains("Shawarma", ignoreCase = true) ||
      name.contains("Fork", ignoreCase = true) -> R.drawable.img_burgers_fastfood

    category.contains("Karahi", ignoreCase = true) ||
      category.contains("BBQ", ignoreCase = true) ||
      name.contains("Tikka", ignoreCase = true) ||
      name.contains("Nihari", ignoreCase = true) ||
      name.contains("Kabab", ignoreCase = true) ||
      name.contains("Shinwari", ignoreCase = true) ||
      name.contains("Charsi", ignoreCase = true) ||
      name.contains("Butt", ignoreCase = true) -> R.drawable.img_karahi_bbq

    else -> R.drawable.img_hero_food_banner
  }
}

fun getStoreGraphic(store: Store): Int = getStoreGraphic(store.name, store.category)

@DrawableRes
fun get3DFoodGraphic(name: String, category: String = ""): Int {
  return when {
    name.contains("Biryani", ignoreCase = true) ||
      name.contains("Pulao", ignoreCase = true) ||
      category.contains("Biryani", ignoreCase = true) -> R.drawable.img_3d_biryani_bowl

    name.contains("Karahi", ignoreCase = true) ||
      name.contains("Tikka", ignoreCase = true) ||
      name.contains("BBQ", ignoreCase = true) ||
      name.contains("Kabab", ignoreCase = true) ||
      name.contains("Nihari", ignoreCase = true) ||
      name.contains("Qorma", ignoreCase = true) ||
      name.contains("Haleem", ignoreCase = true) ||
      name.contains("Sajji", ignoreCase = true) ||
      category.contains("Karahi", ignoreCase = true) ||
      category.contains("BBQ", ignoreCase = true) -> R.drawable.img_3d_karahi_feast

    name.contains("Burger", ignoreCase = true) ||
      name.contains("Zinger", ignoreCase = true) ||
      name.contains("Pizza", ignoreCase = true) ||
      name.contains("Fries", ignoreCase = true) ||
      name.contains("Roll", ignoreCase = true) ||
      name.contains("Shawarma", ignoreCase = true) ||
      category.contains("Fast Food", ignoreCase = true) ||
      category.contains("Pizza", ignoreCase = true) ||
      category.contains("Sides", ignoreCase = true) -> R.drawable.img_3d_burger_combo

    name.contains("Chai", ignoreCase = true) ||
      name.contains("Halwa", ignoreCase = true) ||
      name.contains("Gulab", ignoreCase = true) ||
      name.contains("Paratha", ignoreCase = true) ||
      name.contains("Kheer", ignoreCase = true) ||
      name.contains("Zarda", ignoreCase = true) ||
      name.contains("Lassi", ignoreCase = true) ||
      name.contains("Falooda", ignoreCase = true) ||
      category.contains("Dessert", ignoreCase = true) ||
      category.contains("Sweet", ignoreCase = true) ||
      category.contains("Breakfast", ignoreCase = true) ||
      category.contains("Beverage", ignoreCase = true) -> R.drawable.img_3d_chai_mithai

    else -> R.drawable.img_3d_biryani_bowl
  }
}

data class CuisineCategory(
  val name: String,
  val imageRes: Int,
  val subtitle: String
)

data class Featured3DDish(
  val id: String,
  val title: String,
  val subtitle: String,
  val priceTag: String,
  val categoryFilter: String,
  @param:DrawableRes val imageRes: Int,
  val accentColorHex: Long
)

val FEATURED_3D_DISHES = listOf(
  Featured3DDish(
    id = "biryani",
    title = "Saffron Dum Biryani",
    subtitle = "Aromatic Karachi masala & raita",
    priceTag = "From Rs. 390",
    categoryFilter = "Biryani",
    imageRes = R.drawable.img_3d_biryani_bowl,
    accentColorHex = 0xFFFFBA08
  ),
  Featured3DDish(
    id = "karahi",
    title = "Desi Ghee Karahi",
    subtitle = "Sizzling wok mutton & garlic naan",
    priceTag = "Fresh on Flame",
    categoryFilter = "Karahi",
    imageRes = R.drawable.img_3d_karahi_feast,
    accentColorHex = 0xFFE85D04
  ),
  Featured3DDish(
    id = "burger",
    title = "Crispy Zinger Combo",
    subtitle = "Crunchy fillet, melted cheddar & fries",
    priceTag = "From Rs. 550",
    categoryFilter = "Fast Food",
    imageRes = R.drawable.img_3d_burger_combo,
    accentColorHex = 0xFFFF7B00
  ),
  Featured3DDish(
    id = "chai",
    title = "Karak Chai & Mithai",
    subtitle = "Kulhad cardamom tea & warm gulab jamun",
    priceTag = "From Rs. 150",
    categoryFilter = "Desi Nashta",
    imageRes = R.drawable.img_3d_chai_mithai,
    accentColorHex = 0xFF138A7E
  )
)

val POPULAR_CUISINES = listOf(
  CuisineCategory("All", R.drawable.img_3d_biryani_bowl, "Sab Cuisines"),
  CuisineCategory("Biryani", R.drawable.img_3d_biryani_bowl, "Dum & Pulao"),
  CuisineCategory("Karahi", R.drawable.img_3d_karahi_feast, "Desi Ghee & BBQ"),
  CuisineCategory("Fast Food", R.drawable.img_3d_burger_combo, "Burgers & Pizza"),
  CuisineCategory("Desi Nashta", R.drawable.img_3d_chai_mithai, "Chai & Paratha"),
  CuisineCategory("Sweets", R.drawable.img_mithai_sweets, "Mithai & Halwa"),
  CuisineCategory("Home Chef", R.drawable.img_hero_food_banner, "Ghar ka Khana")
)
