package com.project.chatApp.repository;

import com.mongodb.BasicDBObject;
import com.project.chatApp.dataTransferObject.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;

public class UserRepositoryImpl {

    private static final String COLLECTION = "user";

    @Autowired
    private MongoTemplate mongoTemplate;

    public UserDTO getUserDataDTO(String username) {
        // Define the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("username").is(username)),
                Aggregation.lookup("conversation", "conversationIds", "_id", "conversations"),
                Aggregation.unwind("conversations", true),
                Aggregation.project()
                        .andInclude("_id", "username", "profilePicUrl", "conversations")
                        .and("conversations.messageIds").slice(-20).as("lastMessages"),
                Aggregation.lookup("user", "conversations.userIds", "_id", "conversations.members"),
                Aggregation.lookup("message", "lastMessages", "_id", "conversations.messages"),
                Aggregation.group("_id") // Group back to the original user document
                        .first("username").as("username")
                        .first("profilePicUrl").as("profilePicUrl")
                        .push(new BasicDBObject()
                                .append("_id", "$conversations._id")
                                .append("messages", "$conversations.messages")
                                .append("members", new BasicDBObject("$map", new BasicDBObject()
                                        .append("input", "$conversations.members")
                                        .append("as", "members")
                                        .append("in", new BasicDBObject()
                                                .append("_id", "$$members._id")
                                                .append("username", "$$members.username")
                                                .append("profilePicUrl", "$$members.profilePicUrl")))))
                        .as("conversations")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION, Document.class);

        Document rawResults = results.getRawResults();
        List<Document> docs = (List<Document>) rawResults.get("results"); // Get the first result
        Document doc = docs.get(0);

        UserDTO user = new UserDTO();
        user.setId(doc.getObjectId("_id").toHexString());
        user.setUsername(doc.getString("username"));
        user.setProfilePicUrl(doc.getString("profilePicUrl"));

        // Extract conversations
        List<Document> conversationDocs = (List<Document>) doc.get("conversations");
        List<ConversationDTO> conversations = new ArrayList<>();

        if (conversationDocs != null && !conversationDocs.isEmpty()) {
            for (Document conversationDoc : conversationDocs) {
                // if this conversation is empty then continue
                if(!conversationDoc.containsKey("_id")) continue;
                // else convert to conversationDTO
                ConversationDTO conversation = new ConversationDTO();
                conversation.setId(conversationDoc.getObjectId("_id").toHexString());

                // Extract messages
                List<Document> messageDocs = (List<Document>) conversationDoc.get("messages");
                List<MessageDTO> messages = new ArrayList<>();
                if (messageDocs != null && !messageDocs.isEmpty()) {
                    for (Document messageDoc : messageDocs) {
                        MessageDTO message = new MessageDTO();
                        message.setId(messageDoc.getObjectId("_id").toHexString());
                        message.setMessage(messageDoc.getString("message"));
                        message.setStatus(messageDoc.getString("status"));
                        message.setTimestamp(messageDoc.getLong("timestamp"));
                        message.setConversationId(messageDoc.getObjectId("conversationId").toHexString());
                        message.setSenderId(messageDoc.getObjectId("senderId").toHexString());
                        messages.add(message);
                    }
                }
                conversation.setMessages(messages);

                // Extract members
                List<Document> memberDocs = (List<Document>) conversationDoc.get("members");
                List<PublicUserDTO> members = new ArrayList<>();
                if (memberDocs != null && !memberDocs.isEmpty()) {
                    for (Document memberDoc : memberDocs) {
                        if(memberDoc.getString("username").equals(username)) continue;
                        PublicUserDTO member = new PublicUserDTO();
                        member.setId(memberDoc.getObjectId("_id").toHexString());
                        member.setUsername(memberDoc.getString("username"));
                        member.setProfilePicUrl(memberDoc.getString("profilePicUrl"));
                        members.add(member);
                    }
                }
                conversation.setMembers(members);

                conversations.add(conversation);
            }
        }

        user.setConversations(conversations);

        return user;
    }

    // Update documents that match the criteria
    public List<ObjectId> updateMyMessagesOfAllConversationsToReceived(ObjectId userId) {

        Criteria criteria = new Criteria();

        // Define the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("_id").is(userId)),
                Aggregation.lookup("conversation", "conversationIds", "_id","conversations"),
                Aggregation.unwind("conversations", true),
                Aggregation.lookup("message", "conversations.messageIds", "_id", "conversations.messages"),
                Aggregation.project().andInclude("conversations"),
                Aggregation.unwind("conversations.messages", true), // Unwind to process each message
                Aggregation.match(criteria.andOperator(Criteria.where("conversations.messages.status").is("Sent"), Criteria.where("conversations.messages.senderId").ne(userId))),
                Aggregation.group("conversations._id")
                        .push("conversations.messages._id").as("messageIds")

        );

        // Execute the aggregation to get messageIds
        List<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION, Document.class).getMappedResults();

        // Extract conversationIds which have at least one matched massageId
        // Extract message IDs to update
        List<ObjectId> conversationIds = new ArrayList<>();
        List<String> messageIdsToUpdate = new ArrayList<>();
        for (Document result : results) {
            List<String> messageIds = (List<String>) result.get("messageIds");
            if (messageIds.isEmpty()) break;
            conversationIds.add((ObjectId) result.get("_id"));
            messageIdsToUpdate.addAll(messageIds);
        }

        // Update status for all messages found
        if (!messageIdsToUpdate.isEmpty()) {
            mongoTemplate.updateMulti(
                    Query.query(Criteria.where("_id").in(messageIdsToUpdate)),
                    Update.update("status", "Received"),
                    "message"
            );
        }

        return conversationIds;
    }

    public void addConversationIdToUsers(List<ObjectId> userId, ObjectId conversationId) {
        // Create a query to find the conversation by id
        Query query = new Query(Criteria.where("_id").in(userId));

        // Create an update operation to add the new message to the messageIds array
        Update update = new Update().addToSet("conversationIds", conversationId);

        // Perform the update
        mongoTemplate.updateMulti(query, update, COLLECTION);
    }

    public void updateProfilePicUrl(ObjectId userId, String profilePicUrl) {

        // Create a query to find the conversation by id
        Query query = new Query(Criteria.where("_id").in(userId));

        // Create an update operation to add the new message to the messageIds array
        Update update = new Update().set("profilePicUrl", profilePicUrl);

        // Perform the update
        mongoTemplate.updateMulti(query, update, COLLECTION);

    }

}
