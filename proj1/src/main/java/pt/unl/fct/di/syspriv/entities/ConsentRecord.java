package pt.unl.fct.di.syspriv.entities;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity 
@Table(name="consent_records")
public class ConsentRecord {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "employee_subject_id")
    private Long employeeSubjectId;

    @Column(name = "patient_subject_id")
    private Long patientSubjectId;

    @Column(name="subject_type", nullable = false, length = 20)
    private String subjectType;

    @Column(name="purpose", nullable = false, length = 50)
    private String purpose;

    @Column(name="granted", nullable = false)
    private Boolean granted;

    @Column(name="granted_at")
    private LocalDateTime grantedAt;

    @Column(name="withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name="policy_version", length = 20)
    private String policyVersion;

    @Column(name="source", length= 50)
    private String source;


    public ConsentRecord(){

    }

    public ConsentRecord(Long employeeSubjectId, Long patientSubjectId, String subjectType,                       String purpose, boolean granted, LocalDateTime grantedAt, LocalDateTime withdrawnAt, String policyVersion, String source){
        this.employeeSubjectId = employeeSubjectId;
        this.patientSubjectId = patientSubjectId;
        this.subjectType = subjectType;
        this.purpose = purpose;
        this.granted = granted;
        this.grantedAt = grantedAt;
        this.withdrawnAt = withdrawnAt;
        this.policyVersion = policyVersion;
        this.source= source;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeSubjectId() { return employeeSubjectId; }
    public void setEmployeeSubjectId(Long v) { this.employeeSubjectId = v; }
    public Long getPatientSubjectId() { return patientSubjectId; }
    public void setPatientSubjectId(Long v) { this.patientSubjectId = v; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String v) { this.subjectType = v; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String v) { this.purpose = v; }
    public boolean isGranted() { return granted; }
    public void setGranted(boolean v) { this.granted = v; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime v) { this.grantedAt = v; }
    public LocalDateTime getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(LocalDateTime v) { this.withdrawnAt = v; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String v) { this.policyVersion = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }

    @Override
    public String toString() {
        return "ConsentRecord{id=" + id + ", purpose='" + purpose + "', granted=" + granted
                + ", grantedAt=" + grantedAt + ", withdrawnAt=" + withdrawnAt + '}';
    }
}
