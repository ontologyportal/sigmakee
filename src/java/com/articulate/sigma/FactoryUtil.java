/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of
the GNU license.  This software is released under the GNU Public
License <http://www.gnu.org/copyleft/gpl.html>.  Users of this code
also consent, by use of this code, to credit Articulate Software and
Teknowledge in any writings, briefings, publications, presentations,
or other representations of any software which incorporates, builds
on, or uses this code.  Please cite the following article in any
publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in
Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed
Systems, August 9, Acapulco, Mexico.
*/

package com.articulate.sigma;


/** ***************************************************************
 * This class defines static accessors and factory methods for Sigma
 * facilities that require extensive initialization, or depend on
 * reflection, or must be created in a special context, or for which
 * creation through invocation of a simple constructor is not
 * adequate.
 */
public class FactoryUtil {

    /** *************************************************************
     * This class provides only static methods and should have no
     * instances.
     */
    private FactoryUtil() {
        // This class should not have any instances.
    }

//     /** *************************************************************
//      * This static method returns an instance that implements the
//      * <code>com.articulate.sigma.IPageGenerator</code> interface,
//      * which defines the single method
//      * <code>dispatch(com.articulate.sigma.RequestHandler.RequestMap
//      * request) for processing HTTP requests.
//      */
//     public static IPageGenerator getIPageGenerator() {
//         IPageGenerator ipg = null;
//         try {
//             Class clazz = Class.forName("com.articulate.sigma.PageGenerator");
//             if (clazz != null) {
//                 ipg = (IPageGenerator) clazz.newInstance();
//             }
//         }
//         catch (Exception ex) {
//             ex.printStackTrace();
//         }
//         return ipg;
//     }

} // FactoryUtil
