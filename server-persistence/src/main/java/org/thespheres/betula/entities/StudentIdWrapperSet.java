/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.util.AbstractSet;
import java.util.Iterator;
import org.thespheres.betula.StudentId;

/**
 *
 * @author boris.heithecker
 */
final class StudentIdWrapperSet extends AbstractSet<StudentId> {

    private final UnitDocumentEntity entity;

    StudentIdWrapperSet(final UnitDocumentEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean add(StudentId e) {
        return entity.studentIds.add(new EmbeddableStudentId(e));
    }

    @Override
    public Iterator<StudentId> iterator() {
        final Iterator<EmbeddableStudentId> it = entity.studentIds.iterator();
        return new Iterator<StudentId>() {

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public StudentId next() {
                return it.next().getStudentId();
            }

            @Override
            public void remove() {
                it.remove();
            }

        };
    }

    @Override
    public int size() {
        return entity.studentIds.size();
    }

}
