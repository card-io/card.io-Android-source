package io.card.payment;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.utils.CardType;
import com.braintreepayments.cardform.view.CardEditText;
import com.braintreepayments.cardform.view.CardForm;
import com.braintreepayments.cardform.view.ErrorEditText;
import com.braintreepayments.cardform.view.SupportedCardTypesView;

import io.card.payment.i18n.LocalizedStrings;
import io.card.payment.ui.ActivityHelper;

public class ManualActivity extends AppCompatActivity implements OnCardFormSubmitListener,
        CardEditText.OnCardTypeChangedListener {

    private static final com.braintreepayments.cardform.utils.CardType[] SUPPORTED_CARD_TYPES = { com.braintreepayments.cardform.utils.CardType.VISA, com.braintreepayments.cardform.utils.CardType.MASTERCARD, com.braintreepayments.cardform.utils.CardType.DISCOVER,
            com.braintreepayments.cardform.utils.CardType.AMEX, com.braintreepayments.cardform.utils.CardType.DINERS_CLUB, com.braintreepayments.cardform.utils.CardType.JCB, com.braintreepayments.cardform.utils.CardType.MAESTRO, com.braintreepayments.cardform.utils.CardType.UNIONPAY };

    private SupportedCardTypesView mSupportedCardTypesView;

    protected CardForm mCardForm;
    private CreditCard capture;

    private boolean useApplicationTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        useApplicationTheme = getIntent().getBooleanExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, false);
        ActivityHelper.setActivityTheme(this, useApplicationTheme);

        LocalizedStrings.setLanguage(getIntent());
        capture = getIntent().getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
        autoAcceptDone = getIntent().getBooleanExtra("debug_autoAcceptResult", false);

        setContentView(R.layout.card_form);

        boolean requireExpiry = getIntent().getBooleanExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false);
        boolean requireCVV = getIntent().getBooleanExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false);
        boolean requirePostalCode = getIntent().getBooleanExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false);

        mSupportedCardTypesView = (SupportedCardTypesView) findViewById(R.id.supported_card_types);
        mSupportedCardTypesView.setSupportedCardTypes(SUPPORTED_CARD_TYPES);

        mCardForm = (CardForm) findViewById(R.id.card_form);
        mCardForm.cardRequired(true)
                .expirationRequired(requireExpiry)
                .cvvRequired(requireCVV)
                .postalCodeRequired(requirePostalCode)
                .mobileNumberRequired(false)
                .actionLabel(getString(R.string.purchase))
                .setup(this);
        mCardForm.setOnCardFormSubmitListener(this);
        mCardForm.setOnCardTypeChangedListener(this);

        if (capture != null) {
            ((ErrorEditText)mCardForm.findViewById(R.id.bt_card_form_card_number)).setText(capture.cardNumber);
            ((ErrorEditText)mCardForm.findViewById(R.id.bt_card_form_card_number)).focusNextView();
        }

    }

    @Override
    public void onCardTypeChanged(com.braintreepayments.cardform.utils.CardType cardType) {
        if (cardType == CardType.EMPTY) {
            mSupportedCardTypesView.setSupportedCardTypes(SUPPORTED_CARD_TYPES);
        } else {
            mSupportedCardTypesView.setSelected(cardType);
        }
    }

    @Override
    public void onCardFormSubmit() {
        if (mCardForm.isValid()) {
            // TODO: We need cardform to have CardHolderNameEditText
            CreditCard result = new CreditCard(mCardForm.getCardNumber(), Integer.valueOf(mCardForm.getExpirationMonth()),
                    Integer.valueOf(mCardForm.getExpirationYear()), mCardForm.getCvv(), mCardForm.getPostalCode(),
                    "Card Name");
            Intent dataIntent = new Intent();
            dataIntent.putExtra(CardIOActivity.EXTRA_SCAN_RESULT, result);
            if(getIntent().hasExtra(CardIOActivity.EXTRA_CAPTURED_CARD_IMAGE)){
                dataIntent.putExtra(CardIOActivity.EXTRA_CAPTURED_CARD_IMAGE,
                        getIntent().getByteArrayExtra(CardIOActivity.EXTRA_CAPTURED_CARD_IMAGE));
            }
            ManualActivity.this.setResult(CardIOActivity.RESULT_CARD_INFO, dataIntent);
            finish();
            Toast.makeText(this, R.string.valid, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.invalid, Toast.LENGTH_SHORT).show();
        }
    }

    public void onClick(View v) {
        onCardFormSubmit();
    }
}
