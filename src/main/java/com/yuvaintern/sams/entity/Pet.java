package com.yuvaintern.sams.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** A registered pet and its owner's contact details. The auto-generated {@code id} is the Pet ID. */
@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Pet name is required")
    @Size(max = 100, message = "Pet name must be at most 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Pet type is required (e.g. Dog, Cat)")
    @Size(max = 50, message = "Pet type must be at most 50 characters")
    @Column(nullable = false, length = 50)
    private String type;

    @Size(max = 100, message = "Breed must be at most 100 characters")
    @Column(length = 100)
    private String breed;

    @NotNull(message = "Pet age is required")
    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 60, message = "Age must be 60 or less")
    @Column(nullable = false)
    private Integer age;

    @NotBlank(message = "Owner name is required")
    @Size(max = 100, message = "Owner name must be at most 100 characters")
    @Column(nullable = false, length = 100)
    private String ownerName;

    @NotBlank(message = "Owner phone is required")
    @Pattern(regexp = "^\\+?[0-9][0-9\\- ]{6,14}$",
            message = "Owner phone must be 7-15 digits (spaces, dashes and a leading + are allowed)")
    @Column(nullable = false, length = 20)
    private String ownerPhone;

    @Email(message = "Owner email is not a valid email address")
    @Size(max = 150, message = "Owner email must be at most 150 characters")
    @Column(length = 150)
    private String ownerEmail;

    /** Deleting a pet also deletes its appointments. Hidden from JSON to avoid loops. */
    @JsonIgnore
    @OneToMany(mappedBy = "pet", cascade = CascadeType.REMOVE)
    private List<Appointment> appointments = new ArrayList<>();

    public Pet() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public List<Appointment> getAppointments() { return appointments; }
    public void setAppointments(List<Appointment> appointments) { this.appointments = appointments; }
}
