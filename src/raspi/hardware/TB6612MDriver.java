/**
 * 
 */
package raspi.hardware;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;

import raspi.hardware.i2c.PCA9685;

/**
 * Die Klasse TB6612MDriver beschreibt das Verhalten
 * des Motor Driver Module mit dem IC TB6612.
 * Der Driver umfasst die Ansteuerung zweier DC-Motoren
 * ueber zwei Steuerleitungen und zweier PWM-Kanaele.
 * 
 * <p>
 * Die Klasse TB6612MDriver kapselt die Funktionalitaet des Schaltkreises
 * TB6612. Dieser Schaltkreis ist der Motortreiber zwischen dem PCA9685 
 * (PWM-Erzeugung) und den beiden DC-Motoren MA und MB.
 * </p>
 * <p>
 * Zusaetzlich zur PWM erfogt die Freigabe einer Drehrichtung ueber 
 * zwei Signale (outputPinMA und outputPinMB, je ein Signal pro Motor).
 * </p>
 * 
 * <p>
 * Der IC TB6612 finden Verwendung im PiCar-S von Sunfounder.
 * </p>
 * 
 * @author Detlef Tribius
 *
 */
public class TB6612MDriver
{
    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(TB6612MDriver.class);    
    
    /**
     * outputPinMA - Steuerleitung Raspi => TB6612 (Motor A)
     */
    private final GpioPinDigitalOutput outputPinMA;
    /**
     * outputPinMB - Steuerleitung Raspi => TB6612 (Motor B)
     */
    private final GpioPinDigitalOutput outputPinMB;
    /**
     * motorA - PWM-Kanal Motor A
     */
    private final PCA9685.Motor motorA;
    /**
     * motorB - PWM-Kanal Motor B
     */
    private final PCA9685.Motor motorB;
    
    /**
     * Motor-Status
     */
    private MotorState motorState = MotorState.STOP;
    
    /**
     * MAXIMUM_POWER = 1.0f - Normierungsgroesse zur Sollwertangabe
     * <p>
     * Anm.: Wenn die Normierungsgroesse erreicht ist, gib die PWM den
     * Maximalwert aus.
     * </p>
     */
    public final static float MAXIMUM_POWER = 1.0f;
    
    /**
     * MAXIMUM_PWM ergibt sich aus der Aufloesung (12 Bit => 4096-1)...
     */
    private final static int MAXIMUM_PWM = PCA9685.RESOLUTION-1;;
    
    /**
     * Konstruktor TB6612MDriver
     * <p>
     * Die Pins und pWM-Kanaele werden ausserhalb instanziiert
     * und jeweils als Referenzen uebergeben!
     * </p>
     * @param outputPinMA
     * @param outputPinMB
     * @param motorA
     * @param motorB
     */
    public TB6612MDriver(GpioPinDigitalOutput outputPinMA, 
                         GpioPinDigitalOutput outputPinMB,
                         PCA9685.Motor motorA,
                         PCA9685.Motor motorB)
    {
        // Pins auf Output eingerichtet...
        this.outputPinMA = outputPinMA;
        this.outputPinMB = outputPinMB;
        
        this.motorA = motorA;
        this.motorB = motorB;
    }
    
    /**
     * setPWM(float speed)
     * @param speed
     * @throws IOException 
     * @throws InterruptedException 
     */
    public void setPWM(float speed) throws IOException, InterruptedException
    {
        final int pwmValue = Math.round(Math.abs(speed/MAXIMUM_POWER)*MAXIMUM_PWM);

        this.motorState = getMotorState(speed);
        
        final boolean isMA = this.motorState.isPinMA();
        final boolean isMB = this.motorState.isPinMB();
        
        Thread.sleep(50);
        
        this.outputPinMA.setState(isMA? PinState.HIGH : PinState.LOW);
        this.outputPinMB.setState(isMB? PinState.HIGH : PinState.LOW);
        
        this.motorA.setPWM(pwmValue);
        this.motorB.setPWM(pwmValue);

        logger.debug("pwmValue: " + pwmValue + ", isMA: " + isMA + " isMB: " + isMB);
    }
    
    /**
     * getGpioPins() - liefert Array mit den Pins, um sie ggf. zurueckzusetzen...
     * @return
     */
    public GpioPin[] getGpioPins()
    {
        return new GpioPin[]{this.outputPinMA, this.outputPinMB};
    }
    
    /**
     * getMotorState(float speed) - liefert den Motorstatus...
     * @param float speed
     * @return MotorState
     */
    private static MotorState getMotorState(float speed)
    {
        if (speed > 0)
        {
            return MotorState.FORWARD;
        }
        if (speed < 0)
        {
            return MotorState.BACKWARD;
        }
        return MotorState.STOP;
    }
    
    /**
     * reset()
     */
    public void reset()
    {
        logger.debug("reset()...");
        if (this.outputPinMA != null)
        {
            this.outputPinMA.setShutdownOptions(false, PinState.LOW);
            this.outputPinMA.setState(PinState.HIGH);
        }
        if (this.outputPinMB != null)
        {
            this.outputPinMB.setShutdownOptions(false, PinState.LOW);
            this.outputPinMB.setState(PinState.HIGH);
        }
        
    }
    
    /**
     * enum MotorState
     * 
     * <p>
     * Der MotorState haelt die Vorgaben fuer die Belegung der 
     * Eingaben MA und MB zur Vorgabe der Drehrichtung...
     * </p>
     * @author Detlef Tribius
     */
    enum MotorState
    {
        /** Zustand "Stop" */
        STOP(0, "stop") 
        {
            @Override
            public boolean isPinMA()
            {
                return false;
            }

            @Override
            public boolean isPinMB()
            {
                return false;
            }
        },
        /** Zustand "Vorwaerts" */
        FORWARD(1, "forward") 
        {
            @Override
            public boolean isPinMA()
            {
                return false;
            }

            @Override
            public boolean isPinMB()
            {
                return false;
            }
        },
        /** Zustand "Rueckwaerts" */
        BACKWARD(-1, "backward") 
        {
            @Override
            public boolean isPinMA()
            {
                return true;
            }

            @Override
            public boolean isPinMB()
            {
                return true;
            }
        };

        /**
         * Konstruktor
         * @param state
         */
        private MotorState(int value, String name)
        {
            this.value = value;
            this.name = name;
        }
        
        private final int value;
        /**
         * String state - Zustandsbeschreibung...
         */
        private final String name;
        
        /**
         * isPinMA() - Steuerung Ausgabe pinMA je nach Status (Vorwaerts/Rueckwaerts...)
         * <p>
         * isPinMA() wird im Status implementiert...
         * </p>
         * @return boolean fuer pinA
         */
        public abstract boolean isPinMA();
        
        /**
         * isPinMB() - Steuerung Ausgabe pinMA je nach Status (Vorwaerts/Rueckwaerts...)
         * <p>
         * isPinMB() wird im Status implementiert...
         * </p>
         * @return boolean fuer pinB
         */
        public abstract boolean isPinMB();
        
        /**
         * @return the value
         */
        public final int getValue()
        {
            return value;
        }

        /**
         * @return the name
         */
        public final String getName()
        {
            return name;
        }
    }

}
