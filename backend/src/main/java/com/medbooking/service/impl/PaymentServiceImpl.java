package com.medbooking.service.impl;

import com.medbooking.dto.request.ConfirmPaymentRequest;
import com.medbooking.dto.response.PaymentResponse;
import com.medbooking.entity.Payment;
import com.medbooking.exception.BusinessException;
import com.medbooking.patterns.payment.PaymentPreparationStrategyRegistry;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.repository.PaymentRepository;
import com.medbooking.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentPreparationStrategyRegistry strategyRegistry;

    @Value("${stripe.publishable-key:}")
    private String stripePublishableKey;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              AppointmentRepository appointmentRepository,
                              PaymentPreparationStrategyRegistry strategyRegistry) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(ConfirmPaymentRequest request) {
        // The DB procedure expects exactly the supported enum values.
        Payment.PaymentMethod paymentMethod = parseMethod(request.getPaymentMethod());
        Map<String, Object> resultMap = appointmentRepository.spConfirmPayment(
                request.getAppointmentId(), paymentMethod.name());
        Object isSuccessObj = resultMap.get("p_is_success");
        String message = (String) resultMap.get("p_message");
        boolean isSuccess = Boolean.TRUE.equals(isSuccessObj)
                || (isSuccessObj instanceof Number && ((Number) isSuccessObj).intValue() == 1);
        if (!isSuccess) {
            throw new BusinessException(400, message != null ? message : "Xác nhận thanh toán thất bại");
        }
        Payment payment = paymentRepository.findByAppointmentId(request.getAppointmentId())
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy thông tin hóa đơn thanh toán"));
        return mapToResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByAppointmentId(Integer appointmentId) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BusinessException(404, "Hóa đơn thanh toán không tồn tại"));
        return mapToResponse(payment);
    }

    /** Existing /create-intent behavior is preserved for the frontend (card flow). */
    @Override
    @Transactional
    public Map<String, Object> createPaymentIntent(Integer appointmentId) {
        return preparePayment(appointmentId, "card");
    }

    /** New API allows switching Strategy at runtime without duplicating controllers. */
    @Override
    @Transactional
    public Map<String, Object> preparePayment(Integer appointmentId, String method) {
        Payment.PaymentMethod selected = parseMethod(method);
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy hóa đơn thanh toán"));
        if (payment.getStatus() == Payment.Status.success) {
            throw new BusinessException(400, "Đơn hàng này đã được thanh toán");
        }
        if (payment.getStatus() == Payment.Status.failed) {
            throw new BusinessException(400, "Không thể khởi tạo thanh toán cho hóa đơn đã thất bại");
        }
        return strategyRegistry.forMethod(selected).prepare(payment);
    }

    @Override
    public String getStripePublishableKey() { return stripePublishableKey; }

    private Payment.PaymentMethod parseMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new BusinessException(400, "Chưa chọn phương thức thanh toán");
        }
        try {
            return Payment.PaymentMethod.valueOf(method.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "Phương thức thanh toán không hợp lệ: " + method);
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse res = new PaymentResponse();
        res.setId(payment.getId());
        res.setAppointmentId(payment.getAppointment().getId());
        res.setAmount(payment.getAmount());
        res.setPaymentMethod(payment.getPaymentMethod().name());
        res.setStatus(payment.getStatus().name());
        res.setPaidAt(payment.getPaidAt());
        return res;
    }
}
