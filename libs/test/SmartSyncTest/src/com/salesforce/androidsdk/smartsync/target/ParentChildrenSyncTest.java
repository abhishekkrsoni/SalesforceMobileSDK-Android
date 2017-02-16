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

import android.util.Log;

import com.salesforce.androidsdk.smartstore.store.IndexSpec;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.manager.SyncManagerTestCase;
import com.salesforce.androidsdk.smartsync.util.ChildrenInfo;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.smartsync.util.ParentInfo;
import com.salesforce.androidsdk.util.JSONObjectHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedSet;

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
    public void testGetQuery() {
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
    public void testGetSoqlForRemoteIds() {
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
    public void testGetDirtyRecordIdsSql() {
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
    public void testGetNonDirtyRecordIdsSql() {
        ParentChildrenSyncDownTarget target = new ParentChildrenSyncDownTarget(
                new ParentInfo("Parent", "ParentId", "ParentModifiedDate"),
                Arrays.asList("ParentName", "Title"),
                "School = 'MIT'",
                new ChildrenInfo("Child", "Children", "ChildId", "ChildLastModifiedDate", "childrenSoup", "parentId", "parentLocalId"),
                Arrays.asList("ChildName", "School"),
                ParentChildrenSyncDownTarget.RelationshipType.LOOKUP);

        assertEquals("SELECT {ParentSoup:IdForQuery} FROM {ParentSoup} WHERE {ParentSoup:_soupEntryId} NOT IN (SELECT DISTINCT {ParentSoup:_soupEntryId} FROM {childrenSoup},{ParentSoup} WHERE {childrenSoup:parentLocalId} = {ParentSoup:_soupEntryId} AND ({ParentSoup:__local__} = 'true' OR {childrenSoup:__local__} = 'true'))", target.getNonDirtyRecordIdsSql("ParentSoup", "IdForQuery"));

    }

    /**
     * Test getDirtyRecordIds and getNonDirtyRecordIds for ParentChildrenSyncDownTarget when parent and/or all and/or some children are dirty
     */
    public void testGetDirtyAndNonDirtyRecordIds() throws JSONException {
        String[] accountNames = new String[] {
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT)
        };
        Map<JSONObject, JSONObject[]> mapAccountToContacts = createAccountsAndContactsLocally(accountNames, 3);
        JSONObject[] accounts = mapAccountToContacts.keySet().toArray(new JSONObject[]{});

        // All Accounts should be returned
        tryGetDirtyRecordIds(accounts);

        // No accounts should be returned
        tryGetNonDirtyRecordIds(new JSONObject[]{});


        // Cleaning up:
        // accounts[0]: dirty account and dirty contacts
        // accounts[1]: clean account and dirty contacts
        // accounts[2]: dirty account and clean contacts
        // accounts[3]: clean account and clean contacts
        // accounts[4]: dirty account and some dirty contacts
        // accounts[5]: clean account and some dirty contacts

        cleanRecord(ACCOUNTS_SOUP, accounts[1]);
        cleanRecords(CONTACTS_SOUP, mapAccountToContacts.get(accounts[2]));
        cleanRecord(ACCOUNTS_SOUP, accounts[3]);
        cleanRecords(CONTACTS_SOUP, mapAccountToContacts.get(accounts[3]));
        cleanRecord(CONTACTS_SOUP, mapAccountToContacts.get(accounts[4])[0]);
        cleanRecord(ACCOUNTS_SOUP, accounts[5]);
        cleanRecord(CONTACTS_SOUP, mapAccountToContacts.get(accounts[5])[0]);

        // Only clean account with clean contacts should not be returned
        tryGetDirtyRecordIds(new JSONObject[] { accounts[0], accounts[1], accounts[2], accounts[4], accounts[5]});

        // Only clean account with clean contacts should be returned
        tryGetNonDirtyRecordIds(new JSONObject[] { accounts[3] });
    }

    /**
     * Test deleteRecordsFromLocalStore with a master-detail relationship (children should be deleted too)
     */
    public void testDeleteRecordsFromLocalStoreWithMasterDetail() throws JSONException {
        tryDeleteFromLocalStore(ParentChildrenSyncDownTarget.RelationshipType.MASTER_DETAIL, false /* multiple records */ );
    }

    /**
     * Test deleteRecordsFromLocalStore with a lookup relationship (children should NOT be deleted)
     */
    public void testDeleteRecordsFromLocalStoreWithLookup() throws JSONException {
        tryDeleteFromLocalStore(ParentChildrenSyncDownTarget.RelationshipType.LOOKUP, false /* multiple records */ );
    }

    /**
     * Test deleteFromLocalStore with a master-detail relationship (children should be deleted too)
     */
    public void testDeleteFromLocalStoreWithMasterDetail() throws JSONException {
        tryDeleteFromLocalStore(ParentChildrenSyncDownTarget.RelationshipType.MASTER_DETAIL, true /* single record */ );
    }

    /**
     * Test deleteFromLocalStore with a lookup relationship (children should be deleted too)
     */
    public void testDeleteFromLocalStoreWithLookup() throws JSONException {
        tryDeleteFromLocalStore(ParentChildrenSyncDownTarget.RelationshipType.LOOKUP, true /* single record */ );
    }

    /**
     * Test saveRecordsToLocalStore
     */
    public void testSaveRecordsToLocalStore() throws JSONException {
        // Putting together a JSONArray of accounts with contacts looking like what we would get back from the server:
        // - not having local fields
        // - not have _soupEntryId field
        final int numberAccounts = 4;
        final int numberContactsPerAccount = 3;

        JSONObject accountAttributes = new JSONObject();
        accountAttributes.put(TYPE, Constants.ACCOUNT);

        JSONObject contactAttributes = new JSONObject();
        contactAttributes.put(TYPE, Constants.CONTACT);

        JSONObject[] accounts = new JSONObject[numberAccounts];
        Map<JSONObject, JSONObject[]> mapAccountContacts = new HashMap<>();

        for (int i = 0; i < numberAccounts; i++) {
            JSONObject account = new JSONObject();
            account.put(Constants.ID, createLocalId());
            account.put(Constants.ATTRIBUTES, accountAttributes);

            JSONObject[] contacts = new JSONObject[numberContactsPerAccount];
            for (int j = 0; j < numberContactsPerAccount; j++) {
                JSONObject contact = new JSONObject();
                contact.put(Constants.ID, createLocalId());
                contact.put(Constants.ATTRIBUTES, contactAttributes);
                contact.put(ACCOUNT_ID, account.get(Constants.ID));
                contacts[j] = contact;
            }
            mapAccountContacts.put(account, contacts);
            accounts[i] = account;
        }

        JSONArray records = new JSONArray();
        for (JSONObject account : accounts) {
            JSONObject record = new JSONObject(account.toString());
            JSONArray contacts = new JSONArray();
            for (JSONObject contact : mapAccountContacts.get(account)) {
                contacts.put(contact);
            }
            record.put("Contacts", contacts);
            records.put(record);
        }

        // Now calling saveRecordsToLocalStore
        ParentChildrenSyncDownTarget target = getAccountContactsSyncDownTarget(ParentChildrenSyncDownTarget.RelationshipType.MASTER_DETAIL);
        target.saveRecordsToLocalStore(syncManager, ACCOUNTS_SOUP, records);

        // Checking accounts and contacts soup
        // Making sure local fields are populated
        // Making sure accountId and accountLocalId fields are populated on contacts

        JSONObject[] accountsFromDb = queryWithInClause(ACCOUNTS_SOUP, Constants.ID, JSONObjectHelper.pluck(accounts, Constants.ID).toArray(new String[0]), null);
        assertEquals("Wrong number of accounts in db", accounts.length, accountsFromDb.length);
        for (int i = 0; i < accountsFromDb.length; i++) {
            JSONObject account = accounts[i];
            JSONObject accountFromDb = accountsFromDb[i];

            assertEquals(account.getString(Constants.ID), accountFromDb.getString(Constants.ID));
            assertEquals(Constants.ACCOUNT, accountFromDb.getJSONObject(Constants.ATTRIBUTES).getString(TYPE));
            assertEquals(false, accountFromDb.getBoolean(SyncTarget.LOCAL));
            assertEquals(false, accountFromDb.getBoolean(SyncTarget.LOCALLY_CREATED));
            assertEquals(false, accountFromDb.getBoolean(SyncTarget.LOCALLY_DELETED));
            assertEquals(false, accountFromDb.getBoolean(SyncTarget.LOCALLY_UPDATED));

            JSONObject[] contactsFromDb = queryWithInClause(CONTACTS_SOUP, ACCOUNT_ID, new String[]{account.getString(Constants.ID)}, SmartStore.SOUP_ENTRY_ID);
            JSONObject[] contacts = mapAccountContacts.get(account);
            assertEquals("Wrong number of contacts in db", contacts.length, contactsFromDb.length);
            for (int j = 0; j < contactsFromDb.length; j++) {
                JSONObject contact = contacts[j];
                JSONObject contactFromDb = contactsFromDb[j];

                assertEquals(contact.getString(Constants.ID), contactFromDb.getString(Constants.ID));
                assertEquals(Constants.CONTACT, contactFromDb.getJSONObject(Constants.ATTRIBUTES).getString(TYPE));
                assertEquals(false, contactFromDb.getBoolean(SyncTarget.LOCAL));
                assertEquals(false, contactFromDb.getBoolean(SyncTarget.LOCALLY_CREATED));
                assertEquals(false, contactFromDb.getBoolean(SyncTarget.LOCALLY_DELETED));
                assertEquals(false, contactFromDb.getBoolean(SyncTarget.LOCALLY_UPDATED));
                assertEquals(accountFromDb.getString(Constants.ID), contactFromDb.getString(ACCOUNT_ID));
                assertEquals(accountFromDb.getString(SmartStore.SOUP_ENTRY_ID), contactFromDb.getString(ACCOUNT_LOCAL_ID));
            }

        }
    }

    /**
     * Test getLatestModificationTimeStamp
     */
    public void testGetLatestModificationTimeStamp() throws JSONException {
        // Putting together a JSONArray of accounts with contacts looking like what we would get back from the server with different fields for last modified time
        final int numberAccounts = 4;
        final int numberContactsPerAccount = 3;

        final long[] timeStamps = new long[]{
                100000000,
                200000000,
                300000000,
                400000000
        };


        final String[] timeStampStrs = new String[]{
                Constants.TIMESTAMP_FORMAT.format(new Date(timeStamps[0])),
                Constants.TIMESTAMP_FORMAT.format(new Date(timeStamps[1])),
                Constants.TIMESTAMP_FORMAT.format(new Date(timeStamps[2])),
                Constants.TIMESTAMP_FORMAT.format(new Date(timeStamps[3])),
        };

        JSONObject accountAttributes = new JSONObject();
        accountAttributes.put(TYPE, Constants.ACCOUNT);

        JSONObject contactAttributes = new JSONObject();
        contactAttributes.put(TYPE, Constants.CONTACT);

        JSONObject[] accounts = new JSONObject[numberAccounts];
        Map<JSONObject, JSONObject[]> mapAccountContacts = new HashMap<>();

        for (int i = 0; i < numberAccounts; i++) {
            JSONObject account = new JSONObject();
            account.put(Constants.ID, createLocalId());
            account.put("AccountTimeStamp1", timeStampStrs[i % timeStampStrs.length]);
            account.put("AccountTimeStamp2", timeStampStrs[0]);

            JSONObject[] contacts = new JSONObject[numberContactsPerAccount];
            for (int j = 0; j < numberContactsPerAccount; j++) {
                JSONObject contact = new JSONObject();
                contact.put(Constants.ID, createLocalId());
                contact.put(ACCOUNT_ID, account.get(Constants.ID));
                contact.put("ContactTimeStamp1", timeStampStrs[1]);
                contact.put("ContactTimeStamp2", timeStampStrs[j % timeStampStrs.length]);
                contacts[j] = contact;
            }
            mapAccountContacts.put(account, contacts);
            accounts[i] = account;
        }

        JSONArray records = new JSONArray();
        for (JSONObject account : accounts) {
            JSONObject record = new JSONObject(account.toString());
            JSONArray contacts = new JSONArray();
            for (JSONObject contact : mapAccountContacts.get(account)) {
                contacts.put(contact);
            }
            record.put("Contacts", contacts);
            records.put(record);
        }

        // Maximums

        // Get max time stamps based on fields AccountTimeStamp1 / ContactTimeStamp1
        assertEquals(
                timeStamps[numberAccounts % timeStampStrs.length],
                getAccountContactsSyncDownTarget(ParentChildrenSyncDownTarget.RelationshipType.LOOKUP, "AccountTimeStamp1", "ContactTimeStamp1").getLatestModificationTimeStamp(records)
        );

        // Get max time stamps based on fields AccountTimeStamp1 / ContactTimeStamp2
        assertEquals(
                timeStamps[numberAccounts % timeStampStrs.length],
                getAccountContactsSyncDownTarget(ParentChildrenSyncDownTarget.RelationshipType.LOOKUP, "AccountTimeStamp1", "ContactTimeStamp2").getLatestModificationTimeStamp(records)
        );

        // Get max time stamps based on fields AccountTimeStamp2 / ContactTimeStamp1
        assertEquals(
                timeStamps[1],
                getAccountContactsSyncDownTarget(ParentChildrenSyncDownTarget.RelationshipType.LOOKUP, "AccountTimeStamp2", "ContactTimeStamp1").getLatestModificationTimeStamp(records)
        );

        // Get max time stamps based on fields AccountTimeStamp2 / ContactTimeStamp2
        assertEquals(
                timeStamps[numberContactsPerAccount % timeStampStrs.length],
                getAccountContactsSyncDownTarget(ParentChildrenSyncDownTarget.RelationshipType.LOOKUP, "AccountTimeStamp2", "ContactTimeStamp2").getLatestModificationTimeStamp(records)
        );
    }

    /**
     * Test ParentChildrenSyncDownTarget's constructor that takes only a SOQL query
     * An exception is expected
     */
    public void testConstructorWithQuery() {
        try {
            new ParentChildrenSyncDownTarget("SELECT Name FROM Account");
            fail("Exception should have been thrown");
        }
        catch (UnsupportedOperationException e) {
        }
    }


    /**
     * Helper method for the testDelete*
     * @param relationshipType
     * @param singleDelete
     * @throws JSONException
     */
    protected void tryDeleteFromLocalStore(ParentChildrenSyncDownTarget.RelationshipType relationshipType, boolean singleDelete) throws JSONException {
        String[] accountNames = {
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT)
        };
        Map<JSONObject, JSONObject[]> mapAccountToContacts = createAccountsAndContactsLocally(accountNames, 3);
        JSONObject[] accounts = mapAccountToContacts.keySet().toArray(new JSONObject[] {});

        String[] contactIdsOfFirstAccount = JSONObjectHelper.pluck(mapAccountToContacts.get(accounts[0]), Constants.ID).toArray(new String[0]);
        String[] contactIdsOfSecondAccount = JSONObjectHelper.pluck(mapAccountToContacts.get(accounts[1]), Constants.ID).toArray(new String[0]);
        String[] contactIdsOfThirdAccount = JSONObjectHelper.pluck(mapAccountToContacts.get(accounts[2]), Constants.ID).toArray(new String[0]);

        ParentChildrenSyncDownTarget target = getAccountContactsSyncDownTarget(relationshipType);

        // Delete one account with deleteFromLocalStore
        if (singleDelete) {
            target.deleteFromLocalStore(syncManager, ACCOUNTS_SOUP, accounts[1]);

            // Check that account was indeed deleted but none others
            checkDbDeleted(ACCOUNTS_SOUP, new String[] {accounts[1].getString(Constants.ID)}, Constants.ID);
            checkDbExist(ACCOUNTS_SOUP, new String[] {accounts[0].getString(Constants.ID), accounts[2].getString(Constants.ID)}, Constants.ID);

            // Checking contacts
            checkDbExist(CONTACTS_SOUP, contactIdsOfFirstAccount, Constants.ID);
            checkDbExist(CONTACTS_SOUP, contactIdsOfThirdAccount, Constants.ID);
            switch (relationshipType) {
                case MASTER_DETAIL:
                    checkDbDeleted(CONTACTS_SOUP, contactIdsOfSecondAccount, Constants.ID);
                    break;

                case LOOKUP:
                    checkDbExist(CONTACTS_SOUP, contactIdsOfSecondAccount, Constants.ID);
                    break;
            }
        }
        // Delete multiple accounts with deleteRecordsFromLocalStore
        else {
            String[] accountIdsToDelete = {accounts[0].getString(Constants.ID), accounts[2].getString(Constants.ID)};
            target.deleteRecordsFromLocalStore(syncManager, ACCOUNTS_SOUP, new HashSet(Arrays.asList(accountIdsToDelete)), Constants.ID);

            // Check that the accounts were indeed deleted but none others
            checkDbExist(ACCOUNTS_SOUP, new String[] {accounts[1].getString(Constants.ID)}, Constants.ID);
            checkDbDeleted(ACCOUNTS_SOUP, new String[] {accounts[0].getString(Constants.ID), accounts[2].getString(Constants.ID)}, Constants.ID);

            // Checking contacts
            checkDbExist(CONTACTS_SOUP, contactIdsOfSecondAccount, Constants.ID);
            switch (relationshipType) {
                case MASTER_DETAIL:
                    checkDbDeleted(CONTACTS_SOUP, contactIdsOfFirstAccount, Constants.ID);
                    checkDbDeleted(CONTACTS_SOUP, contactIdsOfThirdAccount, Constants.ID);
                    break;

                case LOOKUP:
                    checkDbExist(CONTACTS_SOUP, contactIdsOfFirstAccount, Constants.ID);
                    checkDbExist(CONTACTS_SOUP, contactIdsOfThirdAccount, Constants.ID);
                    break;
            }
        }

    }

    private void tryGetDirtyRecordIds(JSONObject[] expectedRecords) throws JSONException {
        ParentChildrenSyncDownTarget target = getAccountContactsSyncDownTarget(ParentChildrenSyncDownTarget.RelationshipType.MASTER_DETAIL);
        SortedSet<String> dirtyRecordIds = target.getDirtyRecordIds(syncManager, ACCOUNTS_SOUP, Constants.ID);
        assertEquals("Wrong number of dirty records", expectedRecords.length, dirtyRecordIds.size());
        for (JSONObject expectedRecord : expectedRecords) {
            assertTrue(dirtyRecordIds.contains(expectedRecord.getString(Constants.ID)));
        }
    }

    private void tryGetNonDirtyRecordIds(JSONObject[] expectedRecords) throws JSONException {
        ParentChildrenSyncDownTarget target = getAccountContactsSyncDownTarget(ParentChildrenSyncDownTarget.RelationshipType.MASTER_DETAIL);
        SortedSet<String> nonDirtyRecordIds = target.getNonDirtyRecordIds(syncManager, ACCOUNTS_SOUP, Constants.ID);
        assertEquals("Wrong number of non-dirty records", expectedRecords.length, nonDirtyRecordIds.size());
        for (JSONObject expectedRecord : expectedRecords) {
            assertTrue(nonDirtyRecordIds.contains(expectedRecord.getString(Constants.ID)));
        }
    }

    private void cleanRecords(String soupName, JSONObject[] records) throws JSONException {
        for (JSONObject record : records) {
            cleanRecord(soupName, record);
        }
    }

    private void cleanRecord(String soupName, JSONObject record) throws JSONException {
        record.put(SyncTarget.LOCAL, false);
        record.put(SyncTarget.LOCALLY_CREATED, false);
        record.put(SyncTarget.LOCALLY_UPDATED, false);
        record.put(SyncTarget.LOCALLY_DELETED, false);
        syncManager.getSmartStore().upsert(soupName, record);
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

    private ParentChildrenSyncDownTarget getAccountContactsSyncDownTarget(ParentChildrenSyncDownTarget.RelationshipType relationshipType) {
        return getAccountContactsSyncDownTarget(relationshipType, Constants.LAST_MODIFIED_DATE, Constants.LAST_MODIFIED_DATE);
    }

    private ParentChildrenSyncDownTarget getAccountContactsSyncDownTarget(ParentChildrenSyncDownTarget.RelationshipType relationshipType, String accountModificationDateFieldName, String contactModificationDateFieldName) {
        return new ParentChildrenSyncDownTarget(
                new ParentInfo(Constants.ACCOUNT, Constants.ID, accountModificationDateFieldName),
                Arrays.asList(Constants.ID, Constants.NAME, Constants.DESCRIPTION),
                "",
                new ChildrenInfo(Constants.CONTACT, Constants.CONTACT + "s", Constants.ID, contactModificationDateFieldName, CONTACTS_SOUP, ACCOUNT_ID, ACCOUNT_LOCAL_ID),
                Arrays.asList(Constants.NAME),
                relationshipType);
    }


    private Map<JSONObject, JSONObject[]> createAccountsAndContactsLocally(String[] names, int numberOfContactsPerAccount) throws JSONException {
        Map<JSONObject, JSONObject[]> mapAccountContacts = new HashMap<>();
        JSONObject[] accounts = createAccountsLocally(names);

        JSONObject attributes = new JSONObject();
        attributes.put(TYPE, Constants.CONTACT);

        for (JSONObject account : accounts) {
            JSONObject[] contacts = new JSONObject[numberOfContactsPerAccount];
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
                contacts[i] = smartStore.create(CONTACTS_SOUP, contact);
            }
            mapAccountContacts.put(account, contacts);
        }
        return mapAccountContacts;
    }

    private JSONObject[] queryWithInClause(String soupName, String fieldName, String[] values, String orderBy) throws JSONException {
        final String sql = String.format("SELECT {%s:%s} FROM {%s} WHERE {%s:%s} IN %s %s",
                soupName, SmartSqlHelper.SOUP, soupName, soupName, fieldName,
                makeInClause(values),
                orderBy == null ? "" : String.format(" ORDER BY {%s:%s} ASC", soupName, orderBy)
        );

        QuerySpec querySpec = QuerySpec.buildSmartQuerySpec(sql, Integer.MAX_VALUE);
        JSONArray rows = smartStore.query(querySpec, 0);
        JSONObject[] arr = new JSONObject[rows.length()];
        for (int i=0; i<rows.length(); i++) {
            arr[i] = rows.getJSONArray(i).getJSONObject(0);
        }
        return arr;
    }

}
