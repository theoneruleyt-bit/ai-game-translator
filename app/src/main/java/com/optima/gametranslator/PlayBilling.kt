package com.optima.gametranslator

import android.app.Activity
import android.util.Base64
import com.android.billingclient.api.*
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** Never grants access from a button or an unverified purchase. */
class PlayBilling(
    private val activity: Activity,
    private val changed: () -> Unit,
    private val authority: ProService
) : PurchasesUpdatedListener {

    data class Plan(
        val name: String,
        val price: String,
        val details: ProductDetails,
        val token: String?
    )

    val plans = mutableListOf<Plan>()

    var status = ""
        private set

    private val verificationKey by lazy {
        try {
            KeyFactory.getInstance("RSA").generatePublic(
                X509EncodedKeySpec(
                    Base64.decode(
                        activity.getString(R.string.play_license_key),
                        Base64.DEFAULT
                    )
                )
            )
        } catch (_: Exception) {
            null
        }
    }

    val configured: Boolean
        get() = if (authority.enabled) {
            authority.signedIn
        } else {
            verificationKey != null
        }

    private var localPro = false

    val pro: Boolean
        get() = if (authority.enabled) authority.pro else localPro

    private var closed = false
    private var refreshId = 0
    private val catalogReports = linkedMapOf<String, String>()
    private val purchaseReports = linkedMapOf<String, String>()
    private var entitlementDecision = "No completed ownership check yet."

    @Suppress("DEPRECATION")
    fun diagnostics(): String {
        val installed = runCatching {
            val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        }.getOrDefault("unavailable")
        return buildString {
            appendLine("AI Game Translator - purchase support")
            appendLine("Installed version: $installed")
            appendLine("Billing connected: ${client.isReady}")
            appendLine("Server authority enabled: ${authority.enabled}")
            appendLine("Pro displayed: $pro; locally verified Pro: $localPro")
            appendLine("Last decision: $entitlementDecision")
            appendLine("Query generation: $refreshId")
            appendLine(authority.diagnostics())
            appendLine("Expected plans: monthly, yearly-auto")
            appendLine("--- Catalog ---")
            if (catalogReports.isEmpty()) appendLine("No response recorded.")
            catalogReports.forEach { (type, report) -> appendLine("[$type] $report") }
            appendLine("Visible plans: ${plans.joinToString { it.name }}")
            appendLine("--- Ownership ---")
            if (purchaseReports.isEmpty()) appendLine("No response recorded.")
            purchaseReports.forEach { (type, report) -> appendLine("[$type] $report") }
            appendLine("No tokens, full order numbers, license keys or account/payment details included. Purchase time and last 5 order characters only.")
        }
    }

    private fun catalogReport(result: BillingResult, products: List<ProductDetails>): String =
        buildString {
            appendLine("Response=${result.responseCode}; products=${products.size}")
            products.forEach { product ->
                appendLine("Product=${product.productId}")
                product.subscriptionOfferDetails.orEmpty().forEach { offer ->
                    appendLine("Base=${offer.basePlanId}; plainBase=${offer.offerId == null}")
                    offer.pricingPhases.pricingPhaseList.forEach { phase ->
                        appendLine("Period=${phase.billingPeriod}; recurrence=${phase.recurrenceMode}; price=${phase.formattedPrice}")
                    }
                }
            }
        }

    private fun ownershipReport(result: BillingResult, purchases: List<Purchase>): String =
        buildString {
            appendLine("Response=${result.responseCode}; returned=${purchases.size}")
            purchases.forEach { purchase ->
                val products = purchase.products.filter {
                    it == "translator_pro" || it == "translator_pro_lifetime"
                }
                if (products.isNotEmpty()) {
                    appendLine("Products=${products.joinToString()}; state=${purchase.purchaseState}; acknowledged=${purchase.isAcknowledged}; autoRenewing=${purchase.isAutoRenewing}")
                    val purchasedAt = java.time.Instant.ofEpochMilli(purchase.purchaseTime).toString()
                    val orderSuffix = purchase.orderId?.takeLast(5) ?: "unavailable"
                    appendLine("PurchasedAtUTC=$purchasedAt; orderSuffix=***$orderSuffix")
                }
            }
        }


    private val client = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    private fun report(result: BillingResult) {
        status = when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> ""
            BillingClient.BillingResponseCode.USER_CANCELED ->
                activity.getString(R.string.billing_canceled)
            else ->
                activity.getString(R.string.billing_retry)
        }
        changed()
    }

    fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (!closed) {
                    report(result)
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        refresh()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    fun refresh() {
        if (closed || !client.isReady) return

        val request = ++refreshId
        plans.clear()
        catalogReports.clear()
        purchaseReports.clear()
        listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP).forEach {
            catalogReports[it] = "Loading..."
            purchaseReports[it] = "Loading..."
        }

        listOf(
            BillingClient.ProductType.SUBS,
            BillingClient.ProductType.INAPP
        ).forEach { type ->
            val id = if (type == BillingClient.ProductType.SUBS) {
                "translator_pro"
            } else {
                "translator_pro_lifetime"
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(id)
                            .setProductType(type)
                            .build()
                    )
                )
                .build()

            client.queryProductDetailsAsync(params) { result, data ->
                activity.runOnUiThread {
                    if (!closed && request == refreshId) {
                        catalogReports[type] = catalogReport(result, data.productDetailsList)
                    }
                    if (
                        !closed &&
                        request == refreshId &&
                        result.responseCode == BillingClient.BillingResponseCode.OK
                    ) {
                        data.productDetailsList.forEach { details ->
                            if (type == BillingClient.ProductType.SUBS) {
                                details.subscriptionOfferDetails.orEmpty()
                                    .filter {
                                        it.offerId == null &&
                                            it.basePlanId in listOf(
                                                "monthly",
                                                "yearly-auto"
                                            )
                                    }
                                    .forEach { offer ->
                                        val planName = if (
                                            offer.basePlanId == "yearly-auto"
                                        ) {
                                            "yearly"
                                        } else {
                                            "monthly"
                                        }

                                        plans.add(
                                            Plan(
                                                planName,
                                                offer.pricingPhases
                                                    .pricingPhaseList
                                                    .last()
                                                    .formattedPrice,
                                                details,
                                                offer.offerToken
                                            )
                                        )
                                    }
                            } else {
                                details.oneTimePurchaseOfferDetails?.let {
                                    plans.add(
                                        Plan(
                                            "lifetime",
                                            it.formattedPrice,
                                            details,
                                            it.offerToken
                                        )
                                    )
                                }
                            }
                        }
                        changed()
                    } else if (!closed && request == refreshId) {
                        changed()
                    }
                }
            }
        }

        val owned = mutableListOf<Purchase>()
        var responses = 0
        var failed = false

        listOf(
            BillingClient.ProductType.SUBS,
            BillingClient.ProductType.INAPP
        ).forEach { type ->
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(type)
                    .build()
            ) { result, purchases ->
                activity.runOnUiThread {
                    if (!closed && request == refreshId) {
                        responses++
                        purchaseReports[type] = ownershipReport(result, purchases)
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            entitlementDecision = "Ownership query failed; existing in-memory state retained."
                            changed()
                        }

                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            owned.addAll(purchases)
                        } else {
                            failed = true
                        }

                        if (responses == 2 && !failed) {
                            if (authority.enabled) {
                                entitlementDecision = "Using configured server authority."
                                verifyOnServer(owned)
                            } else {
                                entitlementDecision = "Ownership queries completed; no verified entitlement yet."
                                localPro = false
                                owned.forEach { verify(it, request) }
                                changed()
                            }
                        }
                    }
                }
            }
        }
    }

    fun buy(plan: Plan) {
        if (closed || !client.isReady) return

        if (!configured) {
            status = activity.getString(R.string.pro_unavailable)
            changed()
            return
        }

        val item = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(plan.details)

        plan.token?.let { item.setOfferToken(it) }

        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(item.build()))

        if (authority.enabled) {
            flow.setObfuscatedAccountId(authority.accountId)
        }

        report(client.launchBillingFlow(activity, flow.build()))
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (closed) return

        report(result)

        if (purchases.orEmpty().any {
                it.purchaseState == Purchase.PurchaseState.PENDING
            }
        ) {
            status = activity.getString(R.string.billing_pending)
            changed()
        }

        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            if (authority.enabled) {
                refresh()
            } else {
                val request = ++refreshId
                purchases.orEmpty().forEach { verify(it, request) }
            }
        }
    }

    private fun verifyOnServer(purchases: List<Purchase>) {
        val payload = org.json.JSONArray()

        purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }.forEach { purchase ->
            purchase.products.filter {
                it == "translator_pro" || it == "translator_pro_lifetime"
            }.forEach { product ->
                payload.put(
                    org.json.JSONObject()
                        .put("product", product)
                        .put("token", purchase.purchaseToken)
                )
            }
        }

        authority.refresh(payload) {
            if (!closed) changed()
        }
    }

    private fun verify(purchase: Purchase, request: Int) {
        if (
            closed ||
            purchase.purchaseState != Purchase.PurchaseState.PURCHASED ||
            purchase.products.none {
                it == "translator_pro" || it == "translator_pro_lifetime"
            }
        ) {
            return
        }

        val key = activity.getString(R.string.play_license_key)
        if (key.isBlank()) return

        val valid = try {
            if (
                org.json.JSONObject(purchase.originalJson)
                    .optString("packageName") != activity.packageName
            ) {
                return
            }

            val publicKey = verificationKey ?: return

            Signature.getInstance("SHA1withRSA").run {
                initVerify(publicKey)
                update(purchase.originalJson.toByteArray(Charsets.UTF_8))
                verify(Base64.decode(purchase.signature, Base64.DEFAULT))
            }
        } catch (_: Exception) {
            false
        }

        if (!valid) {
            entitlementDecision = "Purchase signature could not be verified."
            return
        }

        if (purchase.isAcknowledged) {
            activity.runOnUiThread {
                if (!closed && request == refreshId) {
                    localPro = true
                    entitlementDecision = "Verified acknowledged purchase: ${purchase.products.filter { it == "translator_pro" || it == "translator_pro_lifetime" }.joinToString()}"
                    changed()
                }
            }
            return
        }

        client.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        ) { result ->
            activity.runOnUiThread {
                if (
                    !closed &&
                    request == refreshId &&
                    result.responseCode == BillingClient.BillingResponseCode.OK
                ) {
                    localPro = true
                    entitlementDecision = "Signature verified and acknowledgement completed: ${purchase.products.filter { it == "translator_pro" || it == "translator_pro_lifetime" }.joinToString()}"
                    changed()
                }
            }
        }
    }

    fun close() {
        closed = true
        client.endConnection()
    }
}
