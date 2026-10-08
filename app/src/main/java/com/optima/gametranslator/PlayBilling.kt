package com.optima.gametranslator

import android.app.Activity
import com.android.billingclient.api.*
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import android.util.Base64

/** Never grants access from a button or an unverified purchase. Configure the Play license key. */
class PlayBilling(private val activity: Activity, private val changed: () -> Unit, private val authority: ProService) : PurchasesUpdatedListener {
    data class Plan(val name: String,val price: String,val details: ProductDetails,val token: String?)
    val plans=mutableListOf<Plan>()
    private var localPro=false
    val pro get()=if(authority.enabled)authority.pro else localPro
    private var closed=false
    private var refreshId=0
    private val client=BillingClient.newBuilder(activity).setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection().build()
    fun connect() {
        client.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) { if(result.responseCode==BillingClient.BillingResponseCode.OK&&!closed)refresh() }
            override fun onBillingServiceDisconnected() { }
        })
    }
    fun refresh() {
        if(closed||!client.isReady)return
        val request=++refreshId
        plans.clear()
        listOf(BillingClient.ProductType.SUBS,BillingClient.ProductType.INAPP).forEach { type ->
            val id=if(type==BillingClient.ProductType.SUBS)"translator_pro" else "translator_pro_lifetime"
            val params=QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build())).build()
            client.queryProductDetailsAsync(params) { result, data ->
                activity.runOnUiThread { if(!closed&&request==refreshId&&result.responseCode==BillingClient.BillingResponseCode.OK) {
                    data.productDetailsList.forEach { details ->
                        if(type==BillingClient.ProductType.SUBS)details.subscriptionOfferDetails.orEmpty().filter { it.offerId==null&&it.basePlanId in listOf("monthly","yearly") }.forEach { offer ->
                            plans.add(Plan(offer.basePlanId,offer.pricingPhases.pricingPhaseList.last().formattedPrice,details,offer.offerToken))
                        } else details.oneTimePurchaseOfferDetails?.let { plans.add(Plan("lifetime",it.formattedPrice,details,null)) }
                    }
                    changed()
                } }
            }
        }
        val owned=mutableListOf<Purchase>();var responses=0;var failed=false
        listOf(BillingClient.ProductType.SUBS,BillingClient.ProductType.INAPP).forEach { type ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result,purchases ->
                activity.runOnUiThread {
                    if(!closed&&request==refreshId) {
                        responses++;if(result.responseCode==BillingClient.BillingResponseCode.OK)owned.addAll(purchases) else failed=true
                        if(responses==2&&!failed) {
                            if(authority.enabled)verifyOnServer(owned)
                            else { localPro=false;owned.forEach { verify(it,request) };changed() }
                        }
                    }
                }
            }
        }
    }
    fun buy(plan: Plan) {
        if(closed||!client.isReady||authority.enabled&&!authority.signedIn)return
        val item=BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(plan.details)
        plan.token?.let { item.setOfferToken(it) }
        val flow=BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item.build()))
        if(authority.enabled)flow.setObfuscatedAccountId(authority.accountId)
        client.launchBillingFlow(activity,flow.build())
    }
    override fun onPurchasesUpdated(result: BillingResult,purchases: MutableList<Purchase>?) {
        if(result.responseCode==BillingClient.BillingResponseCode.OK) { if(authority.enabled)refresh() else { val request=++refreshId;purchases.orEmpty().forEach { verify(it,request) } } }
    }
    private fun verifyOnServer(purchases: List<Purchase>) {
        val payload=org.json.JSONArray()
        purchases.filter { it.purchaseState==Purchase.PurchaseState.PURCHASED }.forEach { purchase ->
            purchase.products.filter { it=="translator_pro"||it=="translator_pro_lifetime" }.forEach { product ->
                payload.put(org.json.JSONObject().put("product",product).put("token",purchase.purchaseToken))
            }
        }
        authority.refresh(payload) { if(!closed)changed() }
    }
    private fun verify(purchase: Purchase,request: Int) {
        if(closed||purchase.purchaseState!=Purchase.PurchaseState.PURCHASED||purchase.products.none { it=="translator_pro"||it=="translator_pro_lifetime" })return
        val key=activity.getString(R.string.play_license_key)
        if(key.isBlank())return
        val valid=try {
            if(org.json.JSONObject(purchase.originalJson).optString("packageName")!=activity.packageName)return
            val publicKey=KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.decode(key,Base64.DEFAULT)))
            Signature.getInstance("SHA1withRSA").run { initVerify(publicKey);update(purchase.originalJson.toByteArray(Charsets.UTF_8));verify(Base64.decode(purchase.signature,Base64.DEFAULT)) }
        } catch (_: Exception) { false }
        if(!valid)return
        if(purchase.isAcknowledged) { activity.runOnUiThread { if(!closed&&request==refreshId) { localPro=true;changed() } };return }
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result ->
            activity.runOnUiThread { if(!closed&&request==refreshId&&result.responseCode==BillingClient.BillingResponseCode.OK) { localPro=true;changed() } }
        }
    }
    fun close() { closed=true;client.endConnection() }
}
