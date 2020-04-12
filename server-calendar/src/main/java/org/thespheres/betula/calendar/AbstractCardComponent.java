/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 *
 */
@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class AbstractCardComponent {

    @Version
    @Column(name = "COMPONENT_VERSION")
    private long version;
    @Column(name = "FN_VALUE")
    private String fullname;
    @Column(name = "N_VALUE")
    private String nprop;
    @Column(name = "GENDER_VALUE", length = 8)
    private String gender;

    public long getVersion() {
        return version;
    }

    public String getName() {
        return VCard.VCARD;
    }

    public abstract List<EmbeddableComponentProperty> getProperties();

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getNprop() {
        return nprop;
    }

    public void setNprop(String nprop) {
        this.nprop = nprop;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
