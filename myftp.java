import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;

public class myftp {
    private static Socket s = null;
    private static DataInputStream dis = null;
    private static DataOutputStream dos = null;
    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Error: Invalid Number of arguments");
            System.exit(0);
        }
        // Connect to server
        try {
            s = new Socket(args[0], Integer.valueOf(args[1]));
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
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
            System.exit(0);
        }
        catch (Exception e) {
            // Handle exception
            e.printStackTrace();
            System.out.println("Error - Connection Failed");
            System.exit(0);
        }
    }    

    private static void interactiveShell() {
        Scanner userInput = new Scanner(System.in);
        try {
            while(true) {
//                Send command to server and call the function accordingly
                System.out.print("myftp> ");
                String cmd = userInput.nextLine();
                dos.writeUTF(cmd);
                if(cmd.split(" ", 2)[0].equals("get"))
                    get(cmd);
                if(cmd.split(" ", 2)[0].equals("put"))
                    put(cmd);
                if(cmd.split(" ", 2)[0].equals("cd"))
                    chdir(cmd);
                if(cmd.split(" ", 2)[0].equals("pwd"))
                    abspath(cmd);
                if(cmd.split(" ", 2)[0].equals("delete"))
                    delete(cmd);
                if(cmd.split(" ", 2)[0].equals("mkdir"))
                    mkdir(cmd);
                if(cmd.split(" ", 2)[0].equals("ls"))
                    list(cmd);
                if(cmd.trim().equals("quit")) {System.out.println("Untill next time Dawgs.....!!!");s.close(); break;}
                dos.flush();
            }
        } catch (IOException ex) {
            Logger.getLogger(myftp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void get(String cmd) {
        try {
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
            int bytes = 0;
            FileOutputStream fos = new FileOutputStream(getFile);
            long fileSize = dis.readLong(); // read file size
            System.out.println("receiving " + fileSize + " bytes...");
            byte[] buffer = new byte[4 * 1024];
            while (fileSize > 0 &&
                    (bytes = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                if(!getFile.exists() || cdir.canWrite()) {
                    fos.write(buffer, 0, bytes);
                }
                fileSize -= bytes;
            }
            if(getFile.exists() || !cdir.canWrite()) {
//                error if file already exists or file cannot be written into the dir
                System.out.println("Error: file already exists or don't have access");
                return;
            }
            System.out.println("File is Received");
            fos.close();

            dos.flush();
        } catch (IOException ex) {
            Logger.getLogger(myftp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void put(String cmd) {
        if(!cmd.contains(" ") || cmd.split(" ").length < 2) {
            try {
                System.out.println("Invalid get command recevied");
                dos.writeUTF("Error: Invalid 'get' command, check man page for details");
            } catch (IOException io) {
                io.printStackTrace();
            }
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
            dos.writeUTF("file received..");
//            close the file after transfer
            fis.close();
            dos.flush();
        } catch (IOException io) {
            Logger.getLogger(myftp.class.getName()).log(Level.SEVERE, null, io);
        }
    }
    public static void chdir(String cmd) {
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
            System.out.println(dis.readUTF());
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    public static void delete(String cmd) {
        try {
            System.out.println(dis.readUTF());
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    public static void mkdir(String cmd) {
        try {
            System.out.println(dis.readUTF());
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    public static void list(String cmd) {
        File[] files = null;
        try {
            String response = dis.readUTF();
            if(response.contains("Error")){
                System.out.println(response);
                return;
            }
            String resp = dis.readUTF();
            if(!resp.contains("Error")) {
                // Create object input stream
                ObjectInputStream inFiles = new ObjectInputStream(s.getInputStream());
                // Read the array of files
                files = (File[]) inFiles.readObject();
            } else{
                System.out.println(resp);
                return;
            }
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
}

