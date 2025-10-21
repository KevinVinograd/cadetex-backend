package com.cadetex.model

import java.util.*

class FakeTaskRepository : TaskRepository {
    private val tasks = mutableListOf<Task>()

    override suspend fun allTasks(): List<Task> = tasks

    override suspend fun tasksByOrganization(organizationId: String): List<Task> = 
        tasks.filter { it.organizationId == organizationId }

    override suspend fun tasksByCourier(courierId: String): List<Task> = 
        tasks.filter { it.courierId == courierId }

    override suspend fun tasksByStatus(status: TaskStatus): List<Task> = 
        tasks.filter { it.status == status }

    override suspend fun findById(id: String): Task? = 
        tasks.find { it.id == id }

    override suspend fun create(task: Task): Task {
        val newTask = task.copy(
            id = UUID.randomUUID().toString(),
            createdAt = java.time.LocalDateTime.now().toString(),
            updatedAt = java.time.LocalDateTime.now().toString()
        )
        tasks.add(newTask)
        return newTask
    }

    override suspend fun update(id: String, updateRequest: UpdateTaskRequest): Task? {
        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return null

        val existing = tasks[index]
        val updated = existing.copy(
            type = updateRequest.type ?: existing.type,
            referenceNumber = updateRequest.referenceNumber ?: existing.referenceNumber,
            clientId = updateRequest.clientId ?: existing.clientId,
            providerId = updateRequest.providerId ?: existing.providerId,
            addressOverride = updateRequest.addressOverride ?: existing.addressOverride,
            courierId = updateRequest.courierId ?: existing.courierId,
            status = updateRequest.status ?: existing.status,
            priority = updateRequest.priority ?: existing.priority,
            scheduledDate = updateRequest.scheduledDate ?: existing.scheduledDate,
            notes = updateRequest.notes ?: existing.notes,
            mbl = updateRequest.mbl ?: existing.mbl,
            hbl = updateRequest.hbl ?: existing.hbl,
            freightCert = updateRequest.freightCert ?: existing.freightCert,
            foCert = updateRequest.foCert ?: existing.foCert,
            bunkerCert = updateRequest.bunkerCert ?: existing.bunkerCert,
            linkedTaskId = updateRequest.linkedTaskId ?: existing.linkedTaskId,
            updatedAt = java.time.LocalDateTime.now().toString()
        )
        tasks[index] = updated
        return updated
    }

    override suspend fun delete(id: String): Boolean {
        return tasks.removeIf { it.id == id }
    }
}
