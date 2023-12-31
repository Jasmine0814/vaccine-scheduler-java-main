package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.*;
import scheduler.model.Vaccine.VaccineGetter;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if(tokens.length != 3) {
            System.out.println("Please try again！");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }

    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password). get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) throws SQLException {
        // TODO: Part 2
        // search_caregiver_schedule <date>
        // check 1: check if someone's already logged-in if not please log in
        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login as a caregiver or patient first!");
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if(tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        Date d = Date.valueOf(date);

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            PreparedStatement getAvailCaregiver = con.prepareStatement("SELECT Username FROM Availabilities WHERE time = ?");
            getAvailCaregiver.setDate(1, d);
            ResultSet rss = getAvailCaregiver.executeQuery();
            while (rss.next()) {
                System.out.println("Available caregiver: " + rss.getString(1));
            }

            PreparedStatement getAllVaccines = con.prepareStatement("SELECT * FROM vaccines");
            ResultSet rs = getAllVaccines.executeQuery();
            while (rs.next()) {
                System.out.println("Vaccine Name: " + rs.getString(1) +
                        ", Available Doses: " + rs.getInt(2));
            }
        } catch (SQLException e) {
            System.out.println("SQL error");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

    }

    private static void reserve(String[] tokens) throws SQLException {
        // reserve <date> <vaccine>
        // check 1: check if the current logged-in user is a patient
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        String date = tokens[1];
        String vaccineName = tokens[2];
        Vaccine vaccine = null;

        try{
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            Date d = Date.valueOf(date);
            PreparedStatement getAvailCaregiver = con.prepareStatement("SELECT Username FROM Availabilities WHERE time = ?");
            getAvailCaregiver.setDate(1, d);
            ResultSet rs = getAvailCaregiver.executeQuery();
            String availCaregiver = null;
            if(rs.next()) {
                availCaregiver = rs.getString(1);
            } else {
                System.out.println("There is no available caregiver on that day.");
                return;
            }

            PreparedStatement availVaccine = con.prepareStatement("SELECT Name,Doses FROM Vaccines WHERE Name = ? AND Doses > 0");
            availVaccine.setString(1,vaccineName);
            ResultSet rss = availVaccine.executeQuery();
            if(!rss.next()) {
                System.out.println("No Vaccine left.");
                return;
            } else {
                try {
                    vaccine = new Vaccine.VaccineGetter(vaccineName).get();
                    vaccine.decreaseAvailableDoses(1);
                } catch (SQLException e) {
                    System.out.println("Error occured when decreasing doses.");
                    e.printStackTrace();
                }
            }

            String addAppointment = "Insert into Appointments VALUES(?, ?, ?, ?)";
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setString(1, currentPatient.getUsername());
            statement.setString(2, availCaregiver);
            statement.setString(3, vaccineName);
            statement.setDate(4, d);
            statement.executeUpdate();
        } catch(SQLException e) {
            System.out.println("Failed to reserve");
            e.printStackTrace();
        }

        try {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            PreparedStatement getReservation = con.prepareStatement("SELECT Appointment_id," +
                    "Caregiver_Username FROM Appointments WHERE Patient_Username = ?");
            getReservation.setString(1, currentPatient.getUsername());
            ResultSet re = getReservation.executeQuery();
            while (re.next()) {
                System.out.println("Your Reservation: " +
                        "Appointment ID: " + re.getInt(1) +
                        ", Caregiver: " + re.getString(2)
                );
            }
        } catch(SQLException e) {
            System.out.println("Failed to print");
            e.printStackTrace();
        }
    }


        private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) throws SQLException {
        // TODO: Extra credit
        int aid = Integer.parseInt(tokens[1]);
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String cancel  = "DELETE FROM APPOINTMENTS WHERE appointment_id = ?;";
        try {
            PreparedStatement statement = con.prepareStatement(cancel);
            statement.setInt(1, aid);
            statement.executeUpdate();
            System.out.println("Cancel successfully");
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) throws SQLException {
        // TODO: Part 2
        // check 1: check it currently has user
        if(currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login as a caregiver or patient first!");
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // For caregivers, you should print the appointment ID, vaccine name, date, and patient name.
        // For patients, you should print the appointment ID, vaccine name, date, and caregiver name.

        if(currentCaregiver != null) {
            String Caregiver = currentCaregiver.getUsername();
            PreparedStatement getCaregiverSchedule = con.prepareStatement("SELECT Appointment_id, " +
                    "Vaccine_name, Appointment_time, Patient_Username FROM Appointments WHERE Caregiver_Username = ?");
            getCaregiverSchedule.setString(1, Caregiver);

            ResultSet rs = getCaregiverSchedule.executeQuery();
            while (rs.next()) {
                System.out.println("appointment id: " + rs.getInt(1) + ", vaccine name: " + rs.getString(2) +
                        ", date: " + rs.getString(3) + ", patient name.: " + rs.getString(4));
            }
        } else {
            String Patient = currentPatient.getUsername();
            PreparedStatement getPatientSchedule = con.prepareStatement("SELECT Appointment_id, " +
                    "Vaccine_name, Appointment_time, Caregiver_Username FROM Appointments WHERE Patient_Username = ?");
            getPatientSchedule.setString(1, Patient);

            ResultSet rs = getPatientSchedule.executeQuery();
            while (rs.next()) {
                System.out.println("appointment id: " + rs.getInt(1) + ", vaccine name: " + rs.getString(2) +
                        ", date: " + rs.getString(3) + ", caregiver name: " + rs.getString(4));
            }
        }
    }

    private static void logout(String[] tokens) {
        // logout current user
        currentCaregiver = null;
        currentPatient = null;
        System.out.println("*** Account log out successfully ***");
    }
}
