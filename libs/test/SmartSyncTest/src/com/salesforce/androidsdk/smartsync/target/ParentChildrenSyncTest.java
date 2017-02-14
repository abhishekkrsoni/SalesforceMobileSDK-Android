package com.salesforce.androidsdk.smartsync.target;

/*
 * Copyright (c) 2017-present, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import com.salesforce.androidsdk.smartstore.store.IndexSpec;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.manager.SyncManagerTestCase;
import com.salesforce.androidsdk.smartsync.util.ChildrenInfo;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.smartsync.util.ParentInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Test class for ParentChildrenSyncDownTarget
 *
 */
public class ParentChildrenSyncTest extends SyncManagerTestCase {

    private static final String CONTACTS_SOUP = "contacts";
    private static final String ACCOUNT_ID = "AccountId";
    private static final String ACCOUNT_LOCAL_ID = "AccountLocalId";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createAccountsSoup();
        createContactsSoup();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        dropContactsSoup();
        dropAccountsSoup();
    }

    /**
     * Test getQuery for ParentChildrenSyncDownTarget
     */
    public void testGetQueryForParentChildrenSyncDownTarget() {
        ParentChildrenSyncDownTarget target = new ParentChildrenSyncDownTarget(
                new ParentInfo("Parent", "ParentId", "ParentModifiedDate"),
                Arrays.asList("ParentName", "Title"),
                "School = 'MIT'",
                new ChildrenInfo("Child", "Children", "ChildId", "ChildLastModifiedDate", "childrenSoup", "parentId", "parentLocalId"),
                Arrays.asList("ChildName", "School"),
                ParentChildrenSyncDownTarget.RelationshipType.LOOKUP);

        assertEquals("select ParentName, Title, ParentId, ParentModifiedDate, (select ChildName, School, ChildId, ChildLastModifiedDate from Children) from Parent where School = 'MIT'", target.getQuery());

        // With default id and modification date fields
        target = new ParentChildrenSyncDownTarget(
                new ParentInfo("Parent"),
                Arrays.asList("ParentName", "Title"),
                "School = 'MIT'",
                new ChildrenInfo("Child", "Children", "childrenSoup", "parentId", "parentLocalId"),
                Arrays.asList("ChildName", "School"),
                ParentChildrenSyncDownTarget.RelationshipType.LOOKUP);


        assertEquals("select ParentName, Title, Id, LastModifiedDate, (select ChildName, School, Id, LastModifiedDate from Children) from Parent where School = 'MIT'", target.getQuery());
    }

    /**
     * Test getSoqlForRemoteIds for ParentChildrenSyncDownTarget
     */
    public void testGetSoqlForRemoteIdsForParentChildrenSyncDownTarget() {
        ParentChildrenSyncDownTarget target = new ParentChildrenSyncDownTarget(
                new ParentInfo("Parent", "ParentId", "ParentModifiedDate"),
                Arrays.asList("ParentName", "Title"),
                "School = 'MIT'",
                new ChildrenInfo("Child", "Children", "ChildId", "ChildLastModifiedDate", "childrenSoup", "parentId", "parentLocalId"),
                Arrays.asList("ChildName", "School"),
                ParentChildrenSyncDownTarget.RelationshipType.LOOKUP);

        assertEquals("select ParentId, (select ChildId from Children) from Parent where School = 'MIT'", target.getSoqlForRemoteIds());

        // With default id and modification date fields
        target = new ParentChildrenSyncDownTarget(
                new ParentInfo("Parent"),
                Arrays.asList("ParentName", "Title"),
                "School = 'MIT'",
                new ChildrenInfo("Child", "Children", "childrenSoup", "parentId", "parentLocalId"),
                Arrays.asList("ChildName", "School"),
                ParentChildrenSyncDownTarget.RelationshipType.LOOKUP);

        assertEquals("select Id, (select Id from Children) from Parent where School = 'MIT'", target.getSoqlForRemoteIds());
    }

    /**
     * Test getDirtyRecordIdsSql for ParentChildrenSyncDownTarget
     */
    public void testGetDirtyRecordIdsSqlForParentChildrenSyncDownTarget() {
        ParentChildrenSyncDownTarget target = new ParentChildrenSyncDownTarget(
                new ParentInfo("Parent", "ParentId", "ParentModifiedDate"),
                Arrays.asList("ParentName", "Title"),
                "School = 'MIT'",
                new ChildrenInfo("Child", "Children", "ChildId", "ChildLastModifiedDate", "childrenSoup", "parentId", "parentLocalId"),
                Arrays.asList("ChildName", "School"),
                ParentChildrenSyncDownTarget.RelationshipType.LOOKUP);

        assertEquals("SELECT DISTINCT {ParentSoup:IdForQuery} FROM {childrenSoup},{ParentSoup} WHERE {childrenSoup:parentLocalId} = {ParentSoup:_soupEntryId} AND ({ParentSoup:__local__} = 'true' OR {childrenSoup:__local__} = 'true')", target.getDirtyRecordIdsSql("ParentSoup", "IdForQuery"));
    }


    /**
     * Test getNonDirtyRecordIdsSql for ParentChildrenSyncDownTarget
     */
    public void testGetNonDirtyRecordIdsSqlForParentChildrenSyncDownTarget() {
        ParentChildrenSyncDownTarget target = new ParentChildrenSyncDownTarget(
                new ParentInfo("Parent", "ParentId", "ParentModifiedDate"),
                Arrays.asList("ParentName", "Title"),
                "School = 'MIT'",
                new ChildrenInfo("Child", "Children", "ChildId", "ChildLastModifiedDate", "childrenSoup", "parentId", "parentLocalId"),
                Arrays.asList("ChildName", "School"),
                ParentChildrenSyncDownTarget.RelationshipType.LOOKUP);

        assertEquals("SELECT DISTINCT {ParentSoup:IdForQuery} FROM {childrenSoup},{ParentSoup} WHERE {childrenSoup:parentLocalId} = {ParentSoup:_soupEntryId} AND ({ParentSoup:__local__} = 'false' OR {childrenSoup:__local__} = 'false')", target.getNonDirtyRecordIdsSql("ParentSoup", "IdForQuery"));

    }


    private void createContactsSoup() {
        final IndexSpec[] contactsIndexSpecs = {
                new IndexSpec(Constants.ID, SmartStore.Type.string),
                new IndexSpec(Constants.NAME, SmartStore.Type.string),
                new IndexSpec(SyncTarget.LOCAL, SmartStore.Type.string),
                new IndexSpec(ACCOUNT_ID, SmartStore.Type.string),
                new IndexSpec(ACCOUNT_LOCAL_ID, SmartStore.Type.integer)
        };
        smartStore.registerSoup(CONTACTS_SOUP, contactsIndexSpecs);
    }

    private void dropContactsSoup() {
        smartStore.dropSoup(CONTACTS_SOUP);
    }

    private ParentChildrenSyncDownTarget getAccountContactsSyncDownTarget() {
        return new ParentChildrenSyncDownTarget(
                new ParentInfo(Constants.ACCOUNT),
                Arrays.asList(Constants.ID, Constants.NAME, Constants.DESCRIPTION),
                "",
                new ChildrenInfo(Constants.CONTACT, Constants.CONTACT + "s", CONTACTS_SOUP, ACCOUNT_ID, ACCOUNT_LOCAL_ID),
                Arrays.asList(Constants.NAME),
                ParentChildrenSyncDownTarget.RelationshipType.MASTER_DETAIL);
    }

    private void createAccountsAndContacts(String[] names, int numberOfContactsPerAccount) throws JSONException {
        JSONObject[] accounts = createAccountsLocally(names);

        JSONObject attributes = new JSONObject();
        attributes.put(TYPE, Constants.CONTACT);

        for (JSONObject account : accounts) {
            for (int i=0; i<numberOfContactsPerAccount; i++) {
                JSONObject contact = new JSONObject();
                contact.put(Constants.ID, createLocalId());
                contact.put(Constants.NAME, "Contact_" + account.get(Constants.NAME) + "_" + i);
                contact.put(Constants.ATTRIBUTES, attributes);
                contact.put(SyncTarget.LOCAL, true);
                contact.put(SyncTarget.LOCALLY_CREATED, true);
                contact.put(SyncTarget.LOCALLY_DELETED, false);
                contact.put(SyncTarget.LOCALLY_UPDATED, false);
                contact.put(ACCOUNT_ID, account.get(Constants.ID));
                contact.put(ACCOUNT_LOCAL_ID, account.get(SmartStore.SOUP_ENTRY_ID));
                smartStore.create(CONTACTS_SOUP, contact);
            }
        }
    }
}
