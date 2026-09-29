package com.dentist.rendez_vous.model;


import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patient")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    private boolean emailVerifie;

    @Column(name = "patient_code", nullable = false, unique = true, updatable = false)
    private String patientCode;

    private String tokenVerification;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RendezVous> rendezVousList = new ArrayList<>();

    @PrePersist
    public void genererPatientCode() {
        if (this.patientCode == null) {
            this.patientCode = UUID.randomUUID().toString();
        }
    }
}