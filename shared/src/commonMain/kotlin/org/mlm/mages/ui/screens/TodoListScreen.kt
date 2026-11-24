package org.mlm.mages.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.mlm.mages.todo.TodoEvent
import org.mlm.mages.todo.TodoItem
import org.mlm.mages.todo.TodoRoom
import org.mlm.mages.todo.TodoScreenState
import org.mlm.mages.todo.TodoViewModel
import org.mlm.mages.ui.components.core.EmptyState
import org.mlm.mages.ui.theme.Spacing

@Composable
fun TodoListRoute(
    onBack: () -> Unit,
    viewModel: TodoViewModel = koinInject(),
) {
    val state by viewModel.state
    val scope = rememberCoroutineScope()

    TodoListScreen(
        state = state,
        onBack = onBack,
        onEvent = { event ->
            scope.launch {
                viewModel.onEvent(event)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    state: TodoScreenState,
    onBack: () -> Unit,
    onEvent: (TodoEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todo Lists", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEvent(TodoEvent.Refresh) }
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(TodoEvent.CreateRoom("New Todo List", null)) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create new todo list")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state) {
                TodoScreenState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TodoScreenState.Content -> {
                    if (state.rooms.isEmpty()) {
                        CreateFirstTodoList(
                            onCreate = { name, description ->
                                onEvent(TodoEvent.CreateRoom(name, description))
                            }
                        )
                    } else {
                        TodoListContent(
                            rooms = state.rooms,
                            selectedRoom = state.selectedRoom,
                            onEvent = onEvent
                        )
                    }
                }
                is TodoScreenState.Error -> {
                    EmptyState(
                        title = "Error",
                        subtitle = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateFirstTodoList(
    onCreate: (String, String?) -> Unit,
) {
    var newTodoText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No todo lists yet",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Create your first todo list to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = newTodoText,
            onValueChange = { newTodoText = it },
            label = { Text("New todo item") },
            placeholder = { Text("Enter a todo...") },
            modifier = Modifier.fillMaxWidth(),
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                if (newTodoText.isNotBlank()) {
                    onCreate("My First Todo List", null)
                }
            },
            enabled = newTodoText.isNotBlank()
        ) {
            Text("Create Todo List")
        }
    }
}

@Composable
fun TodoListContent(
    rooms: List<TodoRoom>,
    selectedRoom: TodoRoom?,
    onEvent: (TodoEvent) -> Unit,
) {
    val listState = rememberLazyListState()

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sidebar with room list
        Card(
            modifier = Modifier
                .padding(end = 8.dp)
                .fillMaxHeight()
                .width(250.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Todo Lists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                Button(
                    onClick = { onEvent(TodoEvent.CreateRoom("New Todo List", null)) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text("Create New")
                }
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rooms, key = { it.roomId }) { room ->
                        TodoRoomItem(
                            room = room,
                            isSelected = selectedRoom?.roomId == room.roomId,
                            onClick = { onEvent(TodoEvent.SelectRoom(room)) }
                        )
                    }
                }
            }
        }

        // Main content area
        if (selectedRoom != null) {
            TodoRoomContent(
                room = selectedRoom,
                onEvent = onEvent,
                modifier = Modifier.fillMaxSize()
            )
        } else if (rooms.isNotEmpty()) {
            // Select first room by default
            LaunchedEffect(rooms) {
                onEvent(TodoEvent.SelectRoom(rooms.first()))
            }
        }
    }
}

@Composable
fun TodoRoomItem(
    room: TodoRoom,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = room.metadata?.name ?: room.roomName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (room.metadata?.description != null) {
                Text(
                    text = room.metadata.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        if (room.todos.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = room.todos.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun TodoRoomContent(
    room: TodoRoom,
    onEvent: (TodoEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newTodoText by remember { mutableStateOf("") }
    var editingTodoId by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Room header
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = room.metadata?.name ?: room.roomName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (room.metadata?.description != null) {
                Text(
                    text = room.metadata.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add new todo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTodoText,
                onValueChange = { newTodoText = it },
                label = { Text("Add a new todo") },
                placeholder = { Text("What needs to be done?") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                onValueChange = { newTodoText = it }
            )
            
            Spacer(modifier = Modifier.size(8.dp))
            
            IconButton(
                onClick = {
                    if (newTodoText.isNotBlank()) {
                        onEvent(TodoEvent.AddTodo(room.roomId, newTodoText))
                        newTodoText = ""
                    }
                },
                enabled = newTodoText.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Todo list
        if (room.todos.isEmpty()) {
            EmptyState(
                title = "No todos yet",
                subtitle = "Add a todo to get started",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(room.todos, key = { it.id }) { todo ->
                    TodoItemView(
                        todo = todo,
                        isEditing = editingTodoId == todo.id,
                        editText = editText,
                        onEditStart = {
                            editingTodoId = todo.id
                            editText = todo.text
                        },
                        onEditTextChange = { editText = it },
                        onEditSave = {
                            onEvent(TodoEvent.EditTodo(room.roomId, todo.id, editText))
                            editingTodoId = null
                            editText = ""
                        },
                        onEditCancel = {
                            editingTodoId = null
                            editText = ""
                        },
                        onToggle = { completed ->
                            onEvent(TodoEvent.ToggleTodo(room.roomId, todo.id, completed))
                        },
                        onDelete = {
                            onEvent(TodoEvent.DeleteTodo(room.roomId, todo.id))
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun TodoItemView(
    todo: TodoItem,
    isEditing: Boolean,
    editText: String,
    onEditStart: () -> Unit,
    onEditTextChange: (String) -> Unit,
    onEditSave: () -> Unit,
    onEditCancel: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.completed,
            onCheckedChange = onToggle
        )
        
        Spacer(modifier = Modifier.size(8.dp))
        
        if (isEditing) {
            BasicTextField(
                value = editText,
                onValueChange = onEditTextChange,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            Row {
                IconButton(
                    onClick = onEditSave,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save")
                }
                IconButton(
                    onClick = onEditCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        } else {
            Text(
                text = todo.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEditStart() },
                color = if (todo.completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
