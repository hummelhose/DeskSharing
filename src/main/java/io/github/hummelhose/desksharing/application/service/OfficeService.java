package io.github.hummelhose.desksharing.application.service;

import io.github.hummelhose.desksharing.domain.model.Office;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.OfficeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OfficeService {

    private final OfficeRepository officeRepository;

    public OfficeService(OfficeRepository officeRepository) {
        this.officeRepository = officeRepository;
    }

    public List<Office> getAllOffices() {
        return officeRepository.findAll();
    }

    public List<Office> getAllActiveOffices() {
        return officeRepository.findByActiveTrue();
    }

    public Optional<Office> getOfficeById(Long id) {
        return officeRepository.findById(id);
    }

    public Office createOffice(String name,
                               String description,
                               boolean active,
                               Integer layoutWidth,
                               Integer layoutHeight) {

        Office office = new Office(
                name,
                description,
                active,
                layoutWidth,
                layoutHeight
        );

        return officeRepository.save(office);
    }

    public Optional<Office> updateOffice(Long id,
                                         String name,
                                         String description,
                                         boolean active,
                                         Integer layoutWidth,
                                         Integer layoutHeight) {

        return officeRepository.findById(id)
                .map(existingOffice -> {
                    existingOffice.setName(name);
                    existingOffice.setDescription(description);
                    existingOffice.setActive(active);
                    existingOffice.setLayoutWidth(layoutWidth);
                    existingOffice.setLayoutHeight(layoutHeight);

                    return officeRepository.save(existingOffice);
                });
    }

    @Transactional
    public boolean deleteOffice(Long officeId) {
        Optional<Office> officeOptional =
                officeRepository.findById(officeId);

        if (officeOptional.isEmpty()) {
            return false;
        }

        officeRepository.delete(officeOptional.get());
        return true;
    }
}