package in.papayacoders.instasaver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button buyNow;
    public final static String IAP_TAG = "IAP_Tag";

    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        // To be implemented in a later section.
        Log.i(IAP_TAG, "onPurchasesUpdated() method called ");
        if (billingResult.getResponseCode() ==
                BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                completePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() ==
                BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(IAP_TAG, "onPurchasesUpdated: Purchase Canceled");
        } else {
            Log.i(IAP_TAG, "onPurchasesUpdated: Error");
        }
    };

    private BillingClient billingClient;

    private ProductDetails productDetail;
    private Purchase purchase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buyNow = findViewById(R.id.buynow);
        buyNow.setEnabled(false);
        buyNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makePurchase(productDetail);
            }
        });

        startConnection();
    }

    private void startConnection() {
        //Initialise
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(purchasesUpdatedListener)
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                Log.i(IAP_TAG, "Billing Service set up finished. Response code :: "+billingResult.getResponseCode());
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            buyNow.setEnabled(true);
                        }
                    });
                    // The BillingClient is ready. You can query purchases here.
                    QueryProductDetailsParams queryProductDetailsParams =
                            QueryProductDetailsParams.newBuilder()
                                    .setProductList(
                                            ImmutableList.of(QueryProductDetailsParams.Product.newBuilder()
                                                    .setProductId("38")
                                                    .setProductType(BillingClient.ProductType.SUBS)
                                                    .build()))
                                    .build();


                    billingClient.queryProductDetailsAsync(
                            queryProductDetailsParams,
                            (billingResult1, productDetailsList) -> {
                                Log.i(IAP_TAG,"List of Products : "+productDetailsList);
                                // check BillingResult
                                // process returned ProductDetails list
                                productDetail = productDetailsList.get(0);
                            }
                    );

                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.i(IAP_TAG, "Billing Service Disconnected");
            }
        });
    }

    private void makePurchase(ProductDetails productDetails) {
        BillingFlowParams billingFlowParams =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                                ImmutableList.of(
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                                // fetched via queryProductDetailsAsync
                                                .setProductDetails(productDetails)

                                                // to get an offer token, call ProductDetails.getOfferDetails()
                                                // for a list of offers that are available to the user
                                                .setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken())
                                                .build()
                                )
                        )
                        .build();

        billingClient.launchBillingFlow(this, billingFlowParams);
    }

    private void completePurchase(Purchase item) {

        purchase = item;

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            runOnUiThread(() -> {
                Log.i(IAP_TAG, "Purchase completed.");
            });
        }
    }
}