package matlabcontrol;

/*
 * Copyright (c) 2011, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;

/**
 * Contains important configuration information regarding the setup of MATLAB and matlabcontrol.
 * 
 * @since 3.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class Configuration
{
    public static void main(String[] args) throws Throwable
    {
        System.out.println(System.getProperty("java.class.path"));
        System.out.println(getClassPathAsRMICodebase());
    }
    
    private Configuration() { }
    
    /**
     * If running on OS X.
     * 
     * @return 
     */
    static boolean isOSX()
    {
        return getOperatingSystem().startsWith("Mac OS X");
    }
    
    /**
     * If running on Windows.
     * 
     * @return 
     */
    static boolean isWindows()
    {
        return getOperatingSystem().startsWith("Windows");
    }
    
    /**
     * If running on Linux.
     * 
     * @return 
     * @throws
     */
    static boolean isLinux()
    {
        return getOperatingSystem().startsWith("Linux");
    }
    
    /**
     * Gets a string naming the operating system.
     * 
     * @return 
     */
    private static String getOperatingSystem()
    {
        return System.getProperty("os.name");
    }

    /**
     * Returns the location or alias of MATLAB on an operating system specific basis.
     * <br><br>
     * For OS X this will be the location, for Windows or Linux this will be an alias. For any other operating system an 
     * exception will be thrown.
     * 
     * @return MATLAB's location or alias
     * 
     * @throws MatlabConnectionException thrown if the location of MATLAB cannot be determined on OS X, or the alias
     *                                       cannot be determined because the operating system is not Windows or Linux
     */
    static String getMatlabLocation() throws MatlabConnectionException
    {
        //Determine the location of MATLAB
        String matlabLoc;

        //OS X
        if(isOSX())
        {
            matlabLoc = getOSXMatlabLocation();
        }
        //Windows or Linux
        else if(isWindows() || isLinux())
        {
            matlabLoc = "matlab";
        }
        //Other unsupported operating system
        else
        {
            throw new MatlabConnectionException("MATLAB's location or alias can only be determined for OS X, " +
                    "Windows, & Linux. For this operating system the location or alias must be specified " +
                    "explicitly.");
        }
        
        return matlabLoc;
    }
    
    /**
     * Determines the location of the MATLAB executable on OS X. If multiple versions are found, the last one
     * encountered will be used.
     * 
     * @return MATLAB's location on OS X
     * 
     * @throws MatlabConnectionException if the location cannot be determined
     */
    private static String getOSXMatlabLocation() throws MatlabConnectionException
    {
        //Search for MATLAB in the Applications directory
        String matlabName = null;
        for(String fileName : new File("/Applications/").list())
        {
            if(fileName.startsWith("MATLAB"))
            {
                matlabName = fileName;
            }
        }
        
        //If no installation is found
        if(matlabName == null)
        {
            throw new MatlabConnectionException("No installation of MATLAB on OS X can be found");
        }
        
        //Build path to the executable location
        String matlabLocation = "/Applications/" + matlabName + "/bin/matlab";
        
        //Check the path actually exists
        if(!new File(matlabLocation).exists())
        {
            throw new MatlabConnectionException("An installation of MATLAB on OS X was found but the main " +
                    "executable file was not found in the anticipated location: " + matlabLocation);
        }
        
        return matlabLocation;
    }
    
    /**
     * The codebase format is a list of URL formatted strings separated by spaces. What would normally be a space is
     * instead represented as {@code %20}. Directories must be terminated with a {@code /} or they are treated as if
     * they were a JAR. Paths must be absolute. The normal classpath format does not have these requirement. This method
     * converts the classpath to RMI's codebase format.
     * 
     * @return codebase
     */
    static String getClassPathAsRMICodebase() throws MatlabConnectionException
    {
        try
        {
            StringBuilder codebaseBuilder = new StringBuilder();
            String[] paths = System.getProperty("java.class.path", "").split(File.pathSeparator);
            
            for(String path : paths)
            {
                codebaseBuilder.append("file://");

                File file = new File(path);
                String transformedPath = file.getCanonicalPath();
                transformedPath = transformedPath.replace(" ", "%20");

                //Windows URIs need to be of the form file:///C:/Documents%20and%20Settings/davris/FileSchemeURIs.doc
                if(isWindows())
                {
                    transformedPath = transformedPath.replace("\\", "/");
                    if(!transformedPath.startsWith("/"))
                    {
                        codebaseBuilder.append("/");
                    }
                }

                codebaseBuilder.append(transformedPath);

                if(!transformedPath.endsWith("/") && file.isDirectory())
                {
                    codebaseBuilder.append("/");
                }

                codebaseBuilder.append(" ");
            }

            String codebase = codebaseBuilder.toString();
        
            return codebase;
        }
        catch(IOException e)
        {
            throw new MatlabConnectionException("Unable to resolve canonical path of classpath entry", e);
        }
    }
    
    /**
     * Splits the classpath into individual entries and converts each entry into its canonical path.
     * 
     * @return
     * @throws MatlabConnectionException 
     */
    static String[] getClassPathAsCanonicalPaths() throws MatlabConnectionException
    {
        try
        {
            String classpath = System.getProperty("java.class.path", "");
            String[] paths = classpath.split(File.pathSeparator);
            
            for(int i = 0; i < paths.length; i++)
            {
                paths[i] = new File(paths[i]).getCanonicalPath();
            }
        
            return paths;
        }
        catch(IOException e)
        {
            throw new MatlabConnectionException("Unable to resolve canonical path of classpath entry", e);
        }
    }
    
    /**
     * Determines the location of this source code. Either it will be the directory or jar this .class file is in. (That
     * is, the .class file built from compiling this .java file.) The location format is then adjusted to be compatible
     * with MATLAB.
     * 
     * @return directory or jar file this class is in, for MATLAB
     * 
     * @throws MatlabConnectionException
     */
    static String getSupportCodeLocation() throws MatlabConnectionException
    {
        URL location = Configuration.class.getProtectionDomain().getCodeSource().getLocation();
        String protocol = location.getProtocol();
        String path;

        if(protocol.equals("jar"))
        {
            try
            {
                JarURLConnection connection = (JarURLConnection) location.openConnection();
                path = connection.getJarFileURL().getPath();
            }
            catch(ClassCastException e)
            {
                throw new MatlabConnectionException("Support code location is specified by the jar protocol; " + 
                        "however, the connection returned was not for a jar", e);
            }
            catch(IOException e)
            {
                throw new MatlabConnectionException("Support code location is specified by the jar protocol; " + 
                        "however, a connection to the jar cannot be established", e);
            }
        }
        else if(protocol.equals("file"))
        {
            path = location.getPath();
        }
        else
        {
            throw new MatlabConnectionException("Support code location is specified by an unknown protocol: " +
                    protocol);
        }
        
        //Convert URL encoded space to an actual space
        path = path.replace("%20", " ");

        //If under Windows, convert to a Windows path
        if(isWindows())
        {
            path = path.replaceFirst("/", "");
            path = path.replace("/", "\\");
        }
        
        //Confirm the path actually exists
        if(!new File(path).exists())
        {
            throw new MatlabConnectionException("Support code location was determined improperly; location does " +
                    "not actually exist. Location determined as: " + path);
        }

        return path;
    }
    
    /**
     * Whether this code is running inside of MATLAB.
     * 
     * @return 
     */
    static boolean isRunningInsideMatlab()
    {
        boolean available;
        try
        {
            //Load the class com.mathworks.jmi.Matlab and then calls its static method isMatlabAvailable()
            //All of this is done with reflection so that this class does not cause the class loader to attempt
            //to load JMI classes (and if not running inside of MATLAB - fail)
            Class<?> matlabClass = Class.forName("com.mathworks.jmi.Matlab");
            Method isAvailableMethod = matlabClass.getMethod("isMatlabAvailable");
            available = (Boolean) isAvailableMethod.invoke(null);
        }
        catch(Throwable t)
        {
            available = false;
        }
        
        return available;
    }
}