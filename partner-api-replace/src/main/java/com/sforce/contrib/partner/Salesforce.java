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
    private User User;

    public Salesforce() {
        this.RecordType = new RecordType();
        this.CaseComment = new CaseComment();
        this.User = new User();

        this.RecordType.init(this);
        this.CaseComment.init(this);
        this.User.init(this);

        add(this.RecordType);
        add(this.CaseComment);
        add(this.User);
    }

    public RecordType RecordType() {
        return this.RecordType;
    }
    public CaseComment CaseComment() {
        return this.CaseComment;
    }
    public User User() {
        return this.User;
    }
}
