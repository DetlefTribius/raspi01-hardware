/**
 * 
 */
package raspi.hardware;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * @author Detlef Tribius
 * 
 * 
 * 
 * Vgl. auch https://www.fambach.net/raspberry-pi-3-us-100/
 *      auch https://sites.google.com/site/myscratchbooks/home/projects/project-09-ultrasonic-sensor
 *
 *
 *  Using the US-100 Sensor in Pulse Width Mode
 *
 *  Select the pulse mode by removing the shunt from the operating mode selection jumper. 
 *  Connect the Trig/TX pin to a digital output on your microcontroller and the Echo/RX pin 
 *  to a digital input.
 *
 *  To obtain a distance measurement, set the Trig/TX pin high for at least 50 microseconds 
 *  then set it low to trigger the measurement. The module will output a high pulse on the 
 *  Echo/RX line with a width that corresponds to the distance measured. 
 *  Use your microcontroller to measure the pulse width using microseconds. 
 *  Use the following formula to calculate the distance:
 *
 *   Millimeters = PulseWidth * 34 / 100 / 2
 */
public class US100Sensor
{

    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(US100Sensor.class);

    /**
     * gpio - Referenz auf den GpioController
     */
    private final GpioController gpio;
   
    private final boolean isRaspi;
    
    private final GpioPinDigitalOutput trigTxOutput;
    
    private final GpioPinDigitalInput echoRxInput;
    
    /**
     * 
     */
    private Status status = US100Sensor.Status.NEUTRAL;
    
    /**
     * nanoTimeRising - nanoTime ansteigende Flanke 
     */
    private long nanoTimeRising = 0;
    
    /**
     * nanoTimeFalling - nanoTime fallende Flanke
     * <p>
     * Die Laufzeit des Schalls ergibt sich aus: <br> 
     * <code>nanoTimeFalling - nanoTimeRising</code>
     * </p>
     */
    private long nanoTimeFalling = 0;
    /**
     * deltaNanoTime - Laufzeit des Schalls
     * <p>
     * Die Laufzeit des Schalls deltaNanoTime ergibt sich aus: <br>
     * <code>deltaNanoTime = nanoTimeFalling - nanoTimeRising</code>
     * </p>
     */
    private long deltaNanoTime = 0;
    
    /**
     * Laufzeit in ms
     */
    private BigDecimal deltaTime = BigDecimal.ZERO;
    
    /**
     * NANO_TO_MILLIS = 6 - Umrechnungs-Verschiebung (Zehnerpotenz)
     * von Nano zu Milli bei Anzeige der Laufzeit in ms.
     */
    private final static int NANO_TO_MILLIS = 6;
    
    private final static int SCALE_DELTA_TIME = 1;
    
    /**
     * Abstand in cm
     */
    private BigDecimal distance = BigDecimal.ZERO;
    
    /**
     * 
     */
    private final static long FACTOR = 34/2;
    
    /**
     * BENCHMARK_DISTANCE = 6 - Anzahl der Linksverschiebungen des Dezimalpunktes
     * zum Ergebnis in cm.
     * <p>
     * Umsetzung von Nanosekunden zu cm, 
     * Nano- zu Mikrosekunden: 3
     * dividiert durch 100: 2
     * Millimeter zu cm: 1
     * Summe der Verschiebungen: 6
     * </p>
     */
    private final static int BENCHMARK_DISTANCE = 6;
    
    /**
     * SCALE_DISTANCE = 2 - Genauigkeit, 2 Nachkommastellen
     */
    private final static int SCALE_DISTANCE = 2;
    
    /**
     * 
     * @param trigTxPin
     * @param echoRxPin
     */
    public US100Sensor(GpioController gpio,
                       Pin trigTxPin, 
                       Pin echoRxPin)
    {
        this.gpio = gpio;
        this.isRaspi = (gpio != null);
        // ==> DigitalOutputPin auf dem Raspi instanziieren, sonst null!
        this.trigTxOutput = (this.isRaspi)? this.gpio.provisionDigitalOutputPin(trigTxPin,
                                                                                trigTxPin.getName(),
                                                                                PinState.LOW)
                                          : null;
        // ==> DigitalInputPin auf dem Raspi instanziieren, sonst null!
        this.echoRxInput = (this.isRaspi)? this.gpio.provisionDigitalInputPin(echoRxPin,
                                                                              echoRxPin.getName(),
                                                                              PinPullResistance.OFF)
                                         : null;
        
        if (!this.isRaspi)
        {
            return;
        }
        this.echoRxInput.addListener(new GpioPinListenerDigital() 
        {
            /**
             * handleGpioPinDigitalStateChangeEvent()...
             */
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event)
            {
                final GpioPin gpioPin = event.getPin();
                final PinEdge pinEdge = event.getEdge();
                if (PinEdge.RISING == pinEdge)
                {
                    final long nowRising = System.nanoTime();
                    // logger.debug("Rising: " + nowRising);
                    if (US100Sensor.this.status == US100Sensor.Status.STARTED)
                    {
                        US100Sensor.this.status = US100Sensor.Status.RISING;
                        US100Sensor.this.nanoTimeRising = nowRising;
                        US100Sensor.this.nanoTimeFalling = 0L;
                        US100Sensor.this.deltaNanoTime = 0L;
                    }
                    return;
                }
                if (PinEdge.FALLING == pinEdge)
                {
                    final long nowFalling = System.nanoTime();
                    // logger.debug("Falling: " + nowFalling);
                    if (US100Sensor.this.status == US100Sensor.Status.RISING)
                    {
                        US100Sensor.this.status = US100Sensor.Status.FALLING;
                        US100Sensor.this.nanoTimeFalling = nowFalling;
                        // deltaNanoTime - long-Laufzeit in nano-s 
                        US100Sensor.this.deltaNanoTime = US100Sensor.this.nanoTimeFalling - US100Sensor.this.nanoTimeRising;

                        // Laufzeit in ms...
                        US100Sensor.this.deltaTime = BigDecimal.valueOf(US100Sensor.this.deltaNanoTime).movePointLeft(NANO_TO_MILLIS)
                                                                                                       .setScale(SCALE_DELTA_TIME, BigDecimal.ROUND_HALF_UP);

                        logger.debug("deltaTime: " + US100Sensor.this.deltaTime.toString());
                        
                        // Distance in cm...
                        US100Sensor.this.distance = BigDecimal.valueOf(US100Sensor.this.deltaNanoTime * FACTOR).movePointLeft(BENCHMARK_DISTANCE)
                                                                                                               .setScale(SCALE_DISTANCE, BigDecimal.ROUND_HALF_UP);
                        logger.debug("distance: " + US100Sensor.this.distance.toString() );
                    }
                    return;
                }
            }
        });
    }

    /**
     * startMeasuring()
     * @throws InterruptedException 
     */
    public void startMeasuring() throws InterruptedException
    {
        if (isRaspi)
        {
            while(this.echoRxInput.isHigh())
            {
                Thread.sleep(1);
            }
            //
            this.status = US100Sensor.Status.STARTED;
            this.nanoTimeRising = 0L;
            this.nanoTimeFalling = 0L;
            this.deltaNanoTime = 0L;
            //
            this.trigTxOutput.low();
            Thread.sleep(2);
            this.trigTxOutput.high();
            Thread.sleep(2);
            this.trigTxOutput.low();
            Thread.sleep(10);
        }
        else
        {
            // ==> Lauf nicht auf dem Raspi!!
            Thread.sleep(100);
            logger.debug("No Raspi...");
        }
    }
    
    /**
     * @return the deltaTime
     */
    public final BigDecimal getDeltaTime()
    {
        return this.deltaTime;
    }

    /**
     * @return the distance
     */
    public final BigDecimal getDistance()
    {
        return this.distance;
    }

    /**
     * 
     * Status - enum beschreibt den Status
     * des Messvorganges
     * 
     * @author Detlef Tribius
     *
     */
    enum Status
    {
        NEUTRAL("Neutral"),
        STARTED("Started"),
        RISING("Rising"),
        FALLING("Falling");
        /**
         * Status
         * @param status
         */
        private Status(String status)
        {
            this.status = status;
        }
        /**
         * status - textuelle Beschreibung des Status
         */
        private final String status;
        
        /**
         * @return the status
         */
        public final String getStatus()
        {
            return this.status;
        }
    }
    
}
