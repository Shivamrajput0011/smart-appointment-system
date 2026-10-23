package com.yuvaintern.sams.repository;

import com.yuvaintern.sams.entity.Appointment;
import com.yuvaintern.sams.entity.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("select a from Appointment a join fetch a.pet order by a.appointmentDate desc, a.appointmentTime desc")
    List<Appointment> findAllWithPet();

    @EntityGraph(attributePaths = "pet")
    List<Appointment> findByAppointmentDateOrderByAppointmentTimeAsc(LocalDate appointmentDate);

    /** Scheduled appointments that are today (not yet passed) or in the future, soonest first. */
    @Query("select a from Appointment a join fetch a.pet "
            + "where a.status = :status "
            + "and (a.appointmentDate > :today or (a.appointmentDate = :today and a.appointmentTime >= :now)) "
            + "order by a.appointmentDate asc, a.appointmentTime asc")
    List<Appointment> findUpcoming(@Param("status") AppointmentStatus status,
                                   @Param("today") LocalDate today,
                                   @Param("now") LocalTime now);

    long countByStatus(AppointmentStatus status);

    /** Duplicate check for new bookings: same pet, same slot, not cancelled. */
    boolean existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            Long petId, LocalDate date, LocalTime time, AppointmentStatus status);

    /** Duplicate check for updates: same as above but ignores the appointment being edited. */
    boolean existsByPetIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
            Long petId, LocalDate date, LocalTime time, AppointmentStatus status, Long id);
}
