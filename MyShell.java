/*
 * @file    MyShell.java
 * @brief   This class provides a simple command interpreter for the ThreadOS
 *           environment. It provides a prompt that indicates the different
 *           interpreter and shows the number of the line of command to be
 *           read. Functionally, it removes the requirement to use "l" to load
 *           each command and it allows multiple commands to be executed from a
 *           single line in sequence or concurrently, depending on whether they
 *           are separated by ";" or by "&". If neither symbol follows the last
 *           command, sequential execution is assumed.
 * @author  Brendan Sweeney, SID 1161836
 * @date    October 12, 2012
 *


/**
 *
 */
public class MyShell extends Thread
{
    private int           line;         // current execution line number
    private boolean       done;         // execution completion flag
    private String[]      commands;     // list of arguments from user
    
    
    /**
     * Default constructor.
     * @pre    The ThreadOS environment has been initialized.
     * @post   A MyShell object is ready to run and is in an unfinished state.
     */
    public MyShell()
    {
        line     = 1;
        done     = false;
        commands = null;
    } // end default constructor
    
    
    /**
     * Allows this class to be run in its own thread.
     * @pre    Same as prompt(), parse(), and execute().
     * @post   The thread for this class is cleanly terminated.
     */
    public void run()
    {
        // go until we are finished
        while(!done)
        {
            prompt();   // get a line of input
            parse();    // parse commands and arguments from input
            execute();  // execute all commands
        } // end while(!done)
        
        // terminate cleanly
        SysLib.exit();
    } // end run()
    
    
    /**
     * Displays a shell prompt to the user, including the command line number.
     * @pre    None.
     * @post   A prompt is displayed on standard output.
     */
    public void prompt()
    {
        SysLib.cout("b-shell[" + line + "]% ");
    } // end prompt()
    
    
    /**
     * Gets a line of command(s) from the user and parses it into sets of
     *  commands and arguments, and delimeters.
     * @pre    The user has provided valid input at the shell prompt.
     * @post   The user input is stored in the array, commands, with each
     *          white-space separated value at a unique index.
     */
    public void parse()
    {
        StringBuffer input = new StringBuffer();    // for reading in a line
        
        try
        {
            // get the input, then break it into an array of tokens
            SysLib.cin(input);
            commands = SysLib.stringToArgs(input.toString());
        }
        catch(NullPointerException e)
        {
            SysLib.cerr("ERROR: read input: " + e.getMessage());
        } // end try SysLib.cin(input)
    } // end parse()
    
    
    /**
     * Execute the array of commands, possibly with arguments. ";" and "&" are
     *  expected to be placed between all commands, so the elements between
     *  those characters are taken as a command and its arguments. If an
     *  argument list is followed by "&", then the command is executed
     *  concurrently with the next command, or the user is given access to the
     *  shell while the command executes, if no command follow "&". If an
     *  argument list is followed by ";", then the command is allowed to finish
     *  execution before the next command is processed or control of the shell
     *  is returned to the user. If neither ";" nor "&" follow the last
     *  command, then it is executed as though it were followed by ";". If the
     *  only command is "exit", then the shell is set to a finished state and
     *  this method returns. The line count is increased only if there was a
     *  command other than "exit" to process.
     * @pre    commands contains a list of commands that ThreadOS can run, each
     *          separated by either ";" or "&".
     * @post   All commands have been executed and control has returned to the
     *          shell thread; line is incremented; upon exit, done is set to
     *          true.
     */
    public void execute()
    {
        int cmdIndex = 0;   // start of an argument list
        
        // check for exit status
        if (commands.length == 1)
        {
            // if the only command it "exit"...
            if (commands[0].contentEquals("exit"))
            {
                // acknowledge and let caller know we're done
                SysLib.cout("Exit called...terminating\n");
                done = true;
                return;
            } // end if (commands[0].contentEquals("exit"))
        } // end if (commands.length == 1)

        // search through commands for argument lists
        for (int i = 0; i < commands.length; ++i)
        {
            // concurrent execution; no special action
            if (commands[i].contentEquals("&"))
            {
                SysLib.exec(getArgList(cmdIndex, i));
                cmdIndex = i + 1;       // start of next command
            } // end if (commands[i].contentEquals("&"))
            else if (commands[i].contentEquals(";"))
            {
                // sequential execution; special call made
                sequential(cmdIndex, i);
                cmdIndex = i + 1;       // start of next command
            } // end else if (commands[i].contentEquals(";"))
        } // end for (; i < commands.length;)
        
        // no delimeter at end of command list; one command left
        if (cmdIndex < commands.length)
        {
            sequential(cmdIndex, commands.length);
        } // end if (cmdIndex < commands.length)
        
        // increment line number of something was executed
        if (commands.length > 0)
        {
            ++line;
        } // end if (commands.length > 0)
    } // end execute()
    
    
    /**
     * Pulls a single argument list from commands. Output is meant to be parsed
     *  by SysLib.exec().
     * @param  begin  The index of the first element to copy from the list.
     * @param  end  The index one past the last element to copy from the list.
     * @pre    begin and end are within the bounds of commands; begin <= end.
     * @post   None.
     * @return A String[] that is a sequential subset of commands.
     */
    private String[] getArgList(int begin, int end)
    {
        String[] argList = new String[end - begin];     // command and args
        
        // copy a section of commands, from begin to end - 1.
        for (int i = begin; i < end; ++i)
        {
            argList[i - begin] = commands[i];
        } // end for (; i < end;)
        
        return argList;
    } // end getArg(String[], int, int)
    
    
    /**
     * Execute a command and wait for it to finish. The command should be
     *  represented by a list of arguments in commands.
     * @param  first  Index of the first argument in commands.
     * @param  last  Index one past the last argument in commands.
     * @pre    first and last are within the bounds of commands; first <= last.
     * @post   The specified command has completed execution.
     */
    private void sequential(int first, int last)
    {
        // grab ID of thread for command in specified index range of commands
        int tid = SysLib.exec(getArgList(first, last));

        // wait for execution of command to complete
        while(SysLib.join() != tid)
        {
            SysLib.sleep(10);
        } // end while(SysLib.join() != tid)
    } // end concur(int, int)
} // end class MyShell
