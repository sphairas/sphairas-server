/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "BASE_CHANNEL")
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
@XmlRootElement
public class BaseChannel implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern NAMEPATTERN = Pattern.compile("[äöüß\\p{Alpha}]+(-[äöüß\\p{Alpha}\\d]+)*(.\\d*)?", 0);
    @Id
    @Column(name = "CHANNEL_NAME")
    private String name;
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BaseMessage> messages = new HashSet<>();
    @javax.persistence.Version
    @Column(name = "BASE_CHANNEL_VERSION")
    private long entityVersion;
    @Column(name = "CHANNEL_POLICY")
    private String policyType;
    @Column(name = "CHANNEL_POLICY_INITARGS", length = 512)
    private Properties policyArgs;
    @Transient
    private ChannelPolicy policy;

    public BaseChannel() {
    }

    public BaseChannel(String name) {
        if (!NAMEPATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(name + " is not a valid channel name.");
        }
        this.name = name;
    }

    public BaseChannel(String baseName, int reification) {
        this(baseName + "." + Integer.toString(reification));
    }

    public String getName() {
        return this.name;
    }

    public int getReification() {
        int pos;
        if ((pos = name.indexOf('.')) != -1) {
            String num = name.substring(pos);
            return Integer.parseInt(num);
        }
        return 1;
    }

    public ChannelPolicy getChannelPolicy() {
        if (policy == null) {
            try {
                Class<ChannelPolicy> clz = (Class<ChannelPolicy>) Class.forName(policyType, true, Thread.currentThread().getContextClassLoader());
                Constructor c = clz.getDeclaredConstructor();
                policy = (ChannelPolicy) c.newInstance();
                Properties prop = policyArgs;
                if (prop == null) {
                    prop = new Properties();
                }
                policy.init(prop);
            } catch (ClassNotFoundException | ClassCastException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(BaseChannel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return policy;
    }

    @XmlTransient
    public Set<BaseMessage> getMessages() {
        return messages;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return 59 * hash + Objects.hashCode(this.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BaseChannel other = (BaseChannel) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return "org.thespheres.acer.entities.messages.BaseChannel[ id=" + name + " ]";
    }

}
