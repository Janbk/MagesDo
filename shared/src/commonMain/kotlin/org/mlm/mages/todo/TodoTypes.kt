package org.mlm.mages.todo

import kotlinx.serialization.Serializable

@Serializable
data class TodoListMetadata(
    val name: String,
    val description: String? = null,
    val createdAtMs: ULong,
    val createdBy: String,
    val color: String? = null,
    val isPublic: Boolean = false,
)

@Serializable
data class TodoItem(
    val id: String,
    val text: String,
    val completed: Boolean,
    val createdAtMs: ULong,
    val createdBy: String,
    val dueDate: String? = null,
    val priority: String? = null,
    val tags: List<String>? = null,
)

@Serializable
data class TodoToggle(
    val itemId: String,
    val completed: Boolean,
    val toggledAtMs: ULong,
    val toggledBy: String,
)

@Serializable
data class TodoDelete(
    val itemId: String,
    val deletedAtMs: ULong,
    val deletedBy: String,
)

@Serializable
data class TodoEdit(
    val itemId: String,
    val text: String,
    val editedAtMs: ULong,
    val editedBy: String,
)

@Serializable
data class TodoRoom(
    val roomId: String,
    val roomName: String,
    val metadata: TodoListMetadata? = null,
    val todos: List<TodoItem> = emptyList(),
)

// UI state for todo list
sealed class TodoScreenState {
    object Loading : TodoScreenState()
    data class Content(
        val rooms: List<TodoRoom>,
        val selectedRoom: TodoRoom? = null,
    ) : TodoScreenState()
    data class Error(val message: String) : TodoScreenState()
}

// Events for todo list actions
sealed class TodoEvent {
    data class CreateRoom(val name: String, val description: String? = null) : TodoEvent()
    data class SelectRoom(val room: TodoRoom) : TodoEvent()
    data class AddTodo(val roomId: String, val text: String) : TodoEvent()
    data class ToggleTodo(val roomId: String, val itemId: String, val completed: Boolean) : TodoEvent()
    data class DeleteTodo(val roomId: String, val itemId: String) : TodoEvent()
    data class EditTodo(val roomId: String, val itemId: String, val newText: String) : TodoEvent()
    data class LoadTodos(val roomId: String) : TodoEvent()
    object Refresh : TodoEvent()
}
