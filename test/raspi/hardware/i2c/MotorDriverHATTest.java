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
     * DELAY = 1000 Pausenzeit (1000 ms = 1 s) fuer einzelne Aktionen...
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
        logger.info("tearDownAfterClass()...");        
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception
    {
        logger.info("setUp()...");
        // Objekt MotorDriverHAT() instanziieren...
        this.motorDriverHAT = new MotorDriverHAT(MotorDriverHATTest.i2cBus.getDevice(ADDRESS), FREQUENCY);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception
    {
        logger.info("tearDown()..."); 
    }

    @Test
    void testMotorA_01()
    {
        logger.info("testMotorA_01()...");

        // 'Parameter'...
        final float maxSpeed = 1.0F;
        final float deltaSpeed = 0.05F;
        
        try
        {
            for(int sign: new int[]{1,-1})
            {
                float speed = 0.0F;
                
                while (speed <= maxSpeed)
                {
                    logger.info("Sollwert: " + (sign*speed) + "..." );
                    getMotorDriverHAT().setPwmMA(sign*speed);
                    speed += deltaSpeed;
                    Thread.sleep(DELAY);
                }
                speed = maxSpeed;
                while (speed >= 0.0F)
                {
                    logger.info("Sollwert: " + (sign*speed) + "..." );
                    getMotorDriverHAT().setPwmMA(sign*speed);
                    speed -= deltaSpeed;
                    Thread.sleep(DELAY);                
                }
                speed = 0.0F;
                logger.info("Sollwert: " + speed + "..." );
                getMotorDriverHAT().setPwmMA(speed);
                Thread.sleep(DELAY);
            }
            logger.info("testMotorA_01() erfolgreich.");            
        } 
        catch (IOException | InterruptedException exception)
        {
            fail("Exception in testMotorA_01()", exception);
        }
    }
    
    @Test
    void testMotorB_01()
    {
        logger.info("testMotorB_01()...");

        // 'Parameter'...
        final float maxSpeed = 1.0F;
        final float deltaSpeed = 0.05F;
        
        try
        {
            for(int sign: new int[]{1,-1})
            {
                float speed = 0.0F;
                
                while (speed <= maxSpeed)
                {
                    logger.info("Sollwert: " + (sign*speed) + "..." );
                    getMotorDriverHAT().setPwmMB(sign*speed);
                    speed += deltaSpeed;
                    Thread.sleep(DELAY);
                }
                speed = maxSpeed;
                while (speed >= 0.0F)
                {
                    logger.info("Sollwert: " + (sign*speed) + "..." );
                    getMotorDriverHAT().setPwmMB(sign*speed);
                    speed -= deltaSpeed;
                    Thread.sleep(DELAY);                
                }
                speed = 0.0F;
                logger.info("Sollwert: " + speed + "..." );
                getMotorDriverHAT().setPwmMB(speed);
                Thread.sleep(DELAY);
            }
            logger.info("testMotorB_01() erfolgreich.");            
        } 
        catch (IOException | InterruptedException exception)
        {
            fail("Exception in testMotorB_01()", exception);
        }
    }

    /**
     * getMotorDriverHAT()
     * @return this.MotorDriverHAT
     */
    private final MotorDriverHAT getMotorDriverHAT()
    {
        return this.motorDriverHAT;
    }
    
}
