/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jline;

import java.io.IOException;
import junit.framework.TestCase;

/**
 *
 * @author mikio
 */
public class TerminalKeyParserTest extends TestCase {

    public TerminalKeyParserTest(String testName) {
        super(testName);
    }

    private TerminalKeyParser parserFromString(short[] input) {
        return new TerminalKeyParser(new ShortArrayInputStream(input));
    }

    public void testPlain() throws IOException {
        TerminalKeyParser tkp = parserFromString(new short[]{1, 2, 3});

        assertEquals(1, tkp.readKey());
        assertEquals(2, tkp.readKey());
        assertEquals(3, tkp.readKey());
        assertEquals(-1, tkp.readKey());

        tkp.readKey();
    }

    public void testKeyParserMatch() throws IOException {
        TerminalKeyParser tkp = parserFromString(new short[]{'a', 'b'});
        int code;

        tkp.defineKey("a", 1);

        code = tkp.readKey();
        assertEquals(1, code);

        code = tkp.readKey();
        assertEquals('b', code);

        code = tkp.readKey();
    }

    public void testKeyParserMoreKeys() throws IOException {
        TerminalKeyParser tkp = parserFromString(new short[]{'a', 'b', '[', 'b', 'b', '[', 'c'});

        tkp.defineKey("a", 1);
        tkp.defineKey("[b", 2);

        // First, check whether we recognize the ('a')
        assertEquals(1, tkp.readKey());
        // Then, check whether an unknown keycode is passed through ('b')
        assertEquals('b', tkp.readKey());
        // Now, we want to parse '[b' correctly.
        assertEquals(2, tkp.readKey());
        // See whether state was reset properly
        assertEquals('b', tkp.readKey());
        // Parsing '[c' which seems to be defined, but isn't
        assertEquals('[', tkp.readKey());
        assertEquals('c', tkp.readKey());
        // And EOF.
        assertEquals(-1, tkp.readKey());
    }

    public void testReadUTF8Key() throws IOException {
        TerminalKeyParser tkp = parserFromString(new short[] {
            '$',
            0xc3, 0xb6, // ö
            0xe2, 0x82, 0xac, // €
            0xF0,0xA4,0xAD,0xA2 // ???
        });

        assertEquals('$', tkp.readUTF8Key());
        assertEquals('ö', tkp.readUTF8Key());
        assertEquals('€', tkp.readUTF8Key());
        assertEquals(0x24b62, tkp.readUTF8Key());
        assertEquals(-1, tkp.readUTF8Key());
    }

    public void testUTF8KeySequences() throws IOException {
        TerminalKeyParser tkp = parserFromString(new short[]{'a', 0xc3, 0xb6, '<', 'a'});

        tkp.defineKey("ö<", 1);

        assertEquals('a', tkp.readKey());
        assertEquals(1, tkp.readKey());
        assertEquals('a', tkp.readKey());
        assertEquals(-1, tkp.readKey());
    }
}
