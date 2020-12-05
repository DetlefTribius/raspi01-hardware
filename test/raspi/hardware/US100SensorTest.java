/**
 * 
 */
package raspi.hardware;

import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

/**
 * @author Detlef Tribius
 *
 */
class US100SensorTest
{

    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(US100SensorTest.class);    

    /**
     * isRaspi - Kennung fuer Lauf auf einem Raspi...
     */
    private static boolean isRaspi = false;

    /**
     * OS_NAME_RASPI = "linux" - Kennung fuer Linux.
     * <p>
     * ...wird verwendet, um einen Raspi zu erkennen...
     * </p>
     */
    public final static String OS_NAME_RASPI = "linux";
    /**
     * OS_ARCH_RASPI = "arm" - Kennung fuer die ARM-Architektur.
     * <p>
     * ...wird verwendet, um einen Raspi zu erkennen...
     * </p>
     */
    public final static String OS_ARCH_RASPI = "arm";

    /**
     * 
     */
    public final static Pin TRIG_TX_PIN = RaspiPin.GPIO_01;
    
    /**
     * 
     */
    public final static Pin ECHO_RX_PIN = RaspiPin.GPIO_02;

    
    /**
     * DELAY = 1000 Pausenzeit (1000 ms) fuer einzelne Aktionen...
     */
    public final static int DELAY = 1000;
    
    /**
     * COUNTER_LIMIT = 20 - Grenze Durchlaufzaehler...
     */
    public final static int COUNTER_LIMIT = 20;
    
    /**
     * gpio - Referenz auf den GpioController
     */
    private GpioController gpio = null;
    
    /**
     * us100Sensor - Referenz auf den Sensor...
     */
    private US100Sensor us100Sensor = null;;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeAll
    static void setUpBeforeClass() throws Exception
    {
        logger.info("setUpBeforeClass()...");

        // 1.) Wo erfolgt der Lauf, auf einem Raspi?
        final String os_name = System.getProperty("os.name").toLowerCase();
        final String os_arch = System.getProperty("os.arch").toLowerCase();
        logger.debug("Betriebssytem: " + os_name + " " + os_arch);
        // Kennung isRaspi setzen...
        US100SensorTest.isRaspi = OS_NAME_RASPI.equals(os_name) && OS_ARCH_RASPI.equals(os_arch);
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
        // Controller "besorgen"...
        this.gpio = US100SensorTest.isRaspi? GpioFactory.getInstance() : null;
        // Zugriff auf den Sensor instanziieren...
        this.us100Sensor = new US100Sensor(this.gpio, 
                                           US100SensorTest.TRIG_TX_PIN, 
                                           US100SensorTest.ECHO_RX_PIN);

    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception
    {
    }

    @Test
    void testSensor01()
    {
        logger.info("testSensor01()...");
        try
        {
            int counter = 0;
            while(counter++ < US100SensorTest.COUNTER_LIMIT)
            {
                this.us100Sensor.startMeasuring();
                Thread.sleep(DELAY);
                final BigDecimal distance = this.us100Sensor.getDistance();
                logger.info("Distance " + counter + " : " + distance);
                Thread.sleep(DELAY);
            }    
        } 
        catch (InterruptedException exception)
        {
            fail("Exception in testSensor01()!", exception);
        }
    }
}
