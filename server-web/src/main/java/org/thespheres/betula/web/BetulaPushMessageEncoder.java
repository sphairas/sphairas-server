/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import org.primefaces.json.JSONObject;
import org.primefaces.push.Encoder;

/**
 *
 * @author boris.heithecker
 */
public class BetulaPushMessageEncoder implements Encoder<BetulaPushMessage, String> {

    @Override
    public String encode(BetulaPushMessage s) {
        return new JSONObject(s).toString();
//        return new JSONObject(s).toString();
    }

}
