/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;

import java.util.Hashtable;
import jpl.*;

public class SWIUtil
{
  // ask SWI-Prolog to list its database to standard output.
  public static void listing()
  {
    System.out.print( "listing ..." );
    Query query = new Query("listing" );

    if ( !query.query() ){
            System.out.println( "listing : listing failed" );
            System.exit( 1 );
    }
    System.out.println( "listing succeeds." );
  }

  public static void consult(String fileToLoad)
  {
          System.out.println( "consult " + fileToLoad );
          Query query =
                  new Query(
                          "consult",
                          JPLUtil.toTermArray(
                                  new Atom( fileToLoad ) ) );

          if ( !query.query() ){
                  System.out.println( "consult: failed :" + fileToLoad );
                  System.exit( 1 );
          }
          System.out.println( "consult: succeed." );
  }

  public static void cd(String workingDirPath)
  {
          System.out.println( "cd " + workingDirPath );
          Query query =
                  new Query(
                          "cd",
                          JPLUtil.toTermArray(
                                  new Atom( workingDirPath ) ) );

          if ( !query.query() ){
                  System.out.println( "cd: failed :" + workingDirPath );
                  System.exit( 1 );
          }
          System.out.println( "cd: succeed." );
  }

  // show the current working directory path to standard output
  public static void pwd()
  {
          System.out.println( "pwd. ");
          Query query = new Query("pwd");

          if ( !query.query() ){
                  System.out.println( "pwd failed :");
                  System.exit( 1 );
          }
          System.out.println( "pwd: succeed." );
  }

}

