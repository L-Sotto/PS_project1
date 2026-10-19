package pt.unl.fct.di.syspriv;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import pt.unl.fct.di.syspriv.entities.Appointment;
import pt.unl.fct.di.syspriv.entities.Employee;
import pt.unl.fct.di.syspriv.entities.Patient;
import pt.unl.fct.di.syspriv.entities.Payment;
import pt.unl.fct.di.syspriv.storage.Hibernate;
import org.junit.jupiter.api.Test;

import pt.unl.fct.di.syspriv.xacml.XACMLPDP;

/**
 * Unit test for simple App.
 */
public class AppTest {

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }


    //Policy1: Patients access own data
    @Test 
    void shouldAllowPatientSeeOwnData()throws Exception {
            var db = Hibernate.getInstance();
            var pdp = new XACMLPDP();
            var patients = db.sql("SELECT * FROM patients", Patient.class);
            for(Patient pat : patients) {
                assertTrue(pdp.checkAccess(pat, pat, "something"));
            }
    }
    @Test
    void patientCanAccessOwnAppointment() throws Exception {
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var patient = db.sql("SELECT * FROM patients", Patient.class).get(0);
        var appo = db.sql("SELECT * FROM appointments", Appointment.class).get(0);
        assertTrue(pdp.checkAccess(patient, appo, "Appointment"));
    }

    //Policy2: Employee access own data
    @Test 
    void EmployeeOwnData() throws Exception {
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var employees = db.sql("SELECT * FROM employees", Employee.class);
        for(Employee emp: employees){
            System.out.println("id=[" + emp.getId() + "] role=[" + emp.getRole() + "]");
            assertTrue(pdp.checkAccess(emp, emp, "something"));
        }
    }

    //policy3: Clerk scheduling
    @Test 
    void ClerckDataAcessSchedule() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var clerk= db.sql("SELECT * FROM employees WHERE Role = 'Clerk'", Employee.class);
        var appo= db.sql("SELECT * FROM appointments", Appointment.class);
        assertTrue(!clerk.isEmpty() && !appo.isEmpty()); 
        assertTrue(pdp.checkAccess(clerk.get(0), appo.get(0), "Scheduling"));
    }

    //Policy3: Clerk check-in
    @Test 
    void ClerkDataAccessCheckin() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var clerk= db.sql("SELECT * FROM employees WHERE Role = 'Clerk'", Employee.class);
        var appo= db.sql("SELECT * FROM appointments", Appointment.class);
        assertTrue(!clerk.isEmpty() && !appo.isEmpty()); 
        assertTrue(pdp.checkAccess(clerk.get(0), appo.get(0), "Check-in"));
    }

    //Policy3: Clerks shouldn't be able to access info outside proper context
    @Test 
    void ClerckDataAcessEmergency() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var clerk = db.sql("SELECT * FROM employees WHERE Role = 'Clerk'", Employee.class);
        var appo = db.sql("SELECT * FROM appointments", Appointment.class);
        assertFalse(pdp.checkAccess(clerk, appo, "Emergency"));
    }

    //Policy3: Clerks cant access patient data
    @Test
    void shouldDenyClerkAccessPatientData() throws Exception {
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var clerks = db.sql("SELECT * FROM employees WHERE Role = 'Clerk'", Employee.class);
        var patients = db.sql("SELECT * FROM patients", Patient.class);
        
        assertFalse(pdp.checkAccess(clerks.get(0), patients.get(0), "Scheduling"));
    }

    //Policy4: Accountants access payment data for billing purposes
    @Test 
    void AccounatantAccessPayment() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var account = db.sql("SELECT * FROM employees WHERE Role = 'Accountant'", Employee.class);
        var pay = db.sql("SELECT * FROM payment", Payment.class);
        assertTrue(pdp.checkAccess(account.get(0), pay.get(0), "Billing and Payments"));
    }

    //Policy4: Accountants access employee data for payment purposes 
    @Test 
    void AccounatantAccessEMPPayment() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var account = db.sql("SELECT * FROM employees WHERE Role = 'Accountant'", Employee.class);
        var emp = db.sql("SELECT * FROM employees", Employee.class);
        assertTrue(pdp.checkAccess(account.get(0),emp.get(0), "Billing and Payments"));
    }

    //Policy4: Accountants can't access payment information for wrong context
    @Test 
    void Accountantcantaccess() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var account = db.sql("SELECT * FROM employees WHERE Role = 'Accountant'", Employee.class);
        var pay = db.sql("SELECT * FROM payment", Payment.class);
        var appo = db.sql("SELECT * FROM appointments", Appointment.class);
        assertFalse(pdp.checkAccess(account, pay, "Scheduling"));
        assertFalse(pdp.checkAccess(account, appo, "Billing and Payments"));
    }

    @Test 
    void DoctorAccessPatient() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var patient = db.sql("SELECT * FROM patients ", Patient.class).stream().filter(x -> x.getPrimaryDoctor() != null).findFirst().orElseThrow();
        assertTrue(pdp.checkAccess(patient.getPrimaryDoctor(), patient, "Appointment"));
        
        
    }

    //Policy5
    @Test
    void doctorCannotAccessOtherDoctorsPatient() throws Exception {
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var patients = db.sql("SELECT * FROM patients", Patient.class);

        Patient mine = patients.stream().filter(x -> x.getPrimaryDoctor() != null).findFirst().orElseThrow();
        Patient other = patients.stream()
                .filter(x -> x.getPrimaryDoctor() != null
                        && !x.getPrimaryDoctor().getId().equals(mine.getPrimaryDoctor().getId()))
                .findFirst().orElseThrow();

        assertFalse(pdp.checkAccess(mine.getPrimaryDoctor(), other, "Appointment"));
    }

    @Test
    void nurseCannotAccessPatient() throws Exception {
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var nurse = db.sql("SELECT * FROM employees", Employee.class).stream()
                .filter(e -> "Nurse".equals(e.getRole())).findFirst().orElseThrow();
        var p = db.sql("SELECT * FROM patients", Patient.class).get(0);

        assertFalse(pdp.checkAccess(nurse, p, "Appointment"));
    }

    @Test
    void emergencyAccess() throws Exception{
        var db= Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var doctor = db.sql("SELECT * FROM employees", Employee.class).stream()
                .filter(e -> "Doctor".equals(e.getRole())).findFirst().orElseThrow();
        var p = db.sql("SELECT * FROM patients", Patient.class).get(0);

        assertTrue(pdp.checkAccess(doctor, p, "Emergency"));
    }
    
    //Policy 7
    @Test 
    void itAdminEmergencyAccess() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var admin= db.sql("SELECT * FROM employees WHERE role = 'ITAdmin'", Employee.class).get(0);
        var pay = db.sql("SELECT * FROM payment", Payment.class).get(0);
        var patient = db.sql("SELECT * FROM patients", Patient.class).get(0);
        assertTrue(pdp.checkAccess(admin, patient, "Emergency"));
        assertTrue(pdp.checkAccess(admin, pay, "Emergency"));
    }

    @Test
    void itNoAccessNoEmergency() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var admin= db.sql("SELECT * FROM employees WHERE role = 'ITAdmin'", Employee.class).get(0);
        var pay = db.sql("SELECT * FROM payment", Payment.class).get(0);
        var patient = db.sql("SELECT * FROM patients", Patient.class).get(0);
        assertFalse(pdp.checkAccess(admin, patient, "something"));
        assertFalse(pdp.checkAccess(admin, pay, "something"));
    }

    //Policy 8
    @Test 
    void itAdminAccessHiring() throws Exception{
        var db = Hibernate.getInstance();
        var pdp= new XACMLPDP();
        var admin= db.sql("SELECT * FROM employees WHERE role = 'ITAdmin'", Employee.class);
        var emp = db.sql("SELECT * FROM employees", Employee.class);
        
        assertTrue(pdp.checkAccess(admin.get(0), emp.get(0), "Hiring"));
    }

    @Test 
    void itAdminnoaccessHiring() throws Exception{
        var db = Hibernate.getInstance();
        var pdp= new XACMLPDP();
        var admin= db.sql("SELECT * FROM employees WHERE role = 'ITAdmin'", Employee.class);
        var emp = db.sql("SELECT * FROM employees", Employee.class);
        assertFalse(pdp.checkAccess(admin.get(0), emp.get(0), "Scheduling"));
    }


    @Test
        void sameIdInAnotherTableIsDeniedB() throws Exception {
            var pdp = new XACMLPDP();

            Employee doctor = new Employee();
            doctor.setId(3L);
            doctor.setRole("Doctor");

            Employee otherDoctor = new Employee();
            otherDoctor.setId(99L);
            otherDoctor.setRole("Doctor");

            Patient patient = new Patient();
            patient.setId(3L);
            patient.setPrimaryDoctor(otherDoctor);

            assertFalse(pdp.checkAccess(doctor, patient, "Appointment"));
        }
        @Test
        void printNames() throws Exception {
            var db = Hibernate.getInstance();
            db.sql("SELECT * FROM employees", Employee.class)
            .forEach(e -> System.out.println(e.getId() + " | " + e.getName() + " | " + e.getRole()));
            db.sql("SELECT * FROM patients", Patient.class)
            .forEach(p -> System.out.println(p.getId() + " | " + p.getName()
                + " | doctor=" + (p.getPrimaryDoctor() == null ? "none" : p.getPrimaryDoctor().getId())));
        }
}
