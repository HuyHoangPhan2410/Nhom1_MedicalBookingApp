package com.medbooking.service;

import com.medbooking.dto.request.ConfirmPaymentRequest;
import com.medbooking.dto.response.PaymentResponse;
import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.repository.PaymentRepository;
import com.medbooking.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho quy trình thanh toán hóa đơn khám bệnh.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Kiểm thử xác nhận thanh toán thành công qua Stored Procedure")
    void testConfirmPayment_Success() {
        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setAppointmentId(10);
        req.setPaymentMethod("cash");

        Map<String, Object> procRes = new HashMap<>();
        procRes.put("p_is_success", true);
        procRes.put("p_message", "Thanh toán thành công");

        when(appointmentRepository.spConfirmPayment(10, "cash")).thenReturn(procRes);

        Payment payment = createDummyPayment(10, Payment.Status.success);
        when(paymentRepository.findByAppointmentId(10)).thenReturn(Optional.of(payment));

        PaymentResponse res = paymentService.confirmPayment(req);

        assertNotNull(res, "Thông tin thanh toán trả về không được null");
        assertEquals(10, res.getAppointmentId(), "Mã lịch hẹn thanh toán phải là 10");
        assertEquals("success", res.getStatus(), "Trạng thái hóa đơn phải là success");
    }

    private Payment createDummyPayment(Integer appointmentId, Payment.Status status) {
        Payment payment = new Payment();
        payment.setId(1);
        
        Appointment app = new Appointment();
        app.setId(appointmentId);
        payment.setAppointment(app);
        payment.setAmount(new BigDecimal("300000"));
        payment.setPaymentMethod(Payment.PaymentMethod.cash);
        payment.setStatus(status);
        return payment;
    }
}
