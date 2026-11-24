package org.mlm.mages.todo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mlm.mages.CoreClient
import org.mlm.mages.FfiError

class TodoViewModel(
    private val coreClient: CoreClient,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow<TodoScreenState>(TodoScreenState.Loading)
    val state: StateFlow<TodoScreenState> = _state.asStateFlow()

    init {
        loadTodoRooms()
    }

    fun onEvent(event: TodoEvent) {
        when (event) {
            is TodoEvent.CreateRoom -> createTodoRoom(event.name, event.description)
            is TodoEvent.SelectRoom -> selectRoom(event.room)
            is TodoEvent.AddTodo -> addTodoItem(event.roomId, event.text)
            is TodoEvent.ToggleTodo -> toggleTodoItem(event.roomId, event.itemId, event.completed)
            is TodoEvent.DeleteTodo -> deleteTodoItem(event.roomId, event.itemId)
            is TodoEvent.EditTodo -> editTodoItem(event.roomId, event.itemId, event.newText)
            is TodoEvent.LoadTodos -> loadRoomTodos(event.roomId)
            TodoEvent.Refresh -> loadTodoRooms()
        }
    }

    private fun loadTodoRooms() {
        scope.launch {
            _state.value = TodoScreenState.Loading
            try {
                val rooms = coreClient.getTodoRooms()
                val currentState = _state.value
                val selectedRoom = when (currentState) {
                    is TodoScreenState.Content -> currentState.selectedRoom
                    else -> rooms.firstOrNull()
                }
                _state.update {
                    TodoScreenState.Content(
                        rooms = rooms,
                        selectedRoom = selectedRoom
                    )
                }
            } catch (e: FfiError) {
                _state.value = TodoScreenState.Error(e.message ?: "Failed to load todo rooms")
            } catch (e: Exception) {
                _state.value = TodoScreenState.Error(e.message ?: "Failed to load todo rooms")
            }
        }
    }

    private fun createTodoRoom(name: String, description: String?) {
        scope.launch {
            try {
                coreClient.createTodoRoom(name, description)
                loadTodoRooms()
            } catch (e: FfiError) {
                // Handle error
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun selectRoom(room: TodoRoom) {
        scope.launch {
            _state.update { currentState ->
                when (currentState) {
                    is TodoScreenState.Content -> {
                        currentState.copy(selectedRoom = room)
                    }
                    else -> currentState
                }
            }
        }
    }

    private fun loadRoomTodos(roomId: String) {
        scope.launch {
            try {
                val room = coreClient.loadTodoRoom(roomId)
                _state.update { currentState ->
                    when (currentState) {
                        is TodoScreenState.Content -> {
                            val updatedRooms = currentState.rooms.map {
                                if (it.roomId == room.roomId) room else it
                            }
                            currentState.copy(
                                rooms = updatedRooms,
                                selectedRoom = room
                            )
                        }
                        else -> currentState
                    }
                }
            } catch (e: FfiError) {
                // Handle error
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun addTodoItem(roomId: String, text: String) {
        scope.launch {
            try {
                val newTodo = coreClient.addTodoItem(roomId, text)
                val currentState = _state.value
                if (currentState is TodoScreenState.Content) {
                    val updatedRooms = currentState.rooms.map { room ->
                        if (room.roomId == roomId) {
                            val updatedTodos = room.todos + listOf(newTodo)
                            room.copy(todos = updatedTodos)
                        } else {
                            room
                        }
                    }
                    _state.update {
                        currentState.copy(rooms = updatedRooms)
                    }
                }
            } catch (e: FfiError) {
                // Handle error
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun toggleTodoItem(roomId: String, itemId: String, completed: Boolean) {
        scope.launch {
            try {
                coreClient.toggleTodoItem(roomId, itemId, completed, null)
                val currentState = _state.value
                if (currentState is TodoScreenState.Content) {
                    val updatedRooms = currentState.rooms.map { room ->
                        if (room.roomId == roomId) {
                            val updatedTodos = room.todos.map { todo ->
                                if (todo.id == itemId) {
                                    todo.copy(completed = completed)
                                } else {
                                    todo
                                }
                            }
                            room.copy(todos = updatedTodos)
                        } else {
                            room
                        }
                    }
                    _state.update {
                        currentState.copy(rooms = updatedRooms)
                    }
                }
            } catch (e: FfiError) {
                // Handle error
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun deleteTodoItem(roomId: String, itemId: String) {
        scope.launch {
            try {
                coreClient.deleteTodoItem(roomId, itemId, null)
                val currentState = _state.value
                if (currentState is TodoScreenState.Content) {
                    val updatedRooms = currentState.rooms.map { room ->
                        if (room.roomId == roomId) {
                            val updatedTodos = room.todos.filter { todo ->
                                todo.id != itemId
                            }
                            room.copy(todos = updatedTodos)
                        } else {
                            room
                        }
                    }
                    _state.update {
                        currentState.copy(rooms = updatedRooms)
                    }
                }
            } catch (e: FfiError) {
                // Handle error
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun editTodoItem(roomId: String, itemId: String, newText: String) {
        scope.launch {
            try {
                coreClient.editTodoItem(roomId, itemId, newText, null)
                val currentState = _state.value
                if (currentState is TodoScreenState.Content) {
                    val updatedRooms = currentState.rooms.map { room ->
                        if (room.roomId == roomId) {
                            val updatedTodos = room.todos.map { todo ->
                                if (todo.id == itemId) {
                                    todo.copy(text = newText)
                                } else {
                                    todo
                                }
                            }
                            room.copy(todos = updatedTodos)
                        } else {
                            room
                        }
                    }
                    _state.update {
                        currentState.copy(rooms = updatedRooms)
                    }
                }
            } catch (e: FfiError) {
                // Handle error
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}
