package com.medbooking.patterns;

import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import com.medbooking.exception.BusinessException;
import com.medbooking.patterns.payment.*;
import com.medbooking.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentPatternsTest {
    @Test
    void cardStrategyCallsGatewayAdapterInterfaceAndPersistsIntent() {
        PaymentGateway adapter = mock(PaymentGateway.class);
        PaymentRepository repository = mock(PaymentRepository.class);
        Payment payment = samplePayment();
        when(adapter.createPaymentIntent(10, 300000L))
                .thenReturn(new PaymentIntentInfo("pi_42", "secret_42", 300000L));

        Map<String, Object> result = new CardPaymentPreparationStrategy(adapter, repository).prepare(payment);

        assertEquals("pi_42", result.get("paymentIntentId"));
        assertEquals("secret_42", result.get("clientSecret"));
        assertEquals(Payment.PaymentMethod.card, payment.getPaymentMethod());
        assertEquals("pi_42", payment.getStripePaymentIntentId());
        verify(repository).save(payment);
    }

    @Test
    void cashStrategyDoesNotCallOnlineProviderOrMarkPaid() {
        Payment payment = samplePayment();
        Map<String, Object> result = new CashPaymentPreparationStrategy().prepare(payment);
        assertEquals("cash", result.get("paymentMethod"));
        assertEquals("pending", result.get("status"));
        assertEquals(Payment.Status.pending, payment.getStatus());
    }

    @Test
    void strategyRegistrySwitchesAlgorithmsAndRejectsUnsupportedOrDuplicate() {
        PaymentPreparationStrategy cash = new CashPaymentPreparationStrategy();
        PaymentPreparationStrategy card = mock(PaymentPreparationStrategy.class);
        when(card.method()).thenReturn(Payment.PaymentMethod.card);
        PaymentPreparationStrategyRegistry registry = new PaymentPreparationStrategyRegistry(List.of(cash, card));
        assertSame(cash, registry.forMethod(Payment.PaymentMethod.cash));
        assertSame(card, registry.forMethod(Payment.PaymentMethod.card));
        assertThrows(BusinessException.class, () -> new PaymentPreparationStrategyRegistry(List.of(cash))
                .forMethod(Payment.PaymentMethod.card));
        assertThrows(IllegalArgumentException.class, () -> new PaymentPreparationStrategyRegistry(List.of(cash, cash)));
    }

    @Test
    void stripeAdapterRefusesMissingCredentialsBeforeExternalCall() {
        StripePaymentAdapter adapter = new StripePaymentAdapter("");
        BusinessException ex = assertThrows(BusinessException.class, () -> adapter.createPaymentIntent(10, 300000));
        assertEquals(500, ex.getStatus());
    }

    private Payment samplePayment() {
        Appointment appointment = new Appointment();
        appointment.setId(10);
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAmount(new BigDecimal("300000"));
        payment.setPaymentMethod(Payment.PaymentMethod.cash);
        payment.setStatus(Payment.Status.pending);
        return payment;
    }
}
