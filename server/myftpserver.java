import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
public class myftpserver {
    private static HashMap<String, Boolean> processRecord; //processRecord(cid, command finish status) True=command terminated
    private static HashMap<String, Boolean> lockRecord; //lockRecord(path, lock status) true=file is locked
    private static int nport = 0;
    private static int tport = 0;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Error: invalid arguments");
            System.exit(0);
        }
        try {
            nport = Integer.parseInt(args[0]);
            tport = Integer.parseInt(args[1]);
        } catch (NumberFormatException portEx){
            System.out.println("Error: invalid port type provided, port must be an integer value : " + portEx);
            System.exit(0);
        } catch (Exception e) {
            // Handle exception
            e.printStackTrace();
            System.out.println("Error - Connection Failed");
            System.exit(0);
        }
        if (nport <= 1023 || nport > 65535) {
            System.out.println("Error: invalid port range, port range 1024 to 65535 are valid for non system processes");
            System.exit(0);
        }
        if (tport <= 1023 || tport > 65535) {
            System.out.println("Error: invalid port range, port range 1024 to 65535 are valid for non system processes");
            System.exit(1);
        }

        processRecord = new HashMap<>();
        lockRecord = new HashMap<>();
//        Thread cleaning = new Thread(new GarbageCollector(processRecord, lockRecord));
//        cleaning.start();
        Thread commandProcess = new Thread(new CommandServer(nport, processRecord, lockRecord));
        commandProcess.start();
        Thread terminateProcess = new Thread(new TerminateServer(tport, processRecord));
        terminateProcess.start();
    }
}

class CommandServer implements Runnable{
    // private ServerSocket sS;
    //private Socket s;
    private int port;
    HashMap<String, Boolean> processRecord;
    HashMap<String, Boolean> lockRecord;
    public CommandServer(int port, HashMap<String, Boolean> processRecord, HashMap<String, Boolean> lockRecord){
        this.port = port;
        this.processRecord = processRecord;
        this.lockRecord = lockRecord;
    }

    @Override
    public void run() { //non synchronized run
        try {
//            binding server at the provided port
            ServerSocket sS = new ServerSocket(port);
            System.out.println("Command Server started on port " + port);
            while(true) {
//            accept clients incoming connection
                try {
//                establish the connection with a new client
                    Socket client = sS.accept();
                    System.out.println("New client connected : " + client.getInetAddress().getHostAddress());
//                    creating threads to talk to multiple clients
                    new Thread(new ClientThreadHandler(client, processRecord, lockRecord)).start();
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
class TerminateServer implements Runnable{
    //private ServerSocket sS;
    //private Socket s;
    private int port;
    HashMap<String, Boolean> processRecord;

    public TerminateServer(int port, HashMap<String, Boolean> processRecord){
        this.port = port;
        this.processRecord = processRecord;
    }

    @Override
    public void run() { //non synchronized run
        try {
//            binding server at the provided port
            ServerSocket sS = new ServerSocket(port);
            System.out.println("Terminate Server started on port " + port);
            while(true) {
//            accept clients incoming connection
                try {
//                establish the connection with client
                    Socket client = sS.accept();
                    System.out.println("New client connected on tport : " + client.getInetAddress().getHostAddress());
//                    creating threads to terminate service
                    new Thread(new TerminateService(client,processRecord)).start();
                } catch (IOException e) {
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

class TerminateService implements Runnable{
    private Socket s;
    private DataOutputStream dos;
    private DataInputStream dis;
    HashMap<String, Boolean> processRecord;

    public TerminateService(Socket s, HashMap<String, Boolean> processRecord)
    {
        // Instantiate data members
        try {
            this.s = s;
            dos = new DataOutputStream(s.getOutputStream());
            dis = new DataInputStream(s.getInputStream());
            this.processRecord = processRecord;
        } catch (IOException ex) {
            Logger.getLogger(TerminateService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() { //non synchronized run
        try {
            String command = dis.readUTF();
            System.out.println(command);
            terminate(command);
            s.close();
        } catch (IOException ex) {
            Logger.getLogger(TerminateService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void terminate(String command) {
        if(command.length() != 14 || !command.contains(" ")) {
            try {
                dos.writeUTF("Error: Invalid argument");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String cid = command.split(" ")[1];
        if(processRecord.containsKey(String.valueOf(cid))) {
            try {
                processRecord.put(cid, Boolean.TRUE);
                dos.writeUTF("terminating "+cid);
            } catch (IOException ex) {
                Logger.getLogger(TerminateService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            try {
                dos.writeUTF("Error: Command ID not found");
            } catch (IOException ex) {
                Logger.getLogger(TerminateService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}


class ClientThreadHandler implements Runnable{
    private Socket clientInstance;
    private DataOutputStream dos;
    private DataInputStream dis;
    HashMap<String, Boolean> processRecord;
    HashMap<String, Boolean> lockRecord;

    public ClientThreadHandler(Socket client, HashMap<String, Boolean> processRecord, HashMap<String, Boolean> lockRecord) {
        try {
            this.clientInstance = client;
            this.processRecord = processRecord;
            this.lockRecord = lockRecord;
//            dis = new DataInputStream(clientInstance.getInputStream());
//            dos = new DataOutputStream(clientInstance.getOutputStream());
        }
        catch(Exception e){
            Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    public void run() {
        Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.INFO, "Client Connected");
        try {
            while(true) {
                dis = new DataInputStream(clientInstance.getInputStream());
//                listening continuously to what client has to say
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
                if(cmd.split(" ", 2)[0].equals("quit")) {
                    System.out.println("Until Next Time Dawgs !!!!");
                    clientInstance.close();
                    if(!cmd.contains("thread")) System.exit(0);
                    return;
                }
            }
        } catch(IOException io) {
            io.printStackTrace();
        }
    }

    public synchronized void get(String cmd, String address) {
//        System.out.println(cmd.split(" ").length);
        try {dos = new DataOutputStream(clientInstance.getOutputStream());}
        catch(IOException io) {io.printStackTrace();}
        // if (!cmd.contains(" ") || cmd.split(" ").length < 2) {
        //     try {
        //         System.out.println("Invalid get command recevied");
        //         dos.writeUTF("Error: Invalid 'get' command, check man page for details");
        //         return;
        //     } catch (IOException io) {
        //         io.printStackTrace();
        //     }
        // }
//        get the filename to be transfered
        String file = cmd.split(" ")[1];
        File cdir = new File(System.getProperty("user.dir"));
        File sendFile = new File(cdir, file);
        try {
            if (!sendFile.exists() || !sendFile.canRead()) {
//                error if file doesn't exists or is unreadable by access
                dos.writeUTF("Error: cannot access '" + file + "': No such file or directory or unable to read file");
                return;
            } else {
                dos.writeUTF("server is sending " + sendFile.length() + " bytes...");
                System.out.println("sending File " + file + " to client " + address);
            }
            dos.writeLong(sendFile.length());
            String id;
            while (true) {
                UUID processID = UUID.randomUUID();
                id = processID.toString().substring(0, 4);
                System.out.println("Command ID : " + id);
                if (processRecord.containsKey(id))
                    continue;
                else {
                    processRecord.put(id, Boolean.FALSE);
                    dos.writeUTF(id.toString()); //sending command id to client
                    break;
                }
            }
            String path = sendFile.getAbsolutePath();
            while (true) {
                if (!lockRecord.containsKey(path)) {
                    lockRecord.put(path, true);
                    break;
                } else {
                    Thread.sleep(500);
                }
            }
//            open file that is to be transfered
            FileInputStream fis = new FileInputStream(sendFile);
            int bytes = 0;
//            break file in chucks and send it to client
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fis.read(buffer)) != -1) {
                if (processRecord.get(id).equals(Boolean.TRUE)) {
                    System.out.println("get command " + id + " terminated");
                    fis.close();
                    lockRecord.remove(id);
                    processRecord.remove(id);
                    return;
                }
                // System.out.println(".equals(Boolean.TRUE)");
                dos.write(buffer, 0, bytes);
            }
            lockRecord.remove(path);
            processRecord.remove(id);
            System.out.println("file transferred");
//            close the file after transfer
            fis.close();
            dos.flush();
        } catch (IOException io) {
            Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.SEVERE, null, io);
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void put(String cmd, String address) throws IOException {
        try {
            dos = new DataOutputStream(clientInstance.getOutputStream());
            dis = new DataInputStream(clientInstance.getInputStream());
        } catch(IOException io) {io.printStackTrace();}
        try {
            if(!cmd.contains(" ") || cmd.split(" ").length < 2) {
                System.out.println("Invalid put command received");
                dos.writeUTF("Error: Invalid 'put' command, check man page for details");
            } else {
                dos.writeUTF("server ready to receive");
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
        String cid;
        while (true) {
            UUID processID = UUID.randomUUID();
            cid = processID.toString().substring(0, 4);
            if (processRecord.containsKey(cid))
                continue;
            else {
                processRecord.put(cid, Boolean.FALSE);
                dos.writeUTF(cid.toString());
                break;
            }
        }

        //get the filename to be received
        String file = cmd.split(" ")[1];
        File cdir = new File(System.getProperty("user.dir"));
        File getFile = new File(cdir, file);
//        receive file from client
        try {
//            if(getFile.exists() || !cdir.canWrite()) {
////                error if file already exists or file cannot be written into the dir
//                dos.writeUTF("Error transferring file '" + file + "', file already exists or don't have access");
//                System.out.println("Error: file already exists or don't have access");
//                return;
//            }
//            else {
//                dos.writeUTF("ready to receive..");
//                System.out.println("receiving from client " + address);
//            }

            String path = getFile.getAbsolutePath();
            while (true) {
                if (!lockRecord.containsKey(path)) {
                    lockRecord.put(path, true);
                    break;
                } else {
                    Thread.sleep(500);
                }
            }
            int bytes = 0;
            FileOutputStream fos = new FileOutputStream(getFile, true);
            long fileSize = dis.readLong(); // read file size
            System.out.println("receiving " + fileSize + " bytes...");
            byte[] buffer = new byte[4 * 1024];
            while (fileSize > 0 &&
                    (bytes = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                if (processRecord.get(cid).equals(Boolean.TRUE)) {
                    fos.close();
                    getFile.delete();
                    lockRecord.remove(path);
                    processRecord.remove(cid);
                    return;
                }
                fos.write(buffer, 0, bytes);
                fileSize -= bytes;
                // Thread.sleep(5000); //remove this sleep later
            }
            System.out.println(dis.readUTF());
            lockRecord.remove(path);
            processRecord.remove(cid);
            fos.close();
            dos.flush();
        } catch (IOException ex) {
            Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientThreadHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void chdir(String cmd, String address) {
        try {
            dos = new DataOutputStream(clientInstance.getOutputStream());
        } catch(IOException io) {io.printStackTrace();}
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
        // File destdirname = new File(dest.getAbsolutePath());
        // System.out.println(destdirname);
        try {
            if(destdir.equals("..")) {
                System.setProperty("user.dir", cdir.getParent());
                dos.writeUTF("INFO: directory changed to " + cdir.getParent());
                System.out.println("INFO: directory changed to " + cdir.getParent());
                return;
            }
            if(!destdirname.isAbsolute()){ // && destdirname.isDirectory()
                try {
                    System.setProperty("user.dir", String.valueOf(cdir + "/" + destdirname));
                    dos.writeUTF("Directory is set to "+System.getProperty("user.dir"));
                    return;
                } catch (Exception e) {
                    System.out.println("Error: directory doesn't exists!");
                    dos.writeUTF("Error: directory doesn't exists!");
                    e.printStackTrace();
                    return;
                }
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
        try {dos = new DataOutputStream(clientInstance.getOutputStream());}
        catch(IOException io) {io.printStackTrace();}
        File cdir = new File(System.getProperty("user.dir"));
        try {
            System.out.println("Sending present working directory : " + String.valueOf(cdir));
            dos.writeUTF(String.valueOf(cdir));
            dos.flush();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    public void delete(String cmd){
        try {dos = new DataOutputStream(clientInstance.getOutputStream());}
        catch(IOException io) {io.printStackTrace();}
        if(!cmd.contains(" ") || cmd.split(" ").length < 2) {
            try {
                System.out.println("Invalid delete command recevied");
                dos.writeUTF("Error: Invalid 'delete' command, check main page for details");
            } catch (IOException io) {
                io.printStackTrace();
            }
            return;
        }
//      File to be deleted
        String file = cmd.split(" ")[1];
        File cdir=new File(System.getProperty("user.dir"));
        File deleteFile = new File(cdir, file);
        try {
            if(!deleteFile.exists() || !deleteFile.canWrite()) {
//                error if file doesn't exists or cannot be modified
                dos.writeUTF("Error: no such file or directory exist or don't have access");
                System.out.println("Error: no such file or directory exist or don't have access");
                return;
            }
            if(deleteFile.delete()){
                dos.writeUTF("INFO: " + file + " deleted successfully");
                System.out.println("INFO: " + file + " deleted successfully");
            }
            else{
                dos.writeUTF("Error: Unable to delete file "+file);
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    public void mkdir(String cmd) {
        try {dos = new DataOutputStream(clientInstance.getOutputStream());}
        catch(IOException io) {io.printStackTrace();}
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
        try {dos = new DataOutputStream(clientInstance.getOutputStream());}
        catch(IOException io) {io.printStackTrace();}
        try{
            File lsPath = null;
            File[] fileList = null;
            File cdir=new File(System.getProperty("user.dir"));
            File file=new File(cdir.getAbsolutePath());
//            System.out.println(String.valueOf(file));
//            System.out.println(cmd.split(" ").length);
            if(cmd.equals("ls") || (cmd.equals("ls") && cmd.split(" ")[cmd.split(" ").length -1].equals("."))) {
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
                } else if (!dest.isAbsolute()&& dest.canRead() && dest.isDirectory()) { // if path is non abs, it does not detects ../../filesname
                    lsPath = new File(String.valueOf(file),String.valueOf(dest));
                    System.out.println(String.valueOf(lsPath));
                    dos.writeUTF("sending list...");
                } else {
                    lsPath = new File(String.valueOf(file),String.valueOf(dest));
                    if(lsPath.listFiles() != null) {
                        dos.writeUTF("sending list...");
                    } else {
                        dos.writeUTF("Error: file or directory doesn't exists");
                        System.out.println("Error: file or directory doesn't exists");
                        return;
                    }
                }
            }
            fileList = lsPath.listFiles();
//            Create object output stream
            ObjectOutputStream outFiles = new ObjectOutputStream(clientInstance.getOutputStream());
//            Write the files to output stream
            System.out.println("INFO: sending file list..");
            outFiles.writeObject(fileList);
        } catch (IOException io) {
            io.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

}
