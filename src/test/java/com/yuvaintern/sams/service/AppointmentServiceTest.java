package com.yuvaintern.sams.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuvaintern.sams.dto.AppointmentRequest;
import com.yuvaintern.sams.dto.AppointmentResponse;
import com.yuvaintern.sams.entity.Appointment;
import com.yuvaintern.sams.entity.AppointmentStatus;
import com.yuvaintern.sams.entity.Pet;
import com.yuvaintern.sams.exception.DuplicateAppointmentException;
import com.yuvaintern.sams.exception.InvalidAppointmentException;
import com.yuvaintern.sams.exception.ResourceNotFoundException;
import com.yuvaintern.sams.repository.AppointmentRepository;
import com.yuvaintern.sams.repository.PetRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** "Today" is frozen at 2026-10-23 10:00 so the date rules are predictable. */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalTime TEN_THIRTY = LocalTime.of(10, 30);

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PetRepository petRepository;

    private AppointmentService service;
    private Pet bruno;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
        service = new AppointmentService(appointmentRepository, petRepository, fixed);

        bruno = new Pet();
        bruno.setId(1L);
        bruno.setName("Bruno");
        bruno.setOwnerName("Asha Verma");
    }

    private AppointmentRequest request(LocalDate date, LocalTime time) {
        return new AppointmentRequest(1L, date, time, "  Vaccination  ", null);
    }

    private Appointment existingAppointment(Long id, AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setId(id);
        a.setPet(bruno);
        a.setAppointmentDate(TOMORROW);
        a.setAppointmentTime(TEN_THIRTY);
        a.setReason("Checkup");
        a.setStatus(status);
        return a;
    }

    private void stubSaveReturnsArgument() {
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------ scheduling

    @Test
    @DisplayName("schedule: valid request is saved as SCHEDULED with a trimmed reason")
    void schedule_success() {
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));
        when(appointmentRepository.existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                1L, TOMORROW, TEN_THIRTY, AppointmentStatus.CANCELLED)).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        AppointmentResponse result = service.schedule(request(TOMORROW, TEN_THIRTY));

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.petName()).isEqualTo("Bruno");
        assertThat(result.reason()).isEqualTo("Vaccination");
        assertThat(result.status()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("schedule: seconds in the time are dropped before saving")
    void schedule_normalizesSeconds() {
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));
        when(appointmentRepository.existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                any(), any(), any(), any())).thenReturn(false);
        stubSaveReturnsArgument();

        AppointmentResponse result = service.schedule(request(TOMORROW, LocalTime.of(10, 30, 45)));

        assertThat(result.appointmentTime()).isEqualTo(TEN_THIRTY);
    }

    @Test
    @DisplayName("schedule: unknown pet throws ResourceNotFoundException")
    void schedule_petMissing() {
        when(petRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.schedule(request(TOMORROW, TEN_THIRTY)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("schedule: a date in the past is rejected")
    void schedule_pastDate() {
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));

        assertThatThrownBy(() -> service.schedule(request(TODAY.minusDays(1), TEN_THIRTY)))
                .isInstanceOf(InvalidAppointmentException.class)
                .hasMessageContaining("past");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("schedule: a time that already passed today is rejected")
    void schedule_pastTimeToday() {
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));

        assertThatThrownBy(() -> service.schedule(request(TODAY, LocalTime.of(9, 0))))
                .isInstanceOf(InvalidAppointmentException.class)
                .hasMessageContaining("passed");
    }

    @Test
    @DisplayName("schedule: a later time today is accepted")
    void schedule_laterToday() {
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));
        when(appointmentRepository.existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                any(), any(), any(), any())).thenReturn(false);
        stubSaveReturnsArgument();

        AppointmentResponse result = service.schedule(request(TODAY, LocalTime.of(15, 0)));

        assertThat(result.appointmentDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("schedule: a date more than a year ahead is rejected")
    void schedule_tooFarAhead() {
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));

        LocalDate tooFar = TODAY.plusDays(AppointmentService.MAX_DAYS_AHEAD + 1);

        assertThatThrownBy(() -> service.schedule(request(tooFar, TEN_THIRTY)))
                .isInstanceOf(InvalidAppointmentException.class)
                .hasMessageContaining("ahead");
    }

    @Test
    @DisplayName("schedule: same pet, same date and time is rejected as a duplicate")
    void schedule_duplicate() {
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));
        when(appointmentRepository.existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                1L, TOMORROW, TEN_THIRTY, AppointmentStatus.CANCELLED)).thenReturn(true);

        assertThatThrownBy(() -> service.schedule(request(TOMORROW, TEN_THIRTY)))
                .isInstanceOf(DuplicateAppointmentException.class)
                .hasMessageContaining("Bruno");
        verify(appointmentRepository, never()).save(any());
    }

    // -------------------------------------------------------------- updating

    @Test
    @DisplayName("update: changing the time of a scheduled appointment works")
    void update_success() {
        Appointment existing = existingAppointment(5L, AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));
        when(appointmentRepository.existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
                1L, TOMORROW, LocalTime.of(14, 0), AppointmentStatus.CANCELLED, 5L)).thenReturn(false);
        stubSaveReturnsArgument();

        AppointmentResponse result = service.update(5L, request(TOMORROW, LocalTime.of(14, 0)));

        assertThat(result.appointmentTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(result.reason()).isEqualTo("Vaccination");
    }

    @Test
    @DisplayName("update: moving the appointment into the past is rejected")
    void update_pastDate() {
        when(appointmentRepository.findById(5L))
                .thenReturn(Optional.of(existingAppointment(5L, AppointmentStatus.SCHEDULED)));
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));

        assertThatThrownBy(() -> service.update(5L, request(TODAY.minusDays(2), TEN_THIRTY)))
                .isInstanceOf(InvalidAppointmentException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: moving onto another booking of the same pet is rejected")
    void update_duplicate() {
        when(appointmentRepository.findById(5L))
                .thenReturn(Optional.of(existingAppointment(5L, AppointmentStatus.SCHEDULED)));
        when(petRepository.findById(1L)).thenReturn(Optional.of(bruno));
        when(appointmentRepository.existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
                eq(1L), any(), any(), eq(AppointmentStatus.CANCELLED), eq(5L))).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request(TOMORROW, LocalTime.of(16, 0))))
                .isInstanceOf(DuplicateAppointmentException.class);
    }

    @Test
    @DisplayName("update: a cancelled appointment cannot be edited")
    void update_cancelledRejected() {
        when(appointmentRepository.findById(5L))
                .thenReturn(Optional.of(existingAppointment(5L, AppointmentStatus.CANCELLED)));

        assertThatThrownBy(() -> service.update(5L, request(TOMORROW, TEN_THIRTY)))
                .isInstanceOf(InvalidAppointmentException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("update: unknown appointment throws ResourceNotFoundException")
    void update_missing() {
        when(appointmentRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(77L, request(TOMORROW, TEN_THIRTY)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------- cancel / complete / delete

    @Test
    @DisplayName("cancel: scheduled appointment becomes CANCELLED")
    void cancel_success() {
        when(appointmentRepository.findById(5L))
                .thenReturn(Optional.of(existingAppointment(5L, AppointmentStatus.SCHEDULED)));
        stubSaveReturnsArgument();

        assertThat(service.cancel(5L).status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel: an already cancelled appointment is rejected")
    void cancel_alreadyCancelled() {
        when(appointmentRepository.findById(5L))
                .thenReturn(Optional.of(existingAppointment(5L, AppointmentStatus.CANCELLED)));

        assertThatThrownBy(() -> service.cancel(5L)).isInstanceOf(InvalidAppointmentException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("complete: scheduled appointment becomes COMPLETED")
    void complete_success() {
        when(appointmentRepository.findById(5L))
                .thenReturn(Optional.of(existingAppointment(5L, AppointmentStatus.SCHEDULED)));
        stubSaveReturnsArgument();

        assertThat(service.complete(5L).status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("delete: unknown appointment throws ResourceNotFoundException")
    void delete_missing() {
        when(appointmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        verify(appointmentRepository, never()).delete(any(Appointment.class));
    }

    // ---------------------------------------------------------------- search

    @Test
    @DisplayName("search: by date uses the date query")
    void search_byDate() {
        when(appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(TOMORROW))
                .thenReturn(List.of(existingAppointment(5L, AppointmentStatus.SCHEDULED)));

        List<AppointmentResponse> result = service.search(null, TOMORROW);

        assertThat(result).extracting(AppointmentResponse::id).containsExactly(5L);
    }

    @Test
    @DisplayName("search: unknown ID returns an empty list instead of an error")
    void search_byIdNotFound() {
        when(appointmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(service.search(404L, null)).isEmpty();
    }

    @Test
    @DisplayName("search: ID and date together must both match")
    void search_byIdAndDateMismatch() {
        when(appointmentRepository.findById(5L))
                .thenReturn(Optional.of(existingAppointment(5L, AppointmentStatus.SCHEDULED)));

        assertThat(service.search(5L, TOMORROW.plusDays(3))).isEmpty();
    }
}
