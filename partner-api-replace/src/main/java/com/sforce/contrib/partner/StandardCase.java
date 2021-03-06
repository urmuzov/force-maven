package com.sforce.contrib.partner;

import org.joda.time.DateTime;

public class StandardCase extends SObjectType {
    private Field<String> account;
    private Field<String> asset;
    private Field<String> businessHours;
    private Field<String> caseNumber;
    private Field<String> origin;
    private Field<String> owner;
    private Field<String> reason;
    private Field<Boolean> isClosedOnCreate;
    private Field<String> contactEmail;
    private Field<String> contactFax;
    private Field<String> contactMobile;
    private Field<String> contact;
    private Field<String> contactPhone;
    private Field<DateTime> closedDate;
    private Field<DateTime> createdDate;
    private Field<String> description;
    private Field<Boolean> isEscalated;
    private Field<String> comments;
    private Field<String> parent;
    private Field<String> priority;
    private Field<String> status;
    private Field<String> subject;
    private Field<String> type;
    private Field<String> suppliedCompany;
    private Field<String> suppliedEmail;
    private Field<String> suppliedName;
    private Field<String> suppliedPhone;
    private Field<String> recordTypeId;

    public void init(Package pkg, String sfName, String label, String pluralLabel) {
        super.init(pkg, sfName, label, pluralLabel);
        this.account = new Field<String>(this, "account", "Account", "Account", FieldType.LOOKUP, null, null, null, false, false);
        addField(account);
        this.asset = new Field<String>(this, "asset", "Asset", "Asset", FieldType.LOOKUP, null, null, null, false, false);
        addField(asset);
        this.businessHours = new Field<String>(this, "businessHours", "BusinessHours", "Business Hours", FieldType.LOOKUP, null, null, null, false, false);
        addField(businessHours);
        this.caseNumber = new Field<String>(this, "caseNumber", "CaseNumber", "Case Number", FieldType.NUMBER, null, null, null, false, false);
        addField(caseNumber);
        this.origin = new Field<String>(this, "origin", "Origin", "Origin", FieldType.PICKLIST, null, null, null, false, false);
        addField(origin);
        this.owner = new Field<String>(this, "owner", "Owner", "Owner", FieldType.LOOKUP, null, null, null, false, false);
        addField(owner);
        this.reason = new Field<String>(this, "reason", "Reason", "Reason", FieldType.PICKLIST, null, null, null, false, false);
        addField(reason);
        this.isClosedOnCreate = new Field<Boolean>(this, "isClosedOnCreate", "IsClosedOnCreate", "Is Closed on Create", FieldType.CHECKBOX, null, null, null, false, false);
        addField(isClosedOnCreate);
        this.contactEmail = new Field<String>(this, "contactEmail", "ContactEmail", "Contact Email", FieldType.EMAIL, null, null, null, false, false);
        addField(contactEmail);
        this.contactFax = new Field<String>(this, "contactFax", "ContactFax", "Contact Fax", FieldType.PHONE, null, null, null, false, false);
        addField(contactFax);
        this.contactMobile = new Field<String>(this, "contactMobile", "ContactMobile", "Contact Mobile", FieldType.PHONE, null, null, null, false, false);
        addField(contactMobile);
        this.contact = new Field<String>(this, "contact", "Contact", "Contact", FieldType.LOOKUP, null, null, null, false, false);
        addField(contact);
        this.contactPhone = new Field<String>(this, "contactPhone", "ContactPhone", "Contact Phone", FieldType.PHONE, null, null, null, false, false);
        addField(contactPhone);
        this.closedDate = new Field<DateTime>(this, "closedDate", "ClosedDate", "Closed Date", FieldType.DATE_TIME, null, null, null, false, false);
        addField(closedDate);
        this.createdDate = new Field<DateTime>(this, "createdDate", "CreatedDate", "Created Date", FieldType.DATE_TIME, null, null, null, false, false);
        addField(createdDate);
        this.description = new Field<String>(this, "description", "Description", "Description", FieldType.LONG_TEXT_AREA, 32000, null, null, false, false);
        addField(description);
        this.isEscalated = new Field<Boolean>(this, "isEscalated", "IsEscalated", "Is Escalated", FieldType.CHECKBOX, null, null, null, false, false);
        addField(isEscalated);
        this.comments = new Field<String>(this, "comments", "Comments", "Comments", FieldType.TEXT_AREA, 4000, null, null, false, false);
        addField(comments);
        this.parent = new Field<String>(this, "parent", "Parent", "Parent", FieldType.LOOKUP, null, null, null, false, false);
        addField(parent);
        this.priority = new Field<String>(this, "priority", "Priority", "Priority", FieldType.PICKLIST, null, null, null, false, false);
        addField(priority);
        this.status = new Field<String>(this, "status", "Status", "Status", FieldType.PICKLIST, null, null, null, false, false);
        addField(status);
        this.subject = new Field<String>(this, "subject", "Subject", "Subject", FieldType.TEXT, 255, null, null, false, false);
        addField(subject);
        this.type = new Field<String>(this, "type", "Type", "Type", FieldType.PICKLIST, null, null, null, false, false);
        addField(type);
        this.suppliedCompany = new Field<String>(this, "suppliedCompany", "SuppliedCompany", "Supplied Company", FieldType.TEXT, 80, null, null, false, false);
        addField(suppliedCompany);
        this.suppliedEmail = new Field<String>(this, "suppliedEmail", "SuppliedEmail", "Supplied Email", FieldType.EMAIL, null, null, null, false, false);
        addField(suppliedEmail);
        this.suppliedName = new Field<String>(this, "suppliedName", "SuppliedName", "Supplied Name", FieldType.TEXT, 80, null, null, false, false);
        addField(suppliedName);
        this.suppliedPhone = new Field<String>(this, "suppliedPhone", "SuppliedPhone", "Supplied Phone", FieldType.TEXT, 40, null, null, false, false);
        addField(suppliedPhone);
        this.recordTypeId = new Field<String>(this, "recordTypeId", "RecordTypeId", "Record Type Id", FieldType.LOOKUP, null, null, null, false, false);
        addField(recordTypeId);
    }

    public Field<Boolean> isEscalated() {
        return isEscalated;
    }

    public Field<String> account() {
        return account;
    }

    public Field<String> asset() {
        return asset;
    }

    public Field<String> businessHours() {
        return businessHours;
    }

    public Field<String> caseNumber() {
        return caseNumber;
    }

    public Field<String> origin() {
        return origin;
    }

    public Field<String> owner() {
        return owner;
    }

    public Field<String> reason() {
        return reason;
    }

    public Field<Boolean> isClosedOnCreate() {
        return isClosedOnCreate;
    }

    public Field<String> contactEmail() {
        return contactEmail;
    }

    public Field<String> contactFax() {
        return contactFax;
    }

    public Field<String> contactMobile() {
        return contactMobile;
    }

    public Field<String> contact() {
        return contact;
    }

    public Field<String> contactPhone() {
        return contactPhone;
    }

    public Field<DateTime> closedDate() {
        return closedDate;
    }

    public Field<DateTime> createdDate() {
        return createdDate;
    }

    public Field<String> description() {
        return description;
    }

    public Field<String> comments() {
        return comments;
    }

    public Field<String> parent() {
        return parent;
    }

    public Field<String> priority() {
        return priority;
    }

    public Field<String> status() {
        return status;
    }

    public Field<String> subject() {
        return subject;
    }

    public Field<String> type() {
        return type;
    }

    public Field<String> suppliedCompany() {
        return suppliedCompany;
    }

    public Field<String> suppliedEmail() {
        return suppliedEmail;
    }

    public Field<String> suppliedName() {
        return suppliedName;
    }

    public Field<String> suppliedPhone() {
        return suppliedPhone;
    }

    public Field<String> recordTypeId() {
        return recordTypeId;
    }
}
