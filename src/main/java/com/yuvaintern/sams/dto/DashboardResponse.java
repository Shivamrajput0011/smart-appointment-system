package com.yuvaintern.sams.dto;

import java.util.List;

/** Numbers and the short upcoming list shown on the dashboard. */
public record DashboardResponse(
        long totalPets,
        long totalAppointments,
        long upcomingAppointments,
        long completedAppointments,
        long cancelledAppointments,
        List<AppointmentResponse> upcomingList) {
}
