package com.sforce.contrib.connection;

import com.google.common.collect.Lists;
import com.sforce.contrib.partner.Context;
import com.sforce.contrib.partner.SObject;
import com.sforce.ws.ConnectionException;
import junit.framework.TestCase;

import java.util.List;
import java.util.UUID;

/**
 * User: urmuzov
 * Date: 15-05-08
 * Time: 23:13
 */
public class ReplacerTest extends TestCase {
    public void test1() throws ConnectionException {
        doTest(23, 23, 23, 0, 0);
    }

    public void test2() throws ConnectionException {
        doTest(20, 23, 20, 3, 0);
    }

    public void test3() throws ConnectionException {
        doTest(30, 23, 23, 0, 7);
    }

    public void test4() throws ConnectionException {
        doTest(230, 230, 230, 0, 0);
    }

    public void test5() throws ConnectionException {
        doTest(200, 230, 200, 30, 0);
    }

    public void test6() throws ConnectionException {
        doTest(300, 230, 230, 0, 70);
    }

    public void test7() throws ConnectionException {
        doTest(9423, 9423, 9423, 0, 0);
    }

    public void test8() throws ConnectionException {
        doTest(9400, 9423, 9400, 23, 0);
    }

    public void test9() throws ConnectionException {
        doTest(9500, 9423, 9423, 0, 77);
    }

    void doTest(int dbCount, int insertCount, int updates, int inserts, int deletes) throws ConnectionException {
        ConnectionMock c = new ConnectionMock(new Context());
        List<SObject> inDb = Lists.newArrayList();
        for (int i = 0; i < dbCount; i++) {
            inDb.add(new SObject("Test__c").put("id", UUID.randomUUID().toString()));
        }
        Soql q = Soql.from("Test__c").select("id");
        c.addTestQueryResult(q, inDb);

        List<SObject> test1 = Lists.newArrayList();
        for (int i = 0; i < insertCount; i++) {
            test1.add(new SObject("Test__c"));
        }
        Replacer r = new Replacer(c, q);
        for (SObject o : test1) {
            r.updateOrInsert(o);
        }
        r.deleteUnused();
        assertEquals(updates, c.getUpdated().size());
        assertEquals(inserts, c.getCreated().size());
        assertEquals(0, c.getUpserted().size());
        assertEquals(deletes, c.getDeleted().size());
    }
}