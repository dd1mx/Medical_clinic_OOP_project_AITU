package domain;

import strategy.AppointmentState;
import strategy.ScheduledState;

import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {

    private int id;
    private Patient patient;
    private Doctor doctor;
    private LocalDate date;
    private LocalTime time;
    private String office;
    private AppointmentState state;  // только state, без status enum

    public Appointment(int id, Patient patient, Doctor doctor,
                       LocalDate date, LocalTime time, String office) {
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.date = date;
        this.time = time;
        this.office = office;
        this.state = new ScheduledState(); // начальное состояние
    }

    // Делегируем поведение state-объекту
    public void cancel() {
        state.cancel(this);
    }

    public void complete() {
        state.complete(this);
    }

    // Getters
    public int getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getOffice() { return office; }

    // Вместо getStatus() используем getName() от state
    public String getStatus() {
        return state.getName();
    }

    // Setter для state (используется state-объектами)
    public void setState(AppointmentState state) {
        this.state = state;
    }
}