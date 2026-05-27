package com.pdfapp.reader.point

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.pdfapp.reader.prefers.AppPrefs
import com.vtsoft.pdfapp.reader.databinding.ActivityPointListBinding

class PointListActivity : AppCompatActivity() {

    private var billingClient: BillingClient? = null
    private var clientPackageList: MutableList<ClientPackage> = mutableListOf()
    private var getProductIdList: MutableList<String> = mutableListOf()
    private val mProductDetails: MutableList<ProductDetails> = ArrayList()
    private var pointAdapter: PointAdapter? = null
    private var clickPosition = -1
    private lateinit var binding: ActivityPointListBinding

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult: BillingResult, purchases: List<Purchase?>? ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases?.forEach { purchase ->
                    purchase?.let {
                        Log.d("GGGG", "Purchase OK")
                        handlePurchase(it)
                    }
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.d("GGGG", "User canceled purchase flow")
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                Log.d("GGGG", "ITEM_ALREADY_OWNED")
            } else {
                Log.d("GGGG", "Purchase failed: ${billingResult.responseCode}")
            }
        }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPointListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPayment()

        binding.back.setOnClickListener {
            finish()
        }

        binding.youPointTxt.text = "Your Point: ${AppPrefs.get().pointUser}"
    }

    private fun setupPayment() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                billingClient?.startConnection(this)
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    requestListPointPackage()
                }
            }
        })
    }

    private fun requestListPointPackage() {
        val listServerPackage = listOf(
            ServerPackage(productId = "driftly_point_01"),
            ServerPackage(productId = "driftly_point_02"),
            ServerPackage(productId = "driftly_point_03"),
            ServerPackage(productId = "driftly_point_04"),
            ServerPackage(productId = "driftly_point_05")
        )
        addPointClientPackage(listServerPackage)

        val productsList = getProductIdList.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productsList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val list =
                    mergePointPackageWithProductDetails(clientPackageList, productDetailsList)
                clientPackageList.clear()
                clientPackageList.addAll(list)

                Log.d("GGGG", "list point size: ${clientPackageList.size}")

                runOnUiThread { fillPointPackageToList(clientPackageList) }
            } else {
                Log.e("GGGG", "queryProductDetailsAsync failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun addPointClientPackage(packages: List<ServerPackage>?) {
        packages?.forEach {
            val clientPackage = ClientPackage(
                it.packageId,
                it.point ?: 0,
                it.productionId
            )
            clientPackage.serverPrice = "" // sẽ được set trong merge từ Play Store
            clientPackageList.add(clientPackage)
            getProductIdList.add(it.productionId ?: "")
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fillPointPackageToList(clientPackageList: MutableList<ClientPackage>) {
        pointAdapter = PointAdapter(clientPackageList) {
            clickPosition = it
            startPayment()
        }
        binding.recyclerView.adapter = pointAdapter
        pointAdapter?.notifyDataSetChanged()
    }

    private fun startPayment() {
        try {
            val productDetailsParams = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(mProductDetails[clickPosition])
                    .build()
            )
            val billingFlowParams: BillingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParams)
                .build()
            billingClient?.launchBillingFlow(this, billingFlowParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handlePurchase(purchase: Purchase) {
        val consumeParams =
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        val listener =
            ConsumeResponseListener { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    runOnUiThread {
                        Toast.makeText(this, "Buy point success", Toast.LENGTH_SHORT).show()
                        AppPrefs.get().isPurchase = true
                        AppPrefs.get().isPurchaseNumberAction += 1
                        val pointToAdd = when (clickPosition) {
                            0 -> {
                                50
                            }

                            1 -> {
                                99
                            }

                            2 -> {
                                199
                            }

                            3 -> {
                                499
                            }

                            4 -> {
                                999
                            }

                            else -> {
                                0
                            }
                        }
                        AppPrefs.get().pointUser += pointToAdd
                        binding.youPointTxt.text = "Your Point: ${AppPrefs.get().pointUser}"

                    }
                }
            }
        billingClient?.consumeAsync(consumeParams, listener)
    }

    private fun mergePointPackageWithProductDetails(
        pointPackages: MutableList<ClientPackage>?,
        productDetails: MutableList<ProductDetails>
    ): MutableList<ClientPackage> {
        val mergedList = mutableListOf<ClientPackage>()
        productDetails.forEach { productDetail ->
            pointPackages?.find {
                it.productId == productDetail.productId
            }?.let { pointPackage ->
                pointPackage.productDetails = productDetail

                val offerDetails = productDetail.oneTimePurchaseOfferDetails
                // giữ giá trị dạng số nếu cần tính toán
                pointPackage.amount =
                    offerDetails?.priceAmountMicros?.toDouble()?.div(1_000_000) ?: 0.0
                pointPackage.currency = offerDetails?.priceCurrencyCode.orEmpty()
                // giá hiển thị formatted
                pointPackage.serverPrice = offerDetails?.formattedPrice.orEmpty()
                pointPackage.description = productDetail.name

                mergedList.add(pointPackage)

                mProductDetails.add(productDetail)
            }
        }
        return mergedList
    }
}
