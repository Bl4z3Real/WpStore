package com.metrostore.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Small persistence layer: tracked installs + accent colour. */
class LocalStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("metro_store", Context.MODE_PRIVATE)

    val apkDir: File = File(ctx.filesDir, "apks").apply { mkdirs() }
    val downloadDir: File = File(ctx.filesDir, "downloads").apply { mkdirs() }

    var accent: Int
        get() = prefs.getInt("accent", 0xFF1E9E1E.toInt())
        set(value) { prefs.edit().putInt("accent", value).apply() }

    fun loadTracked(): List<TrackedApp> {
        val raw = prefs.getString("tracked", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                TrackedApp(
                    packageName = o.getString("packageName"),
                    label = o.optString("label", ""),
                    catalogId = if (o.isNull("catalogId")) null else o.optString("catalogId")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveTracked(list: List<TrackedApp>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("packageName", it.packageName); put("label", it.label)
                put("catalogId", it.catalogId ?: JSONObject.NULL)
            })
        }
        prefs.edit().putString("tracked", arr.toString()).apply()
    }
}
