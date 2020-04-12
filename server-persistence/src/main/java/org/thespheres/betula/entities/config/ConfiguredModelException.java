/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import java.util.Arrays;
import java.util.StringJoiner;
import javax.ejb.ApplicationException;
import javax.ejb.EJBException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author boris.heithecker
 */
@ApplicationException(rollback = true)
@Messages({"ConfiguredModelException.message.missingProperties=Mindestens eine der folgenden Konfigurations-Eigenschaften werden vermisst: {0}.",
    "ConfiguredModelException.message.cause=Verursachter Konfigurationsfehler für die Resource {2}. Ursache: {0}, Message: {1}."})
public class ConfiguredModelException extends EJBException {

    private String[] missingProperties;
    private String resourceName;

    public ConfiguredModelException() {
        super();
    }

    public ConfiguredModelException(String[] missingProperties) {
        this();
        this.missingProperties = missingProperties;
    }

    public ConfiguredModelException(String missingProperty) {
        this();
        this.missingProperties = new String[]{missingProperty};
    }

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    public ConfiguredModelException(String resourceName, Exception cause) {
        this();
        initCause(cause);
        this.resourceName = resourceName;
    }

    @Override
    public String getMessage() {
        if (missingProperties != null) {
            StringJoiner sj = new StringJoiner(", ");
            Arrays.stream(missingProperties).forEach(sj::add);
            return NbBundle.getMessage(ConfiguredModelException.class, "ConfiguredModelException.message.missingProperties", sj.toString());
        } else if (getCause() != this) {
            return NbBundle.getMessage(ConfiguredModelException.class, "ConfiguredModelException.message.cause", getCause().getClass().getName(), getCause().getMessage(), resourceName);
        }
        return super.getMessage(); //To change body of generated methods, choose Tools | Templates.
    }

}
