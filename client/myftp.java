import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;
import java.util.HashMap;

public class myftp {
    private static Socket s = null;
    private static DataInputStream dis = null;
    private static DataOutputStream dos = null;
    private static String hostname = null;
    private static int nport = 0;
    private static int tport = 0;
    private static HashMap<String, Boolean> procTable; //procTable(Command ID, Command termination status)
    private static HashMap<String, String> rmFiles;   //rmFiles(Command ID, File absPath)

    private static void interactiveShell() {
        Scanner userInput = new Scanner(System.in);
        try {
            Thread cleaning = new Thread(new GarbageCollector(procTable, rmFiles));
            cleaning.start();
            while(s.isConnected()) {
                Thread.sleep(500);
//                Send command to server and call the function accordingly
                System.out.print("\nmyftp> ");
                String cmd = userInput.nextLine();
                if(cmd.trim().startsWith("get") && cmd.trim().endsWith("&")) {
                    Thread getThread = new Thread(new GetInBackend(hostname, nport, cmd, procTable, rmFiles));
                    getThread.start();
                    continue;
                }
                else if(cmd.trim().startsWith("put") && cmd.trim().endsWith("&")) {
                    Thread putThread = new Thread(new PutInBackend(hostname, nport  , cmd, procTable));
                    putThread.start();
                    continue;
                }
                else if(cmd.split(" ", 2)[0].equals("get"))
                    get(cmd);
                else if(cmd.split(" ", 2)[0].equals("put"))
                    put(cmd);
                else if(cmd.split(" ", 2)[0].equals("cd"))
                    chdir(cmd);
                else if(cmd.split(" ", 2)[0].equals("pwd"))
                    abspath(cmd);
                else if(cmd.split(" ", 2)[0].equals("delete"))
                    delete(cmd);
                else if(cmd.split(" ", 2)[0].equals("mkdir"))
                    mkdir(cmd);
                else if(cmd.split(" ", 2)[0].equals("ls"))
                    list(cmd);
                else if(cmd.startsWith("terminate")) {
                    Thread terminateThread = new Thread(new Terminate(hostname, tport, cmd, procTable));
                    terminateThread.start();
                }
                else if(cmd.trim().equals("quit")) {System.out.println("Untill next time Dawgs.....!!!");quit(cmd);s.close(); break;}
                else {System.out.println("Error: Invalid input");}
            }
        } catch (IOException io) {
            Logger.getLogger(myftp.class.getName()).log(Level.SEVERE, null, io);
        } catch (InterruptedException ie) {
            Logger.getLogger(myftp.class.getName()).log(Level.SEVERE, null, ie);
        }
    }
    public static void quit(String cmd) throws IOException {
        dos = new DataOutputStream(s.getOutputStream());
        dos.writeUTF(cmd);
    }
    public static void get(String cmd) {
        try {
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(cmd);
            String response = dis.readUTF();
            if(response.contains("Error")){
                System.out.println(response);
                return;
            }
            System.out.println(response);
        } catch (IOException io) {
            io.printStackTrace();
        }
//        get the filename to be transfered
        String file = cmd.split(" ")[1];
        File cdir = new File(System.getProperty("user.dir"));
        File getFile = new File(cdir, file);
//        receive file from client
        try {
            if(!cdir.canWrite()) { //getFile.exists() || if append is not desirable
//                error if file already exists or file cannot be written into the dir
                System.out.println("Error: don't have access to write");
                return;
            }
            int bytes = 0;
            FileOutputStream fos = new FileOutputStream(getFile, true);
            long fileSize = dis.readLong(); // read file size
            System.out.println("receiving " + fileSize + " bytes...");
            String cid = dis.readUTF();
            System.out.print("Command ID : " + cid + "\n");
            byte[] buffer = new byte[4 * 1024];
            while (fileSize > 0 &&
                    (bytes = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                if(!getFile.exists() || cdir.canWrite()) {
                    fos.write(buffer, 0, bytes);
                }
                fileSize -= bytes;
            }
            System.out.println("File is Received");
            fos.close();

            dos.flush();
        } catch (IOException ex) {
            Logger.getLogger(myftp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void put(String cmd) {
        try {
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(cmd);
        } catch(IOException io) {
            io.printStackTrace();
        }
//        get the filename to be transfered
        String file = cmd.split(" ")[1];
        File cdir=new File(System.getProperty("user.dir"));
        File sendFile = new File(cdir, file);
        System.out.println(sendFile);
        try {
            if(!sendFile.exists() || !sendFile.canRead()) {
//                error if file doesn't exists or is unreadable by access
                System.out.println("Error: cannot access '" + file + "': No such file or directory or unable to read file");
                return;
            }
            else {
                String response = dis.readUTF();
                if(response.contains("Error")){
                    System.out.println(response);
                    return;
                }
            }
//            open file that is to be transfered
            System.out.println("Transferring file to server");
            FileInputStream fis = new FileInputStream(sendFile);
            int bytes = 0;
            dos.writeLong(sendFile.length());
//            break file in chucks and send it to client
            byte[] buffer = new byte[4*1024];
            while((bytes = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytes);
                dos.flush();
            }
            System.out.println("file transferred");
//            close the file after transfer
            fis.close();
            dos.flush();
        } catch (IOException io) {
            Logger.getLogger(myftp.class.getName()).log(Level.SEVERE, null, io);
        }
    }
    public static void chdir(String cmd) {
        try {
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(cmd);
        } catch(IOException io) {
            io.printStackTrace();
        }
        try {
            String ack = dis.readUTF();
            if(ack.contains("Error")){
                System.out.println(ack);
                return;
            }
            System.out.println(ack);
            dos.flush();
        } catch(IOException io) {
            io.printStackTrace();
        }
    }
    public static void abspath(String cmd) {
        try {
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(cmd);
            System.out.println(dis.readUTF());
        } catch(IOException io) {
            io.printStackTrace();
        }
    }
    public static void delete(String cmd) {
        try {
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(cmd);
            System.out.println(dis.readUTF());
        } catch(IOException io) {
            io.printStackTrace();
        }
    }
    public static void mkdir(String cmd) {
        try {
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(cmd);
            System.out.println(dis.readUTF());
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    public static void list(String cmd) {
        File[] files = null;
        try {
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(cmd);
            String response = dis.readUTF();
            if(response.contains("Error")){
                System.out.println(response);
                return;
            }
        } catch(IOException io) {
            io.printStackTrace();
        }
        try {
            ObjectInputStream inFiles = new ObjectInputStream(s.getInputStream());
            // Read the array of files
            files = (File[]) inFiles.readObject();
        }
        catch (ClassNotFoundException cl) {
            cl.printStackTrace();
        }
        catch (IOException io) {
            io.printStackTrace();
        }
        // Print the file name for each file
        for(File file: files){
            System.out.println(file.getName() + " ");
        }
    }
    public static void main(String[] args) {
        if(args.length != 3){
            System.out.println("Error: Invalid Number of arguments");
            System.exit(0);
        }
        // Connect to server
        try {
            hostname = args[0];
            nport = Integer.valueOf(args[1]);
            tport = Integer.valueOf(args[2]);
            s = new Socket(hostname, nport);
            procTable = new HashMap<>();
            rmFiles = new HashMap<>();
            System.out.println("Client connected to server " + args[0] + " on port " + Integer.valueOf(args[1]));
//            start interactive ftp shell
            interactiveShell();

        }
        catch (NumberFormatException portEx){
            System.out.println("Error: invalid port type provided, port must be an integer value : " + portEx);
            System.exit(0);
        }
        catch (IOException io){
            io.printStackTrace();
            System.out.println("Error: unable to initialize the server : " + io);
            System.exit(1);
        }
        catch (Exception e) {
            // Handle exception
            e.printStackTrace();
            System.out.println("Error - Connection Failed");
            System.exit(0);
        }
    }
}

class PutInBackend implements Runnable {
    private Socket s;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String command;
    HashMap<String, Boolean> procTable;

    public PutInBackend(String hostname, int port, String command, HashMap<String, Boolean> procTable) {
        try {
            this.command = command;
            this.procTable = procTable;
            this.s = new Socket(hostname, port);
            dis = new DataInputStream(this.s.getInputStream());
            dos = new DataOutputStream(this.s.getOutputStream());
        } catch(IOException io) {
            Logger.getLogger(PutInBackend.class.getName()).log(Level.SEVERE, null, io);
        }
    }
    public synchronized void run() {
        try {
            String file = command.split(" ")[1];
            File cdir=new File(System.getProperty("user.dir"));
            File sendFile = new File(cdir, file);
            System.out.println(sendFile);
            try {
                if(!sendFile.exists() || !sendFile.canRead()) {
//                error if file doesn't exists or is unreadable by access
                    System.out.println("Error: cannot access '" + file + "': No such file or directory or unable to read file");
                    return;
                }
                else {
                    String response = dis.readUTF();
                    if(response.contains("Error")){
                        System.out.println(response);
                        return;
                    }
                }
                String cid = dis.readUTF();
                System.out.println("Command ID is : " + cid);
                procTable.put(cid, Boolean.FALSE);
                //open file that is to be transfered
                System.out.println("Transferring file to server");
                FileInputStream fis = new FileInputStream(sendFile);
                int bytes = 0;
                dos.writeLong(sendFile.length());
                //break file in chucks and send it to client
                byte[] buffer = new byte[4*1024];
                while((bytes = fis.read(buffer)) != -1) {
                    if(procTable.get(cid).equals(Boolean.TRUE)){
                        procTable.remove(cid);
                        fis.close();
                        dos.writeUTF("quit");
                        dos.flush();
                        s.close();
                        return;
                    }
                    dos.write(buffer, 0, bytes);
                }
                System.out.println("\n"+sendFile+" File sent");
                System.out.print("myftp> ");
                dos.writeUTF("file transfer complete..");
                procTable.remove(cid);
                dos.writeUTF("quit");
                //close the file after transfer and quit the thread and close connection
                fis.close();
                dos.flush();
                s.close();
            } catch (IOException io) {
                Logger.getLogger(PutInBackend.class.getName()).log(Level.SEVERE, null, io);
            }
        } catch(Exception e) {
            Logger.getLogger(PutInBackend.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
class GetInBackend implements Runnable {
    private Socket s;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String command;
    HashMap<String, Boolean> procTable;
    HashMap<String, String> rmFiles;

    public GetInBackend(String hostname, int port, String command, HashMap<String, Boolean> procTable, HashMap<String, String> rmFiles) {
        try {
            this.command = command;
            this.rmFiles = rmFiles;
            this.procTable = procTable;
            this.s = new Socket(hostname, port);
            dis = new DataInputStream(this.s.getInputStream());
            dos = new DataOutputStream(this.s.getOutputStream());
        } catch(IOException io) {
            Logger.getLogger(GetInBackend.class.getName()).log(Level.SEVERE, null, io);
        }
    }
    //Get the files from server passively
    public synchronized  void run() {
        try {
            dos.writeUTF(command);
            String response = dis.readUTF();
            if(!response.contains("sending")){
                System.out.println(response); 
                return;
            }
            long fileSize = dis.readLong(); // read file size
            String cid = dis.readUTF();
            System.out.println("Command ID is : " + cid);
            procTable.put(cid, Boolean.FALSE);
            //get file size to be received
            //start getting
            String file = command.split(" ")[1];
            File cdir = new File(System.getProperty("user.dir"));
            File getFile = new File(cdir, file);
            //receive file from client
            try {
                int bytes = 0;
                FileOutputStream fos = new FileOutputStream(getFile);
                rmFiles.put(cid, String.valueOf(getFile));
                
                System.out.println("receiving " + fileSize + " bytes...");
                byte[] buffer = new byte[4 * 1024];
                while (fileSize > 0 && (bytes = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                    if(!procTable.get(cid).equals(Boolean.TRUE)) {
                        fos.write(buffer, 0, bytes);
                        fileSize -= bytes;
                    } else {
                        getFile.delete();
                        dos.writeUTF("quit");
                        rmFiles.remove(cid);
                        procTable.remove(cid);
                        fos.close();
                        dos.flush();
                        s.close();
                        return;
                    }
                }
//                if(getFile.exists() || !cdir.canWrite()) {
////                error if file already exists or file cannot be written into the dir
//                    System.out.println("Error: file already exists or don't have access");
//                    return;
//                }
                rmFiles.remove(cid);
                procTable.remove(cid);
                System.out.println("\n"+getFile+" file received");
                System.out.print("myftp> ");
                dos.writeUTF("quit");
                s.close();
                fos.close();
                dos.flush();
            } catch (IOException io) {
                Logger.getLogger(GetInBackend.class.getName()).log(Level.SEVERE, null, io);
            }
            //finish getting
        } catch (IOException io) {
            Logger.getLogger(GetInBackend.class.getName()).log(Level.SEVERE, null, io);
        }
    }
}

class Terminate implements Runnable {
    private Socket s;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String command;
    HashMap<String, Boolean> procTable;

    public Terminate(String hostname, int port, String command, HashMap<String, Boolean> procTable) {
        try {
            s = new Socket(hostname, port);
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            this.command = command;
            this.procTable = procTable;
        } catch (IOException io) {
            Logger.getLogger(Terminate.class.getName()).log(Level.SEVERE, null, io);
        }
    }

    public void run() {
        try {
            String cid = command.split(" ")[1];
            dos.writeUTF(command);
            String response = dis.readUTF();
            if(response.contains("terminating") && procTable.containsKey(cid)){
                procTable.put(cid, Boolean.TRUE);
                System.out.println("Terminating command: " + cid);
            }
            if(!response.contains("terminating")) {
                System.out.println(response);
            }
            if(!procTable.containsKey(cid)) {
                System.out.println("CommandID" + cid + " either already terminated or doesn't exists");
            }
            s.close();
        } catch(IOException io) {
            Logger.getLogger(Terminate.class.getName()).log(Level.SEVERE, null, io);
        }
    }

}

class GarbageCollector implements Runnable {
    HashMap<String, Boolean> procTable;
    HashMap<String, String> rmFiles;

    public GarbageCollector(HashMap<String, Boolean> procTable, HashMap<String, String> rmFiles) {
        this.procTable = procTable;
        this.rmFiles = rmFiles;
    }

    public void run() {
        while(true) {
            try {
                for(String cid : procTable.keySet()) {
                    if(procTable.get(cid).equals(Boolean.TRUE) && rmFiles.containsKey(cid)) { //procTable entry vaule TRUE means that the file is not in use anymore and needs to be removed
                        File file = new File(rmFiles.get(cid));
                        file.delete();
                        rmFiles.remove(cid);
                        procTable.remove(cid);
                    }
                }
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Logger.getLogger(GarbageCollector.class.getName()).log(Level.SEVERE, null, ie);
            }
        }
    }
}
