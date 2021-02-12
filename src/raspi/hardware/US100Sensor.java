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
 * <p>
 * 
 * Vgl. auch https://www.fambach.net/raspberry-pi-3-us-100/ <br>
 *      auch https://sites.google.com/site/myscratchbooks/home/projects/project-09-ultrasonic-sensor <br>
 * </p>
 * 
 * <p>
 *  <b>Using the US-100 Sensor in Pulse Width Mode </b>
 * </p>
 * <p>
 *  Select the pulse mode by removing the shunt from the operating mode selection jumper. 
 *  Connect the Trig/TX pin to a digital output on your microcontroller and the Echo/RX pin 
 *  to a digital input.
 * </p>
 * <p>
 *  To obtain a distance measurement, set the Trig/TX pin high for at least 50 microseconds 
 *  then set it low to trigger the measurement. The module will output a high pulse on the 
 *  Echo/RX line with a width that corresponds to the distance measured. 
 *  Use your microcontroller to measure the pulse width using microseconds. 
 *  Use the following formula to calculate the distance:
 * </p>
 * <p>
 *   <code>Millimeters = PulseWidth * 34 / 100 / 2</code>
 * </p>
 */
public abstract class US100Sensor
{

    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(US100Sensor.class);

    /**
     * gpio - Referenz auf den GpioController
     */
    private final GpioController gpio;
   
    /**
     * isRaspi - boolsche Kennung, wenn Lauf im BS des Raspi wird Kennung
     * auf true gesetzt. Damit ist Lauffaehigkeit auch in anderen
     * Umgebungen moeglich...
     */
    private final boolean isRaspi;
    
    /**
     * trigTxOutput - beschreibt Trigger-Pin (als Output)
     */
    private final GpioPinDigitalOutput trigTxOutput;
    
    /**
     * echoRxInput - beschreibt Echo-Pin (als Input)
     */
    private final GpioPinDigitalInput echoRxInput;
    
    /**
     * Status status - Statusablage...
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
    
    /**
     * SCALE_DELTA_TIME = 1 - Anzeigegenauigkeit der Differenzzeit, 
     * 1 Nachkommastelle
     */
    private final static int SCALE_DELTA_TIME = 1;
    
    /**
     * distance - Abstand in cm
     */
    private BigDecimal distance = BigDecimal.ZERO;
    
    /**
     * FACTOR - Konstante 34/2 (zur Berechnung des Abstandes aus der Laufzeit
     * des Schalls)
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
     * SCALE_DISTANCE = 1 - Genauigkeit, 1 Nachkommastellen (Angabe in cm)
     */
    private final static int SCALE_DISTANCE = 1;
    
    /**
     * US100Sensor(...) - Konstruktor zum US100Sensor
     * @param gpio - Referenz auf Controller
     * @param trigTxPin - Referenz auf Trigger-Pin
     * @param echoRxPin - Referenz auf Echo-Pin 
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
                    /////////////////////////////////////////////////////////////////////////////////////
                    // => Weniger Ausgaben...
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
                    /////////////////////////////////////////////////////////////////////////////////////
                    // => Weniger Ausgaben...
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

                        /////////////////////////////////////////////////////////////////////////////////////
                        // => Weniger Ausgaben...
                        // logger.debug("deltaTime: " + US100Sensor.this.deltaTime.toString());
                        
                        // Distance in cm...
                        US100Sensor.this.distance = BigDecimal.valueOf(US100Sensor.this.deltaNanoTime * FACTOR).movePointLeft(BENCHMARK_DISTANCE)
                                                                                                               .setScale(SCALE_DISTANCE, BigDecimal.ROUND_HALF_UP);

                        /////////////////////////////////////////////////////////////////////////////////////
                        // => Weniger Ausgaben...
                        // logger.debug("distance: " + US100Sensor.this.distance.toString() );
                        
                        // Messergebnis uebermitteln...
                        final ResultVO resultVO = new ResultVO(nowFalling,
                                                               US100Sensor.this.deltaTime,
                                                               US100Sensor.this.distance);
                        setResultVO(resultVO);
                    }
                    return;
                }
            }
        });
        
        logger.debug("US100Sensor instanziiert.");
    }

    /**
     * startMeasuring() - Starten des Messvorganges...
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
     * getStatus()
     * @return the status
     */
    public final Status getStatus()
    {
        return status;
    }

    /**
     * setResultVO(ResultVO resultVO);
     * <p>
     * Die abstract-Methode dient dazu, das Ergebnis des Messvorganges, hier
     * das ResultVO in die Umgebung des Aufrufers zu transferieren. Die aufrufende
     * Umgebung muss diese Methode bereitstellen, die dann am Ende des Messvorganges
     * aufgerufen wird. 
     * </p>
     * @param resultVO
     */
    public abstract void setResultVO(ResultVO resultVO);
    
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
    
    /**
     * 
     * @author Detlef Tribius
     *
     */
    public class ResultVO
    {
        /**
         * VO_NANOTIME = "voNanoTime"
         */
        public final static String VO_NANOTIME = "voNanoTime";
        /**
         * VO_DELTATIME = "voDeltaTime"
         */
        public final static String VO_DELTATIME = "voDeltaTime";
        /**
         * VO_DISTANCE = "voDistance"
         */
        public final static String VO_DISTANCE = "voDistance";
        
        /**
         * nanoTime
         */
        final Long nanoTime;
        /**
         * deltaTime - Messzeit
         */
        final BigDecimal deltaTime;
        /**
         * distance - Absatnd in cm
         */
        final BigDecimal distance;
        /**
         * ResultVO(long nanoTime, BigDecimal deltaTime, BigDecimal distance)
         * @param nanoTime
         * @param deltaTime
         * @param distance
         */
        public ResultVO(long nanoTime, BigDecimal deltaTime, BigDecimal distance)
        {
            this.nanoTime = Long.valueOf(nanoTime);
            this.deltaTime = (deltaTime != null)? deltaTime : BigDecimal.ZERO.setScale(SCALE_DELTA_TIME);
            this.distance = (distance != null)? distance : BigDecimal.ZERO.setScale(SCALE_DISTANCE);
        }
        
        /**
         * getNanoTime() - 
         * @return the nanoTime, Ergebnis von System.nanoTime()
         */
        public final Long getNanoTime()
        {
            return nanoTime;
        }

        /**
         * getDeltaTime()
         * @return the deltaTime - Messzeit
         */
        public final BigDecimal getDeltaTime()
        {
            return deltaTime;
        }

        /**
         * getDistance() - liefert Distanz in cm
         * @return the distance in cm
         */
        public final BigDecimal getDistance()
        {
            return distance;
        }

        /**
         * getKeys() - liefert die Keys zum Zugriff auf die Values...
         * @return new String[] {VO_NANOTIME, VO_DELTATIME, VO_DISTANCE}
         */
        public String[] getKeys()
        {
            return new String[] {VO_NANOTIME, VO_DELTATIME, VO_DISTANCE};
        }
        
        /**
         * getValue(String key)
         * @param key String key
         * @return Object
         */
        public final Object getValue(String key)
        {
            if (ResultVO.VO_NANOTIME.equals(key))
            {
                return this.nanoTime;
            }
            if (ResultVO.VO_DELTATIME.equals(key))
            {
                return this.deltaTime;
            }
            if (ResultVO.VO_DISTANCE.equals(key))
            {
                return this.distance;
            }
            return null;
        }
        
        /**
         * toString() - zu Protokollzwecken...
         * @return String "[...]"
         */
        public String toString()
        {
            return new StringBuilder().append("[")
                                      .append(this.nanoTime)
                                      .append(" ")
                                      .append(this.deltaTime)
                                      .append(" ")
                                      .append(this.distance)
                                      .append("]")
                                      .toString();
        }
    }
}
