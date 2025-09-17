package com.bentzn.util.linestruct;

/**
 * Definition of a Java class to be generated.
 * 
 * @author bentzn/Grok/Claude
 */
public class JavaClassDef {
    private String filename;
    private String code;

    public JavaClassDef(String filename, String code) {
        this.filename = filename;
        this.code = code;
    }



    public String getFilename() {
        return filename;
    }



    public void setFilename(String filename) {
        this.filename = filename;
    }



    public String getCode() {
        return code;
    }



    public void setCode(String code) {
        this.code = code;
    }
}