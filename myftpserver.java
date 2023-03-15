import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class myftpserver {
    public static void main(String args[]) {
        if(args.length != 1){System.out.println("Error: invalid arguments");System.exit(0);}
        if(Integer.parseInt(args[0]) <= 1023 || Integer.parseInt(args[0]) > 65535) {
            System.out.println("Error: invalid port range, port range 1024 to 65535 are valid for non system processes");
            System.exit(0);
        }
//      try inititlizing a server
        try {
//            binding server at the provided port
            ServerSocket sS = new ServerSocket(Integer.parseInt(args[0]));
            System.out.println("Server started on port " + args[0]);
            while(true) {
//            accept clients incomming connection
                try {
//                establish the connection with a new client
                    Socket client = sS.accept();
                    System.out.println("New client connected : " + client.getInetAddress().getHostAddress());
//                    creating threads to talk to multiple clients
                    new Thread(new ClientThreadHandler(client)).start();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error: unable to talk to the client with the following error : " + e);
                }
            }
        }
        catch (NumberFormatException portEx) {System.out.println("Error: invalid port type provided, port must be an integer value : " + portEx);}
        catch (IOException io) {io.printStackTrace();System.out.println("Error: unable to initialize the server : " + io);System.exit(0);}
        catch (Exception e) {System.out.println("Error: following error caught: " + e);}
    }
}
class ClientThreadHandler implements Runnable {
    private Socket clientInstance;
    private DataOutputStream dos;
    private DataInputStream dis;
//    ClientThreadHandler Constructor
    public ClientThreadHandler(Socket client) {
        try {
            this.clientInstance = client;
//            get data from client
            dis = new DataInputStream(clientInstance.getInputStream());
//            send data to client
            dos = new DataOutputStream(clientInstance.getOutputStream());
        }
        catch (IOException io) {
            Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.SEVERE, null, io);
        }
    }
    public void run() {
        Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.INFO, "Cilent Connected");
        try {
//                listening continuously to what client has to say
            while(true) {
                String cmd = dis.readUTF();
//                for(String a : cmd.split(" ", 2)){System.out.println(a);}
                System.out.println();
                if(cmd.split(" ", 2)[0].equals("get"))
                    get(cmd, clientInstance.getInetAddress().getHostAddress());
                if(cmd.split(" ", 2)[0].equals("put"))
                    put(cmd, clientInstance.getInetAddress().getHostAddress());
                if(cmd.split(" ", 2)[0].equals("cd"))
                    chdir(cmd, clientInstance.getInetAddress().getHostAddress());
                if(cmd.trim().equals("pwd"))
                    abspath(cmd);
                if(cmd.split(" ", 2)[0].equals("delete"))
                    delete(cmd);
                if(cmd.split(" ", 2)[0].equals("mkdir"))
                    mkdir(cmd);
                if(cmd.split(" ", 2)[0].equals("ls"))
                    list(cmd);
                if(cmd.trim().equals("quit")) {
                    System.out.println("Client " + clientInstance.getInetAddress().getHostAddress() + " leaving.....");
                    clientInstance.close();
                    break;
                }
            }
        } catch(IOException io) {
            Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.SEVERE, null, io);
        }
    }
    public void get(String cmd, String address) {
//        System.out.println(cmd.split(" ").length);
        if(!cmd.contains(" ") || cmd.split(" ").length < 2) {
            try {
                System.out.println("Invalid get command recevied");
                dos.writeUTF("Error: Invalid 'get' command, check man page for details");
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
//        get the filename to be transfered
        String file = cmd.split(" ")[cmd.split(" ").length-1];
        File cdir=new File(System.getProperty("user.dir"));
        File sendFile = new File(cdir, file);
        try {
            if(!sendFile.exists() || !sendFile.canRead()) {
//                error if file doesn't exists or is unreadable by access
                dos.writeUTF("Error: cannot access '" + file + "': No such file or directory or unable to read file");
                return;
            }
            else {
                dos.writeUTF("receiving " + sendFile.length() + " bytes...");
                System.out.println("sending sendFile " + file + " to client " + address);
            }
//            open file that is to be transfered
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
//            dos.writeUTF("file received..");
//            close the file after transfer
            fis.close();
            dos.flush();
        } catch (IOException io) {
            Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.SEVERE, null, io);
        }
    }
    public void put(String cmd, String address) {
        if(!cmd.contains(" ") || cmd.split(" ").length < 2) {
            try {
                System.out.println("Invalid put command recevied");
                dos.writeUTF("Error: Invalid 'put' command, check man page for details");
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
        String file = cmd.split(" ")[1];
        File cdir = new File(System.getProperty("user.dir"));
        File getFile = new File(cdir, file);
//        receive file from client
        try {
            if(getFile.exists() || !cdir.canWrite()) {
//                error if file already exists or file cannot be written into the dir
                dos.writeUTF("Error transferring file '" + file + "', file already exists or don't have access");
                System.out.println("Error: file already exists or don't have access");
                return;
            }
            else {
                dos.writeUTF("ready to receive..");
                System.out.println("receiving from client " + address);
            }
            int bytes = 0;
            FileOutputStream fos = new FileOutputStream(getFile);
            long fileSize = dis.readLong(); // read file size
            System.out.println("receiving " + fileSize + " bytes...");
            byte[] buffer = new byte[4 * 1024];
            while (fileSize > 0 &&
                    (bytes = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                fos.write(buffer, 0, bytes);
                fileSize -= bytes;
            }
            System.out.println(dis.readUTF());
            fos.close();
            dos.flush();
        } catch (IOException ex) {
            Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void chdir(String cmd, String address) {
        if(!cmd.contains(" ") || cmd.split(" ").length < 2) {
            try {
                System.out.println("Invalid cd command recevied");
                dos.writeUTF("Error: Invalid 'cd' command, check man page for details");
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
        File cdir = new File(System.getProperty("user.dir"));
        String destdir = cmd.split(" ")[1];
        File destdirname = new File(destdir);
        try {
            if(destdir.equals("..")) {
                System.setProperty("user.dir", cdir.getParent());
                dos.writeUTF("INFO: directory changed to " + cdir.getParent());
                System.out.println("INFO: directory changed to " + cdir.getParent());
                return;
            }
            if(!destdirname.isAbsolute()){
                System.setProperty("user.dir", String.valueOf(cdir + "/" + destdirname));
                dos.writeUTF("Directory is set to "+System.getProperty("user.dir"));
                return;
            }
            if(!destdirname.isDirectory()) {
                System.out.println("Error: directory doesn't exists!");
                dos.writeUTF("Error: directory doesn't exists!");
                return;
            }
            System.setProperty("user.dir", String.valueOf(destdirname));
            dos.writeUTF("INFO: directory changed to " + destdirname);
            System.out.println("INFO: directory changed to " + destdirname);
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void abspath(String cmd) {
        File cdir = new File(System.getProperty("user.dir"));
        try {
            dos.writeUTF(String.valueOf(cdir));
            dos.flush();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    public void delete(String cmd){
        if(!cmd.contains(" ") || cmd.split(" ").length < 2) {
            try {
                System.out.println("Invalid delete command recevied");
                dos.writeUTF("Error: Invalid 'delete' command, check main page for details");
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
//      File to be deleted
        String file = cmd.split(" ")[1];
        File cdir=new File(System.getProperty("user.dir"));
        File deleteFile = new File(cdir, file);
        try {
            if(!deleteFile.exists() || !deleteFile.canWrite()) {
//                error if file doesn't exists or cannot be modified
                dos.writeUTF("Error: Invalid filename or file doesn't exist");
                return;
            }
            if(deleteFile.delete()){
                dos.writeUTF("INFO: " + file + " deleted successfully");
                System.out.println("INFO: " + file + " deleted successfully");
            }
            else{
                dos.writeUTF("Error: Unable to delete file "+file);
                System.out.println("INFO: " + file + " deleted successfully");
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    public void mkdir(String cmd) {
        if(!cmd.contains(" ") || cmd.split(" ").length < 2) {
            try {
                System.out.println("Invalid mkdir command recevied");
                dos.writeUTF("Error: Invalid 'mkdir' command, check main page for details");
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
        File cdir=new File(System.getProperty("user.dir"));
        File destdirname = new File(cmd.split(" ")[1]);
        try {
            if(!destdirname.isAbsolute()){
                File f = new File(String.valueOf(cdir + "/" + destdirname));
                if(!f.exists() && f.mkdir() && cdir.canWrite()) {
                    dos.writeUTF("Directory " + f + " created");
                } else {
                    dos.writeUTF("Error: Cannot create " + f);
                }
                return;
            }
            if(!destdirname.exists() && destdirname.mkdir() && destdirname.getParentFile().canWrite()) {
                dos.writeUTF("Directory " + destdirname + " created");
            }
            else{
                dos.writeUTF("Error: Cannot create " + destdirname);
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    public void list(String cmd) {
        try{
            File lsPath = null;
            File[] fileList = null;
            File cdir=new File(System.getProperty("user.dir"));
            File file=new File(cdir.getAbsolutePath());
//            System.out.println(String.valueOf(file));
//            System.out.println(cmd.split(" ").length);
            if(cmd.equals("ls") || cmd.split(" ")[cmd.split(" ").length -1].equals(".")) {
                lsPath = file;
                System.out.println(String.valueOf(lsPath));
                dos.writeUTF("sending list...");
            }
            else {
                File dest = new File(cmd.split(" ")[cmd.split(" ").length -1]);
                if(dest.isAbsolute() && dest.canRead() && dest.isDirectory()) {
                    lsPath = dest;
                    System.out.println(String.valueOf(lsPath));
                    dos.writeUTF("sending list...");
                } else if (!dest.isAbsolute() && dest.canRead() && dest.isDirectory()) {
                    lsPath = new File(String.valueOf(file),String.valueOf(dest));
                    System.out.println(String.valueOf(lsPath));
                    dos.writeUTF("sending list...");
                } else {
                    dos.writeUTF("Error: Unknown Error");
                    System.out.println("Error: Unknown Error");
                    return;
                }
            }
            if(lsPath.canRead()) {
//                if read permission
                dos.writeUTF("sending files..");
                fileList = lsPath.listFiles();
//                Create object output stream
                ObjectOutputStream outFiles = new ObjectOutputStream(clientInstance.getOutputStream());
//                Write the files to output stream
                System.out.println("INFO: sending file list..");
                outFiles.writeObject(fileList);
            } else {
                dos.writeUTF("Error: Permission denied");
            }
        } catch (IOException io) {
            io.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }
}