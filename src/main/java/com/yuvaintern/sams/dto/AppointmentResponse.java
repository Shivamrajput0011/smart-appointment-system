package com.yuvaintern.sams.dto;

import com.yuvaintern.sams.entity.Appointment;
import com.yuvaintern.sams.entity.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;

/** Flat view of an appointment (includes the pet and owner name so the UI needs one call only). */
public record AppointmentResponse(
        Long id,
        Long petId,
        String petName,
        String ownerName,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String reason,
        AppointmentStatus status) {

    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPet().getId(),
                a.getPet().getName(),
                a.getPet().getOwnerName(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                a.getReason(),
                a.getStatus());
    }
}
