package com.softhaxi.marves.core.util;

public class JSONUtil {
    public static String escape(String input) {
      StringBuilder output = new StringBuilder();
  
      for(int i=0; i<input.length(); i++) {
        char ch = input.charAt(i);
        int chx = (int) ch;
  
        // let's not put any nulls in our strings
        assert(chx != 0);
  
        if(ch == '\n') {
          output.append("\\n");
        } else if(ch == '\t') {
          output.append("\\t");
        } else if(ch == '\r') {
          output.append("\\r");
        } else if(ch == '\\') {
          output.append("\\\\");
        } else if(ch == '"') {
          output.append("\\\"");
        } else if(ch == '\b') {
          output.append("\\b");
        } else if(ch == '\f') {
          output.append("\\f");
        } else if(chx >= 0x10000) {
          assert false : "Java stores as u16, so it should never give us a character that's bigger than 2 bytes. It literally can't.";
        } else if(chx > 127) {
          output.append(String.format("\\u%04x", chx));
        } else {
          output.append(ch);
        }
      }
      String strOutput = output.toString();
      output = null;
      return strOutput;
    }
}  