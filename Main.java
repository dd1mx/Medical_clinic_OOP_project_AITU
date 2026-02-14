package app;

import domain.*;
import exception.EntityNotFoundException;
import repository.*;
import service.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        PatientRepository patientRepository = new InMemoryPatientRepository();
        DoctorRepository doctorRepository = new InMemoryDoctorRepository();
        AppointmentRepository appointmentRepository = new InMemoryAppointmentRepository();
        MedicalHistoryRepository medicalHistoryRepository = new InMemoryMedicalHistoryRepository();

        PatientService patientService = new PatientServiceImpl(patientRepository);
        DoctorService doctorService = new DoctorServiceImpl(doctorRepository);
        AppointmentService appointmentService = new AppointmentServiceImpl(appointmentRepository);
        MedicalHistoryService medicalHistoryService = new MedicalHistoryServiceImpl(
                medicalHistoryRepository,
                patientRepository
        );

        System.out.println("=== Добро пожаловать в медицинскую систему ===");

        while (true) {
            System.out.println("\n1. Вход\n2. Регистрация\n0. Выход");
            String choice = scanner.nextLine();

            if (choice.equals("0")) break;

            if (choice.equals("1")) {
                System.out.print("Username: ");
                String username = scanner.nextLine();
                System.out.print("Пароль: ");
                String password = scanner.nextLine();

                Patient patient = patientRepository.findByUsername(username);
                Doctor doctor = doctorRepository.findByUsername(username);

                if (patient != null && patient.getPassword().equals(password)) {
                    handlePatientMenu(scanner, patient, appointmentService, doctorService, medicalHistoryService);
                } else if (doctor != null && doctor.getPassword().equals(password)) {
                    handleDoctorMenu(scanner, doctor, appointmentService, medicalHistoryService, patientService); // FIXED: added patientService
                } else {
                    System.out.println("Неверный логин или пароль!");
                }
            }

            if (choice.equals("2")) {
                System.out.println("Регистрация:\n1. Пациент\n2. Врач");
                String role = scanner.nextLine();

                System.out.print("ID: ");
                int id = Integer.parseInt(scanner.nextLine());
                System.out.print("ФИО: ");
                String name = scanner.nextLine();
                System.out.print("Телефон: ");
                String phone = scanner.nextLine();
                System.out.print("Username: ");
                String uname = scanner.nextLine();
                System.out.print("Пароль: ");
                String pwd = scanner.nextLine();

                if (role.equals("1")) {
                    System.out.print("Страховка: ");
                    String insurance = scanner.nextLine();
                    Patient newPatient = new Patient(id, name, phone, uname, pwd, insurance);
                    patientService.addPatient(newPatient);

                    // Auto-create medical history for the patient
                    medicalHistoryService.getMedicalHistoryByPatient(id);

                    System.out.println("Пациент зарегистрирован!");
                }

                if (role.equals("2")) {
                    System.out.print("Специализация: ");
                    String spec = scanner.nextLine();
                    doctorService.addDoctor(
                            new Doctor(id, name, phone, uname, pwd, spec)
                    );
                    System.out.println("Доктор зарегистрирован!");
                }
            }
        }
    }

    private static void handlePatientMenu(
            Scanner scanner,
            Patient patient,
            AppointmentService appointmentService,
            DoctorService doctorService,
            MedicalHistoryService medicalHistoryService
    ) {
        while (true) {
            System.out.println("\n1. Мои приёмы\n2. Записаться\n3. Моя медицинская карта\n0. Выйти");
            String choice = scanner.nextLine();

            if (choice.equals("0")) return;

            if (choice.equals("1")) {
                // Каждый раз получаем свежий список из репозитория
                List<Appointment> list =
                        appointmentService.getAppointmentsByPatient(patient.getId());
                if (list.isEmpty()) {
                    System.out.println("У вас нет записей на приём");
                } else {
                    System.out.println("\n--- ВАШИ ПРИЁМЫ ---");
                    for (Appointment a : list) {
                        // Получаем свежие данные о докторе
                        Doctor doctor = doctorService.getDoctorById(a.getDoctor().getId());
                        // FIXED: Convert enum to string using name() or toString()
                        String status = a.getStatus().toString(); // или a.getStatus().name()

                        System.out.println("ID: " + a.getId() +
                                " | Доктор: " + doctor.getFullName() +
                                " | Специализация: " + doctor.getSpecialization() +
                                " | Дата: " + a.getDate() +
                                " | Время: " + a.getTime() +
                                " | Кабинет: " + a.getOffice() +
                                " | Статус: " + status);
                    }
                    System.out.println("-------------------");
                }
            }

            if (choice.equals("2")) {
                try {
                    System.out.print("ID приёма: ");
                    int id = Integer.parseInt(scanner.nextLine());

                    // Проверяем, не существует ли уже приём с таким ID
                    if (appointmentService.findById(id) != null) {
                        System.out.println("Приём с таким ID уже существует!");
                        continue;
                    }

                    System.out.print("ID доктора: ");
                    int doctorId = Integer.parseInt(scanner.nextLine());

                    Doctor doctor = doctorService.getDoctorById(doctorId);

                    System.out.print("Дата (YYYY-MM-DD): ");
                    LocalDate date = LocalDate.parse(scanner.nextLine());
                    System.out.print("Время (HH:MM): ");
                    LocalTime time = LocalTime.parse(scanner.nextLine());
                    System.out.print("Кабинет: ");
                    String office = scanner.nextLine();

                    appointmentService.createAppointment(
                            id, patient, doctor, date, time, office
                    );

                    System.out.println("Приём успешно создан!");

                } catch (EntityNotFoundException e) {
                    System.out.println("Доктор не найден!");
                } catch (Exception e) {
                    System.out.println("Ошибка ввода данных: " + e.getMessage());
                }
            }

            if (choice.equals("3")) {
                try {
                    MedicalHistory history = medicalHistoryService.getMedicalHistoryByPatient(patient.getId());

                    System.out.println("\n========== МОЯ МЕДИЦИНСКАЯ КАРТА ==========");
                    System.out.println("Пациент: " + patient.getFullName());
                    System.out.println("Страховка: " + patient.getInsuranceNumber());

                    System.out.println("\n--- ДИАГНОЗЫ ---");
                    List<String> diagnoses = history.getDiagnoses();
                    if (diagnoses.isEmpty()) {
                        System.out.println("Нет записей");
                    } else {
                        for (int i = 0; i < diagnoses.size(); i++) {
                            System.out.println((i + 1) + ". " + diagnoses.get(i));
                        }
                    }

                    System.out.println("\n--- НАЗНАЧЕНИЯ ---");
                    List<String> prescriptions = history.getPrescriptions();
                    if (prescriptions.isEmpty()) {
                        System.out.println("Нет записей");
                    } else {
                        for (int i = 0; i < prescriptions.size(); i++) {
                            System.out.println((i + 1) + ". " + prescriptions.get(i));
                        }
                    }

                    System.out.println("\n--- РЕЗУЛЬТАТЫ АНАЛИЗОВ ---");
                    List<String> testResults = history.getTestResults();
                    if (testResults.isEmpty()) {
                        System.out.println("Нет записей");
                    } else {
                        for (int i = 0; i < testResults.size(); i++) {
                            System.out.println((i + 1) + ". " + testResults.get(i));
                        }
                    }

                    System.out.println("\n--- ЗАМЕТКИ ВРАЧА ---");
                    List<String> doctorNotes = history.getDoctorNotes();
                    if (doctorNotes.isEmpty()) {
                        System.out.println("Нет записей");
                    } else {
                        for (int i = 0; i < doctorNotes.size(); i++) {
                            System.out.println((i + 1) + ". " + doctorNotes.get(i));
                        }
                    }

                    System.out.println("\n============================================");

                } catch (EntityNotFoundException e) {
                    System.out.println("Ошибка при загрузке медицинской карты");
                }
            }
        }
    }


    private static void handleDoctorMenu(
            Scanner scanner,
            Doctor doctor,
            AppointmentService appointmentService,
            MedicalHistoryService medicalHistoryService,
            PatientService patientService
    ) {
        while (true) {
            System.out.println("\n1. Приёмы\n2. Изменить статус приёма\n3. Медицинские карты пациентов\n0. Выйти");
            String choice = scanner.nextLine();

            if (choice.equals("0")) return;

            if (choice.equals("1")) {
                List<Appointment> appointments = appointmentService
                        .getAppointmentsByDoctor(doctor.getId());
                if (appointments.isEmpty()) {
                    System.out.println("У вас нет назначенных приёмов");
                } else {
                    System.out.println("\n--- ВАШИ ПРИЁМЫ ---");
                    for (Appointment a : appointments) {
                        Patient patient = patientService.getPatientById(a.getPatient().getId());
                        // FIXED: Convert enum to string using name() or toString()
                        String status = a.getStatus().toString(); // или a.getStatus().name()

                        System.out.println("ID: " + a.getId() +
                                " | Пациент: " + patient.getFullName() +
                                " | Дата: " + a.getDate() +
                                " | Время: " + a.getTime() +
                                " | Статус: " + status);
                    }
                    System.out.println("-------------------");
                }
            }

            if (choice.equals("2")) {
                try {
                    System.out.print("ID приёма: ");
                    int id = Integer.parseInt(scanner.nextLine());
                    Appointment a = appointmentService.findById(id);

                    if (a == null) {
                        System.out.println("Приём не найден");
                        continue;
                    }

                    // FIXED: Convert enum to string
                    System.out.println("Текущий статус: " + a.getStatus().toString());
                    System.out.println("1. Завершить\n2. Отменить");
                    String s = scanner.nextLine();

                    if (s.equals("1")) {
                        a.complete();
                        System.out.println("Приём завершён");
                    } else if (s.equals("2")) {
                        a.cancel();
                        System.out.println("Приём отменён");
                    } else {
                        System.out.println("Неверный выбор");
                    }

                } catch (EntityNotFoundException e) {
                    System.out.println("Приём не найден");
                } catch (IllegalStateException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            }

            if (choice.equals("3")) {
                try {
                    System.out.print("ID пациента: ");
                    int patientId = Integer.parseInt(scanner.nextLine());

                    MedicalHistory history = medicalHistoryService.getMedicalHistoryByPatient(patientId);
                    Patient patient = patientService.getPatientById(patientId);

                    System.out.println("\n========== МЕДИЦИНСКАЯ КАРТА ПАЦИЕНТА ==========");
                    System.out.println("Пациент: " + patient.getFullName());
                    System.out.println("Страховка: " + patient.getInsuranceNumber());

                    System.out.println("\n--- ДИАГНОЗЫ ---");
                    List<String> diagnoses = history.getDiagnoses();
                    if (diagnoses.isEmpty()) {
                        System.out.println("Нет записей");
                    } else {
                        for (int i = 0; i < diagnoses.size(); i++) {
                            System.out.println((i + 1) + ". " + diagnoses.get(i));
                        }
                    }

                    System.out.println("\n--- НАЗНАЧЕНИЯ ---");
                    List<String> prescriptions = history.getPrescriptions();
                    if (prescriptions.isEmpty()) {
                        System.out.println("Нет записей");
                    } else {
                        for (int i = 0; i < prescriptions.size(); i++) {
                            System.out.println((i + 1) + ". " + prescriptions.get(i));
                        }
                    }

                    System.out.println("\n--- РЕЗУЛЬТАТЫ АНАЛИЗОВ ---");
                    List<String> testResults = history.getTestResults();
                    if (testResults.isEmpty()) {
                        System.out.println("Нет записей");
                    } else {
                        for (int i = 0; i < testResults.size(); i++) {
                            System.out.println((i + 1) + ". " + testResults.get(i));
                        }
                    }

                    System.out.println("\n--- ЗАМЕТКИ ВРАЧА ---");
                    List<String> doctorNotes = history.getDoctorNotes();
                    if (doctorNotes.isEmpty()) {
                        System.out.println("Нет записей");
                    } else {
                        for (int i = 0; i < doctorNotes.size(); i++) {
                            System.out.println((i + 1) + ". " + doctorNotes.get(i));
                        }
                    }

                    System.out.println("\n==================================================");

                    System.out.println("\nДобавить запись в карту:");
                    System.out.println("1. Диагноз");
                    System.out.println("2. Назначение");
                    System.out.println("3. Результат анализа");
                    System.out.println("4. Заметку");
                    System.out.println("0. Назад");

                    String addChoice = scanner.nextLine();

                    if (!addChoice.equals("0")) {
                        System.out.print("Текст: ");
                        String text = scanner.nextLine();

                        switch (addChoice) {
                            case "1":
                                medicalHistoryService.addDiagnosis(patientId, text);
                                System.out.println("Диагноз добавлен!");
                                break;
                            case "2":
                                medicalHistoryService.addPrescription(patientId, text);
                                System.out.println("Назначение добавлено!");
                                break;
                            case "3":
                                medicalHistoryService.addTestResult(patientId, text);
                                System.out.println("Результат анализа добавлен!");
                                break;
                            case "4":
                                medicalHistoryService.addDoctorNote(patientId, text);
                                System.out.println("Заметка добавлена!");
                                break;
                            default:
                                System.out.println("Неверный выбор!");
                        }
                    }

                } catch (EntityNotFoundException e) {
                    System.out.println("Пациент не найден");
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            }
        }
    }
}