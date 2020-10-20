/**
 * 
 */
package raspi.hardware;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import raspi.hardware.i2c.PCA9685;


/**
 * TB6612MDriverTest
 * 
 * <p>
 * Die Klasse TB6612MDriver kapselt die Funktionalitaet des Schaltkreises
 * TB6612. Dieser Schaltkreis ist der Motortreiber zwischen dem PCA9685 
 * (PWM-Erzeugung) und den beiden DC-Motoren MA und MB.
 * 
 * Zusaetzlich zur PWM erfogt die Freigabe einer Drehrichtung ueber zwei Signale
 * (je ein Signal pro Motor).
 * </p>
 * <p>
 * Die Signalleitungen sind beim PiCar-S mit den Raspi-Ausgaengen B17 und B27
 * verbunden.
 * </p>
 * <ul>
 *  <li>MA => B17</li>
 *  <li>MB => B27</li>
 * </ul>
 * <p>
 * Anm.: Pin-Zuordnung:
 * <ul>
 * <li>WiringPi: 0  Pin-Name:  (GPIO_GEN0) GPIO 17  Pin:   11</li>
 * <li>WiringPi: 2  Pin-Name:  (GPIO_GEN2) GPIO 27  Pin    13</li>
 * </ul>
 * </p>
 * 
 * @author Detlef Tribius
 *
 */
class TB6612MDriverTest
{

    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(TB6612MDriverTest.class);
     
    /**
     * gpio - Referenz auf den GpioController
     */
    private GpioController gpio = null;
    
    /**
     * i2cBus - Referenz auf den IC2Bus...
     */
    private static I2CBus i2cBus = null;
    
    /**
     * Referenz auf den PWM-Driver
     */
    private PCA9685 pca9685 = null;
    
    /**
     * Referenz auf den TB6612MDriver...
     */
    private TB6612MDriver motorDriver;
    
    /**
     * ADDRESS - Bus-Adresse des PCA9685-Bausteins (PWM-Driver), 
     * festgelegt durch 'Verdrahtung' auf dem Baustein... 
     */
    public final static int ADDRESS = 0x40; 
    
    /**
     * PIN_MA - zur Ansteuerung des Motor A (Drehrichtung...)
     */
    private final static Pin PIN_MA = RaspiPin.GPIO_00;
    
    /**
     * PIN_MB - zur Ansteuerung des Motor B (Drehrichtung...)
     */
    private final static Pin PIN_MB = RaspiPin.GPIO_02;
    
    /**
     * DELAY = 1000 Pausenzeit (1000 ms) fuer einzelne Aktionen...
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
            TB6612MDriverTest.i2cBus = I2CFactory.getInstance(I2CBus.BUS_1);
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
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception
    {
        logger.info("setUp()...");
        
        this.gpio = GpioFactory.getInstance();
        
        // outputPinMA einrichten...
        final GpioPinDigitalOutput outputPinMA = this.gpio.provisionDigitalOutputPin(TB6612MDriverTest.PIN_MA, 
                                                                                     TB6612MDriverTest.PIN_MA.getName(), 
                                                                                     PinState.LOW);
        // outputPinMB einrichten...
        final GpioPinDigitalOutput outputPinMB = this.gpio.provisionDigitalOutputPin(TB6612MDriverTest.PIN_MB, 
                                                                                     TB6612MDriverTest.PIN_MB.getName(), 
                                                                                     PinState.LOW);
        
        this.pca9685 = PCA9685.getInstance(TB6612MDriverTest.i2cBus.getDevice(ADDRESS));
        this.pca9685.initialize();
        logger.info("initialize() erfolgreich.");
        this.pca9685.setPWMFrequency(50);
        Thread.sleep(DELAY);        
        logger.info("setPWMFrequency(50) erfolgreich.");
        
        // motorA, motorB - Referenzen zum Zugriff auf PWM...
        final PCA9685.Motor motorA = this.pca9685.getMotor(4);
        final PCA9685.Motor motorB = this.pca9685.getMotor(5);
        
        // Die Referenzen auf die Steuer-Pins und die PMW-Kanaele werden dem TB6612MDriver
        // beim Erzeugen mitgegeben...
        this.motorDriver = new TB6612MDriver(outputPinMA, outputPinMB, motorA, motorB);
        this.motorDriver.reset();
        // ...mach mal Pause...
        Thread.sleep(DELAY);        
    }

    /**
     * tearDown()...
     * <p>
     * Die Pins muessen wieder freigegeben werden!
     * </p>
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception
    {
        this.gpio.shutdown();
        final GpioPin[] pins = this.motorDriver.getGpioPins();
        this.gpio.unprovisionPin(pins);
        this.gpio = null;
        logger.info("tearDown()...");
    }

    /**
     * Hochfahren der Motoren in Fahrtrichtung ...
     */
    @Test
    void testTB6612MDriver_1()
    {
        logger.info("testTB6612MDriver_1()...");
        try
        {
            final float maxValue = 0.4f;
            final float deltaValue = 0.025f;
            float value = 0.0f;
            while (value < maxValue)
            {
                this.motorDriver.setPWM(value); 
                Thread.sleep(DELAY);
                value += deltaValue;
            }
            value = maxValue;
            while(value > 0.0f)
            {
                this.motorDriver.setPWM(value); 
                Thread.sleep(DELAY);
                value -= deltaValue;
            }
            this.motorDriver.setPWM(0.0f); 
            Thread.sleep(DELAY);
            
            logger.info("testTB6612MDriver_1() beendet.");
        } 
        catch (IOException | InterruptedException exception)
        {
            fail("IOException in testTB6612MDriver_1()", exception);             
        }
    }
    
    /**
     * Hochfahren der Motoren in Gegen-Fahrtrichtung...
     */
    @Test
    void testTB6612MDriver_2()
    {
        logger.info("testTB6612MDriver_2()...");
        try
        {
            final float maxValue = 0.4f;
            final float deltaValue = 0.025f;
            float value = 0.0f;
            while (value < maxValue)
            {
                this.motorDriver.setPWM(-value); 
                Thread.sleep(DELAY);
                value += deltaValue;
            }
            value = maxValue;
            while(value > 0.0f)
            {
                this.motorDriver.setPWM(-value); 
                Thread.sleep(DELAY);
                value -= deltaValue;
            }
            this.motorDriver.setPWM(0.0f); 
            Thread.sleep(DELAY);
            
            logger.info("testTB6612MDriver_2() beendet.");
        } 
        catch (IOException | InterruptedException exception)
        {
            fail("IOException in testTB6612MDriver_2()", exception);             
        }
    }
}
