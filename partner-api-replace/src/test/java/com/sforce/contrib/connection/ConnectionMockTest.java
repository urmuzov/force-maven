package com.sforce.contrib.connection;

import com.google.common.collect.Lists;
import com.sforce.contrib.partner.Context;
import com.sforce.contrib.partner.SObject;
import junit.framework.TestCase;

import java.util.List;
import java.util.UUID;

/**
 * User: urmuzov
 * Date: 15-05-08
 * Time: 22:55
 */
public class ConnectionMockTest extends TestCase {

    public void testQuery() throws Exception {
        ConnectionMock c = new ConnectionMock(new Context());
        List<SObject> objects = Lists.newArrayList();
        for (int i = 0; i < 9432; i++) {
            objects.add(new SObject("Test__c").put("id", UUID.randomUUID().toString()));
        }
        Soql q = Soql.from("Test__c").select("id");
        c.addTestQueryResult(q, objects);

        List<SObject> query = c.query(q);
        assertEquals(objects.size(), query.size());
    }

}