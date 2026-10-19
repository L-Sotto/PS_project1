package pt.unl.fct.di.syspriv;

import pt.unl.fct.di.syspriv.entities.*;
import pt.unl.fct.di.syspriv.storage.Hibernate;
import pt.unl.fct.di.syspriv.xacml.XACMLPDP;
import pt.unl.fct.di.syspriv.util.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;

public class DataSafe {

    private static final Logger logger = LoggerFactory.getLogger(DataSafe.class);
    private final Hibernate hibernate = Hibernate.getInstance();
    private final XACMLPDP pdp;
    private Object loggedInPerson;

    public DataSafe() throws Exception {
        this.pdp = new XACMLPDP();
        logger.info("DataSafe instance initialized.");
    }

    public static void main(String[] args) {
        try {
            DataSafe app = new DataSafe();
            app.run();

        } catch (Exception e) {
            logger.error("Failed to initialize application: " + e.getMessage());
            e.printStackTrace();
        }
    }



    public void run() {
        Scanner scanner = new Scanner(System.in);
        
        if (!login(scanner)) {
            logger.error("Login failed. Exiting...");
            return;
        }

        displayWelcome();

        boolean running = true;
        while (running) {
            String context = promptContext(scanner);
            if (context == null) {
                System.out.println("Invalid context. Please try again.");
                continue;
            }

            int tableChoice = promptTableSelection(scanner);
            if (tableChoice == 5) {
                running = false;
                System.out.println("Exiting...");
                continue;
            }

            System.out.print("Filter by person name (leave blank to look up by ID): ");
            String person = scanner.nextLine().trim();

            if (!person.isEmpty()) {
                handleQuery(context, tableChoice, person);
                continue;
            }

            Long id = promptId(scanner);
            if (id == null) {
                System.out.println("Invalid ID. Please try again.");
                continue;
            }

            handleDataAccess(context, tableChoice, id);
            // System.out.print("Filter by person name (leave blank to look up by ID): ");
            // String person = scanner.nextLine().trim();

            // if (!person.isEmpty()) {
            //     handleQuery(context, tableChoice, person);
            //     continue;
            // }

            // Long id2 = promptId(scanner);
            // if (id == null) {
            //     System.out.println("Invalid ID. Please try again.");
            //     continue;
            // }
            // handleDataAccess(context, tableChoice, id2);
        }
        

        scanner.close();
    }

    private boolean login(Scanner scanner) {
        System.out.println("Are you an employee or patient?");
        System.out.println("1. Employee");
        System.out.println("2. Patient");
        System.out.print("Select an option: ");

        String choice = scanner.nextLine();

        System.out.print("Enter your name to login: ");
        String loginName = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                List<Employee> employees = hibernate.jpql("FROM Employee", Employee.class);
                for (Employee e : employees) {
                    if (e.getName().equalsIgnoreCase(loginName)) {
                        loggedInPerson = e;
                        return true;
                    }
                }
                System.out.println("Employee not found.");
                return false;

            case "2":
                List<Patient> patients = hibernate.jpql("FROM Patient", Patient.class);
                for (Patient p : patients) {
                    if (p.getName().equalsIgnoreCase(loginName)) {
                        loggedInPerson = p;
                        return true;
                    }
                }
                System.out.println("Patient not found.");
                return false;

            default:
                System.out.println("Invalid option.");
                return false;
        }
    }

    private void displayWelcome() {
        if (loggedInPerson instanceof Employee emp) {
            System.out.println("Welcome, " + emp.getName() + " (" + emp.getRole() + ")!");
        } else if (loggedInPerson instanceof Patient pat) {
            System.out.println("Welcome, " + pat.getName() + "!");
        } else {
            System.out.println("Welcome!");
        }
    }

    private String promptContext(Scanner scanner) {
        System.out.println("\n=== Please select a context ===");
        System.out.println("1. Billing and Payments");
        System.out.println("2. Scheduling");
        System.out.println("3. Check-in");
        System.out.println("4. Hiring");
        System.out.println("5. Appointment");
        System.out.println("6. Emergency");
        System.out.print("Select an option: ");

        String choice = scanner.nextLine();
        return switch (choice) {
            case "1" -> "Billing and Payments";
            case "2" -> "Scheduling";
            case "3" -> "Check-in";
            case "4" -> "Hiring";
            case "5" -> "Appointment";
            case "6" -> "Emergency";
            default -> null;
        };
    }

    private int promptTableSelection(Scanner scanner) {
        System.out.println("\n=== Available tables to request data from ===");
        System.out.println("1. Employees");
        System.out.println("2. Patients");
        System.out.println("3. Appointments");
        System.out.println("4. Payments");
        System.out.println("5. Exit");
        System.out.print("Select an option: ");

        String input = scanner.nextLine();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Long promptId(Scanner scanner) {
        System.out.print("Please enter the ID of the row you'd like to access: ");
        String input = scanner.nextLine();
        try {
            return Long.valueOf(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void handleDataAccess(String context, int tableChoice, Long id) {
        Class<?> entityClass;
        String entityName;

        switch (tableChoice) {
            case 1:
                entityClass = Employee.class;
                entityName = "Employee";
                break;
            case 2:
                entityClass = Patient.class;
                entityName = "Patient";
                break;
            case 3:
                entityClass = Appointment.class;
                entityName = "Appointment";
                break;
            case 4:
                entityClass = Payment.class;
                entityName = "Payment";
                break;
            default:
                System.out.println("Invalid table selection.");
                return;
        }

        try {
            // Fetch the entity from the database
            logger.info("Fetching from" + entityName + "ID " + Long.toString(id));
            Object result = hibernate.get(entityClass, id);

            if (result == null) {
                logger.error("No " + entityName + " found with ID: " + id);
                return;
            }

            // Check access via XACML PDP
            boolean allowed = pdp.checkAccess(loggedInPerson, result, context);

            if (allowed) {
                System.out.println("Access Granted!");
            } else {
                System.out.println("Access Denied: You do not have permission to view " 
                                + entityName + " data.");
            }

        } catch (Exception e) {
            logger.error("Error accessing data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleQuery(String context, int tableChoice, String personName) {
        List<?> rows = switch (tableChoice) {
            case 1 -> hibernate.jpql("FROM Employee", Employee.class);
            case 2 -> hibernate.jpql("FROM Patient", Patient.class);
            case 3 -> hibernate.jpql("FROM Appointment", Appointment.class);
            case 4 -> hibernate.jpql("FROM Payment", Payment.class);
            default -> List.of();
        };

        int granted = 0, denied = 0;
        for (Object row : rows) {
            if (!matchesPerson(row, personName)) continue;
            try {
                if (pdp.checkAccess(loggedInPerson, row, context)) {
                    granted++;
                    System.out.println(row);
                } else {
                    denied++;
                }
            } catch (Exception e) {
                logger.error("Error checking access: " + e.getMessage());
            }
        }
        System.out.println(granted + " row(s) shown, " + denied + " row(s) denied.");
    }

    private boolean matchesPerson(Object row, String name) {
        String n = name.toLowerCase();
        if (row instanceof Employee e) {
            return e.getName().toLowerCase().contains(n);
        }
        if (row instanceof Patient p) {
            return p.getName().toLowerCase().contains(n);
        }
        if (row instanceof Appointment a) {
            return (a.getPatient() != null && a.getPatient().getName().toLowerCase().contains(n))
                || (a.getEmployee() != null && a.getEmployee().getName().toLowerCase().contains(n));
        }
        if (row instanceof Payment p) {
            return p.getPatient() != null && p.getPatient().getName().toLowerCase().contains(n);
        }
        return false;
    }
    
}
