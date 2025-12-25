/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import org.thespheres.ical.impl.ParameterList;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 *
 * @author boris.heithecker
 */
//ACHTUNG: Muss in persistence.xml gelistet sein!!!
@Converter(autoApply = true)
public class ParameterListConverter implements AttributeConverter<ParameterList, String> {

    @Override
    public String convertToDatabaseColumn(ParameterList list) {
        return list.toString();
    }

    @Override
    public ParameterList convertToEntityAttribute(String dbData) {
        return ParameterList.parse(dbData);
    }

}
