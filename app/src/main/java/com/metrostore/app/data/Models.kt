package com.metrostore.app.data

import android.content.Context
import android.graphics.Color as AColor
import org.json.JSONObject

/** One entry of the store catalog (assets/catalog.json). */
data class StoreApp(
    val id: String,
    val name: String,
    val publisher: String,
    val description: String,
    val version: String,
    val category: String,
    val packageName: String?,   // Play Store package -> opens the official listing
    val apkUrl: String?,        // optional authorised HTTPS APK source
    val apkAsset: String?,      // optional APK bundled in assets/apks/
    val iconUrl: String?,
    val rating: Float,          // 0 = hidden
    val price: String,
    val color: Int,
    val featured: Boolean,
    val isNew: Boolean,
    val size: String?,
    val isMine: Boolean = false   // defined in my_apps.json
) {
    val isFree: Boolean get() = price.equals("free", ignoreCase = true)
}

/** An app whose installation was started from this store (verified against the device later). */
data class TrackedApp(val packageName: String, val label: String, val catalogId: String?)

enum class Kind { FEATURED, FREE, NEW, PAID, ALL, CATEGORY }

fun filterApps(all: List<StoreApp>, kind: Kind, arg: String = ""): List<StoreApp> = when (kind) {
    Kind.FEATURED -> all.filter { it.featured }
    Kind.FREE -> all.filter { it.isFree }
    Kind.NEW -> all.filter { it.isNew }
    Kind.PAID -> all.filter { !it.isFree }
    Kind.ALL -> all.sortedBy { it.name.lowercase() }
    Kind.CATEGORY -> all.filter { it.category.equals(arg, ignoreCase = true) }
}

private fun JSONObject.str(key: String): String? =
    if (has(key) && !isNull(key)) getString(key).takeIf { it.isNotBlank() } else null

object CatalogRepository {
    /** catalog.json (Play Store apps) + my_apps.json (your own apps, optional). */
    fun load(context: Context): List<StoreApp> =
        read(context, "catalog.json", "other", false) + read(context, "my_apps.json", "my apps", true)

    private fun read(context: Context, file: String, defaultCategory: String, mine: Boolean): List<StoreApp> {
        val text = try {
            context.assets.open(file).bufferedReader().use { it.readText() }
        } catch (e: java.io.FileNotFoundException) {
            return emptyList()
        }
        val arr = JSONObject(text).getJSONArray("apps")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            StoreApp(
                id = o.getString("id"),
                name = o.getString("name"),
                publisher = o.optString("publisher", ""),
                description = o.optString("description", ""),
                version = o.optString("version", "varies"),
                category = o.optString("category", defaultCategory),
                packageName = o.str("packageName"),
                apkUrl = o.str("apkUrl"),
                apkAsset = o.str("apkAsset"),
                iconUrl = o.str("iconUrl"),
                rating = o.optDouble("rating", 0.0).toFloat(),
                price = o.optString("price", "free"),
                color = runCatching { AColor.parseColor(o.optString("color", "#1E9E1E")) }
                    .getOrDefault(0xFF1E9E1E.toInt()),
                featured = o.optBoolean("featured", false),
                isNew = o.optBoolean("new", false),
                size = o.str("size"),
                isMine = mine
            )
        }
    }
}
