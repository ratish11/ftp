myftp - A Simple File Transfer Protocol Client

This is a basic client implementation for the File Transfer Protocol (FTP). It allows you to connect to an FTP server and perform various file management operations like:

    Uploading files to the server
    Downloading files from the server
    Listing directory contents
    Changing directories
    Deleting files
    Terminating background file transfers

Requirements

    Java Runtime Environment (JRE)

How to Use

    Compile the code:
    Bash

    javac myftp.java

    Use code with caution.

Run the program:
Bash

java myftp <hostname> <port_number> <termination_port_number>

Use code with caution.

        Replace <hostname> with the hostname or IP address of the FTP server.
        Replace <port_number> with the port number on which the FTP server is listening (default: 21).
        Replace <termination_port_number> with the port number used for terminating background transfers (any unused port number).

Supported Commands

The following commands are available in the interactive shell:

    get <filename>& - Initiates a background download of the specified file from the server.
    get <filename> - Downloads the specified file from the server in the foreground.
    put <filename>& - Initiates a background upload of the specified file to the server.
    put <filename> - Uploads the specified file to the server in the foreground.
    cd <directory>& - Changes the working directory on the server (background).
    cd <directory> - Changes the working directory on the server (foreground).
    pwd& - Lists the current working directory on the server (background).
    pwd - Lists the current working directory on the server (foreground).
    delete <filename>& - Deletes the specified file from the server (background).
    delete <filename> - Deletes the specified file from the server (foreground).
    mkdir <directory>& - Creates a new directory on the server (background).
    mkdir <directory> - Creates a new directory on the server (foreground).
    ls& - Lists the contents of the current directory on the server (background).
    ls - Lists the contents of the current directory on the server (foreground).
    terminate - Terminates all background file transfers.
    quit - Closes the connection to the server and exits the program.

Background Transfers

Commands ending with "&" initiate background file transfers. This allows you to continue using the shell while the transfer is in progress. You can monitor the progress or terminate the transfer by using the terminate command.

Notes

    File transfer termination might not be immediate and may take some time to complete depending on the file size and network conditions.
    This is a basic implementation and may not support all advanced FTP features.

For further information, please refer to the source code.
