package com.articulate.sigma.nlg;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author terry
 */
public class KryoTest extends Assert {

    private static final String SIGMA_SRC = System.getenv("SIGMA_SRC");
    private static final String HELLO_KRYO = "Hello Kryo!";

    Kryo kryo;
    Path path;
    Object ob;

    public KryoTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {

        ob = null;
        kryo = new Kryo();

        // Big help with this setting from:
        // https://stackoverflow.com/questions/69893294/why-kryo-gives-me-that-error-when-serializing-object
        kryo.setReferences(true);
        path = Paths.get(SIGMA_SRC, "file.bin");
    }

    @After
    public void tearDown() {

        ob = null;
        kryo = null;
        path.toFile().delete();
        path = null;
    }

    @Test
    public void hello() throws IOException {

        kryo.register(SomeClass.class);

        SomeClass object = new SomeClass();
        object.value = HELLO_KRYO;

        // Serialize
        try (Output output = new Output(Files.newOutputStream(path))) {
            kryo.writeObject(output, object);
        }

        // Deserialize
        try (InputStream is = Files.newInputStream(path); Input input = new Input(is)) {
            ob = kryo.readObject(input, SomeClass.class);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        assertNotNull(ob);
        assertNotNull(((SomeClass) ob).value);
        assertEquals(HELLO_KRYO, ((SomeClass) ob).value);

        System.out.println(((SomeClass) ob).value);
    }

    public static class SomeClass {

      String value;
   }
}
