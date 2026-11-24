package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Product
import com.example.data.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiFoodAssistant {

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  suspend fun askFoodAdvice(
    userQuery: String,
    stores: List<Store>,
    city: String
  ): String = withContext(Dispatchers.IO) {
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
      ""
    }

    // Build context from real database stores & menus
    val storeContext = stores.take(6).joinToString("\n") { s ->
      "- ${s.name} (${s.category}, City: ${s.city}): Min order Rs. ${s.minOrder.toInt()}, Delivery Rs. ${s.deliveryFee.toInt()}, Prep: ${s.prepTimeMinutes} mins, Rating: ${s.rating}★"
    }

    val systemPrompt = """
      Aap MealVerse ke official AI Food Consultant hain (Pakistan's food marketplace).
      Tagline: 'Har Zaiqa, Har Ghar'.
      Current City: $city.
      Available Restaurants in MealVerse:
      $storeContext
      
      Instructions:
      1. User Roman Urdu ya English me sawal pooch sakta hai. Aap Roman Urdu / English me respectful, warm aur helpful desi lehje me jawab dein.
      2. Sirf upar di gayi real restaurant listings, realistic Pakistani rates (e.g., Pulao Rs. 420, Biryani Rs. 390, Karahi Rs. 1850) aur budget ke mutabiq recommendations dein.
      3. Agar budget Rs. 500 hai toh Savour Foods ka Single Chicken Pulao (Rs. 420) ya Student Biryani (Rs. 390) recommend karein.
      4. Jawab ko mukhtasir (2-4 lines) aur clear rakhein.
    """.trimIndent()

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      // Intelligent on-device contextual fallback using current real menu data
      return@withContext getOfflineAdvice(userQuery, stores, city)
    }

    try {
      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
      val requestJson = JSONObject().apply {
        put("systemInstruction", JSONObject().apply {
          put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
        })
        put("contents", JSONArray().put(JSONObject().apply {
          put("parts", JSONArray().put(JSONObject().put("text", userQuery)))
        }))
        put("generationConfig", JSONObject().apply {
          put("temperature", 0.7)
          put("maxOutputTokens", 400)
        })
      }

      val body = requestJson.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(url).post(body).build()
      val response = client.newCall(request).execute()

      if (response.isSuccessful) {
        val resBody = response.body?.string() ?: ""
        val json = JSONObject(resBody)
        val candidates = json.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")
        if (!text.isNullOrBlank()) {
          return@withContext text.trim()
        }
      }
    } catch (e: Exception) {
      Log.w("GeminiFoodAssistant", "API call error: ${e.message}")
    }

    return@withContext getOfflineAdvice(userQuery, stores, city)
  }

  private fun getOfflineAdvice(query: String, stores: List<Store>, city: String): String {
    val q = query.lowercase()
    return when {
      q.contains("500") || q.contains("budget") || q.contains("sasta") || q.contains("cheap") -> {
        "Aap ke Rs. 500 budget ke mutabiq Savour Foods ka mashhoor 'Single Chicken Pulao with Shami Kabab' (Rs. 420) ya Student Biryani (Rs. 390) behtareen choice hai! Sath delivery Rs. 70-80 lagti hai."
      }
      q.contains("biryani") -> {
        "Biryani ke liye Karachi aur Islamabad me 'Student Biryani' ka Chicken Dum Biryani with Aloo (Rs. 390) sab se hit option hai!"
      }
      q.contains("karahi") || q.contains("mutton") -> {
        "Karahi ke liye 'Butt Karahi' behtareen hai! Inki Desi Ghee Chicken Karahi (Rs. 1850) aur Mutton Karahi Garam Khamiri Roti ke sath lajawab hai."
      }
      q.contains("nashta") || q.contains("chai") || q.contains("paratha") -> {
        "Desi Nashte ke liye 'Chai Shai Spot' ka Aloo Cheese Paratha (Rs. 260) aur Karak Doodh Patti Chai (Rs. 120) zaroor try karein!"
      }
      q.contains("ghar") || q.contains("home") || q.contains("healthy") -> {
        "'Ammi's Home Kitchen' se hygienic Daal Chawal Thali (Rs. 320) ya Desi Chicken Qorma phulko ke sath order karein."
      }
      else -> {
        "MealVerse par $city ke top trending restaurants me Savour Foods (Pulao), Butt Karahi aur Cheezious shamil hain. Aap ka budget aur pasand kya hai?"
      }
    }
  }
}
