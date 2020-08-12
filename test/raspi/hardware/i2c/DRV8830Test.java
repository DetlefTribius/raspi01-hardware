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

class DRV8830Test
{

    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(DRV8830Test.class);
    
    /**
     * i2cBus - Referenz auf den IC2Bus...
     */
    private static I2CBus i2cBus = null;
    
    /**
     * ADDRESS - Bus-Adresse des Bausteins, festgelegt durch
     * Verdrahtung auf dem Baustein... 
     */
    public final static int ADDRESS = 0x60; 

    /**
     * drv8830 - Referenz auf den DRV8830-Baustein unter der Adresse ADDRESS
     */
    private DRV8830 drv8830 = null;
    
    /**
     * DELAY = 1000 Pausenzeit (100 ms) fuer einzelne Aktionen...
     */
    public final static int DELAY = 1000;
    
    @BeforeAll
    static void setUpBeforeClass() throws Exception
    {
        logger.info("setUpBeforeClass()...");
        try
        {
            // Besorgen der Referenz auf den I2CBus...
            DRV8830Test.i2cBus = I2CFactory.getInstance(I2CBus.BUS_1); 
        }
        catch (UnsupportedBusNumberException exception)
        {
            fail("UnsupportedBusNumberException bei I2CFactory.getInstance(ADDRESS)!");
        }
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception
    {
        logger.info("tearDownAfterClass()...");    
    }

    @BeforeEach
    void setUp() throws Exception
    {
        logger.info("setUp()...");
        // Referenz anlegen
        this.drv8830 = new DRV8830(DRV8830Test.i2cBus.getDevice(ADDRESS));
        try
        {
            int fault = this.drv8830.getFault(); 
            logger.info("setUp() liefert mit getFault() die Kennung: " + fault);             
        }
        catch(IOException exception)
        {
            fail("IOException in setUp()", exception);  
        }
    }

    @AfterEach
    void tearDown() throws Exception
    {
        logger.info("tearDown()...");
        try
        {
            int fault = this.drv8830.getFault(); 
            logger.info("tearDown() liefert mit getFault() die Kennung: " + fault);
            this.drv8830.standBy();
            logger.info("standBy() erfolgreich.");
        }
        catch(IOException exception)
        {
            fail("IOException in setUp()", exception);  
        }

    }

    @Test
    void test01()
    {
        logger.info("test01()...");
        final int limit = 64;
        try
        {
            for (int speed = 0; speed < limit; speed++)
            {
                logger.info("drive()...: " + speed );
                
                int fault = this.drv8830.getFault(); 
                // Bei fault == 0 => Fehlerfrei, sonst Fehler!
                if (fault != 0)
                {
                    DRV8830.Fault error = DRV8830.Fault.getFault(fault);
                    logger.error("test01(): " + error.getReason());
                }
                
                this.drv8830.drive(speed);
                Thread.sleep(DELAY);                
            }
            for (int speed = limit-1; speed > -1; speed--)
            {
                logger.info("drive()...: " + speed );
                
                int fault = this.drv8830.getFault();
                // Bei fault == 0 => Fehlerfrei, sonst Fehler!
                if (fault != 0)
                {
                    DRV8830.Fault error = DRV8830.Fault.getFault(fault);
                    logger.error("test01(): " + error.getReason());
                }
                
                this.drv8830.drive(speed);
                Thread.sleep(DELAY);                
            }
        }
        catch(IOException | InterruptedException exception)
        {
            fail("IOException in drive()", exception);   
        }
    }

    @Test
    void test02()
    {
        logger.info("test02()...");
        final int limit = 64;
        final int delta = 8;
        try
        {
            for (int speed = 0; speed < limit;)
            {
                logger.info("drive()...: " + speed );
                
                int fault = this.drv8830.getFault(); 
                // Bei fault == 0 => Fehlerfrei, sonst Fehler!
                if (fault != 0)
                {
                    DRV8830.Fault error = DRV8830.Fault.getFault(fault);
                    logger.error("test02(): " + error.getReason());
                }
                
                this.drv8830.drive(speed);
                // "schneller" Anstieg...
                speed += delta;
                Thread.sleep(DELAY);                
            }
            {
                logger.info("brake()..." );
                
                // Abbremsen...
                this.drv8830.brake();
                
                int fault = this.drv8830.getFault(); 
                // Bei fault == 0 => Fehlerfrei, sonst Fehler!
                if (fault != 0)
                {
                    DRV8830.Fault error = DRV8830.Fault.getFault(fault);
                    logger.error("test02(): Nach brake() " + error.getReason());
                }
            }
            
            for (int speed = 0; speed < limit;)
            {
                logger.info("drive()...: " + (-speed) );
                
                int fault = this.drv8830.getFault(); 
                // Bei fault == 0 => Fehlerfrei, sonst Fehler!
                if (fault != 0)
                {
                    DRV8830.Fault error = DRV8830.Fault.getFault(fault);
                    logger.error("test02(): " + error.getReason());
                }
                
                this.drv8830.drive(-speed);
                // "schneller" Anstieg...
                speed += delta;
                Thread.sleep(DELAY);                
            }

            {
                logger.info("brake()..." );
                
                // Abbremsen...
                this.drv8830.brake();
                
                int fault = this.drv8830.getFault(); 
                // Bei fault == 0 => Fehlerfrei, sonst Fehler!
                if (fault != 0)
                {
                    DRV8830.Fault error = DRV8830.Fault.getFault(fault);
                    logger.error("test02(): Nach brake() " + error.getReason());
                }
            }
            
        }
        catch(IOException | InterruptedException exception)
        {
            fail("IOException in drive()", exception);   
        }
    
    }
    
}
