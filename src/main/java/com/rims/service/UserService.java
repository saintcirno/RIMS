package com.rims.service;

import com.rims.model.User;
import com.rims.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User registerBuyer(String name, String email, String password, String phone, String address) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with email " + email + " already exists.");
        }
        User user = new User(name, email, password, "ROLE_BUYER");
        user.setPhone(phone);
        user.setAddress(address);
        return userRepository.save(user);
    }

    @Transactional
    public User registerSeller(String name, String email, String password, String organizationName, 
                               String registrationId, String contactPerson, String phone, String address) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with email " + email + " already exists.");
        }
        User user = new User(name, email, password, "ROLE_SELLER");
        user.setOrganizationName(organizationName != null && !organizationName.isBlank() ? organizationName : name);
        user.setRegistrationId(registrationId);
        user.setContactPerson(contactPerson != null && !contactPerson.isBlank() ? contactPerson : name);
        user.setPhone(phone);
        user.setAddress(address);
        return userRepository.save(user);
    }

    public Optional<User> authenticate(String email, String password, String requiredRole) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Note: plain-text comparison matching database record so SUDO Admin sees exact stored password
            if (user.getPassword().equals(password) && user.isActive()) {
                if (requiredRole == null || user.getRole().equalsIgnoreCase(requiredRole)) {
                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public void toggleUserActive(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setActive(!user.isActive());
            userRepository.save(user);
        });
    }

    public long countTotalUsers() {
        return userRepository.count();
    }

    public long countSellers() {
        return userRepository.countByRole("ROLE_SELLER");
    }

    public long countBuyers() {
        return userRepository.countByRole("ROLE_BUYER");
    }
}
