package org.ngafid;

import org.apache.maven.cli.MavenCli;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MavenStarter {

    public static void main(String[] args) throws IOException, InterruptedException {

        MavenCli cli = new MavenCli();
        //cli.doMain(new String[]{"clean", "install"}, "project_dir", System.out, System.out);
        cli.doMain(new String[]{"exec:java", "-Dexec.mainClass='org.ngafid.ProcessUpload'"}, "/ngafid/ngafid2.0", System.out, System.out);


        /*
        Process p = null;

        try {
            //String command = "mvn exec:java -Dexec.mainClass='org.ngafid.ProcessUpload'";

            //ProcessBuilder pb = new ProcessBuilder("mvn", "--debug", "compile", "exec:java", "-Dexec.args='-cp /ngafid/ngafid2.0/target/classes'", "-Dexec.mainClass='org.ngafid.ProcessUpload'");
            //ProcessBuilder pb = new ProcessBuilder("mvn", "compile", "exec:java", "-Dexec.mainClass='org.ngafid.Database'");
            ProcessBuilder pb = new ProcessBuilder("mvn", "compile", "exec:java", "-Dexec.args='-cp " + System.getProperty("java.class.path") + "'", "-Dexec.mainClass='org.ngafid.ProcessUpload'");
            pb.directory(new File("/ngafid/ngafid2.0"));

            System.out.println("starting maven process!");
            System.out.println("classpath '" + System.getProperty("java.class.path") + "'");

            Map<String,String> current_env = System.getenv();
            Map<String, String> process_env = pb.environment();

            System.out.println("environment variables:");
            for (String key : current_env.keySet()) {
                System.out.println("\t" + key + " - " + current_env.get(key));

                process_env.put(key, current_env.get(key));
            }

            p = pb.start();

            //p = Runtime.getRuntime().exec(command);

            System.out.println("process started!");
        } catch (IOException e) {
            System.err.println("Error on exec() method");
            e.printStackTrace();
        }

        copy(p.getInputStream(), System.out);
        p.waitFor();
        */

        System.out.println("completed!");

    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        while (true) {
            int c = in.read();
            if (c == -1)
                break;
            out.write((char) c);
        }
    }
}

