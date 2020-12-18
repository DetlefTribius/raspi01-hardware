/**
 * 
 */
package raspi.hardware.i2c;

import static org.junit.jupiter.api.Assertions.*;

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
class MotorDriverHATTest
{

    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(MotorDriverHATTest.class);    
    
    /**
     * i2cBus - Referenz auf den IC2Bus...
     */
    private static I2CBus i2cBus = null;
    
    /**
     * ADDRESS - Bus-Adresse des MotorDriverHAT, festgelegt durch
     * Verdrahtung auf dem Baustein, Standard-Vorbelegung lautet: 0x40.
     * <p>
     * Vergleiche "MotorDriver HAT User Manual": ...The address range from 0x40 to 0x5F. 
     *  </p>
     */
    public final static int ADDRESS = 0x40; 
    
    /**
     * motorDriverHAT - Rferenz auf den Baustein...
     */
    private MotorDriverHAT motorDriverHAT = null;
    
    /**
     * FREQUENCY = 100
     */
    public final static int FREQUENCY = 100;
    
    /**
     * DELAY = 1000 Pausenzeit (100 ms) fuer einzelne Aktionen...
     */
    public final static int DELAY = 1000;
    
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
            MotorDriverHATTest.i2cBus = I2CFactory.getInstance(I2CBus.BUS_1);
        }
        catch(UnsupportedBusNumberException exception)
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
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception
    {
        logger.info("setUp()...");
        // Objekt MotorDriverHAT() instanziieren...
        this.motorDriverHAT = new MotorDriverHAT(MotorDriverHATTest.i2cBus.getDevice(ADDRESS));
        this.motorDriverHAT.initialize(FREQUENCY);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception
    {
    }

    @Test
    void test01()
    {
        fail("Not yet implemented");
    }

}
