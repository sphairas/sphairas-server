/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.util.Properties;
import java.util.StringJoiner;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 *
 * @author boris.heithecker
 */
//ACHTUNG: Muss in persistence.xml gelistet sein!!!
@Converter(autoApply = true)
public class PropertiesConverter implements AttributeConverter<Properties, String> {

    @Override
    public String convertToDatabaseColumn(Properties list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        final StringJoiner sj = new StringJoiner(",");
        for (String k : list.stringPropertyNames()) {
            sj.add(k + "=" + list.getProperty(k));
        }
        return sj.toString();
    }

    @Override
    public Properties convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        Properties ret = new Properties();
        for (String p : dbData.split(",")) {
            String[] pp = p.split("=");
            if (pp.length == 2) {
                ret.put(pp[0], pp[1]);
            }
        }
        return ret;
    }

}
