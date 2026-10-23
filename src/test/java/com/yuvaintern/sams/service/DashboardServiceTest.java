package com.yuvaintern.sams.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yuvaintern.sams.dto.DashboardResponse;
import com.yuvaintern.sams.entity.Appointment;
import com.yuvaintern.sams.entity.AppointmentStatus;
import com.yuvaintern.sams.entity.Pet;
import com.yuvaintern.sams.repository.AppointmentRepository;
import com.yuvaintern.sams.repository.PetRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PetRepository petRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private Appointment upcoming(long id) {
        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("Bruno");
        pet.setOwnerName("Asha Verma");
        Appointment a = new Appointment();
        a.setId(id);
        a.setPet(pet);
        a.setAppointmentDate(LocalDate.of(2026, 10, 24));
        a.setAppointmentTime(LocalTime.of(9, 0));
        a.setReason("Checkup");
        a.setStatus(AppointmentStatus.SCHEDULED);
        return a;
    }

    @Test
    @DisplayName("getSummary: combines counts and returns the upcoming list")
    void getSummary_returnsCounts() {
        DashboardService service = new DashboardService(petRepository, appointmentRepository, CLOCK);
        when(petRepository.count()).thenReturn(4L);
        when(appointmentRepository.count()).thenReturn(9L);
        when(appointmentRepository.countByStatus(AppointmentStatus.COMPLETED)).thenReturn(3L);
        when(appointmentRepository.countByStatus(AppointmentStatus.CANCELLED)).thenReturn(2L);
        when(appointmentRepository.findUpcoming(AppointmentStatus.SCHEDULED,
                LocalDate.of(2026, 10, 23), LocalTime.of(10, 0)))
                .thenReturn(List.of(upcoming(1L), upcoming(2L)));

        DashboardResponse summary = service.getSummary();

        assertThat(summary.totalPets()).isEqualTo(4);
        assertThat(summary.totalAppointments()).isEqualTo(9);
        assertThat(summary.upcomingAppointments()).isEqualTo(2);
        assertThat(summary.completedAppointments()).isEqualTo(3);
        assertThat(summary.cancelledAppointments()).isEqualTo(2);
        assertThat(summary.upcomingList()).hasSize(2);
    }

    @Test
    @DisplayName("getSummary: the upcoming count is the full total but the list is capped")
    void getSummary_capsListLength() {
        DashboardService service = new DashboardService(petRepository, appointmentRepository, CLOCK);
        List<Appointment> many = new ArrayList<>();
        for (long i = 1; i <= DashboardService.UPCOMING_LIST_LIMIT + 5; i++) {
            many.add(upcoming(i));
        }
        when(petRepository.count()).thenReturn(1L);
        when(appointmentRepository.count()).thenReturn((long) many.size());
        when(appointmentRepository.countByStatus(AppointmentStatus.COMPLETED)).thenReturn(0L);
        when(appointmentRepository.countByStatus(AppointmentStatus.CANCELLED)).thenReturn(0L);
        when(appointmentRepository.findUpcoming(AppointmentStatus.SCHEDULED,
                LocalDate.of(2026, 10, 23), LocalTime.of(10, 0))).thenReturn(many);

        DashboardResponse summary = service.getSummary();

        assertThat(summary.upcomingAppointments()).isEqualTo(many.size());
        assertThat(summary.upcomingList()).hasSize(DashboardService.UPCOMING_LIST_LIMIT);
    }
}
