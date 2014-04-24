package com.sforce.contrib.partner;

public class CaseComment extends SObjectType {
    private Field<String> commentBody;
    private Field<String> connectionReceivedId;
    private Field<String> connectionSentId;
    private Field<String> creatorFullPhotoUrl;
    private Field<String> creatorName;
    private Field<String> creatorSmallPhotoUrl;
    private Field<Boolean> isDeleted;
    private Field<Boolean> isPublished;
    private Field<String> parentId;

    public void init(Package pkg) {
        super.init(pkg, "CaseComment");
        this.commentBody = new Field<String>(this, "commentBody", "CommentBody", FieldType.TEXT_AREA, 4000, null, null, false, false);
        addField(commentBody);
        this.connectionReceivedId = new Field<String>(this, "connectionReceivedId", "ConnectionReceivedId", FieldType.LOOKUP, null, null, null, false, false);
        addField(connectionReceivedId);
        this.connectionSentId = new Field<String>(this, "connectionSentId", "ConnectionSentId", FieldType.LOOKUP, null, null, null, false, false);
        addField(connectionSentId);
        this.creatorFullPhotoUrl = new Field<String>(this, "creatorFullPhotoUrl", "CreatorFullPhotoUrl", FieldType.URL, null, null, null, false, false);
        addField(creatorFullPhotoUrl);
        this.creatorName = new Field<String>(this, "creatorName", "CreatorName", FieldType.TEXT, null, null, null, false, false);
        addField(creatorName);
        this.creatorSmallPhotoUrl = new Field<String>(this, "creatorSmallPhotoUrl", "CreatorSmallPhotoUrl", FieldType.URL, null, null, null, false, false);
        addField(creatorSmallPhotoUrl);
        this.isDeleted = new Field<Boolean>(this, "isDeleted", "IsDeleted", FieldType.CHECKBOX, null, null, null, false, false);
        addField(isDeleted);
        this.isPublished = new Field<Boolean>(this, "isPublished", "IsPublished", FieldType.CHECKBOX, null, null, null, false, false);
        addField(isPublished);
        this.parentId = new Field<String>(this, "parentId", "ParentId", FieldType.LOOKUP, null, null, null, false, false);
        addField(parentId);
    }

    public Field<String> commentBody() {
        return commentBody;
    }

    public Field<String> connectionReceivedId() {
        return connectionReceivedId;
    }

    public Field<String> connectionSentId() {
        return connectionSentId;
    }

    public Field<String> creatorFullPhotoUrl() {
        return creatorFullPhotoUrl;
    }

    public Field<String> creatorName() {
        return creatorName;
    }

    public Field<String> creatorSmallPhotoUrl() {
        return creatorSmallPhotoUrl;
    }

    public Field<Boolean> isDeleted() {
        return isDeleted;
    }

    public Field<Boolean> isPublished() {
        return isPublished;
    }

    public Field<String> parentId() {
        return parentId;
    }
}
