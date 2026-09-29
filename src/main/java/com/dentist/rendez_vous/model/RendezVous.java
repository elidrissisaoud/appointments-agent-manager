package com.dentist.rendez_vous.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rendez_vous")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutRendezVous statut = StatutRendezVous.EN_COURS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

}
