package com.example.userservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity  //сообщает что объекты класса сохраняются в базе
@Table(name = "users")  //указывает нужную таблицу. Без @Table Hibernate попытался бы определить название автоматически
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //значение создаёт PostgreSQL
    private Long id; // Long, а не long, позволяет новому объекту иметь id = null до сохранения

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "surname", nullable = false, length = 100)
    private String surname;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PaymentCard> paymentCards = new ArrayList<>();

    public User() {
    }

    public User(
            String name,
            String surname,
            LocalDate birthDate,
            String email
    ) {
        this.name = name;
        this.surname = surname;
        this.birthDate = birthDate;
        this.email = email;
        this.active = true;
    }

    public void addPaymentCard(PaymentCard paymentCard) {
        paymentCards.add(paymentCard);
        paymentCard.setUser(this);
    }

    public void removePaymentCard(PaymentCard paymentCard) {
        paymentCards.remove(paymentCard);
        paymentCard.setUser(null);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<PaymentCard> getPaymentCards() {
        return paymentCards;
    }
}