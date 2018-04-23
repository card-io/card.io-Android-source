package io.card.payment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class CardTypeTest {

    @Test
    public void fromCardNumber_returnsVisa() {
        assertEquals(CardType.VISA, CardType.fromCardNumber("4111111111111111"));
        assertEquals(CardType.VISA, CardType.fromString("visa"));
    }

    @Test
    public void fromCardNumber_returnsUnknown() {
        assertEquals(CardType.UNKNOWN, CardType.fromCardNumber("999999"));
    }
}
