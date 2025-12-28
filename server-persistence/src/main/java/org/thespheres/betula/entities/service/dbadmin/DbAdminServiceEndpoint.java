/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.dbadmin;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.thespheres.betula.database.DBAdminTask;
import org.thespheres.betula.database.DBAdminTaskResult;
import org.thespheres.betula.database.DbAdminService;

/**
 *
 * @author boris.heithecker
 */
@WebService(serviceName = "DbAdminService", portName = "DbAdminServicePort", targetNamespace = "http://dbadmin.service.betula.thespheres.org/")
@Stateless
@DeclareRoles("superadmin")
@RolesAllowed({"superadmin", "unitadmin"})
public class DbAdminServiceEndpoint implements DbAdminService {

    @Inject
    private CleanUpTask cleanUpTask;
    @Inject
    private ClearSigneesTask clearSigneesTask;
    @Inject
    private UpgradeDBTask upgradeDBTask;

    @WebMethod(operationName = "submitTask")
    public DBAdminTaskResult submitTask(@WebParam(name = "task") DBAdminTask task) {
        if (task.getName().equals(cleanUpTask.getName())) {
            return cleanUpTask.process(task);
        } else if (task.getName().equals(upgradeDBTask.getName())) {
            return upgradeDBTask.process(task);
        } else if (task.getName().equals(clearSigneesTask.getName())) {
            return clearSigneesTask.process(task);
        }
        return new DBAdminTaskResult(false, "No task.");
    }
}
