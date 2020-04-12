///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package org.thespheres.betula.entities;
//
//import java.io.Serializable;
//import java.util.Date;
//import java.util.HashSet;
//import java.util.Set;
//import javax.persistence.Access;
//import javax.persistence.AccessType;
//import javax.persistence.Entity;
//import javax.persistence.JoinColumn;
//import javax.persistence.JoinTable;
//import javax.persistence.NamedQueries;
//import javax.persistence.NamedQuery;
//import javax.persistence.Table;
//import org.thespheres.betula.document.DocumentId;
//
///**
// *
// * @author boris.heithecker
// */
//@Deprecated
//@NamedQueries({
//    @NamedQuery(name = "findUnitJoinDocumentEntity", query = "SELECT DISTINCT u FROM UnitJoinDocumentEntity u, IN(u.joined) j " // 
//            + "WHERE j=:joined")})
//@Entity
//@Table(name = "UNITJOIN_DOCUMENT")
//@Access(AccessType.FIELD)
//public class UnitJoinDocumentEntity extends BaseDocumentEntity implements Serializable {
//
//    private static final long serialVersionUID = 1L;
//    @JoinTable(name = "JOINUNITS_DOCUMENT_UNITS",
//            joinColumns = {
//                @JoinColumn(name = "JOINUNITS_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
//                @JoinColumn(name = "JOINUNITS_DOCUMENT_ID_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
//                @JoinColumn(name = "JOINUNITS_DOCUMENT_ID_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)},
//            inverseJoinColumns = {
//                @JoinColumn(name = "UNIT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
//                @JoinColumn(name = "UNIT_DOCUMENT_ID_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
//                @JoinColumn(name = "UNIT_DOCUMENT_ID_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
//    private Set<UnitDocumentEntity> joined = new HashSet<>();
//
//    public UnitJoinDocumentEntity() {
//        super();
//    }
//
//    public UnitJoinDocumentEntity(DocumentId unit, SigneeEntity creator) {
//        super(unit, creator);
//    }
//
//    public UnitJoinDocumentEntity(DocumentId unit, SigneeEntity creator, Date creationTime) {
//        super(unit, creator, creationTime);
//    }
//
//    public Set<UnitDocumentEntity> getJoined() {
//        return joined;
//    }
//}
