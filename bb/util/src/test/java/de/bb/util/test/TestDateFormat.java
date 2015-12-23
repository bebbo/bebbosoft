package de.bb.util.test;

/******************************************************************************
 * $Source: /export/CVS/java/de/bb/util/src/test/java/de/bb/util/test/TestDateFormat.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/01 13:30:21 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 ******************************************************************************
 NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Every product and solution using this software, must be free
 of any charge. If the software is used by a client part, the
 server part must also be free and vice versa.

 2. Each redistribution must retain the copyright notice, and
 this list of conditions and the following disclaimer.

 3. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in
 the documentation and/or other materials provided with the
 distribution.

 4. All advertising materials mentioning features or use of this
 software must display the following acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 5. Redistributions of any form whatsoever must retain the following
 acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
 DISCLAIMER OF WARRANTY

 Software is provided "AS IS," without a warranty of any kind.
 You may use it on your own risk.

 ******************************************************************************
 LIMITATION OF LIABILITY

 I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
 AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
 FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
 OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
 SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
 COPYRIGHT

 (c) 2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 Created on 01.01.2004

 *****************************************************************************/

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

import org.junit.Test;

import de.bb.util.DateFormat;

/**
 * @author bebbo
 */
public class TestDateFormat extends TestCase {

    @Test
    public void testFormat1() {
        DateFormat df = new DateFormat("m:ss.S");
        String ms1 = df.format(1);
        assertEquals(" 0:00.0", ms1);
        String ms10 = df.format(10);
        assertEquals(" 0:00.0", ms10);
        String ms100 = df.format(100);
        assertEquals(" 0:00.1", ms100);
        String s1 = df.format(1000);
        assertEquals(" 0:01.0", s1);
    }

    private static void xest(long l) {
        String d1 = DateFormat.dd_MMM_yyyy_HH_mm_ss_zzzz(l);
        String d2 = "" + new Date(l);
        long l2 = DateFormat.parse_dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(d1);
        if (l2 != l) {
            System.out.println(l + ": " + d1 + "!=" + d2);
            DateFormat.calc(l);
        }
    }

    public static void main(String[] args) throws ParseException {
        /*
         * String date = DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss_GMT(1072920000000L); System.out.println(date); date =
         * DateFormat.dd_MMM_yyyy_HH_mm_ss_zzzz(1072974230060L); System.out.println(date);
         */
        /*
         * date = DateFormat.dd_MMM_yyyy_HH_mm_ss_zzzz(1104447600000L); System.out.println("31.12.2004=" + date);
         * 
         * date = DateFormat.dd_MMM_yyyy_HH_mm_ss_zzzz(1104447600000L + 3600*1000*24); System.out.println("01.01.2005="
         * + date + "=" + new Date(date));
         */

        // test(1104447600000L);
        // test(1104447600000L + 3600*1000*24);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        for (int i = 1970; i < 2020; ++i) {
            long l = sdf.parse("01.01." + i).getTime();
            xest(l);
            l = sdf.parse("02.01." + i).getTime();
            xest(l);
            l = sdf.parse("28.02." + i).getTime();
            xest(l);
            l = sdf.parse("01.03." + i).getTime();
            xest(l);
            l = sdf.parse("30.12." + i).getTime();
            xest(l);
            l = sdf.parse("31.12." + i).getTime();
            xest(l);
        }

        long l1 = sdf.parse("01.10.2004").getTime();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(l1);
        int d1 = c.get(Calendar.DAY_OF_MONTH);

        DateFormat df = new DateFormat("dd.MM.yyyy");
        // long l2 = DateFormat.parse_dd_MM_yyyy_HH_mm_ss_GMT_zz_zz("01.10.2004");
        long l2 = df.parse("01.10.2004");
        int d2 = DateFormat.getDayOfMonth(l2);
        System.out.println(d1 + " == " + d2);
        System.out.println(l1 + " == " + l2);
    }

    @Test
    public void test1() throws ParseException {
        main(null);
    }
}

/******************************************************************************
 * Log: $Log: TestDateFormat.java,v $ Log: Revision 1.1 2011/01/01 13:30:21 bebbo Log: @N added to new CVS repo Log:
 * Log: Revision 1.1 2008/03/15 18:28:08 bebbo Log: @N just a check in Log: Log: Revision 1.3 2004/12/13 15:41:25 bebbo
 * Log: @B further changes in DateFormat - more testing required Log: Log: Revision 1.2 2004/12/08 14:56:31 bebbo Log: @R
 * better test Log: Log: Revision 1.1 2004/01/03 18:54:27 bebbo Log: @N simple main to test date format Log:
 ******************************************************************************/
