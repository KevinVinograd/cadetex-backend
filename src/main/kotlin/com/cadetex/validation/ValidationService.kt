package com.cadetex.validation

import com.cadetex.model.*

object ValidationService {

    fun validateCreateUserRequest(request: CreateUserRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.organizationId.isBlank()) {
            errors.add("organizationId is required")
        }
        if (request.name.isBlank()) {
            errors.add("name is required")
        }
        if (request.email.isBlank() || !isValidEmail(request.email)) {
            errors.add("email is required and must be valid")
        }
        if (request.password.isBlank() || request.password.length < 6) {
            errors.add("password is required and must be at least 6 characters")
        }
        if (request.role == null) {
            errors.add("role is required")
        }
        
        return errors
    }

    fun validateUpdateUserRequest(request: UpdateUserRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.name != null && request.name.isBlank()) {
            errors.add("name cannot be blank")
        }
        if (request.email != null && (request.email.isBlank() || !isValidEmail(request.email))) {
            errors.add("email must be valid")
        }
        if (request.password != null && (request.password.isBlank() || request.password.length < 6)) {
            errors.add("password must be at least 6 characters")
        }
        
        return errors
    }

    fun validateLoginRequest(request: LoginRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.email.isBlank() || !isValidEmail(request.email)) {
            errors.add("email is required and must be valid")
        }
        if (request.password.isBlank()) {
            errors.add("password is required")
        }
        
        return errors
    }

    fun validateCreateOrganizationRequest(request: CreateOrganizationRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.name.isBlank()) {
            errors.add("name is required")
        }
        
        return errors
    }

    fun validateUpdateOrganizationRequest(request: UpdateOrganizationRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.name != null && request.name.isBlank()) {
            errors.add("name cannot be blank")
        }
        
        return errors
    }

    fun validateCreateClientRequest(request: CreateClientRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.organizationId.isBlank()) {
            errors.add("organizationId is required")
        }
        if (request.name.isBlank()) {
            errors.add("name is required")
        }
        if (request.address.isBlank()) {
            errors.add("address is required")
        }
        
        return errors
    }

    fun validateUpdateClientRequest(request: UpdateClientRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.name != null && request.name.isBlank()) {
            errors.add("name cannot be blank")
        }
        if (request.address != null && request.address.isBlank()) {
            errors.add("address cannot be blank")
        }
        
        return errors
    }

    fun validateCreateProviderRequest(request: CreateProviderRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.organizationId.isBlank()) {
            errors.add("organizationId is required")
        }
        if (request.name.isBlank()) {
            errors.add("name is required")
        }
        if (request.address.isBlank()) {
            errors.add("address is required")
        }
        
        return errors
    }

    fun validateUpdateProviderRequest(request: UpdateProviderRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.name != null && request.name.isBlank()) {
            errors.add("name cannot be blank")
        }
        if (request.address != null && request.address.isBlank()) {
            errors.add("address cannot be blank")
        }
        
        return errors
    }

    fun validateCreateCourierRequest(request: CreateCourierRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.organizationId.isBlank()) {
            errors.add("organizationId is required")
        }
        if (request.name.isBlank()) {
            errors.add("name is required")
        }
        if (request.phoneNumber.isBlank()) {
            errors.add("phoneNumber is required")
        }
        if (request.email.isNullOrBlank() || !isValidEmail(request.email)) {
            errors.add("email is required and must be valid")
        }
        
        return errors
    }

    fun validateUpdateCourierRequest(request: UpdateCourierRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.name != null && request.name.isBlank()) {
            errors.add("name cannot be blank")
        }
        if (request.phoneNumber != null && request.phoneNumber.isBlank()) {
            errors.add("phoneNumber cannot be blank")
        }
        if (request.email != null && (request.email.isBlank() || !isValidEmail(request.email))) {
            errors.add("email must be valid")
        }
        
        return errors
    }

    fun validateCreateTaskRequest(request: CreateTaskRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.organizationId.isBlank()) {
            errors.add("organizationId is required")
        }
        if (request.type == null) {
            errors.add("type is required")
        }
        if (request.status == null) {
            errors.add("status is required")
        }
        if (request.priority == null) {
            errors.add("priority is required")
        }
        
        return errors
    }

    fun validateUpdateTaskRequest(request: UpdateTaskRequest): List<String> {
        val errors = mutableListOf<String>()
        
        // No required fields for updates, just validate format if provided
        return errors
    }

    fun validateCreateTaskPhotoRequest(request: CreateTaskPhotoRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.taskId.isBlank()) {
            errors.add("taskId is required")
        }
        if (request.photoUrl.isBlank()) {
            errors.add("photoUrl is required")
        }
        
        return errors
    }

    fun validateUpdateTaskPhotoRequest(request: UpdateTaskPhotoRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.photoUrl != null && request.photoUrl.isBlank()) {
            errors.add("photoUrl cannot be blank")
        }
        
        return errors
    }

    fun validateCreateTaskHistoryRequest(request: CreateTaskHistoryRequest): List<String> {
        val errors = mutableListOf<String>()
        
        if (request.taskId.isBlank()) {
            errors.add("taskId is required")
        }
        
        return errors
    }

    fun validateUpdateTaskHistoryRequest(request: UpdateTaskHistoryRequest): List<String> {
        val errors = mutableListOf<String>()
        
        // No required fields for updates
        return errors
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}