package com.yuvaintern.sams.service;

import com.yuvaintern.sams.dto.AppointmentResponse;
import com.yuvaintern.sams.dto.DashboardResponse;
import com.yuvaintern.sams.entity.AppointmentStatus;
import com.yuvaintern.sams.repository.AppointmentRepository;
import com.yuvaintern.sams.repository.PetRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Collects the summary numbers for the dashboard. */
@Service
public class DashboardService {

    static final int UPCOMING_LIST_LIMIT = 10;

    private final PetRepository petRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public DashboardService(PetRepository petRepository, AppointmentRepository appointmentRepository, Clock clock) {
        this.petRepository = petRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary() {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock).withSecond(0).withNano(0);

        List<AppointmentResponse> upcoming = appointmentRepository
                .findUpcoming(AppointmentStatus.SCHEDULED, today, now)
                .stream().map(AppointmentResponse::from).toList();

        return new DashboardResponse(
                petRepository.count(),
                appointmentRepository.count(),
                upcoming.size(),
                appointmentRepository.countByStatus(AppointmentStatus.COMPLETED),
                appointmentRepository.countByStatus(AppointmentStatus.CANCELLED),
                upcoming.stream().limit(UPCOMING_LIST_LIMIT).toList());
    }
}
