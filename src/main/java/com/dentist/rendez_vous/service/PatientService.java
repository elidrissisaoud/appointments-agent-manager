package com.dentist.rendez_vous.service;

import com.dentist.rendez_vous.exception.UserNotFoundException;
import com.dentist.rendez_vous.model.Patient;
import com.dentist.rendez_vous.reposetory.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class PatientService {
    private final PatientRepository repository;

    public Patient getById(Long id) throws UserNotFoundException {
        if (!repository.existsById(id)) {
            throw new UserNotFoundException("this user not found in the database");
        }
        return repository.findById(id).get();
    }
}
