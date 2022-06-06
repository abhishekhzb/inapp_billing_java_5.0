package in.papayacoders.instasaver;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button buyNow;
    private IABHandler mIABHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Initiate IAB
        if (mIABHandler == null) {
            mIABHandler = new IABHandler(this);
            mIABHandler.startConnection();
        } else if (!mIABHandler.isServiceConnected()) {
            mIABHandler.startConnection();
        }
        buyNow = findViewById(R.id.buynow);
        //buyNow.setEnabled(false);
        buyNow.setOnClickListener(view -> {
            if (mIABHandler.isServiceConnected()) {
                mIABHandler.makePurchase();
            } else {
                Toast.makeText(MainActivity.this,"Re-establishing service connection...", Toast.LENGTH_SHORT).show();
                mIABHandler.startConnection();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIABHandler != null && mIABHandler.isServiceConnected()) {
            //Query purchases for refreshing users In-App purchases
            mIABHandler.queryPurchases();
        }
    }
}