package com.goldsilver.livecalc.data.repository

import android.content.Context
import com.goldsilver.livecalc.data.local.daos.AlertDao
import com.goldsilver.livecalc.data.local.daos.RateDao
import com.goldsilver.livecalc.data.local.daos.VerificationDao
import com.goldsilver.livecalc.data.local.entities.AlertEntity
import com.goldsilver.livecalc.data.local.entities.RateEntity
import com.goldsilver.livecalc.data.local.entities.VerificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import org.json.JSONObject

class MetalRepository(
    private val context: Context,
    private val rateDao: RateDao,
    private val alertDao: AlertDao,
    private val verificationDao: VerificationDao
) {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    var firebaseDatabaseUrl: String = ""
    val apiError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun getLatestRateFlow(): Flow<RateEntity?> = rateDao.getLatestRateFlow()

    fun getHistoricalRatesFlow(): Flow<List<RateEntity>> = rateDao.getHistoricalRatesFlow()

    suspend fun getLatestRate(): RateEntity? = rateDao.getLatestRate()

    suspend fun getHistoricalRates(): List<RateEntity> = rateDao.getHistoricalRates()

    suspend fun insertRate(rate: RateEntity) = rateDao.insertRate(rate)

    suspend fun clearAllRates() = rateDao.deleteAllRates()

    // Price alerts
    fun getAllAlertsFlow(): Flow<List<AlertEntity>> = alertDao.getAllAlertsFlow()
    suspend fun getActiveAlerts(): List<AlertEntity> = alertDao.getActiveAlerts()
    suspend fun insertAlert(alert: AlertEntity) = withContext(Dispatchers.IO) {
        alertDao.insertAlert(alert)
        val latestRate = rateDao.getLatestRate()
        if (latestRate != null) {
            checkAndTriggerAlerts(latestRate)
        }
    }
    suspend fun updateAlert(alert: AlertEntity) = alertDao.updateAlert(alert)
    suspend fun deleteAlert(alert: AlertEntity) = alertDao.deleteAlert(alert)
    suspend fun deleteAlertById(id: Int) = alertDao.deleteAlertById(id)

    // Check and trigger price alerts
    suspend fun checkAndTriggerAlerts(rate: RateEntity) = withContext(Dispatchers.IO) {
        val activeAlerts = alertDao.getActiveAlerts()
        val sharedPrefs = context.getSharedPreferences("gold_silver_prefs", Context.MODE_PRIVATE)
        val isNotificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        val notificationHelper = com.goldsilver.livecalc.background.NotificationHelper(context)

        for (alert in activeAlerts) {
            val currentPrice = if (alert.metal == "GOLD") rate.goldPrice24k else rate.silverPrice
            val metalName = if (alert.metal == "GOLD") "Gold 24K" else "Silver"
            
            var isTriggered = false
            if (alert.condition == "ABOVE" && currentPrice >= alert.targetPrice) {
                isTriggered = true
            } else if (alert.condition == "BELOW" && currentPrice <= alert.targetPrice) {
                isTriggered = true
            }

            if (isTriggered) {
                // Trigger notification if enabled
                if (isNotificationsEnabled) {
                    val formattedPrice = String.format("%.2f", currentPrice)
                    val formattedTarget = String.format("%.2f", alert.targetPrice)
                    val title = "Price Alert: $metalName"
                    val message = "$metalName has crossed your target price of $formattedTarget ${rate.currency}! Current price: $formattedPrice ${rate.currency}"
                    notificationHelper.showPriceAlertNotification(title, message)
                }

                // Mark alert as triggered
                alertDao.updateAlert(
                    alert.copy(
                        isActive = false,
                        triggeredAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Hallmark verifications
    fun getAllVerificationsFlow(): Flow<List<VerificationEntity>> = verificationDao.getAllVerificationsFlow()
    suspend fun insertVerification(verification: VerificationEntity) = verificationDao.insertVerification(verification)
    suspend fun clearVerificationHistory() = verificationDao.clearHistory()

    private fun parseFirestoreJson(jsonString: String, defaultCurrency: String): RateEntity {
        val jsonObject = JSONObject(jsonString)
        val fields = jsonObject.optJSONObject("fields") ?: throw java.io.IOException("Invalid Firestore document: missing 'fields' object")

        fun getDouble(name: String): Double {
            val field = fields.optJSONObject(name) ?: return 0.0
            return when {
                field.has("doubleValue") -> field.getDouble("doubleValue")
                field.has("integerValue") -> field.getLong("integerValue").toDouble()
                field.has("stringValue") -> field.getString("stringValue").toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        }

        fun getLong(name: String): Long {
            val field = fields.optJSONObject(name) ?: return 0L
            return when {
                field.has("integerValue") -> field.getLong("integerValue")
                field.has("doubleValue") -> field.getDouble("doubleValue").toLong()
                field.has("stringValue") -> field.getString("stringValue").toLongOrNull() ?: 0L
                else -> 0L
            }
        }

        fun getString(name: String): String {
            val field = fields.optJSONObject(name) ?: return ""
            return field.optString("stringValue", "")
        }

        val timestamp = getLong("timestamp")
        val goldPrice24k = getDouble("goldPrice24k")
        val goldPrice22k = getDouble("goldPrice22k")
        val goldPrice18k = getDouble("goldPrice18k")
        val goldPrice14k = getDouble("goldPrice14k")
        val silverPrice = getDouble("silverPrice")
        val currency = getString("currency").ifBlank { defaultCurrency }

        val finalTimestamp = if (timestamp == 0L) System.currentTimeMillis() else timestamp

        return RateEntity(
            timestamp = finalTimestamp,
            goldPrice24k = goldPrice24k,
            goldPrice22k = goldPrice22k,
            goldPrice18k = goldPrice18k,
            goldPrice14k = goldPrice14k,
            silverPrice = silverPrice,
            currency = currency
        )
    }

    // Fetch and sync rates (Sync with Cloud Firestore or Firebase Realtime Database)
    suspend fun fetchRates(currency: String): Result<RateEntity> = withContext(Dispatchers.IO) {
        try {
            apiError.value = null
            
            // Check if currency changed, clear table to prevent mixed currency chart data
            val latestCached = rateDao.getLatestRate()
            if (latestCached != null && latestCached.currency != currency) {
                rateDao.deleteAllRates()
            }
            seedHistoricalDataIfEmpty(currency)

            val dbUrl = firebaseDatabaseUrl.trim()
            if (dbUrl.isBlank()) {
                // No firebase url, fall back to offline simulation
                val simulatedRate = generateSimulatedRate(currency)
                rateDao.insertRate(simulatedRate)
                checkAndTriggerAlerts(simulatedRate)
                return@withContext Result.success(simulatedRate)
            }

            // Construct the REST endpoint url.
            // If it starts with http/https, use it directly. Otherwise, treat it as a Firestore Project ID.
            val fetchUrl = if (dbUrl.startsWith("http://") || dbUrl.startsWith("https://")) {
                if (dbUrl.contains(".firebaseio.com") || dbUrl.contains(".firebasedatabase.app")) {
                    val normalizedUrl = if (dbUrl.endsWith("/")) dbUrl else "$dbUrl/"
                    "${normalizedUrl}rates/$currency.json"
                } else {
                    dbUrl
                }
            } else {
                "https://firestore.googleapis.com/v1/projects/$dbUrl/databases/(default)/documents/rates/$currency"
            }

            val request = okhttp3.Request.Builder()
                .url(fetchUrl)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw java.io.IOException("Firebase response error: ${response.code} ${response.message}")
                }
                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank() || responseBody == "null") {
                    throw java.io.IOException("No data found at path for $currency")
                }

                // Dual-compatibility parser: Firestore format contains "fields" key
                val parsedRate = if (responseBody.contains("\"fields\"")) {
                    parseFirestoreJson(responseBody, currency)
                } else {
                    json.decodeFromString<RateEntity>(responseBody)
                }

                // Ensure id is not saved from database to avoid key collisions
                val rateToSave = parsedRate.copy(id = 0)
                rateDao.insertRate(rateToSave)
                checkAndTriggerAlerts(rateToSave)
                Result.success(rateToSave)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = e.localizedMessage ?: e.toString()
            apiError.value = errorMsg
            Result.failure(e)
        }
    }

    private fun getBaseRates(currency: String): Pair<Double, Double> {
        return when (currency) {
            "INR" -> Pair(14630.0, 233.0) // Gold: ₹14,630/g, Silver: ₹233/g
            "EUR" -> Pair(123.0, 2.76)
            "AED" -> Pair(491.0, 11.0)
            "GBP" -> Pair(105.0, 2.35)
            else -> Pair(133.74, 3.00) // USD: Gold $133.74/g, Silver $3.00/g
        }
    }

    // Helper to generate simulated rates with mild randomness
    suspend fun generateSimulatedRate(currency: String): RateEntity {
        val (actualBaseGold, actualBaseSilver) = getBaseRates(currency)
        
        // If the latest cached price is too far from base (e.g. > 1.5%), pull it back to the base rate
        val latestCached = rateDao.getLatestRate()
        val baseGold24k = if (latestCached != null && latestCached.currency == currency && 
            Math.abs(latestCached.goldPrice24k - actualBaseGold) / actualBaseGold < 0.015) {
            latestCached.goldPrice24k
        } else {
            actualBaseGold
        }
        
        val baseSilver = if (latestCached != null && latestCached.currency == currency && 
            Math.abs(latestCached.silverPrice - actualBaseSilver) / actualBaseSilver < 0.02) {
            latestCached.silverPrice
        } else {
            actualBaseSilver
        }

        // Apply a tiny random walk offset from the base, but with a small dampening factor towards the actual base
        val goldScale = actualBaseGold * 0.0008
        val silverScale = actualBaseSilver * 0.0016
        
        // Pull towards base slightly (mean reversion)
        val pullGold = (actualBaseGold - baseGold24k) * 0.1
        val pullSilver = (actualBaseSilver - baseSilver) * 0.1
        
        val randomOffsetGold = pullGold + (if (Math.random() > 0.5) 1 else -1) * (actualBaseGold * 0.0001 + Math.random() * goldScale)
        val randomOffsetSilver = pullSilver + (if (Math.random() > 0.5) 1 else -1) * (actualBaseSilver * 0.0002 + Math.random() * silverScale)

        val finalGold24k = baseGold24k + randomOffsetGold
        val finalSilver = baseSilver + randomOffsetSilver

        val (gold22kRatio, gold18kRatio, gold14kRatio) = Triple(0.9167, 0.75, 0.5833)

        return RateEntity(
            timestamp = System.currentTimeMillis(),
            goldPrice24k = finalGold24k,
            goldPrice22k = finalGold24k * gold22kRatio,
            goldPrice18k = finalGold24k * gold18kRatio,
            goldPrice14k = finalGold24k * gold14kRatio,
            silverPrice = finalSilver,
            currency = currency
        )
    }

    private fun getCurrencyMultiplier(currency: String): Double {
        return when (currency) {
            "INR" -> 83.50
            "EUR" -> 0.92
            "AED" -> 3.67
            "GBP" -> 0.78
            else -> 1.0 // USD
        }
    }

    suspend fun seedHistoricalDataIfEmpty(currency: String) = withContext(Dispatchers.IO) {
        val existingRates = rateDao.getHistoricalRates()
        val count = existingRates.size

        // If currency changed, clear rates table
        if (count > 0 && existingRates[0].currency != currency) {
            rateDao.deleteAllRates()
        }

        val currentCount = rateDao.getHistoricalRates().size
        if (currentCount == 0) {
            // Seed 30 days of data ending today
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L
            val (baseGold, baseSilver) = getBaseRates(currency)

            val (gold22kRatio, gold18kRatio, gold14kRatio) = Triple(0.9167, 0.75, 0.5833)

            for (i in 30 downTo 1) {
                val dayTime = now - (i * dayMillis)
                // Use a sine wave + minor random noise for realistic looking chart trends
                val factor = Math.sin(i.toDouble() / 5.0) * (baseGold * 0.015) + (Math.random() - 0.5) * (baseGold * 0.008)
                val goldVal = baseGold + factor
                val silverFactor = Math.sin(i.toDouble() / 6.0) * (baseSilver * 0.03) + (Math.random() - 0.5) * (baseSilver * 0.015)
                val silverVal = baseSilver + silverFactor

                val seedEntity = RateEntity(
                    timestamp = dayTime,
                    goldPrice24k = goldVal,
                    goldPrice22k = goldVal * gold22kRatio,
                    goldPrice18k = goldVal * gold18kRatio,
                    goldPrice14k = goldVal * gold14kRatio,
                    silverPrice = silverVal,
                    currency = currency
                )
                rateDao.insertRate(seedEntity)
            }
        }
    }

    suspend fun fetchRemoteConfig(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val configSettings = com.google.firebase.remoteconfig.remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (com.goldsilver.livecalc.BuildConfig.DEBUG) 0 else 3600
            }
            remoteConfig.setConfigSettingsAsync(configSettings)

            val defaults = mapOf(
                "latest_version" to "1.0.0",
                "latest_version_code" to 1L,
                "update_message" to "A newer version of Gold & Silver Live Calc is available. Please update to access the latest market rates and features.",
                "apk_download_url" to ""  // GitHub Releases direct APK download URL
            )
            remoteConfig.setDefaultsAsync(defaults)

            suspendCancellableCoroutine<Map<String, Any>> { continuation ->
                remoteConfig.fetchAndActivate()
                    .addOnCompleteListener { task ->
                        val latestCode = remoteConfig.getLong("latest_version_code").toInt()
                        val latestName = remoteConfig.getString("latest_version")
                        val message = remoteConfig.getString("update_message")
                        val apkUrl = remoteConfig.getString("apk_download_url")
                        if (continuation.isActive) {
                            continuation.resume(
                                mapOf(
                                    "latest_version" to latestName,
                                    "latest_version_code" to latestCode,
                                    "update_message" to message,
                                    "apk_download_url" to apkUrl
                                )
                            )
                        }
                    }
                    .addOnFailureListener {
                        val latestCode = remoteConfig.getLong("latest_version_code").toInt()
                        val latestName = remoteConfig.getString("latest_version")
                        val message = remoteConfig.getString("update_message")
                        val apkUrl = remoteConfig.getString("apk_download_url")
                        if (continuation.isActive) {
                            continuation.resume(
                                mapOf(
                                    "latest_version" to latestName,
                                    "latest_version_code" to latestCode,
                                    "update_message" to message,
                                    "apk_download_url" to apkUrl
                                )
                            )
                        }
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mapOf(
                "latest_version" to "1.0.0",
                "latest_version_code" to 1,
                "update_message" to "A newer version of Gold & Silver Live Calc is available.",
                "apk_download_url" to ""
            )
        }
    }
}
