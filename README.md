# Chat Application

This chat application is a real-time messaging platform built with Spring Boot and WebSockets, allowing users to communicate instantly, search for other users, and share files securely.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [API Endpoints](#api-endpoints)
- [WebSocket Functionality](#websocket-functionality)
  - [Messaging WebSocket](#messaging-websocket)
  - [Search WebSocket](#search-websocket)

## Features

- **Real-time Messaging:** Users can send and receive messages instantly.
- **User Search:** Real-time searching for users through WebSocket.
- **File Sharing:** Users can share images, videos, and documents.
- **Conversation Management:** Users can create conversations and retrieve messages.
- **Status Tracking:** Automatically set message statuses to "Received" upon connection.

## Tech Stack

- **Backend:** Spring Boot
- **Database:** MongoDB
- **WebSocket:** Spring WebSocket
- **Authentication:** JWT (JSON Web Tokens)

## API Endpoints

### 1. PublicController (No Authentication Required)

This controller handles user authentication and registration.

#### POST /public/login:
Authenticates a user and returns a JWT token.

**Request:**
```json
{
  "username": "user123",
  "password": "password"
}
```

**Response:**
- 200 OK: JWT token
- 400 BAD REQUEST: Error message

#### POST /public/signup:
Registers a new user and automatically logs them in.

**Request:**
```json
{
  "username": "user123",
  "password": "password"
}
```

**Response:**
- 200 OK: JWT token
- 400 BAD REQUEST: Error message

### 2. UserController (Authentication Required)
This controller allows users to manage their profile.

#### POST /user/uploadProfilePic:
Uploads a profile picture for the user.

**Request:**
- Multipart file upload (profilePic).

**Response:**
- 200 OK: "Profile picture uploaded successfully."
- 400 BAD REQUEST: "Please select a valid image file."

### 3. ConversationController (Authentication Required)
This controller handles the creation of new conversations between users.

#### POST /conversation/create:
Creates a new conversation with another user.

**Request:**
```json
"username": "otherUser123"
```

**Response:**
- 201 CREATED: Conversation data
- 400 BAD REQUEST: Error message

### 4. MessageController (Authentication Required)
This controller allows users to send files in chat messages.

#### POST /message/uploadFile:
Uploads a file (image, video, etc.) to be sent as part of a message.

**Request:**
- Multipart file (file)
- Message data in JSON (message)
  
**Response:**
- 200 OK: "File uploaded successfully."
- 400 BAD REQUEST: "Please select a file to upload."

## WebSocket Functionality

### Messaging WebSocket

- **Endpoint:** `/ws/message`
- **Functionality:**
  - On user connection, all new messages are marked as **"Received."**
  - The server sends the user's profile information, all conversation details, and the top 20 messages for each conversation.

#### Accepted WebSocket Data Types
The WebSocket can handle three types of incoming JSON string data. Each type of data serves a specific purpose in the real-time chat system.

**1. Sending a New Message:**

This is used when a user wants to send a new message in a conversation. The WebSocket accepts the following JSON format:
```json
{
  "type": "message",
  "message": "{\"conversationId\":\"<conversationId>\", \"message\":\"<message text>\", \"senderId\":\"<senderId>\", \"status\":\"Sending\", \"timestamp\":<timestamp>}"
}
```
**Example:**
```json
{
  "type": "message",
  "message": "{\"conversationId\":\"66eb9557b0a04e05ca4a77f9\",\"message\":\"hello\",\"senderId\":\"66eb9533b0a04e05ca4a77f7\",\"status\":\"Sending\",\"timestamp\":1727924029031}"
}
```
- conversationId: The ID of the conversation in which the message is being sent.
- message: The actual message content.
- senderId: The ID of the user sending the message.
- status: Indicates the status of the message ("Sending").
- timestamp: The timestamp of when the message was sent.

**2. Status Update for Message:**

This is used to update the status of messages (for example, when the recipient has viewed the message). The JSON structure for this update is:
```json
{
  "type": "status-update",
  "conversationId": "<conversationId>"
}
```
**Example:**
```json
{
  "type": "status-update",
  "conversationId": "66eb9557b0a04e05ca4a77f9"
}
```

**3. Requesting Previous Messages:**

This is used when a user wants to load older messages in a conversation. The WebSocket accepts the following JSON format to retrieve the last 20 messages before a specific message:
```json
{
  "type": "get-messages",
  "conversationId": "<conversationId>",
  "topMessageId": "<topMessageId>"
}

```
**Example:**
```json
{
  "type": "get-messages",
  "conversationId": "66eb9557b0a04e05ca4a77f9",
  "topMessageId": "66eb9569b0a04e05ca4a77fa"
}
```

### Search WebSocket

- **Endpoint:** `/ws/search`
- **Functionality:**
  - Allows users to search for other users in real time.
  
**Search Request**
- Input: Text search query.
- Output: List of users matching the search query.

**Example Search Result**
  
The result returned after searching:  
```json
[
    {
        "id": "userId1",
        "username": "john_doe",
        "profilePic": "url/to/profile-pic.jpg"
    },
    {
        "id": "userId2",
        "username": "jane_doe",
        "profilePic": "url/to/profile-pic2.jpg"
    }
]
```
