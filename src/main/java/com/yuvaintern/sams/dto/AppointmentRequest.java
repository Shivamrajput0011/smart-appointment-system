package com.yuvaintern.sams.dto;

import com.yuvaintern.sams.entity.AppointmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** Data the client sends to create or update an appointment. {@code status} is optional. */
public record AppointmentRequest(
        @NotNull(message = "Pet is required") Long petId,
        @NotNull(message = "Appointment date is required (yyyy-MM-dd)") LocalDate appointmentDate,
        @NotNull(message = "Appointment time is required (HH:mm)") LocalTime appointmentTime,
        @NotBlank(message = "Reason is required")
        @Size(max = 255, message = "Reason must be at most 255 characters") String reason,
        AppointmentStatus status) {
}
