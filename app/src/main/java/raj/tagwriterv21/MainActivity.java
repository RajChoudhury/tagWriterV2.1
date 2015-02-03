package raj.tagwriterv21;

import android.app.ProgressDialog;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;



import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    NfcHelper nfcHelper;
    TextView txtWebLink;
    Boolean isWritingTag = false;
    ProgressDialog writingProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcHelper = new NfcHelper(this);
        if (!nfcHelper.isNfcEnabledDevice()) {
            setContentView(R.layout.activity_main_no_nfc);
            return;
        }
        setContentView(R.layout.activity_main);
        txtWebLink = (TextView) findViewById(R.id.txtWebLink);
        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcHelper.isNfcEnabledDevice()) {
            nfcHelper.enableForegroundDispatch();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcHelper.isNfcEnabledDevice()) {
            nfcHelper.disableForegroundDispatch();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (nfcHelper.isNfcIntent(intent)) {
            if (isWritingTag) {
                String url = txtWebLink.getText().toString();
                NdefMessage ndefMsg = nfcHelper.createUrlNdefMessage(url);
                if (nfcHelper.writeNdefMessage(intent, ndefMsg)) {
                    Toast.makeText(this, R.string.toast_write_successful, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.toast_write_fail, Toast.LENGTH_LONG).show();
                }
                isWritingTag = false;
                writingProgressDialog.dismiss();
            } else {
                // Check Chapter 5 to know how to get tag content
            }
        }
    }

    public void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    txtWebLink.setText(sharedText);
                }
            }
        }
    }

    public void onBtWriteTagClick(View view) {
        String url = txtWebLink.getText().toString();
        if (url.isEmpty() || !URLUtil.isValidUrl(url)) {
            Toast.makeText(this, R.string.toast_invalid_url, Toast.LENGTH_LONG).show();
            return;
        }
        showWaitDialog();
    }

    private void showWaitDialog() {
        writingProgressDialog = ProgressDialog.show(this, "", getString(R.string.dialog_tap_on_tag), false, true, new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                isWritingTag = false;
            }
        });
        isWritingTag = true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
