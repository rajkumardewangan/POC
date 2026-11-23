package com.example.salon.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DayOfWeek;

@Entity
@Table(name = "barbers")
public class Barber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Specialty is required")
    @Column(nullable = false)
    private String specialty;

    private String phone;

    private String email;

    @Column(length = 1000)
    private String address;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private LocalDate joiningDate;

    private String employmentType;

    @Column(name = "base_salary", nullable = false)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(name = "housing_allowance")
    private BigDecimal housingAllowance = BigDecimal.ZERO;

    @Column(name = "transport_allowance")
    private BigDecimal transportAllowance = BigDecimal.ZERO;

    @Column(name = "other_allowance")
    private BigDecimal otherAllowance = BigDecimal.ZERO;

    @Column(name = "fixed_salary_deduction")
    private BigDecimal fixedSalaryDeduction = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "weekly_off_day", nullable = false)
    private DayOfWeek weeklyOffDay = DayOfWeek.SUNDAY;

    @Transient
    private Long appointmentCount = 0L;

    public Barber() {
    }

    public Barber(String name, String specialty, String phone) {
        this.name = name;
        this.specialty = specialty;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary == null ? BigDecimal.ZERO : baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = nonNegativeAmount(baseSalary);
    }

    public BigDecimal getHousingAllowance() {
        return housingAllowance == null ? BigDecimal.ZERO : housingAllowance;
    }

    public void setHousingAllowance(BigDecimal housingAllowance) {
        this.housingAllowance = nonNegativeAmount(housingAllowance);
    }

    public BigDecimal getTransportAllowance() {
        return transportAllowance == null ? BigDecimal.ZERO : transportAllowance;
    }

    public void setTransportAllowance(BigDecimal transportAllowance) {
        this.transportAllowance = nonNegativeAmount(transportAllowance);
    }

    public BigDecimal getOtherAllowance() {
        return otherAllowance == null ? BigDecimal.ZERO : otherAllowance;
    }

    public void setOtherAllowance(BigDecimal otherAllowance) {
        this.otherAllowance = nonNegativeAmount(otherAllowance);
    }

    public BigDecimal getFixedSalaryDeduction() {
        return fixedSalaryDeduction == null ? BigDecimal.ZERO : fixedSalaryDeduction;
    }

    public void setFixedSalaryDeduction(BigDecimal fixedSalaryDeduction) {
        this.fixedSalaryDeduction = nonNegativeAmount(fixedSalaryDeduction);
    }

    public BigDecimal getGrossMonthlySalary() {
        return getBaseSalary().add(getHousingAllowance()).add(getTransportAllowance()).add(getOtherAllowance());
    }

    private BigDecimal nonNegativeAmount(BigDecimal amount) {
        return amount == null || amount.signum() < 0 ? BigDecimal.ZERO : amount;
    }

    public DayOfWeek getWeeklyOffDay() {
        return weeklyOffDay == null ? DayOfWeek.SUNDAY : weeklyOffDay;
    }

    public void setWeeklyOffDay(DayOfWeek weeklyOffDay) {
        this.weeklyOffDay = weeklyOffDay == null ? DayOfWeek.SUNDAY : weeklyOffDay;
    }

    public Long getAppointmentCount() {
        return appointmentCount;
    }

    public void setAppointmentCount(Long appointmentCount) {
        this.appointmentCount = appointmentCount;
    }
}
