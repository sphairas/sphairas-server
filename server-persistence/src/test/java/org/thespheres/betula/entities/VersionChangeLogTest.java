/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.AbstractMarker;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
public class VersionChangeLogTest {

    public VersionChangeLogTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAddLogWithVersionChangeLog() throws InterruptedException {
        System.out.println("addLogWithVersionChangeLog");
        BaseDocumentEntity doc = new BaseDocumentEntity(new DocumentId("auth", "doc", DocumentId.Version.LATEST), null);
        StudentMarkerMapChangeLog cl = new StudentMarkerMapChangeLog(doc, new StudentId("auth", 1000l), null, BaseChangeLog.Action.ADD);
        ChangeLogs.addLogWithVersionChangeLog(doc, Collections.singletonList(cl), null, 3);
        Thread.sleep(10);
        Date insert1 = new Date();
        Thread.sleep(100);
        Date insert2 = new Date();
        Thread.sleep(100);
        StudentMarkerMapChangeLog cl2 = new StudentMarkerMapChangeLog(doc, new StudentId("auth", 1001l), new AbstractMarker("conv", "s1", null), BaseChangeLog.Action.UPDATE);
        StudentMarkerMapChangeLog cl21 = new StudentMarkerMapChangeLog(doc, new StudentId("auth", 1001l), new AbstractMarker("conv", "s1", null), BaseChangeLog.Action.UPDATE);
        ChangeLogs.addLogWithVersionChangeLog(doc, Arrays.asList(cl2, cl21), null, 3);
        Thread.sleep(100);
        StudentMarkerMapChangeLog cl3 = new StudentMarkerMapChangeLog(doc, new StudentId("auth", 1003l), new AbstractMarker("conv", "s1", null), BaseChangeLog.Action.UPDATE);
        StudentMarkerMapChangeLog cl31 = new StudentMarkerMapChangeLog(doc, new StudentId("auth", 1003l), new AbstractMarker("conv", "s1", null), BaseChangeLog.Action.UPDATE);
        ChangeLogs.addLogWithVersionChangeLog(doc, Arrays.asList(cl3, cl31), insert2, 3);
        Thread.sleep(10);
        StudentMarkerMapChangeLog cl4 = new StudentMarkerMapChangeLog(doc, new StudentId("auth", 1004l), new AbstractMarker("conv", "s1", null), BaseChangeLog.Action.UPDATE);
        ChangeLogs.addLogWithVersionChangeLog(doc, Collections.singletonList(cl4), insert1, 4);
        Thread.sleep(10);
                Thread.sleep(10);
        StudentMarkerMapChangeLog cl5 = new StudentMarkerMapChangeLog(doc, new StudentId("auth", 1005l), new AbstractMarker("conv", "zus", null), BaseChangeLog.Action.UPDATE);
        ChangeLogs.addLogWithVersionChangeLog(doc, Collections.singletonList(cl5), insert1, 4);
        Date effective = null;
//        VersionChangeLog.(doc, l, effective);
        doc.getChangeLog()
                .forEach(l -> System.out.println(l.toString()));
        System.out.println(doc.getCurrentVersion().getVersion());
    }

}
