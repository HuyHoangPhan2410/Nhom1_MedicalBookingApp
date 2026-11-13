package com.medbooking.patterns.payment;

import com.medbooking.exception.BusinessException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Adapter (Structural): translates our PaymentGateway into Stripe's API. */
@Component
public class StripePaymentAdapter implements PaymentGateway {
    private final String secretKey;

    public StripePaymentAdapter(@Value("${stripe.secret-key:}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public PaymentIntentInfo createPaymentIntent(Integer appointmentId, long amountVnd) {
        if (secretKey == null || secretKey.isBlank() || "sk_test_placeholder".equals(secretKey)) {
            throw new BusinessException(500, "Stripe chưa được cấu hình. Vui lòng set STRIPE_SECRET_KEY.");
        }
        if (amountVnd <= 0) {
            throw new BusinessException(400, "Số tiền thanh toán phải lớn hơn 0");
        }
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountVnd)
                    .setCurrency("vnd")
                    .setDescription("Thanh toán khám bệnh - Đơn #" + appointmentId)
                    .build();
            RequestOptions options = RequestOptions.builder().setApiKey(secretKey).build();
            PaymentIntent intent = PaymentIntent.create(params, options);
            return new PaymentIntentInfo(intent.getId(), intent.getClientSecret(), amountVnd);
        } catch (Exception ex) {
            throw new BusinessException(502, "Lỗi kết nối cổng thanh toán Stripe: " + ex.getMessage());
        }
    }
}
