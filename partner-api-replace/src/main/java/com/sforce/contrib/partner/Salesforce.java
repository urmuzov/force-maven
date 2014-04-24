package com.sforce.contrib.partner;

/**
 * User: urmuzov
 * Date: 17.04.14
 * Time: 20:58
 */
public class Salesforce extends Package {

    public static final Salesforce INSTANCE = new Salesforce();

    private RecordType RecordType;
    private CaseComment CaseComment;

    public Salesforce() {
        this.RecordType = new RecordType();
        this.CaseComment = new CaseComment();

        this.RecordType.init(this);
        this.CaseComment.init(this);

        add(this.RecordType);
        add(this.CaseComment);
    }

    public RecordType RecordType() {
        return this.RecordType;
    }
    public CaseComment CaseComment() {
        return this.CaseComment;
    }
}
