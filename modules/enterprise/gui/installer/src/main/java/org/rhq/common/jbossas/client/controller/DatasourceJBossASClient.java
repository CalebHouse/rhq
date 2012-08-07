/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.common.jbossas.client.controller;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Provides convienence methods associated with datasource management.
 * 
 * @author John Mazzitelli
 */
public class DatasourceJBossASClient extends JBossASClient {

    public static final String SUBSYSTEM_DATASOURCES = "datasources";
    public static final String DATA_SOURCE = "data-source";
    public static final String XA_DATA_SOURCE = "xa-data-source";
    public static final String JDBC_DRIVER = "jdbc-driver";
    public static final String CONNECTION_PROPERTIES = "connection-properties";
    public static final String XA_DATASOURCE_PROPERTIES = "xa-datasource-properties";

    public DatasourceJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Checks to see if there is already a JDBC driver with the given name.
     *
     * @param jdbcDriverName the name to check
     * @return true if there is a JDBC driver with the given name already in existence
     */
    public boolean isJDBCDriver(String jdbcDriverName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
        ModelNode queryNode = createRequest(READ_RESOURCE, addr);
        ModelNode results = execute(queryNode);
        if (isSuccess(results)) {
            ModelNode drivers = getResults(results).get(JDBC_DRIVER);
            List<ModelNode> list = drivers.asList();
            for (ModelNode driver : list) {
                if (driver.has(jdbcDriverName)) {
                    return true;
                }
            }
            return false;
        } else {
            throw new FailureException(results, "Failed to get JDBC drivers");
        }
    }

    /**
     * Returns a ModelNode that can be used to create a JDBC driver configuration for use by datasources.
     * Callers are free to tweek the JDBC driver request that is returned,
     * if they so choose, before asking the client to execute the request.
     *
     * NOTE: the JDBC module must have already been installed in the JBossAS's modules/ location.
     *
     * @param name the name of the JDBC driver (this is not the name of the JDBC jar or the module name, it is
     *             just a convienence name of the JDBC driver configuration).
     * @param moduleName the name of the JBossAS module where the JDBC driver is installed
     * @param driverXaClassName the JDBC driver's XA datasource classname
     *
     * @return the request to create the JDBC driver configuration.
     */
    public ModelNode createNewJdbcDriverRequest(String name, String moduleName, String driverXaClassName) {
        String dmrTemplate = "" //
            + "{" //
            + "\"driver-module-name\" => \"%s\" " //
            + ", \"driver-xa-datasource-class-name\" => \"%s\" " //
            + "}";

        String dmr = String.format(dmrTemplate, moduleName, driverXaClassName);

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, JDBC_DRIVER, name);
        final ModelNode request = ModelNode.fromString(dmr);
        request.get(OPERATION).set(ADD);
        request.get(ADDRESS).set(addr.getAddressNode());

        return request;
    }

    /**
     * Returns a ModelNode that can be used to create a datasource.
     * Callers are free to tweek the datasource request that is returned,
     * if they so choose, before asking the client to execute the request.
     *
     * @param name
     * @param blockingTimeoutWaitMillis
     * @param connectionUrlExpression
     * @param driverName
     * @param exceptionSorterClassName
     * @param idleTimeoutMinutes
     * @param jta true if this DS should support transactions; false if not
     * @param minPoolSize
     * @param maxPoolSize
     * @param preparedStatementCacheSize
     * @param securityDomain
     * @param staleConnectionCheckerClassName
     * @param transactionIsolation
     * @param validConnectionCheckerClassName
     * @param connectionProperties
     *
     * @return the request that can be used to create the datasource
     */
    public ModelNode createNewDatasourceRequest(String name, int blockingTimeoutWaitMillis,
        String connectionUrlExpression, String driverName, String exceptionSorterClassName, int idleTimeoutMinutes,
        boolean jta, int minPoolSize, int maxPoolSize, int preparedStatementCacheSize, String securityDomain,
        String staleConnectionCheckerClassName, String transactionIsolation, String validConnectionCheckerClassName,
        Map<String, String> connectionProperties) {

        String jndiName = "java:jboss/datasources/" + name;

        String dmrTemplate = "" //
            + "{" //
            + "\"blocking-timeout-wait-millis\" => %dL " //
            + ", \"connection-url\" => expression \"%s\" " //
            + ", \"driver-name\" => \"%s\" " //
            + ", \"exception-sorter-class-name\" => \"%s\" " //
            + ", \"idle-timeout-minutes\" => %dL " //
            + ", \"jndi-name\" => \"%s\" " //
            + ", \"jta\" => %s " //
            + ", \"min-pool-size\" => %d " //
            + ", \"max-pool-size\" => %d " //
            + ", \"prepared-statements-cache-size\" => %dL " //
            + ", \"security-domain\" => \"%s\" " //
            + ", \"stale-connection-checker-class-name\" => \"%s\" " //
            + ", \"transaction-isolation\" => \"%s\" " //
            + ", \"use-java-context\" => true " //
            + ", \"valid-connection-checker-class-name\" => \"%s\" " //
            + "}";

        String dmr = String.format(dmrTemplate, blockingTimeoutWaitMillis, connectionUrlExpression, driverName,
            exceptionSorterClassName, idleTimeoutMinutes, jndiName, jta, minPoolSize, maxPoolSize,
            preparedStatementCacheSize, securityDomain, staleConnectionCheckerClassName, transactionIsolation,
            validConnectionCheckerClassName);

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, DATA_SOURCE, name);
        final ModelNode request1 = ModelNode.fromString(dmr);
        request1.get(OPERATION).set(ADD);
        request1.get(ADDRESS).set(addr.getAddressNode());

        // if there are no conn properties, no need to create a batch request, there is only one ADD request to make
        if (connectionProperties == null || connectionProperties.size() == 0) {
            return request1;
        }

        // create a batch of requests - the first is the main one, the rest create each conn property
        ModelNode[] batch = new ModelNode[1 + connectionProperties.size()];
        batch[0] = request1;
        int n = 1;
        for (Map.Entry<String, String> entry : connectionProperties.entrySet()) {
            addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, DATA_SOURCE, name, CONNECTION_PROPERTIES,
                entry.getKey());
            final ModelNode requestN = new ModelNode();
            requestN.get(OPERATION).set(ADD);
            requestN.get(ADDRESS).set(addr.getAddressNode());
            if (entry.getValue().indexOf("${") > -1) {
                requestN.get(VALUE).setExpression(entry.getValue());
            } else {
                requestN.get(VALUE).set(entry.getValue());
            }
            batch[n++] = requestN;
        }

        return createBatchRequest(batch);
    }

    /**
     * Returns a ModelNode that can be used to create an XA datasource.
     * Callers are free to tweek the datasource request that is returned,
     * if they so choose, before asking the client to execute the request.
     *
     * @param name
     * @param blockingTimeoutWaitMillis
     * @param driverName
     * @param exceptionSorterClassName
     * @param idleTimeoutMinutes
     * @param minPoolSize
     * @param maxPoolSize
     * @param preparedStatementCacheSize
     * @param securityDomain
     * @param staleConnectionCheckerClassName
     * @param transactionIsolation
     * @param validConnectionCheckerClassName
     * @param xaDatasourceProperties
     *
     * @return the request that can be used to create the XA datasource
     */
    public ModelNode createNewXADatasourceRequest(String name, int blockingTimeoutWaitMillis, String driverName,
        String exceptionSorterClassName, int idleTimeoutMinutes, int minPoolSize, int maxPoolSize,
        int preparedStatementCacheSize, String securityDomain, String staleConnectionCheckerClassName,
        String transactionIsolation, String validConnectionCheckerClassName, Map<String, String> xaDatasourceProperties) {

        String jndiName = "java:jboss/datasources/" + name;

        String dmrTemplate = "" //
            + "{" //
            + "\"blocking-timeout-wait-millis\" => %dL " //
            + ", \"driver-name\" => \"%s\" " //
            + ", \"exception-sorter-class-name\" => \"%s\" " //
            + ", \"idle-timeout-minutes\" => %dL " //
            + ", \"jndi-name\" => \"%s\" " //
            + ", \"jta\" => true " //
            + ", \"min-pool-size\" => %d " //
            + ", \"max-pool-size\" => %d " //
            + ", \"prepared-statements-cache-size\" => %dL " //
            + ", \"security-domain\" => \"%s\" " //
            + ", \"stale-connection-checker-class-name\" => \"%s\" " //
            + ", \"transaction-isolation\" => \"%s\" " //
            + ", \"use-java-context\" => true " //
            + ", \"valid-connection-checker-class-name\" => \"%s\" " //
            + "}";

        String dmr = String.format(dmrTemplate, blockingTimeoutWaitMillis, driverName, exceptionSorterClassName,
            idleTimeoutMinutes, jndiName, minPoolSize, maxPoolSize, preparedStatementCacheSize, securityDomain,
            staleConnectionCheckerClassName, transactionIsolation, validConnectionCheckerClassName);

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, XA_DATA_SOURCE, name);
        final ModelNode request1 = ModelNode.fromString(dmr);
        request1.get(OPERATION).set(ADD);
        request1.get(ADDRESS).set(addr.getAddressNode());

        // if there are no xa datasource properties, no need to create a batch request, there is only one ADD request to make
        if (xaDatasourceProperties == null || xaDatasourceProperties.size() == 0) {
            return request1;
        }

        // create a batch of requests - the first is the main one, the rest create each conn property
        ModelNode[] batch = new ModelNode[1 + xaDatasourceProperties.size()];
        batch[0] = request1;
        int n = 1;
        for (Map.Entry<String, String> entry : xaDatasourceProperties.entrySet()) {
            addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, XA_DATA_SOURCE, name, XA_DATASOURCE_PROPERTIES,
                entry.getKey());
            final ModelNode requestN = new ModelNode();
            requestN.get(OPERATION).set(ADD);
            requestN.get(ADDRESS).set(addr.getAddressNode());
            if (entry.getValue().indexOf("${") > -1) {
                requestN.get(VALUE).setExpression(entry.getValue());
            } else {
                requestN.get(VALUE).set(entry.getValue());
            }
            batch[n++] = requestN;
        }

        return createBatchRequest(batch);
    }
}