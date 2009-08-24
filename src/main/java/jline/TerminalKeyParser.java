/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jline;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class parses terminal key sequences like you get them on UNIX
 * terminals. For example, cursor left is "^[[A", and so on... .
 */
public class TerminalKeyParser
        implements ConsoleOperations {

    private InputStream in;
    private final Map keyDefinitions;
    private CharBuffer buffer;
    private boolean replaying;
    private int replayIndex;

    public TerminalKeyParser(InputStream newInputStream) {
        in = newInputStream;
        keyDefinitions = new TreeMap();
        buffer = new CharBuffer();
        replaying = false;
        replayIndex = 0;
    }

    public void defineKey(String sequence, int code) {
        keyDefinitions.put(sequence, new Integer(code));
    }

    /**
     * Parses a key (sequences). Basically, you either get the (virtual) key code as defined
     * in ConsoleOperations, or the constant TerminalKeyParser.MORE_KEYS_AVAILABLE which
     * indicates that the sequence read so far is not yet complete.
     *
     * A special case is the escape key, which can either stand by itself, or
     * be the beginning of a more elaborate escape sequence. The question is
     * whether it is followed by an '[' or not.
     *
     * @param key
     * @return Either the (virtual keycode), or -1 if more keys are expected.
     */
    public int readKey() throws IOException {
        if (replaying) {
            int key = buffer.charAt(replayIndex++);
            if (replayIndex >= buffer.length()) {
                resetParser();
            }
            return key;
        } else {
            int key = readUTF8Key();
            if (key == -1) {
                return -1;
            }

            boolean foundPrefix;

            /*
             * Iterate until no prefix has been found.
             * While you still have a prefix, read more characters,
             */
            do {
                buffer.append((char) key);

                Iterator i = keyDefinitions.keySet().iterator();
                foundPrefix = false;
                
                while (i.hasNext()) {
                    String seq = (String) i.next();
                    int cmp = buffer.compare(seq);
                    switch (cmp) {
                        case CharBuffer.EQUAL:
                            int code = ((Integer) keyDefinitions.get(buffer.toString())).intValue();
                            resetParser();
                            return code;
                        case CharBuffer.PREFIX:
                            key = readUTF8Key();
                            if (key == -1)
                                break;
                            else {
                                foundPrefix = true;
                                break;
                            }
                    }
                }
            } while (foundPrefix);

            if (buffer.length() == 1) {
                resetParser();
                return key;
            }

            // Apparently, we found no match, so we have to start replaying... .
            replayIndex = 0;
            replaying = true;
            return readKey();
        }
    }

    /**
     * Let's the parser forget whatever it was about to parse.
     */
    public void resetParser() {
        replaying = false;
        buffer.clear();
    }

    public int readUTF8Key() throws IOException {
        int key = in.read();
        if (key == -1) {
            return key;
        } else if (key < 128) {
            return key;
        } else if ((key & 0xe0) == 0xc0) {
            int key2 = in.read();
            return ((key & 0x1f) << 6) | (key2 & 0x3f);
        } else if ((key & 0xf0) == 0xe0) {
            int key2 = in.read();
            int key3 = in.read();
            return ((key & 0x0f) << 12) | ((key2 & 0x3f) << 6) | (key3 & 0x3f);
        } else if ((key & 0xf8) == 0xf0) {
            int key2 = in.read();
            int key3 = in.read();
            int key4 = in.read();
            return ((key & 0x07) << 18) | ((key2 & 0x3f) << 12) | ((key3 & 0x3f) << 6) | (key4 & 0x3f);
        } else {
            return key;
        }
    }
}
