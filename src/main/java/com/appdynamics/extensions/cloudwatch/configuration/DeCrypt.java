/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.appdynamics.extensions.cloudwatch.configuration;
import org.appdynamics.crypto.*;

/**
 *
 * @author gilbert.solorzano
 */
public class DeCrypt {
    
    public static String decrypt(String str1){
        try{
            StringLogger sl = CryptoTool.getStringLogger();
            return sl.toLower1(sl.format1(str1));
        }catch(Exception e){
            e.printStackTrace();
        }
        return str1;
    }
}
