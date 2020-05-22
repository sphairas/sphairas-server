/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbNumberFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.config.PropertyOrderStrategy;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class Document {

//    @JsonbProperty("person-name")
//    private String name;
//    @JsonbTransient
//    private String name2;
//    @JsonbDateFormat("dd.MM.yyyy")
//    private Date birthDate;

    @JsonbNumberFormat("#0.00")
    public BigDecimal salary;
    @JsonbProperty("configuration")
    private Configuration config;
    @JsonbProperty("columns")
    private final List<Column> columns = new ArrayList<>();
    @JsonbProperty("rows")
    private final List<Row> row = new ArrayList<>();

    public Configuration getConfig() {
        return config;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public List<Row> getRow() {
        return row;
    }

}
