use matrix_sdk::{
    Client as SdkClient,
    ruma::{
        EventId, OwnedEventId, OwnedRoomId, RoomId, UserId,
        events::{
            AnyMessageLikeEventContent, AnySyncTimelineEvent,
            room::message::RoomMessageEventContent,
        },
        serde::Raw,
    },
};
use serde_json;
use std::sync::Arc;

use crate::{
    TodoDelete, TodoEdit, TodoItem, TodoListMetadata, TodoRoom, TodoToggle, FfiError,
};

/// Custom event types for todo lists
const TODO_LIST_METADATA: &str = "custom.todo.list";
const TODO_ITEM: &str = "custom.todo.item";
const TODO_TOGGLE: &str = "custom.todo.toggle";
const TODO_DELETE: &str = "custom.todo.delete";
const TODO_EDIT: &str = "custom.todo.edit";

/// Generate a unique todo ID
fn generate_todo_id() -> String {
    use std::time::{SystemTime, UNIX_EPOCH};
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis();
    format!("todo_{}_{}", timestamp, rand::random::<u64>())
}

/// Create a new todo list room with metadata
pub async fn create_todo_room(
    sdk: &SdkClient,
    name: String,
    description: Option<String>,
    user_id: &UserId,
) -> Result<String, FfiError> {
    let room = sdk.create_room(name.clone()).await?;
    let room_id = room.room_id().to_string();

    // Set todo list metadata as room state
    let metadata = TodoListMetadata {
        name: name.clone(),
        description: description.clone(),
        created_at_ms: std::time::SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as u64,
        created_by: user_id.to_string(),
        color: None,
        is_public: false,
    };

    // Send metadata as a state event
    let metadata_event = serde_json::json!({
        "type": TODO_LIST_METADATA,
        "state_key": "",
        "content": metadata,
    });

    room.send_raw(
        AnyMessageLikeEventContent::RoomMessage(RoomMessageEventContent::text_plain(
            metadata_event.to_string(),
        )),
        None,
    )
    .await?;

    Ok(room_id)
}

/// Get todo list metadata from room state
pub async fn get_todo_list_metadata(
    sdk: &SdkClient,
    room_id: &str,
) -> Result<Option<TodoListMetadata>, FfiError> {
    let room_id = OwnedRoomId::try_from(room_id)?;
    let room = sdk.get_room(&room_id).ok_or(FfiError::Msg("Room not found".into()))?;

    // Try to get the metadata from room state
    if let Some(state_event) = room.get_state_event_static::<Raw>().await.ok().flatten() {
        if let Ok(event) = state_event.deserialize() {
            if event.event_type.to_string() == TODO_LIST_METADATA {
                if let Ok(metadata) = serde_json::from_str::<TodoListMetadata>(&event.content) {
                    return Ok(Some(metadata));
                }
            }
        }
    }

    Ok(None)
}

/// Add a new todo item to a room
pub async fn add_todo_item(
    sdk: &SdkClient,
    room_id: &str,
    text: String,
    user_id: &UserId,
) -> Result<TodoItem, FfiError> {
    let room_id = OwnedRoomId::try_from(room_id)?;
    let room = sdk.get_room(&room_id).ok_or(FfiError::Msg("Room not found".into()))?;

    let item_id = generate_todo_id();
    let timestamp = std::time::SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64;

    let todo_item = TodoItem {
        id: item_id.clone(),
        text: text.clone(),
        completed: false,
        created_at_ms: timestamp,
        created_by: user_id.to_string(),
        due_date: None,
        priority: None,
        tags: None,
    };

    let event = serde_json::json!({
        "type": TODO_ITEM,
        "content": todo_item,
    });

    let _event_id = room.send_raw(
        AnyMessageLikeEventContent::RoomMessage(RoomMessageEventContent::text_plain(
            event.to_string(),
        )),
        None,
    )
    .await?;

    Ok(todo_item)
}

/// Toggle todo item completion status
pub async fn toggle_todo_item(
    sdk: &SdkClient,
    room_id: &str,
    item_id: String,
    completed: bool,
    user_id: &UserId,
    original_event_id: Option<String>,
) -> Result<(), FfiError> {
    let room_id = OwnedRoomId::try_from(room_id)?;
    let room = sdk.get_room(&room_id).ok_or(FfiError::Msg("Room not found".into()))?;

    let timestamp = std::time::SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64;

    let toggle = TodoToggle {
        item_id: item_id.clone(),
        completed,
        toggled_at_ms: timestamp,
        toggled_by: user_id.to_string(),
    };

    let mut event = serde_json::json!({
        "type": TODO_TOGGLE,
        "content": toggle,
    });

    if let Some(event_id) = original_event_id {
        event["relates_to"] = serde_json::json!({
            "event_id": event_id,
            "rel_type": "m.reference",
        });
    }

    room.send_raw(
        AnyMessageLikeEventContent::RoomMessage(RoomMessageEventContent::text_plain(
            event.to_string(),
        )),
        None,
    )
    .await?;

    Ok(())
}

/// Delete a todo item
pub async fn delete_todo_item(
    sdk: &SdkClient,
    room_id: &str,
    item_id: String,
    user_id: &UserId,
    original_event_id: Option<String>,
) -> Result<(), FfiError> {
    let room_id = OwnedRoomId::try_from(room_id)?;
    let room = sdk.get_room(&room_id).ok_or(FfiError::Msg("Room not found".into()))?;

    let timestamp = std::time::SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64;

    let delete_action = TodoDelete {
        item_id: item_id.clone(),
        deleted_at_ms: timestamp,
        deleted_by: user_id.to_string(),
    };

    let mut event = serde_json::json!({
        "type": TODO_DELETE,
        "content": delete_action,
    });

    if let Some(event_id) = original_event_id {
        event["relates_to"] = serde_json::json!({
            "event_id": event_id,
            "rel_type": "m.reference",
        });
    }

    room.send_raw(
        AnyMessageLikeEventContent::RoomMessage(RoomMessageEventContent::text_plain(
            event.to_string(),
        )),
        None,
    )
    .await?;

    Ok(())
}

/// Edit a todo item
pub async fn edit_todo_item(
    sdk: &SdkClient,
    room_id: &str,
    item_id: String,
    new_text: String,
    user_id: &UserId,
    original_event_id: Option<String>,
) -> Result<(), FfiError> {
    let room_id = OwnedRoomId::try_from(room_id)?;
    let room = sdk.get_room(&room_id).ok_or(FfiError::Msg("Room not found".into()))?;

    let timestamp = std::time::SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64;

    let edit = TodoEdit {
        item_id: item_id.clone(),
        text: new_text.clone(),
        edited_at_ms: timestamp,
        edited_by: user_id.to_string(),
    };

    let mut event = serde_json::json!({
        "type": TODO_EDIT,
        "content": edit,
    });

    if let Some(event_id) = original_event_id {
        event["relates_to"] = serde_json::json!({
            "event_id": event_id,
            "rel_type": "m.replace",
        });
    }

    room.send_raw(
        AnyMessageLikeEventContent::RoomMessage(RoomMessageEventContent::text_plain(
            event.to_string(),
        )),
        None,
    )
    .await?;

    Ok(())
}

/// Load all todo items from a room's timeline
pub async fn load_todo_items(
    sdk: &SdkClient,
    room_id: &str,
) -> Result<Vec<TodoItem>, FfiError> {
    let room_id = OwnedRoomId::try_from(room_id)?;
    let room = sdk.get_room(&room_id).ok_or(FfiError::Msg("Room not found".into()))?;

    let timeline = room.timeline().await?;
    let events = timeline.events().await?;

    let mut items: std::collections::HashMap<String, TodoItem> = std::collections::HashMap::new();
    let mut deleted_items: std::collections::HashSet<String> = std::collections::HashSet::new();
    let mut edited_items: std::collections::HashMap<String, String> = std::collections::HashMap::new();
    let mut toggled_items: std::collections::HashMap<String, bool> = std::collections::HashMap::new();

    for event in events {
        if let AnySyncTimelineEvent::MessageLike(message) = event {
            if let Ok(json_value) = serde_json::to_value(&message.content) {
                if let Some(event_type) = json_value.get("type").and_then(|v| v.as_str()) {
                    match event_type {
                        TODO_ITEM => {
                            if let Ok(content) = serde_json::from_value::<TodoItem>(json_value["content"].clone()) {
                                items.insert(content.id.clone(), content);
                            }
                        }
                        TODO_TOGGLE => {
                            if let Ok(content) = serde_json::from_value::<TodoToggle>(json_value["content"].clone()) {
                                toggled_items.insert(content.item_id.clone(), content.completed);
                            }
                        }
                        TODO_DELETE => {
                            if let Ok(content) = serde_json::from_value::<TodoDelete>(json_value["content"].clone()) {
                                deleted_items.insert(content.item_id.clone());
                            }
                        }
                        TODO_EDIT => {
                            if let Ok(content) = serde_json::from_value::<TodoEdit>(json_value["content"].clone()) {
                                edited_items.insert(content.item_id.clone(), content.text.clone());
                            }
                        }
                        _ => {}
                    }
                }
            }
        }
    }

    let mut final_items: Vec<TodoItem> = items
        .into_values()
        .filter(|item| !deleted_items.contains(&item.id))
        .map(|mut item| {
            if let Some(text) = edited_items.get(&item.id) {
                item.text = text.clone();
            }
            if let Some(&completed) = toggled_items.get(&item.id) {
                item.completed = completed;
            }
            item
        })
        .collect();

    // Sort by creation time
    final_items.sort_by(|a, b| a.created_at_ms.cmp(&b.created_at_ms));

    Ok(final_items)
}

/// Load a todo room with its metadata and items
pub async fn load_todo_room(
    sdk: &SdkClient,
    room_id: &str,
) -> Result<TodoRoom, FfiError> {
    let room_id_owned = OwnedRoomId::try_from(room_id)?;
    let room = sdk.get_room(&room_id_owned).ok_or(FfiError::Msg("Room not found".into()))?;

    let metadata = get_todo_list_metadata(sdk, room_id).await?;
    let todos = load_todo_items(sdk, room_id).await?;

    let room_name = room.display_name().await.unwrap_or_else(|| room_id.to_string());

    Ok(TodoRoom {
        room_id: room_id.to_string(),
        room_name,
        metadata,
        todos,
    })
}

/// Get all joined rooms that are todo list rooms
pub async fn get_todo_rooms(
    sdk: &SdkClient,
) -> Result<Vec<TodoRoom>, FfiError> {
    let joined_rooms = sdk.joined_rooms().await?;
    let mut todo_rooms = Vec::new();

    for room in joined_rooms {
        let room_id = room.room_id().to_string();
        
        // Check if this room has todo list metadata
        if let Ok(Some(_metadata)) = get_todo_list_metadata(sdk, &room_id).await {
            if let Ok(todo_room) = load_todo_room(sdk, &room_id).await {
                todo_rooms.push(todo_room);
            }
        }
    }

    Ok(todo_rooms)
}
