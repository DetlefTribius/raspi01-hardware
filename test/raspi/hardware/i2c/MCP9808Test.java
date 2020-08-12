/**
 * 
 */
package raspi.hardware.i2c;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;



/**
 * @author Detlef Tribius
 *
 */
class MCP9808Test
{

    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(MCP9808Test.class);
    
    /**
     * i2cBus - Referenz auf den IC2Bus...
     */
    private static I2CBus i2cBus = null;
    
    /**
     * ADDRESS - Bus-Adresse des Bausteins, festgelegt durch
     * Verdrahtung auf dem Baustein... 
     */
    public final static int ADDRESS = 0x18; 
    
    /**
     * mcp9808 - Referenz auf den MCP9808-Baustein unter der Adresse ADDRESS  
     */
    private MCP9808 mcp9808 = null;
    
    /**
     * DELAY = 300 Pausenzeit (30 ms) fuer einzelne Aktionen...
     */
    public final static int DELAY = 300;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeAll
    static void setUpBeforeClass() throws Exception
    {
        logger.info("setUpBeforeClass()...");
        try
        {
        	// Besorgen der Referenz auf den I2CBus...
            MCP9808Test.i2cBus = I2CFactory.getInstance(I2CBus.BUS_1);
        }
        catch (UnsupportedBusNumberException exception)
        {
            fail("UnsupportedBusNumberException bei I2CFactory.getInstance(ADDRESS)!");
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterAll
    static void tearDownAfterClass() throws Exception
    {
        logger.info("tearDownAfterClass()...");        
    }

    /**
     * setUp() - Erzeugen der Referenz auf den MCP9808-Baustein
     * 
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception
    {
        logger.info("setUp()...");
        // Referenz anlegen
        this.mcp9808 = new MCP9808(MCP9808Test.i2cBus.getDevice(ADDRESS));
        try
        {
        	// Reset des Bausteins...
            this.mcp9808.reset();
        }
        catch (IOException exception)
        {
            fail("IOException in setUp()", exception);
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception
    {
        logger.info("tearDown()..."); 
    }

    /**
     * testGetTemp()
     */
    @Test
    void testGetTemp()
    {
        try
        {
            final double ambientTemp = this.mcp9808.getAmbientTemp();
            logger.info("mcp9808.getAmbientTemp(): " + ambientTemp);
        }
        catch (IOException exception)
        {
            fail("IOException in testGetTemp()", exception);
        }
    }
    
    @Test
    void testResolution()
    {
        // Array mit den Aufloesungen des Bausteins...
    	final int resolutions[] = new int[]
    	{
    	    MCP9808.RES05,
    	    MCP9808.RES025,
    	    MCP9808.RES0125,
    	    MCP9808.RES00625
    	};
    	try
    	{
    	    for(int resolution: resolutions)
    	    {
    	        this.mcp9808.setResultion(resolution); 
    	        Thread.sleep(DELAY);
    	        final double ambientTemp = this.mcp9808.getAmbientTemp();
    	        logger.info("Resolution = " + resolution + " mcp9808.getAmbientTemp(): " + ambientTemp);
    	    }
    	}
    	catch(IOException | InterruptedException exception)
    	{
    	    fail("IOException in testResolution()", exception);  
    	}
    }
    
    
    /**
     * Setzen eines unteren Temperaturgrenzwertes testen...
     * 
     * 
     */
    @Test
    void testLowerTemp()
    {
        final double diffTemp = 4.0d;
        try
        {
            final double ambientTemp = this.mcp9808.getAmbientTemp();
            logger.info("mcp9808.getAmbientTemp(): " + ambientTemp);
            Thread.sleep(DELAY);
            // Setzen Untere Temp.grenze auf ambientTemp+diff... 
            this.mcp9808.setLowerTemp(ambientTemp + diffTemp);
            Thread.sleep(DELAY);
            assertTrue(this.mcp9808.isAmbientLessLowerBoundary(), 
                       "isAmbientLessLowerBoundary() true erwartet");
            Thread.sleep(DELAY);
            this.mcp9808.setLowerTemp(ambientTemp - diffTemp);
            Thread.sleep(DELAY);
            assertFalse(this.mcp9808.isAmbientLessLowerBoundary(), 
                       "isAmbientLessLowerBoundary() false erwartet");
            // Ende testLowerTemp()...
            logger.info("testLowerTemp() erfolgreich.");
        } 
        catch (IOException | InterruptedException exception)
        {
            fail("IOException in testLowerTemp()", exception);  
        }
    }

    /**
     * Setzen eines unteren Temperaturgrenzwertes testen...
     * 
     * 
     */
    @Test
    void testUpperTemp()
    {
        final double diffTemp = 4.0d;
        try
        {
            final double ambientTemp = this.mcp9808.getAmbientTemp();
            logger.info("mcp9808.getAmbientTemp(): " + ambientTemp);
            Thread.sleep(DELAY);
            // Setzen Obere Temp.grenze auf ambientTemp-diff... 
            this.mcp9808.setUpperTemp(ambientTemp - diffTemp);
            Thread.sleep(DELAY);
            // ...die ambientTemp sollte jetzt immer groesser als UpperTemp sein!
            assertTrue(this.mcp9808.isAmbientGreaterUpperBoundary(), 
                       "isAmbientGreaterUpperBoundary() true erwartet");
            Thread.sleep(DELAY);
            
            // Setzen Obere Temp.grenze auf ambientTemp+diff... 
            this.mcp9808.setUpperTemp(ambientTemp + diffTemp);
            Thread.sleep(DELAY);
            // ...die ambientTemp sollte jetzt nie groesser als UpperTemp sein!
            assertFalse(this.mcp9808.isAmbientGreaterUpperBoundary(), 
                       "isAmbientGreaterUpperBoundary() false erwartet");
            
            // Ende testUpperTemp()...
            logger.info("testUpperTemp() erfolgreich.");
        } 
        catch (IOException | InterruptedException exception)
        {
            fail("IOException in testUpperTemp()", exception);  
        }
    }

    /**
     * testCritTemp()
     * 
     */
    @Test
    void testCritTemp()
    {
        final double diffTemp = 4.0d;
        try
        {
            final double ambientTemp = this.mcp9808.getAmbientTemp();
            logger.info("mcp9808.getAmbientTemp(): " + ambientTemp);
            Thread.sleep(DELAY);
            // Setze Kritische Temperatur auf ambientTemp - diffTemp...
            this.mcp9808.setCritTemp(ambientTemp - diffTemp);
            Thread.sleep(DELAY);
            // ...die ambientTemp sollte jetzt immer groesser als UpperTemp sein!
            assertTrue(this.mcp9808.isAmbientGreaterEqualCrit(), 
                       "isAmbientGreaterEqualCrit() true erwartet");
            Thread.sleep(DELAY);
            
            // Setze Kritische Temperatur auf ambientTemp + diffTemp...
            this.mcp9808.setCritTemp(ambientTemp + diffTemp);
            Thread.sleep(DELAY);
            // ...die ambientTemp sollte jetzt immer groesser als UpperTemp sein!
            assertFalse(this.mcp9808.isAmbientGreaterEqualCrit(), 
                       "isAmbientGreaterEqualCrit() false erwartet");

            // Ende testCritTemp()...
            logger.info("testCritTemp() erfolgreich.");
        }
        catch (IOException | InterruptedException exception)
        {
            fail("IOException in testCritTemp()", exception);  
        }
    }
    
    /**
     * testSetterGetterTemp()
     */
    @Test
    void testSetterGetterTemp()
    {
        // Vorgaben fuer die einzelnen Temperaturen...
        final double critTemp = 30.0d;
        
        final double upperTemp = 25.0d;
        
        final double lowerTemp = 20.0d;
        // Annahme einer zulaessige Abweichung...
        final double delta = 0.001;
        
        try
        {
            this.mcp9808.setCritTemp(critTemp); 
            this.mcp9808.setUpperTemp(upperTemp);
            this.mcp9808.setLowerTemp(lowerTemp);
            Thread.sleep(DELAY);
            // API: assertEquals(double expected, double actual, double delta, java.lang.String message)
            assertEquals(critTemp, this.mcp9808.getCritTemp(), delta, 
                         "Abweichung bei getCritTemp()");
            assertEquals(upperTemp, this.mcp9808.getUpperTemp(), delta, 
                         "Abweichung bei getUpperTemp()");
            assertEquals(lowerTemp, this.mcp9808.getLowerTemp(), delta, 
                         "Abweichung bei getLowerTemp()");
            
            // Ende testSetterGetterTemp()...
            logger.info("testSetterGetterTemp() erfolgreich.");
        }
        catch (IOException | InterruptedException exception)
        {
            fail("IOException in testSetterGetterTemp()", exception);  
        }
    }
}
