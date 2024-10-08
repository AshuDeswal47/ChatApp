package com.project.chatApp.repository;

import com.mongodb.client.result.UpdateResult;
import com.project.chatApp.dataTransferObject.AttachmentDTO;
import com.project.chatApp.dataTransferObject.MessageDTO;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

public class ConversationRepositoryImpl {

    private static final String COLLECTION = "conversation";

    @Autowired
    private MongoTemplate mongoTemplate;

    public boolean addMessageInConversation(ObjectId conversationId, ObjectId messageId) {
        // Create a query to find the conversation by id
        Query query = new Query(Criteria.where("_id").is(conversationId));

        // Create an update operation to add the new message to the messageIds array
        Update update = new Update().addToSet("messageIds", messageId);

        // Perform the update
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, COLLECTION);

        return (updateResult.wasAcknowledged() && updateResult.getMatchedCount()>0 && updateResult.getModifiedCount()>0);
    }

    public List<MessageDTO> getLast20MessageEntitiesBeforeMessageId(ObjectId conversationId, ObjectId topMessageId) {

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("_id").is(conversationId)),
                Aggregation.unwind("messageIds", true),
                Aggregation.match(Criteria.where("messageIds").lt(topMessageId)),
                Aggregation.group().push("messageIds").as("messageIds"),
                Aggregation.project().and("messageIds").slice(-20).as("messageIds"),
                Aggregation.lookup("message", "messageIds", "_id", "messages"),
                Aggregation.project().andInclude("messages"),
                Aggregation.unwind("messages", true),
                Aggregation.lookup("attachment", "messages.attachmentId", "_id", "messages.attachment"),
                Aggregation.group("_id")
                        .push("messages").as("messages")
        );

        // Execute the aggregation to get messageIds
        List<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION, Document.class).getMappedResults();

        return results.stream()
                .flatMap(doc -> ((List<Document>) doc.get("messages")).stream())
                .map(messageDoc -> {
                    MessageDTO message = new MessageDTO();
                    message.setId(messageDoc.getObjectId("_id").toHexString());
                    message.setConversationId(messageDoc.getObjectId("conversationId").toHexString());
                    message.setSenderId(messageDoc.getObjectId("senderId").toHexString());
                    message.setMessage(messageDoc.getString("message"));
                    message.setStatus(messageDoc.getString("status"));
                    message.setTimestamp(messageDoc.getLong("timestamp"));
                    List<Document> attachmentDocs = (List<Document>) messageDoc.get("attachment");
                    if(attachmentDocs != null && !attachmentDocs.isEmpty()) {
                        Document attachmentDoc =  attachmentDocs.get(0);
                        AttachmentDTO attachment = new AttachmentDTO();
                        attachment.setId(attachmentDoc.getObjectId("_id").toHexString());
                        attachment.setUrl(attachmentDoc.getString("url"));
                        attachment.setContentType(attachmentDoc.getString("contentType"));
                        attachment.setSize(attachmentDoc.getLong("size"));
                        message.setAttachment(attachment);
                    }
                    return message;
                }).toList();
    }

    // Update documents that match the criteria
    public boolean updateMyMessagesOfConversationToViewed(ObjectId conversationId, ObjectId userId) {

        Criteria criteria = new Criteria();

        // Define the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("_id").is(conversationId)),
                Aggregation.lookup("message", "messageIds", "_id", "messages"),
                Aggregation.project().andInclude("messages"),
                Aggregation.unwind("messages", true), // Unwind to process each message
                Aggregation.match(criteria.andOperator(Criteria.where("messages.status").in("Sent", "Received"), Criteria.where("messages.senderId").ne(userId))),
                Aggregation.group()
                        .push("messages._id").as("messageIds")

        );

        // Execute the aggregation to get messageIds
        List<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION, Document.class).getMappedResults();

        // Extract message IDs to update
        List<String> messageIdsToUpdate = results.stream()
                .flatMap(doc -> ((List<String>) doc.get("messageIds")).stream()).toList();

        // Update status for all messages found
        UpdateResult updateResult = null;
        if (!messageIdsToUpdate.isEmpty()) {
            updateResult = mongoTemplate.updateMulti(
                    Query.query(Criteria.where("_id").in(messageIdsToUpdate)),
                    Update.update("status", "Viewed"),
                    "message"
            );
        }

        return !(updateResult == null || (updateResult.wasAcknowledged() && updateResult.getMatchedCount()>0 && updateResult.getModifiedCount()>0));
    }

}
