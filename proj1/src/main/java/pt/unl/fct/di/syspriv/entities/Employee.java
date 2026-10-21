package pt.unl.fct.di.syspriv.entities;

import jakarta.persistence.*;
//import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(nullable = false, length = 50)
    private String department;

    @Column(nullable = false)
    private Boolean isAdmin;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appointment> appointments;
    
    @OneToMany(mappedBy = "primaryDoctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Patient> patients;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;

    @Column(name = "erasure_requested")
    private Boolean erasureRequested = false;

    @Column(name = "erasure_completed_at")
    private LocalDateTime erasureCompletedAt;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_subject_id", insertable = false, updatable = false)
    private List<ConsentRecord> consentRecords = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_subject_id", insertable = false, updatable = false)
    private List<GdprRequest> gdprRequests = new ArrayList<>();

    public Employee() {}

    public Employee(String name, String role, String department, Boolean isAdmin, LocalDateTime createdAt, LocalDateTime updateAt, LocalDateTime retentionUntil, Boolean erasureRequested, LocalDateTime erasureCompletedAt) {
        this.name = name;
        this.role = role;
        this.department = department;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
        this.updatedAt = updateAt;
        this.retentionUntil = retentionUntil;
        this.erasureRequested = erasureRequested;
        this.erasureCompletedAt = erasureCompletedAt;
    
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getStringId() { return Long.toString(id); }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
    public List<Appointment> getAppointments() { return appointments; }
    public void setAppointments(List<Appointment> appointments) { this.appointments = appointments; }
    public List<Patient> getPatients() { return patients; }
    public void setPatients(List<Patient> patients) { this.patients = patients; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getRetentionUntil() { return retentionUntil; }
    public void setRetentionUntil(LocalDateTime retentionUntil) { this.retentionUntil = retentionUntil; }
    public Boolean getErasureRequested() { return erasureRequested; }
    public void setErasureRequested(Boolean erasureRequested) { this.erasureRequested = erasureRequested; }
    public LocalDateTime getErasureCompletedAt() { return erasureCompletedAt; }
    public void setErasureCompletedAt(LocalDateTime erasureCompletedAt) { this.erasureCompletedAt = erasureCompletedAt; }
    public List<ConsentRecord> getConsentRecords() { return consentRecords; }
    public void setConsentRecords(List<ConsentRecord> consentRecords) { this.consentRecords = consentRecords; }
    public List<GdprRequest> getGdprRequests() { return gdprRequests; }
    public void setGdprRequests(List<GdprRequest> gdprRequest) { this.gdprRequests = gdprRequest; }

    // For debugging and logging purposes.
    @Override
    public String toString() {
        return "employee{" +
                "id=" + Long.toString(id) +
                ", name='" + name + '\'' +
                ", role=" + role +
                ", department='" + department + '\'' +
                ", admin='" + String.valueOf(isAdmin) + '\'' +
                '}';
    }
}
