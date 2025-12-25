/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.util.List;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "DISCUSSION_MESSAGE")
@Access(AccessType.FIELD)
public class DiscussionMessage extends BaseTextMessage {

    @OneToMany(mappedBy = "parent", cascade = {CascadeType.ALL})
    @OrderColumn(name = "DISCUSSION_MESSAGE_CHILD_ORDER")
    private List<DiscussionMessage> children;
    @JoinColumns({
        @JoinColumn(name = "PARENT_MESSAGE_ID", referencedColumnName = "MESSAGE_ID"),
        @JoinColumn(name = "PARENT_MESSAGE_AUTHORITY", referencedColumnName = "MESSAGE_AUTHORITY"),
        @JoinColumn(name = "PARENT_MESSAGE_VERSION", referencedColumnName = "MESSAGE_VERSION")})
    @ManyToOne
    private DiscussionMessage parent;

}
