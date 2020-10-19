/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.dbadmin;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.thespheres.betula.database.DBAdminTask;
import org.thespheres.betula.database.DBAdminTaskResult;

/**
 *
 * @author boris.heithecker
 */
@WebService(serviceName = "DbAdminService", portName = "DbAdminServicePort", targetNamespace = "http://dbadmin.service.betula.thespheres.org/")
@Stateless
@DeclareRoles("superadmin")
@RolesAllowed({"superadmin", "unitadmin"})
public class DbAdminService {

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
