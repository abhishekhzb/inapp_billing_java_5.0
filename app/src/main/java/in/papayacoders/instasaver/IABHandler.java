package in.papayacoders.instasaver;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.util.List;

/*
* In-App Billing library and payment handler*/
public class IABHandler {
    public final static String IAP_DEMO_TAG = "IAPDemo";

    //Purchase update listener
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        Log.i(IAP_DEMO_TAG, "onPurchasesUpdated() method called ");
        if (billingResult.getResponseCode() ==
                BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                completePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() ==
                BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(IAP_DEMO_TAG, "onPurchasesUpdated: Purchase Canceled");
        } else {
            Log.i(IAP_DEMO_TAG, "onPurchasesUpdated: Error : "+billingResult.getResponseCode());
        }
    };

    //Purchase response listener implementation
    private final PurchasesResponseListener mPurchasesResponseListener = new PurchasesResponseListener() {
        @Override
        public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
            // Check BillingResult
            // Process returned purchase list, e.g. display the plans user owns
            Log.i(IAP_DEMO_TAG, "Purchases response received. BillingResult == "+billingResult.getResponseCode());
            Log.i(IAP_DEMO_TAG, "Total Purchases == "+list.size());
            for (Purchase purchase : list) {
                //Acknowledge
                if (!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                    //Make Piano Receipt API call and submit Receipt details
                }
            }

        }
    };

    //Purchase acknowledge listener
    private final AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
            Log.i(IAP_DEMO_TAG, "Acknowledged Purchase :: "+billingResult.getResponseCode());
        }
    };

    //Billing client instance
    private BillingClient billingClient;
    //Product detail consist of price, plan name, currency etc.
    public ProductDetails productDetail;
    //Purchase information for a successful purchase
    private Purchase purchase;
    //Activity instance
    private final AppCompatActivity mActivity;
    //This is to understand if Service connection has been established or not
    private boolean isServiceConnected;
    private int mResponseCode;


    public IABHandler(AppCompatActivity activity){
        mActivity = activity;
    }

    /*
    * It establish the IAB with Google Play Store
    * Once connection is established, query for a particular Product(INAPP or SUBS type)
    * @productId
    * */
    public void startConnection() {
        //Initialise
        billingClient = BillingClient.newBuilder(mActivity)
                .enablePendingPurchases()
                .setListener(purchasesUpdatedListener)
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                isServiceConnected = true;
                mResponseCode = billingResult.getResponseCode();
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mActivity,"Billing Service set up finished", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.i(IAP_DEMO_TAG, "Billing Service set up finished. Response code :: "+billingResult.getResponseCode());
                queryProductDetails("38");
            }
            @Override
            public void onBillingServiceDisconnected() {
                isServiceConnected = false;
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.i(IAP_DEMO_TAG, "Billing Service Disconnected");
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mActivity,"Billing Service Disconnected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void queryProductDetails(String productId) {
        if (mResponseCode ==  BillingClient.BillingResponseCode.OK) {
            // The BillingClient is ready. You can query Product details here.
            // Construct product details params
            QueryProductDetailsParams queryProductDetailsParams =
                    QueryProductDetailsParams.newBuilder()
                            .setProductList(
                                    ImmutableList.of(QueryProductDetailsParams.Product.newBuilder()
                                            .setProductId(productId)
                                            .setProductType(BillingClient.ProductType.SUBS)
                                            .build()))
                            .build();

            //Make a query with the product details params
            billingClient.queryProductDetailsAsync(
                    queryProductDetailsParams,
                    (billingResult1, productDetailsList) -> {
                        Log.i(IAP_DEMO_TAG,"List of Products : "+productDetailsList.size());
                        // check BillingResult
                        // process returned ProductDetails list
                        productDetail = productDetailsList.get(0);
                        //Launch Purchase
                        //makePurchase();
                    }
            );
        }
    }

    /*
    * Make purchase using IAB by launchBillingFlow
    * */
    public void makePurchase() {
        //Construct BillingFlowParams
        BillingFlowParams billingFlowParams =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                                ImmutableList.of(
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                                // fetched via queryProductDetailsAsync
                                                .setProductDetails(productDetail)

                                                // to get an offer token, call ProductDetails.getOfferDetails()
                                                // for a list of offers that are available to the user
                                                .setOfferToken(productDetail.getSubscriptionOfferDetails().get(0).getOfferToken())
                                                .build()
                                )
                        )
                        .build();

        billingClient.launchBillingFlow(mActivity, billingFlowParams);
    }

    private void completePurchase(Purchase item) {
        purchase = item;
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            mActivity.runOnUiThread(() -> {
                Log.i(IAP_DEMO_TAG, "Purchase completed.");
                //Acknowledge
                if (!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                }

            });
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            //Handle pending state
            Log.i(IAP_DEMO_TAG, "Purchase pending.");
            //Query Purchases
            queryPurchases();
        }
    }

    /*
    * Query purchases to return active subscriptions
    * */
    public void queryPurchases() {
        Log.i(IAP_DEMO_TAG, "Querying Purchases of the user...");
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(), mPurchasesResponseListener);
    }


    public boolean isServiceConnected() {
        return isServiceConnected;
    }
}
