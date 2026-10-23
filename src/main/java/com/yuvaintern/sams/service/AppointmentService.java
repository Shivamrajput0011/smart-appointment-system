package com.yuvaintern.sams.service;

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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules for scheduling, updating, completing and cancelling appointments. */
@Service
public class AppointmentService {

    /** How far ahead an appointment may be booked. */
    static final int MAX_DAYS_AHEAD = 365;

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PetRepository petRepository,
                              Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.petRepository = petRepository;
        this.clock = clock;
    }

    @Transactional
    public AppointmentResponse schedule(AppointmentRequest request) {
        Pet pet = findPet(request.petId());
        LocalDate date = request.appointmentDate();
        LocalTime time = normalize(request.appointmentTime());

        validateSlot(date, time);
        if (appointmentRepository.existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                pet.getId(), date, time, AppointmentStatus.CANCELLED)) {
            throw duplicate(pet, date, time);
        }

        Appointment appointment = new Appointment();
        appointment.setPet(pet);
        appointment.setAppointmentDate(date);
        appointment.setAppointmentTime(time);
        appointment.setReason(request.reason().trim());
        appointment.setStatus(AppointmentStatus.SCHEDULED); // new bookings always start as SCHEDULED
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAll() {
        return appointmentRepository.findAllWithPet().stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id) {
        return AppointmentResponse.from(findAppointment(id));
    }

    /** Search by appointment ID, by date, or both. With neither, returns everything. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> search(Long id, LocalDate date) {
        if (id == null && date == null) {
            return getAll();
        }
        List<Appointment> found;
        if (id != null) {
            Optional<Appointment> byId = appointmentRepository.findById(id);
            found = byId.isPresent() ? List.of(byId.get()) : List.of();
            if (date != null) {
                found = found.stream().filter(a -> a.getAppointmentDate().equals(date)).toList();
            }
        } else {
            found = appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(date);
        }
        return found.stream().map(AppointmentResponse::from).toList();
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = findAppointment(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new InvalidAppointmentException(
                    "Only scheduled appointments can be changed. This one is " + appointment.getStatus() + ".");
        }

        Pet pet = findPet(request.petId());
        LocalDate date = request.appointmentDate();
        LocalTime time = normalize(request.appointmentTime());
        AppointmentStatus newStatus = request.status() != null ? request.status() : appointment.getStatus();

        if (newStatus == AppointmentStatus.SCHEDULED) {
            boolean slotChanged = !date.equals(appointment.getAppointmentDate())
                    || !time.equals(appointment.getAppointmentTime());
            if (slotChanged) {
                validateSlot(date, time);
            }
            if (appointmentRepository.existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
                    pet.getId(), date, time, AppointmentStatus.CANCELLED, id)) {
                throw duplicate(pet, date, time);
            }
        }

        appointment.setPet(pet);
        appointment.setAppointmentDate(date);
        appointment.setAppointmentTime(time);
        appointment.setReason(request.reason().trim());
        appointment.setStatus(newStatus);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancel(Long id) {
        return changeStatus(id, AppointmentStatus.CANCELLED, "cancelled");
    }

    @Transactional
    public AppointmentResponse complete(Long id) {
        return changeStatus(id, AppointmentStatus.COMPLETED, "completed");
    }

    /** Permanently removes an appointment record. */
    @Transactional
    public void delete(Long id) {
        appointmentRepository.delete(findAppointment(id));
    }

    // ---------------------------------------------------------------- helpers

    private AppointmentResponse changeStatus(Long id, AppointmentStatus target, String verb) {
        Appointment appointment = findAppointment(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new InvalidAppointmentException(
                    "Only scheduled appointments can be " + verb + ". This one is already "
                            + appointment.getStatus() + ".");
        }
        appointment.setStatus(target);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    private void validateSlot(LocalDate date, LocalTime time) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            throw new InvalidAppointmentException("Appointment date cannot be in the past.");
        }
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new InvalidAppointmentException(
                    "Appointments can be booked at most " + MAX_DAYS_AHEAD + " days ahead.");
        }
        if (date.isEqual(today) && time.isBefore(normalize(LocalTime.now(clock)))) {
            throw new InvalidAppointmentException("Appointment time has already passed today.");
        }
    }

    /** Drops seconds so 10:30 and 10:30:45 count as the same slot. */
    private LocalTime normalize(LocalTime time) {
        return time.withSecond(0).withNano(0);
    }

    private DuplicateAppointmentException duplicate(Pet pet, LocalDate date, LocalTime time) {
        return new DuplicateAppointmentException(
                pet.getName() + " already has an appointment on " + date + " at " + time + ".");
    }

    private Pet findPet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet with ID " + petId + " was not found."));
    }

    private Appointment findAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment with ID " + id + " was not found."));
    }
}
