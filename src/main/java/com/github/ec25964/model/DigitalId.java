package com.github.ec25964.model;

import java.time.LocalDate;
import java.util.UUID;

public class DigitalId {

    private final String id;
    private String firstName;
    private String lastName;
    private final LocalDate dateOfBirth;
    private String address;
    private String nationality;
    private String email;
    private IdStatus status;

    public static DigitalId create(String firstName, String lastName, LocalDate dateOfBirth,
            String address, String nationality, String email) {
        return new DigitalId(UUID.randomUUID().toString(), firstName, lastName,
                dateOfBirth, address, nationality, email, IdStatus.ACTIVE);
    }

    public DigitalId(String id, String firstName, String lastName, LocalDate dateOfBirth,
            String address, String nationality, String email, IdStatus status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.nationality = nationality;
        this.email = email;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public String getNationality() {
        return nationality;
    }

    public String getEmail() {
        return email;
    }

    public IdStatus getStatus() {
        return status;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setStatus(IdStatus status) {
        this.status = status;
    }
}
