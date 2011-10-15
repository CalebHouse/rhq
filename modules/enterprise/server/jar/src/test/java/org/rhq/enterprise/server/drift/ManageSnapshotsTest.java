/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.drift;

import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;
import static org.rhq.enterprise.server.util.LookupUtil.getSubjectManager;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.drift.JPADriftFile;
import org.rhq.core.domain.drift.JPADriftSet;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.core.domain.util.PageList;
import org.rhq.test.TransactionCallback;

public class ManageSnapshotsTest extends DriftServerTest {

    private final String RESOURCE_TYPE_NAME = getClass().getSimpleName() + "_RESOURCE_TYPE";

    private final String AGENT_NAME = getClass().getSimpleName() + "_AGENT";

    private final String RESOURCE_NAME = getClass().getSimpleName() + "_RESOURCE";

    private ResourceType resourceType;

    private Agent agent;

    private Resource resource;

    private DriftManagerLocal driftMgr;

    @BeforeClass(groups = {"drift", "drift.ejb", "drift.server"})
    public void initClass() throws Exception {
        driftMgr = getDriftManager();
    }

    @Override
    protected void initDB(EntityManager em) {
        initResourceType();
        initAgent();
        initResource();

        em.persist(resourceType);
        em.persist(agent);
        resource.setAgent(agent);
        em.persist(resource);
    }

    @Override
    protected void purgeDB(EntityManager em) {
        deleteEntity(Resource.class, RESOURCE_NAME, em);
        deleteEntity(Agent.class, AGENT_NAME, em);
        deleteEntity(ResourceType.class, RESOURCE_TYPE_NAME, em);
    }

    private void initResourceType() {
        resourceType = new ResourceTypeBuilder().createResourceType()
            .withId(0)
            .withName(RESOURCE_TYPE_NAME)
            .withCategory(SERVER)
            .withPlugin(RESOURCE_TYPE_NAME.toLowerCase())
            .build();
    }

    private void initAgent() {
        agent = new Agent(AGENT_NAME, "localhost", 1, "", AGENT_NAME + "_TOKEN");
    }

    private void initResource() {
        resource = new ResourceBuilder().createResource()
            .withId(0)
            .withName(RESOURCE_NAME)
            .withResourceKey(RESOURCE_NAME)
            .withUuid(RESOURCE_NAME)
            .withResourceType(resourceType)
            .build();
    }

    @Test(groups = {"drift", "drift.ejb", "drift.server"})
    public void setPinnedFlagOnDriftDef() {
        final DriftDefinition driftDef = createAndPersistDriftDef("test::setPinnedFlag");

        // create initial change set
        final JPADriftFile driftFile1 = new JPADriftFile("a1b2c3");
        JPADrift drift = new JPADrift(null, "drift.1", FILE_ADDED, null, driftFile1);

        JPADriftSet driftSet = new JPADriftSet();
        driftSet.addDrift(drift);

        final JPADriftChangeSet changeSet = new JPADriftChangeSet(resource, 0, COVERAGE, driftDef);
        changeSet.setInitialDriftSet(driftSet);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(driftFile1);
                em.persist(changeSet);
            }
        });

        driftMgr.pinSnapshot(getOverlord(), driftDef.getId(), 0);
        DriftDefinition updatedDriftDef = driftMgr.getDriftDefinition(getOverlord(), driftDef.getId());

        assertNotNull("Failed to get " + toString(driftDef), updatedDriftDef);
        assertTrue("Failed to set pinned flag of " + toString(driftDef), updatedDriftDef.isPinned());
    }

    /**
     * This test is temporarily disabled because it looks like there might be a problem with
     * DriftManagerBean.getSnapshot and DriftManagerBean.pinSnapshot relies on getSnapshot.
     * I am going to go back and first get some tests in place for getSnapshot.
     */
    @Test(groups = {"drift", "drift.ejb", "drift.server"}, enabled = false)
    public void makePinnedSnapshotVersionZero() throws Exception {
        final DriftDefinition driftDef = createAndPersistDriftDef("test::makeSnapshotVersionZero");

        // create initial change set
        final JPADriftFile driftFile1 = new JPADriftFile("a1b2c3");
        JPADrift drift1 = new JPADrift(null, "drift.1", FILE_ADDED, null, driftFile1);

        JPADriftSet driftSet = new JPADriftSet();
        driftSet.addDrift(drift1);

        final JPADriftChangeSet changeSet0 = new JPADriftChangeSet(resource, 0, COVERAGE, driftDef);
        changeSet0.setInitialDriftSet(driftSet);

        // create change set v1
        final JPADriftFile driftFile2 = new JPADriftFile("1a2b3c");
        final JPADriftChangeSet changeSet1 = new JPADriftChangeSet(resource, 1, DRIFT, driftDef);
        changeSet1.getDrifts().add(new JPADrift(null, "drift.2", FILE_ADDED, null, driftFile2));

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(driftFile1);
                em.persist(driftFile2);
                em.persist(changeSet0);
                em.persist(changeSet1);
            }
        });

        driftMgr.pinSnapshot(getOverlord(), driftDef.getId(), 1);

        // Verify that there is now only one change set for the drift def
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(driftDef.getId());

        PageList<? extends DriftChangeSet<?>> changeSets = driftMgr.findDriftChangeSetsByCriteria(getOverlord(),
            criteria);
        assertEquals("All change sets except the change set representing the pinned snapshot should be removed",
            1, changeSets.size());
        DriftChangeSet<?> changeSet = changeSets.get(0);
        assertEquals("Expected to find two drift entries in pinned change set", 2, changeSet.getDrifts().size());
    }

    private DriftDefinition createAndPersistDriftDef(String name) {
        final DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName(name);
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                driftDef.setResource(resource);
                getEntityManager().persist(driftDef);
            }
        });

        return driftDef;
    }

    private Subject getOverlord() {
        return getSubjectManager().getOverlord();
    }

    private String toString(DriftDefinition def) {
        return "DriftDefinition[id: " + def.getId() + ", name: " + def.getName() + "]";
    }
}
