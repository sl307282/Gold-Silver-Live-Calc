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

    fun getLatestRateForCurrencyFlow(currency: String): Flow<RateEntity?> = rateDao.getLatestRateForCurrencyFlow(currency)

    fun getHistoricalRatesFlow(): Flow<List<RateEntity>> = rateDao.getHistoricalRatesFlow()

    fun getHistoricalRatesForCurrencyFlow(currency: String): Flow<List<RateEntity>> = rateDao.getHistoricalRatesForCurrencyFlow(currency)

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
    suspend fun updateAlert(alert: AlertEntity) = withContext(Dispatchers.IO) {
        val resetAlert = alert.copy(
            isActive = true,
            triggeredAt = null
        )
        alertDao.updateAlert(resetAlert)
        val latestRate = rateDao.getLatestRate()
        if (latestRate != null) {
            checkAndTriggerAlerts(latestRate)
        }
    }
    suspend fun deleteAlert(alert: AlertEntity) = alertDao.deleteAlert(alert)
    suspend fun deleteAlertById(id: Int) = alertDao.deleteAlertById(id)

    private fun getCurrencySymbol(currencyCode: String): String {
        return when (currencyCode) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "AED" -> "د.إ"
            "GBP" -> "£"
            else -> currencyCode
        }
    }

    // Check and trigger price alerts
    suspend fun checkAndTriggerAlerts(rate: RateEntity) = withContext(Dispatchers.IO) {
        val activeAlerts = alertDao.getActiveAlerts()
        val sharedPrefs = context.getSharedPreferences("gold_silver_prefs", Context.MODE_PRIVATE)
        val isNotificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        val notificationHelper = com.goldsilver.livecalc.background.NotificationHelper(context)

        for (alert in activeAlerts) {
            val currentPrice = if (alert.metal == "GOLD") rate.goldPrice24k else rate.silverPrice
            val metalName = if (alert.metal == "GOLD") "Gold (24K)" else "Silver"
            
            var isConditionMet = false
            if (alert.condition == "ABOVE" && currentPrice >= alert.targetPrice) {
                isConditionMet = true
            } else if (alert.condition == "BELOW" && currentPrice <= alert.targetPrice) {
                isConditionMet = true
            }

            if (isConditionMet) {
                val triggeredTime = alert.triggeredAt
                val isAlreadyTriggeredToday = if (triggeredTime != null) {
                    val triggeredCal = java.util.Calendar.getInstance().apply { timeInMillis = triggeredTime }
                    val currentCal = java.util.Calendar.getInstance()
                    triggeredCal.get(java.util.Calendar.YEAR) == currentCal.get(java.util.Calendar.YEAR) &&
                    triggeredCal.get(java.util.Calendar.DAY_OF_YEAR) == currentCal.get(java.util.Calendar.DAY_OF_YEAR)
                } else {
                    false
                }

                if (!isAlreadyTriggeredToday) {
                    // Trigger notification if enabled
                    if (isNotificationsEnabled) {
                        val symbol = getCurrencySymbol(rate.currency)
                        val formattedPrice = "$symbol${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(currentPrice)}"
                        val formattedTarget = "$symbol${com.goldsilver.livecalc.util.IndianCurrencyFormatter.formatAmount(alert.targetPrice)}"
                        val title = if (alert.metal == "GOLD") "🔔 Gold Price Alert" else "🔔 Silver Price Alert"
                        val message = if (alert.condition == "ABOVE") {
                            "$metalName has reached $formattedPrice/g, crossing your target of $formattedTarget/g."
                        } else {
                            "$metalName has fallen below your target of $formattedTarget/g. Current price: $formattedPrice/g."
                        }
                        notificationHelper.showPriceAlertNotification(title, message)
                    }

                    // Mark alert as triggered today, keeping it active for subsequent days
                    alertDao.updateAlert(
                        alert.copy(
                            isActive = true,
                            triggeredAt = System.currentTimeMillis()
                        )
                    )
                }
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

        fun getOptionalDouble(name: String): Double? {
            val field = fields.optJSONObject(name) ?: return null
            val value = when {
                field.has("doubleValue") -> field.getDouble("doubleValue")
                field.has("integerValue") -> field.getLong("integerValue").toDouble()
                field.has("stringValue") -> field.getString("stringValue").toDoubleOrNull()
                else -> null
            }
            return if (value != null && value > 0.0) value else null
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

        val prevTradingDayGoldPrice = getOptionalDouble("prevTradingDayGoldPrice")
            ?: getOptionalDouble("prevGoldPrice24k")
            ?: getOptionalDouble("previousCloseGold")
            ?: getOptionalDouble("prevCloseGold")

        val prevTradingDaySilverPrice = getOptionalDouble("prevTradingDaySilverPrice")
            ?: getOptionalDouble("prevSilverPrice")
            ?: getOptionalDouble("previousCloseSilver")
            ?: getOptionalDouble("prevCloseSilver")

        val finalTimestamp = if (timestamp == 0L) System.currentTimeMillis() else timestamp

        return RateEntity(
            timestamp = finalTimestamp,
            goldPrice24k = goldPrice24k,
            goldPrice22k = goldPrice22k,
            goldPrice18k = goldPrice18k,
            goldPrice14k = goldPrice14k,
            silverPrice = silverPrice,
            currency = currency,
            prevTradingDayGoldPrice = prevTradingDayGoldPrice,
            prevTradingDaySilverPrice = prevTradingDaySilverPrice
        )
    }

    suspend fun saveOrUpdateTodayRate(rate: RateEntity) = withContext(Dispatchers.IO) {
        val rates = rateDao.getRatesByCurrency(rate.currency)
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val todayStr = dateFormat.format(java.util.Date(rate.timestamp))

        val existingTodayRate = rates.firstOrNull { r ->
            r.date == todayStr || dateFormat.format(java.util.Date(r.timestamp)) == todayStr
        }

        // Look up the immediately previous saved valid rate from DB (strictly before today)
        val previousRecord = rates
            .filter { 
                val d = it.date ?: dateFormat.format(java.util.Date(it.timestamp))
                d < todayStr
            }
            .sortedByDescending { it.timestamp }
            .firstOrNull { it.goldPrice24k > 0 && it.silverPrice > 0 }

        val prevGold = rate.prevTradingDayGoldPrice ?: previousRecord?.goldPrice24k
        val prevSilver = rate.prevTradingDaySilverPrice ?: previousRecord?.silverPrice

        val rateWithMetadata = rate.copy(
            date = todayStr,
            unit = rate.unit ?: "gram",
            prevTradingDayGoldPrice = prevGold,
            prevTradingDaySilverPrice = prevSilver
        )

        if (existingTodayRate != null) {
            // Update today's record in place with the latest price
            rateDao.updateRate(
                rateWithMetadata.copy(id = existingTodayRate.id)
            )
        } else {
            // New day record
            rateDao.insertRate(rateWithMetadata.copy(id = 0))
        }

        // Enforce rolling 1-year window in local storage
        val oneYearAgoTimestamp = System.currentTimeMillis() - (365L * 24L * 60L * 60L * 1000L)
        rateDao.deleteRatesOlderThan(oneYearAgoTimestamp)
    }

    private suspend fun syncHistoryJsonToRoom(jsonString: String, currency: String) = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val existingRates = rateDao.getRatesByCurrency(currency).associateBy { 
                it.date ?: dateFormat.format(java.util.Date(it.timestamp)) 
            }
            val (gold22kRatio, gold18kRatio, gold14kRatio) = Triple(0.9167, 0.75, 0.5833)

            root.keys().forEach { dateKey ->
                try {
                    val dayObj = root.optJSONObject(dateKey) ?: return@forEach
                    val goldPrice24k = dayObj.optDouble("goldPrice24k", 0.0).takeIf { it > 0 } ?: return@forEach
                    val silverPrice = dayObj.optDouble("silverPrice", 0.0).takeIf { it > 0 } ?: return@forEach
                    val goldPrice22k = dayObj.optDouble("goldPrice22k", goldPrice24k * gold22kRatio)
                    val goldPrice18k = dayObj.optDouble("goldPrice18k", goldPrice24k * gold18kRatio)
                    val goldPrice14k = dayObj.optDouble("goldPrice14k", goldPrice24k * gold14kRatio)

                    val ts = dayObj.optLong("timestamp", 0L).takeIf { it > 0 }
                        ?: (dateFormat.parse(dateKey)?.time ?: System.currentTimeMillis())

                    val existing = existingRates[dateKey]
                    val entity = RateEntity(
                        id = existing?.id ?: 0,
                        date = dateKey,
                        unit = dayObj.optString("unit", "gram"),
                        currency = currency,
                        timestamp = ts,
                        goldPrice24k = goldPrice24k,
                        goldPrice22k = goldPrice22k,
                        goldPrice18k = goldPrice18k,
                        goldPrice14k = goldPrice14k,
                        silverPrice = silverPrice
                    )

                    if (existing != null) {
                        rateDao.updateRate(entity)
                    } else {
                        rateDao.insertRate(entity)
                    }
                } catch (ignored: Exception) {}
            }

            // Enforce rolling 1-year window: delete records older than 365 days from local Room database
            val oneYearAgoTimestamp = System.currentTimeMillis() - (365L * 24L * 60L * 60L * 1000L)
            rateDao.deleteRatesOlderThan(oneYearAgoTimestamp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
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

            val dbUrl = firebaseDatabaseUrl.trim()
            if (dbUrl.isBlank()) {
                val latestCachedRate = rateDao.getLatestRate()
                return@withContext if (latestCachedRate != null) {
                    Result.success(latestCachedRate)
                } else {
                    Result.failure(Exception("Firebase Database URL is not configured."))
                }
            }

            val isFirestore = !dbUrl.startsWith("http://") && !dbUrl.startsWith("https://")

            // 1. Fetch Latest Snapshot
            val fetchUrl = if (!isFirestore) {
                val normalizedUrl = if (dbUrl.endsWith("/")) dbUrl else "$dbUrl/"
                "${normalizedUrl}rates/$currency.json"
            } else {
                "https://firestore.googleapis.com/v1/projects/$dbUrl/databases/(default)/documents/rates/$currency"
            }

            val request = okhttp3.Request.Builder()
                .url(fetchUrl)
                .build()

            val parsedRate: RateEntity = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw java.io.IOException("Firebase response error: ${response.code} ${response.message}")
                }
                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank() || responseBody == "null") {
                    throw java.io.IOException("No data found at path for $currency")
                }

                // Dual-compatibility parser: Firestore format contains "fields" key
                if (responseBody.contains("\"fields\"")) {
                    parseFirestoreJson(responseBody, currency)
                } else {
                    json.decodeFromString<RateEntity>(responseBody)
                }
            }

            // 2. Fetch history if present in Firebase Realtime DB to populate historical records
            try {
                if (!isFirestore) {
                    val normalizedUrl = if (dbUrl.endsWith("/")) dbUrl else "$dbUrl/"
                    val historyUrl = "${normalizedUrl}history/$currency.json"
                    val historyReq = okhttp3.Request.Builder().url(historyUrl).build()
                    okHttpClient.newCall(historyReq).execute().use { histResp ->
                        if (histResp.isSuccessful) {
                            val histBody = histResp.body?.string()
                            if (!histBody.isNullOrBlank() && histBody != "null") {
                                syncHistoryJsonToRoom(histBody, currency)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Save or update today's record
            val rateToSave = parsedRate.copy(id = 0)
            saveOrUpdateTodayRate(rateToSave)
            checkAndTriggerAlerts(rateToSave)
            Result.success(rateToSave)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = e.localizedMessage ?: e.toString()
            apiError.value = errorMsg
            val latestCachedRate = rateDao.getLatestRate()
            if (latestCachedRate != null) {
                Result.success(latestCachedRate)
            } else {
                Result.failure(e)
            }
        }
    }


    suspend fun fetchRemoteConfig(): Map<String, Any> = withContext(Dispatchers.IO) {

        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
            val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val configSettings = com.google.firebase.remoteconfig.remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0L
            }
            remoteConfig.setConfigSettingsAsync(configSettings)

            val currentCode = com.goldsilver.livecalc.BuildConfig.VERSION_CODE.toLong()
            val currentName = com.goldsilver.livecalc.BuildConfig.VERSION_NAME
            val defaults = mapOf(
                "latest_version" to currentName,
                "app_version" to currentName,
                "version_name" to currentName,
                "latest_version_code" to currentCode,
                "app_version_code" to currentCode,
                "version_code" to currentCode,
                "update_message" to "A new version is ready with the latest rates and improvements.",
                "apk_download_url" to ""
            )
            remoteConfig.setDefaultsAsync(defaults)

            val remoteResult = kotlinx.coroutines.withTimeoutOrNull(6000L) {
                suspendCancellableCoroutine<Map<String, Any>> { continuation ->
                    fun extractConfig(): Map<String, Any> {
                        remoteConfig.activate()
                        val code1 = remoteConfig.getLong("latest_version_code")
                        val code2 = remoteConfig.getString("latest_version_code").toLongOrNull() ?: 0L
                        val code3 = remoteConfig.getLong("app_version_code")
                        val code4 = remoteConfig.getString("app_version_code").toLongOrNull() ?: 0L
                        val code5 = remoteConfig.getLong("version_code")
                        val code6 = remoteConfig.getString("version_code").toLongOrNull() ?: 0L
                        val latestCode = maxOf(code1, code2, code3, code4, code5, code6).toInt()

                        val name1 = remoteConfig.getString("latest_version")
                        val name2 = remoteConfig.getString("app_version")
                        val name3 = remoteConfig.getString("version_name")
                        val name4 = remoteConfig.getString("version")
                        val latestName = listOf(name1, name2, name3, name4).firstOrNull { it.isNotBlank() } ?: currentName

                        val msg1 = remoteConfig.getString("update_message")
                        val msg2 = remoteConfig.getString("message")
                        val message = if (msg1.isNotBlank()) msg1 else if (msg2.isNotBlank()) msg2 else ""

                        val apkUrl = remoteConfig.getString("apk_download_url")

                        android.util.Log.d("RemoteConfig", "Fetched latestCode=$latestCode (currentCode=$currentCode), latestName=$latestName")

                        return mapOf(
                            "latest_version" to latestName,
                            "latest_version_code" to latestCode,
                            "update_message" to message,
                            "apk_download_url" to apkUrl
                        )
                    }

                    // Use explicit fetch(0L) to bypass any client-side cache
                    remoteConfig.fetch(0L)
                        .addOnCompleteListener { fetchTask ->
                            if (fetchTask.isSuccessful) {
                                remoteConfig.activate().addOnCompleteListener {
                                    if (continuation.isActive) continuation.resume(extractConfig())
                                }
                            } else {
                                if (continuation.isActive) continuation.resume(extractConfig())
                            }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.w("RemoteConfig", "fetch(0L) failed: ${e.message}", e)
                            if (continuation.isActive) {
                                continuation.resume(extractConfig())
                            }
                        }
                }
            }

            if (remoteResult != null && (remoteResult["latest_version_code"] as? Int ?: 0) > currentCode) {
                return@withContext remoteResult
            }

            // Realtime DB fallback in case configuration was stored under /config, /update, or /rates
            val dbUrl = firebaseDatabaseUrl.trim()
            if (dbUrl.isNotBlank() && (dbUrl.startsWith("http://") || dbUrl.startsWith("https://"))) {
                try {
                    val normalizedUrl = if (dbUrl.endsWith("/")) dbUrl else "$dbUrl/"
                    for (endpoint in listOf("config.json", "update.json", "version.json", "rates/INR.json", "rates.json")) {
                        val req = okhttp3.Request.Builder().url("$normalizedUrl$endpoint").build()
                        okHttpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bodyStr = resp.body?.string()
                                if (!bodyStr.isNullOrBlank() && bodyStr != "null") {
                                    val jsonObj = JSONObject(bodyStr)
                                    val dbCode = jsonObj.optInt("latest_version_code", jsonObj.optInt("version_code", jsonObj.optInt("versionCode", 0)))
                                    val dbName = jsonObj.optString("latest_version", jsonObj.optString("version_name", jsonObj.optString("versionName", "")))
                                    val dbMsg = jsonObj.optString("update_message", jsonObj.optString("message", ""))
                                    if (dbCode > currentCode) {
                                        return@withContext mapOf(
                                            "latest_version" to dbName,
                                            "latest_version_code" to dbCode,
                                            "update_message" to dbMsg,
                                            "apk_download_url" to ""
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (ignored: Exception) {}
            }

            return@withContext remoteResult ?: mapOf(
                "latest_version" to currentName,
                "latest_version_code" to currentCode.toInt(),
                "update_message" to "",
                "apk_download_url" to ""
            )
        } catch (e: Exception) {
            e.printStackTrace()
            mapOf(
                "latest_version" to com.goldsilver.livecalc.BuildConfig.VERSION_NAME,
                "latest_version_code" to com.goldsilver.livecalc.BuildConfig.VERSION_CODE,
                "update_message" to "A new version is ready with the latest rates and improvements.",
                "apk_download_url" to ""
            )
        }
    }
}
