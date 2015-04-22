/******************************************************************************
 * This file is part of de.bb.tools.bnm.core.
 *
 *   de.bb.tools.bnm.core is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.core is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.core.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm;

import java.io.File;

/**
 * Main class for bnm (bnm is not maven). An experiment to show what performance
 * is.
 * 
 * @author sfranke
 */
public class Main {

    /**
     * Usage message.
     */
    private static final String USAGE = "java de.bb.tools.bnm.Main [-N] (<phase[@start phase]>|<plugin:goal>+ [-D<name>=<value>]*\r\n"
            + "where\r\n"
            + "  -?               : display this message\r\n"
            + "  -D<name>=<value> : define a property\r\n"
            + "  -N               : process only current pom.xml\r\n"
            + "  -R<path>         : override the repository\r\n"
            + "  -X               : enable debug infos\r\n"
            + "  --snapshots      : download SNAPSHOT artifacts if outside of current project\r\n"
            + "  --epom           : print the effective pom\r\n"
            + "  -t<n>            : specify the max thread count\r\n"
            + "  -s               : skip goals if there is no modification\r\n";

    /**
     * The initial Pom instance.
     */
    private static RootPom rootBnm;

    /**
     * Flags
     */
    private static boolean flagOnlyCurrent;
    private static File repoPath;
    private static Bnm bnm;

    private static boolean skip;

    private static boolean printEpom;

    private static boolean downloadSnapshots;

    /**
     * Main method. Here we start.
     * 
     * @args the command line arguments
     */
    public static void main(String args[]) {
        long start = System.currentTimeMillis();

        String msg = " SUCCESS";
        Log log = Log.getLog();
        try {
            Loader loader = new Loader();
            bnm = new Bnm(loader);
            args = doOptions(args);
            loader.setSnapshotBehaviour(downloadSnapshots);
            bnm.setSkipUnchanged(skip);

            if (args.length == 0 && !printEpom) {
                log.info("nothing to do! use -? for help");
            } else {

                bnm.loadSetting(repoPath);

                File dot = new File(".");
                if (flagOnlyCurrent) {
                    bnm.loadFirst(dot);
                } else {
                    bnm.loadRecursive(dot);
                }

                if (printEpom) {
                    bnm.printEpom();
                } else {
                    if (!bnm.process(args))
                        msg = " ERROR";
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            msg = " ERROR";
        }
        long diff = System.currentTimeMillis() - start;
        log.info(Bnm.LINE);
        log.info("DONE " + Bnm.DOTS.substring(4, 56) + msg
                + Bnm.DF.format(diff));
        log.info(Bnm.LINE);
        Runtime rt = Runtime.getRuntime();
        int max = (int) (rt.totalMemory() >>> 20);
        int used = (int) (rt.totalMemory() - rt.freeMemory() >>> 20);
        log.info(Pom.stats());
        log.info("Memory: " + used + "M/" + max + "M");
        log.info(Bnm.LINE);
        log.flush();
        Log.clear();
        // System.out.println(System.getProperties());
        // user.home
    }

    /**
     * parse the command line for options and return other parameters.
     * 
     * @param args
     *            all command line arguments
     * @return the command line arguments without the options
     * @throws Exception
     */
    private static String[] doOptions(String args[]) throws Exception {
        int j = 0;
        for (int i = 0; i < args.length; ++i) {
            if (args[i].charAt(0) != '-') // no argument
            {
                args[j++] = args[i];
                continue;
            }
            String o = args[i];
            // an argument
            if (o.equals("-?")) {
                System.out.println(USAGE);
                continue;
            }

            if (o.startsWith("-t")) {
                bnm.threadCount = Integer.parseInt(o.substring(2));
                continue;
            }

            if (o.equals("-N")) {
                flagOnlyCurrent = true;
                continue;
            }
            if (o.equals("-X")) {
                Log.DEBUG = true;
                Log.WARN = true;
                continue;
            }

            if (o.startsWith("-D")) {
                o = o.substring(2);
                if (o.length() == 0)
                    continue;
                int eq = o.indexOf('"');
                String val = "";
                if (eq > 0) {
                    val = o.substring(eq + 1);
                    o = o.substring(0, eq);
                }
                rootBnm.addProperty(o, val);
                continue;
            }

            if (o.startsWith("-R")) {
                o = o.substring(2);
                repoPath = new File(o);
                continue;
            }

            if (o.equals("-s")) {
                skip = true;
                continue;
            }

            if (o.equals("--epom")) {
                printEpom = true;
                continue;
            }

            if (o.equals("--snapshots")) {
                downloadSnapshots = true;
                continue;
            }

            /*
             * if (o.equals("-s")) { addStackMap = true; continue; }
             */
            throw new Exception("Invalid option '" + args[i - 1] + "'");
        }

        String res[] = new String[j];
        System.arraycopy(args, 0, res, 0, j);
        return res;
    }
}
