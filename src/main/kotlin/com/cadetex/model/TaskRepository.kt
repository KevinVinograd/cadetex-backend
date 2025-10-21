package com.cadetex.model

interface TaskRepository {
    suspend fun allTasks(): List<Task>
    suspend fun tasksByOrganization(organizationId: String): List<Task>
    suspend fun tasksByCourier(courierId: String): List<Task>
    suspend fun tasksByStatus(status: TaskStatus): List<Task>
    suspend fun findById(id: String): Task?
    suspend fun create(task: Task): Task
    suspend fun update(id: String, task: UpdateTaskRequest): Task?
    suspend fun delete(id: String): Boolean
}
