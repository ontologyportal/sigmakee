
package com.articulate.sigma;


public class Login {

    /************************************************************
     * Trivial case for now that allows only "admin" userId and everything
     * else is given read-only privileges.
     */
    public static String validateUser(String inputUserid, String inputPwd) {

        if (inputUserid.equalsIgnoreCase("admin") &&
            inputPwd.equalsIgnoreCase("admin"))
            return "admin";
        return "user";
    }

}
