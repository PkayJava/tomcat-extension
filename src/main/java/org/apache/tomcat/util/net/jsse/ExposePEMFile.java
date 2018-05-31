package org.apache.tomcat.util.net.jsse;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ExposePEMFile extends org.apache.tomcat.util.net.jsse.PEMFile {

    public ExposePEMFile(String filename) throws IOException, GeneralSecurityException {
        super(filename);
    }

    public ExposePEMFile(String filename, String password) throws IOException, GeneralSecurityException {
        super(filename, password);
    }

}
