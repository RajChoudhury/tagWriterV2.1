package raj.tagwriterv21;

/**
 * Created by UP620432 on 03/02/2015.
 */

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.util.Log;

public class NfcHelper {

    private Activity activity;
    private NfcAdapter nfcAdapter;

    public NfcHelper(Activity activity) {
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
    }

    public boolean isNfcEnabledDevice() {
        boolean hasFeature = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
        boolean isEnabled = (nfcAdapter != null && nfcAdapter.isEnabled());

        return hasFeature && isEnabled;
    }

    public boolean isNfcIntent(Intent intent) {
        return intent.hasExtra(NfcAdapter.EXTRA_TAG);
    }

    public Tag getTagFromIntent(Intent intent) {
        return ((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
    }

    public void enableForegroundDispatch() {
        Intent intent = new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);

        IntentFilter[] intentFilter = new IntentFilter[] {};

        String[][] techList = new String[][] { { android.nfc.tech.Ndef.class.getName() }, { android.nfc.tech.NdefFormatable.class.getName() } };

        if (Build.DEVICE.matches(".*generic.*")) {
            // clean up the tech filter when in emulator since it doesn't work
            // properly.
            techList = null;
        }

        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilter, techList);
    }

    public void disableForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(activity);
    }

    public boolean writeNdefMessage(Intent intent, NdefMessage ndefMessage) {

        Tag tag = getTagFromIntent(intent);

        return writeNdefMessage(tag, ndefMessage);
    }

    private boolean writeNdefMessage(Tag tag, NdefMessage ndefMessage) {

        boolean result = false;

        try {

            if (tag != null) {

                Ndef ndef = Ndef.get(tag);

                if (ndef == null) {
                    result = formatTag(tag, ndefMessage);
                } else {

                    ndef.connect();

                    if (ndef.isWritable()) {
                        ndef.writeNdefMessage(ndefMessage);

                        result = true;
                    }

                    ndef.close();
                }
            }

        } catch (Exception e) {
            Log.e("writeNdefMessage", e.getMessage());
        }

        return result;
    }

    private boolean formatTag(Tag tag, NdefMessage ndefMessage) {

        try {

            NdefFormatable ndefFormat = NdefFormatable.get(tag);

            if (ndefFormat != null) {
                ndefFormat.connect();
                ndefFormat.format(ndefMessage);
                ndefFormat.close();
                return true;
            }

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }

        return false;
    }

    public NdefRecord createUriRecord(String uri) {

        NdefRecord rtdUriRecord = null;

        try {

            byte[] uriField;

            uriField = uri.getBytes("UTF-8");

            byte[] payload = new byte[uriField.length + 1];
            payload[0] = 0x00;

            System.arraycopy(uriField, 0, payload, 1, uriField.length);

            rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], payload);

        } catch (UnsupportedEncodingException e) {
            Log.e("createUriRecord", e.getMessage());
        }

        return rtdUriRecord;
    }

    public NdefRecord createTextRecord(String content) {

        try {

            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");

            final int languageSize = language.length;
            final int textLength = text.length;

            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

        } catch (UnsupportedEncodingException e) {
            Log.e("createTextRecord", e.getMessage());
        }

        return null;
    }

    public NdefMessage createUrlNdefMessage(String uri) {
        NdefRecord record = createUriRecord(uri);
        return new NdefMessage(new NdefRecord[] { record });
    }

}

