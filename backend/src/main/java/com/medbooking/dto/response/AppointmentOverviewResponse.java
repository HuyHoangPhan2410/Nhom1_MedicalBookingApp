package com.medbooking.dto.response;

/** One response for the booking and its payment details (Facade output). */
public record AppointmentOverviewResponse(AppointmentResponse appointment, PaymentResponse payment) {}
