/**
 * 
 */
package raspi.hardware.i2c;

import java.io.IOException;

import com.pi4j.io.i2c.I2CDevice;

/**
 * @author Detlef Tribius
 *
 * <p>
 * <code>http://wiki.sunfounder.cc/index.php?title=PCA9685_16_Channel_12_Bit_PWM_Servo_Driver</code>
 * </p>>
 * 
 *
 * <p>
 * Ausfuehrung als Singleton, vgl. auch <br> 
 * <code>https://www.geeksforgeeks.org/java-singleton-design-pattern-practices-examples/ </code>
 * </p>
 */
public class PCA9685 extends I2C
{

    /**
     * instance - Instance fuer Singleton...
     */
    private static PCA9685 instance;

    /**
     * RESOLUTION = 4096 - Aufloesung, somit 0 ... (RESOLUTION-1)
     */
    public final static int RESOLUTION = 4096;
    
    /** NUMBER_CHANNELS = 16 */
    public final static int NUMBER_CHANNELS = 16;

    /**
     * 
     */
    private final PCA9685.Channel[] channels = new PCA9685.Channel[]
    {
        new PCA9685.Channel(Property.CHANNEL0),
        new PCA9685.Channel(Property.CHANNEL1),
        new PCA9685.Channel(Property.CHANNEL2),
        new PCA9685.Channel(Property.CHANNEL3),
        new PCA9685.Channel(Property.CHANNEL4),
        new PCA9685.Channel(Property.CHANNEL5),
        new PCA9685.Channel(Property.CHANNEL6),
        new PCA9685.Channel(Property.CHANNEL7),
        new PCA9685.Channel(Property.CHANNEL8),
        new PCA9685.Channel(Property.CHANNEL9),
        new PCA9685.Channel(Property.CHANNEL10),
        new PCA9685.Channel(Property.CHANNEL11),
        new PCA9685.Channel(Property.CHANNEL12),
        new PCA9685.Channel(Property.CHANNEL13),
        new PCA9685.Channel(Property.CHANNEL14),
        new PCA9685.Channel(Property.CHANNEL15)
    };
    
    /**
     * frequency - PWM-Frequenz
     */
    private int frequency;
    
    // Register...
    /** MODE1_REGISTER = 0x00 Mode Register 1 */
    public final static int MODE1_REGISTER = 0x00;
    /** MODE2_REGISTER = 0x00 Mode Register 2 */
    public final static int MODE2_REGISTER = 0x01;
    /** SUBADR1 = PCA9685 response to I2C-Bus subaddress 1 */
    public final static int SUBADR1 = 0x02;
    /** SUBADR2 = PCA9685 response to I2C-Bus subaddress 2 */
    public final static int SUBADR2 = 0x03;
    /** SUBADR3 = PCA9685 response to I2C-Bus subaddress 3 */
    public final static int SUBADR3 = 0x04;
    /** ALLCALLADR = PCA9685 response to LED all call I2C-Bus address */
    public final static int ALLCALLADR = 0x05;    
    
    ////////////////////////////////////////////////////////////////
    // Ablage der 'Zeitpunkte' (0 bis 4095) fuer alle
    // 16 Channel, dabei Ablage Low-Teil und High-Teil getrennt.
    // L_REGISTER Bit 0..7; H_REGISTER Bit 0..3, insgesamt 12 Bit.
    ////////////////////////////////////////////////////////////////
    /** ALL_LED_ON_L_REGISTER = 0xfa; */
    public final static int ALL_LED_ON_L_REGISTER = 0xfa;
    /** ALL_LED_ON_H_REGISTER = 0xfb; */
    public final static int ALL_LED_ON_H_REGISTER = 0xfb;
    /** ALL_LED_OFF_L_REGISTER = 0xfc; */
    public final static int ALL_LED_OFF_L_REGISTER = 0xfc;
    /** ALL_LED_OFF_H_REGISTER = 0xfc; */
    public final static int ALL_LED_OFF_H_REGISTER = 0xfd;
    
    /** PRE_SCALE_REGISTER = 0xfe; */
    public final static int PRE_SCALE_REGISTER = 0xfe;
    
    ////////////////////////////////////////////////////////////////
    // Konstanten fuer das MODE1_REGISTER...
    ////////////////////////////////////////////////////////////////
    /** 
     * SLEEP Bit 4, daher Wert 0x10
     * wenn Bit gleich 0, dann 'Normal mode'
     * wenn Bit gleich 1, dann 'Low power mode', Oscillator off 
     */
    public final static int SLEEP = 0x10;

    /**
     * ALLCALL Bit 0, daher Wert 0x01
     * wenn Bit gleich 0, dann 'PCA9685 does not respond to LED All Call I 2 C-bus address.'
     * wenn Bit gleich 1, dann 'PCA9685 responds to LED All Call I2C-bus address.'
     * Evtl. Beauftragung in der Initialisierung: write(MODE1_REGISTER, (byte) ALLCALL);
     */
    public final static int ALLCALL = 0x01;

    /**
     * RESTART Bit 7, daher Wert 0x80
     * User writes logic 1 to this bit to clear it to logic 0. 
     * A user write of logic 0 will have no effect.
     * <ul>
     *  <li>0 - Restart disabled.</li>
     *  <li>1 - Restart enabled.</li>
     * </ul>
     */
    public final static int RESTART = 0x80;
    ////////////////////////////////////////////////////////////////
    // Ende Konstanten fuer das MODE1_REGISTER.
    ////////////////////////////////////////////////////////////////
    
    /**
     * SWRST = 0x06; Software Reset Call (SWRST Call) 
     */
    public final static int SWRST = 0x06;
    
    ////////////////////////////////////////////////////////////////
    // Konstanten fuer das MODE2_REGISTER...
    ////////////////////////////////////////////////////////////////
    /**
     * OUTDRV Bit 2, daher Wert gleich 0x04
     * wenn Bit gleich 0, dann 'The 16 LEDn outputs are configured with an open-drain structure.'
     * wenn Bit gleich 1, dann 'The 16 LEDn outputs are configured with a totem pole structure.'
     * Beauftragung evtl.: write(MODE2_REGISTER, (byte) OUTDRV); waehrend der initialisierung.
     */
    public final static int OUTDRV = 0x04;
    
    /**
     * INTERNAL_FREQUENCY = 25000000; 25 MHz - interne Frequenz...
     */
    public final static long INTERNAL_FREQUENCY = 25000000;
    
    /**
     * 
     * @param dev
     */
    private PCA9685(final I2CDevice dev)
    {
        super(dev); 
    }
    
    /**
     * getInstance(final I2CDevice dev) - Singleton-Implementierung
     * @param dev
     * @return
     */
    synchronized public static PCA9685 getInstance(final I2CDevice dev)
    {
        if (PCA9685.instance == null)
        {
            PCA9685.instance = new PCA9685(dev);
        }
        return PCA9685.instance; 
    }
    
    /**
     * 
     * @param number
     * @return
     */
    public PCA9685.Servo getServo(int number)
    {
        return new PCA9685.Servo(this.channels[number]);
    }
    
    /**
     * 
     * @param number
     * @return
     */
    public PCA9685.Motor getMotor(int number)
    {
        return new PCA9685.Motor(this.channels[number]); 
    }
    
    /**
     * 
     * @param frequency
     * @throws IOException
     */
    synchronized public void initialize() throws IOException
    {
        write(MODE1_REGISTER, (byte) PCA9685.SWRST);
        
        write(ALL_LED_ON_L_REGISTER, (byte) 0);
        write(ALL_LED_ON_H_REGISTER, (byte) 0);
        write(ALL_LED_OFF_L_REGISTER, (byte) 0);
        write(ALL_LED_OFF_H_REGISTER, (byte) 0);
        
        sleep(100);

        configPin(MODE1_REGISTER, 0, PCA9685.SLEEP);        
        
        sleep(100);
    }
    
    /**
     * setPWMFrequency(int frequency)
     * @param frequency
     * @throws IOException
     */
    synchronized public void setPWMFrequency(int frequency) throws IOException
    {
        write(PRE_SCALE_REGISTER, PCA9685.getPrescaleValue(frequency));
        this.frequency = frequency;
    }
    
    /**
     * 
     * @param onValue - On-Zeitpunkt [0..4095] fuer alle Channel 
     * @param offValue - Off-Zeitpunkt [0..4095] fuer alle Channel
     * @throws IOException - Problem bei write().
     */
    synchronized public void setAllChannel(int onValue, int offValue) throws IOException
    {
        write(ALL_LED_ON_L_REGISTER, (byte)(onValue & 0xff));
        write(ALL_LED_ON_H_REGISTER, (byte)((onValue >> 8) & 0xff));
        write(ALL_LED_OFF_L_REGISTER, (byte)(offValue & 0xff));
        write(ALL_LED_OFF_H_REGISTER, (byte)((offValue >> 8) & 0xff));
    }
    
    /**
     * sleep(int millis)
     * @param millis
     */
    private final void sleep(int millis)
    {
        try
        {
            Thread.sleep(millis);
        } 
        catch (InterruptedException exception)
        {
            final String message = new StringBuilder().append("Exception Thread.sleep(): ")
                                                      .append(exception.getMessage())
                                                      .toString();
            
            throw new RuntimeException(message);
        }
    }
    
    /**
     * Anm.:
     * The maximum PWM frequency is 1526 Hz if the PRE_SCALE register is set "0x03h".
     * The minimum PWM frequency is 24 Hz if the PRE_SCALE register is set "0xFFh".
     * 
     * @param int frequency (50 Hz...60 Hz)
     * @return prescaleValue (byte)
     */
    public static byte getPrescaleValue(int frequency)
    {
        //
        // Am Ende -1+0.5 => -0.5 (Anm.: +0.5 zum Runden...) 
        final double value =  Math.floor(((double)INTERNAL_FREQUENCY)/(double)(4096*frequency)-0.5);
        
        final int prescaleValue = (int)value; 
        if (prescaleValue < 0x03)
        {
            return (byte)0x03;
        }
        if (prescaleValue > 0xff)
        {
            return (byte)0xff;
        }
        return (byte)prescaleValue;
    }

    /**
     * getResolution() - liefert die Aufloesung zur PWM (=> 4096)
     * @return int PCA9685.RESOLUTION
     */
    public static final int getResolution()
    {
        return PCA9685.RESOLUTION;
    }

    /**
     * getFrequency()
     * 
     * @return the frequency
     */
    public final int getFrequency()
    {
        return frequency;
    }
    
    /**
     * 
     * @author Detlef Tribius
     *
     */
    class Channel
    {
        /**
         * 
         */
        private final Property property;
        
        private Channel(Property property)
        {
            this.property = property;
        }
        
        /**
         * setPWM(int onValue, int offValue) 
         * @param onValue
         * @param offValue
         * @throws IOException
         */
        synchronized public void setPWM(int onValue, int offValue) throws IOException
        {
            write(this.property.getOnLowByteReg(), (byte)(onValue & 0xff));
            write(this.property.getOnHighByteReg(), (byte)((onValue >> 8) & 0xff));
            write(this.property.getOffLowByteReg(), (byte)(offValue & 0xff));
            write(this.property.getOffHighByteReg(), (byte)((offValue >> 8) & 0xff));
        }
    }
    
    /**
     * 
     * @author Detlef Tribius
     *
     */
    enum Property
    {
        CHANNEL0(0),
        CHANNEL1(1),
        CHANNEL2(2),
        CHANNEL3(3),
        CHANNEL4(4),
        CHANNEL5(5),
        CHANNEL6(6),
        CHANNEL7(7),
        CHANNEL8(8),
        CHANNEL9(9),
        CHANNEL10(10),
        CHANNEL11(11),
        CHANNEL12(12),
        CHANNEL13(13),
        CHANNEL14(14),
        CHANNEL15(15);
        /**
         * 
         * @param number - Nummer des Channels, Beginn der Zaehlung bei 0!
         */
        private Property(int number)
        {
            this.number = number;
        }
        
        private final int number;
        
       
        /**
         * @return int getOnLowByteReg()
         */
        public final int getOnLowByteReg()
        {
            return (this.number<<2)+6;
        }
        /**
         * @return int getOnHighByteReg()
         */
        public final int getOnHighByteReg()
        {
            return (this.number<<2)+7;
        }
        /**
         * @return int getOffLowByteReg()
         */
        public final int getOffLowByteReg()
        {
            return (this.number<<2)+8;
        }
        /**
         * @return int getOffHighByteReg()
         */
        public final int getOffHighByteReg()
        {
            return (this.number<<2)+9;
        }
    }
    
    
    /**
     * Die Klasse Servo beschreibt einen Servo-Channel.
     * 
     * <p>
     * Servo: Typ SF006C, Beschreibung unter<br>
     * <code>http://wiki.sunfounder.cc/images/5/52/SF006C_Clutch_Gear_Digital_Servo180607.pdf</code><br>
     * T: default 20 ms<br>
     * Td: 2500us/1500us/500us  (us = Mikrosekunde)<br>
     * </p>
     * @author Detlef Tribius
     */
    public class Servo
    {
        /**
         * channel - Zugriff auf die PWM-Hardware...
         */
        private final Channel channel;
        
        /////////////////////////////////////////////////////////////////
        // Die einzelnen Groessen:
        //    |===============|=======================|===========>
        // SERVO_ON_VALUE = 0
        //               SERVO_MIN_LIMIT         SERVO_MAX_LIMIT
        //                    |<==     180 Grad    ==>|
        //                        |<=SERVO_DELTA=>|
        // 
        //              SERVO_MIN_VALUE   |    SERVO_MAX_VALUE
        
        /**
         * SERVO_ON_VALUE = 0;
         */
        public final static int SERVO_ON_VALUE = 0;
        
        /**
         * STEERING_DELTA - beschreibt die Groesse des Lenkausschlages.
         */
        public final static int STEERING_DELTA = 500;
        
        /**
         * STEERING_ADJUSTMENT - Korrekturwert zur Justierung der Lenkung...
         * => Wert durch Kalibrierung ermittelt!
         */
        public final static int STEERING_ADJUSTMENT = -20;
        
        /**
         * SERVO_MIN_LIMIT = 400; 
         * 
         * Max. Begrenzung (unterer Wert) fuer den Servomator (Ausschlag ca. 180 Grad)
         */
        public final static int SERVO_MIN_LIMIT = 400;
        
        /**
         * SERVO_MAX_VALUE = 2000;
         * 
         * Max. Begrenzung (oberer Wert) fuer den Servomotor (Ausschlag ca. 180 Grad)
         */
        public final static int SERVO_MAX_LIMIT = 2000;
        
        /**
         * SERVO_MIN_STEERING - Max. Servo-Begrenzung infolge der Lenkung (unterer Wert).
         * <p>
         * Die max. untere Begrenzung ergibt sich aus dem Mittelwert aus SERVO_MIN_LIMIT 
         * und SERVO_MAX_LIMIT verringert um die Haelfte von STEERING_DELTA
         * </p>
         */
        public final static int SERVO_MIN_STEERING = (SERVO_MIN_LIMIT+SERVO_MAX_LIMIT-STEERING_DELTA)/2;
        
        /**
         * SERVO_MAX_STEERING - Max. Servo-Begrenzung infolge der Lenkung (oberer Wert).
         * <p>
         * Die max. obere Begrenzung ergibt sich aus dem Mittelwert aus SERVO_MIN_LIMIT 
         * und SERVO_MAX_LIMIT vergroessert um die Haelfte von STEERING_DELTA
         * </p>
         */
        public final static int SERVO_MAX_STEERING = (SERVO_MIN_LIMIT+SERVO_MAX_LIMIT+STEERING_DELTA)/2;
        
        /**
         * Servo(Channel channel)
         * @param channel
         */
        public Servo(Channel channel)
        {
            this.channel = channel;
        }

        /**
         * setPWM(int offValue) - Einstellung um den Nullpunkt als
         * arithm. Mittelwert (die Mitte) aus SERVO_MAX_VALUE - SERVO_MIN_VALUE.
         * @param relValue
         * @throws IOException
         */
        public void setPWM(int relValue) throws IOException
        {
            // offValue ergibt sich aus dem Mittelwert +- Ausschlag...
            final int offValue = (SERVO_MIN_LIMIT + SERVO_MAX_LIMIT)/2 + STEERING_ADJUSTMENT + relValue;
            // In channel.setPWM(Servo.SERVO_ON_VALUE, offValue) erfolgt 
            // die Begrenzung von offValue...
            channel.setPWM(Servo.SERVO_ON_VALUE, offValue);
        }
        
        /**
         * @param onValue
         * @param offValue
         * @throws IOException
         * @see raspi.hardware.i2c.PCA9685.Channel#setPWM(int, int)
         */
        public void setPWM(int onValue, int offValue) throws IOException
        {
            final int paramOnValue = (onValue < 0)? 0
                                   : ((onValue > SERVO_MIN_STEERING-1)? (SERVO_MIN_STEERING-1) : onValue);
           
            final int paramOffValue = (offValue < SERVO_MIN_STEERING)? SERVO_MIN_STEERING 
                                   : ((offValue > SERVO_MAX_STEERING)? SERVO_MAX_STEERING : offValue);
            
            channel.setPWM(paramOnValue, paramOffValue);
        }

        /**
         * getServoOnValue()
         * @return
         */
        public final int getServoOnValue()
        {
            return Servo.SERVO_ON_VALUE;
        }
        
        /**
         * getServoMinSteering() - liefert den min. zul. Wert fuer einen Lenkausschlag.
         * Dieser Wert ist bedingt durch die Geometrie der Lenkung.
         * <p>
         * Fuer die Lenkung ist <b>dieser</b> Grenzwert relevant!
         * </p>
         * @return Servo.SERVO_MIN_STEERING
         */
        public final int getServoMinSteering()
        {
            return Servo.SERVO_MIN_STEERING;
        }
        
        /**
         * getServoMaxSteering() - liefert den max. zul. Wert fuer einen Lenkausschlag.
         * Dieser Wert ist bedingt durch die Geometrie der Lenkung.
         * <p>
         * Fuer die Lenkung ist <b>dieser</b> Grenzwert relevant!
         * </p>
         * @return Servo.SERVO_MAX_STEERING
         */
        public final int getServoMaxSteering()
        {
            return Servo.SERVO_MAX_STEERING;
        }
        
        /**
         * getServoMinLmit() - liefert den min. zul. Wert fuer
         * die untere Begrenzung. Bei diesem Wert betraegt der Ausschlag
         * des Servos ca. -90 Grad.
         * <p>
         * Fuer die Lenkung ist dieser Wert nicht zu verwenden!
         * </p>
         * @return Servo.SERVO_MIN_LIMIT (400)
         */
        public final int getServoMinLmit()
        {
            return Servo.SERVO_MIN_LIMIT;
        }
        
        /**
         * getServoMaxLimit() - liefert den max. zul. Wert fuer
         * die obere Begrenzung. Bei diesem Wert betraegt der Ausschlag
         * des Servos ca. +90 Grad.
         * <p>
         * Fuer die Lenkung ist dieser Wert nicht zu verwenden!
         * </p>
         * @return Servo.SERVO_MAX_LIMIT (2000)
         */
        public final int getServoMaxLimit()
        {
            return Servo.SERVO_MAX_LIMIT;
        }
    }
    
    /**
     * Die Klasse Motor beschreibt einen Motor-Channel.
     * 
     * @author Detlef Tribius
     */
    public class Motor
    {
        /**
         * channel - PCA9685-Channel zur Bereitstellung der PWM...
         */
        private final Channel channel;
        
        /**
         * MOTOR_ON_VALUE = 0
         */
        public final static int MOTOR_ON_VALUE = 0;
        
        /**
         * MOTOR_MAX_VALUE = PCA9685.RESOLUTION-1 
         * <p>
         * MOTOR_MAX_VALUE - max. zul. Wert fuer den offValue der PWM.
         * Beim PCA9685 betraegt die Aufloesung 4096, daher betraegt 
         * der MOTOR_MAX_VALUE 4095.
         * </p>
         */
        public final static int MOTOR_MAX_VALUE = PCA9685.RESOLUTION-1;
        
        /**
         * Konstruktor mit Mitgabe des Channels
         * 
         * @param channel
         */
        public Motor(Channel channel)
        {
            this.channel = channel;
        }

        /**
         * 
         * @param pwmValue
         * @throws IOException
         */
        public void setPWM(int pwmValue) throws IOException
        {
            setPWM(0, pwmValue);
        }
        
        /**
         * @param onValue
         * @param offValue
         * @throws IOException
         * @see raspi.hardware.i2c.PCA9685.Channel#setPWM(int, int)
         */
        public void setPWM(int onValue, int offValue) throws IOException
        {
            final int paramOnValue = (onValue < 0)? 0
                    : ((onValue > Motor.MOTOR_MAX_VALUE)? Motor.MOTOR_MAX_VALUE : onValue);

            final int paramOffValue = (offValue < Motor.MOTOR_ON_VALUE)? Motor.MOTOR_ON_VALUE 
                    : ((offValue > Motor.MOTOR_MAX_VALUE)? Motor.MOTOR_MAX_VALUE : offValue);
         
            channel.setPWM(paramOnValue, paramOffValue);
        }
        
        /**
         * getMotorMaxValue()
         * @return Motor.MOTOR_MAX_VALUE (=> 4095)
         */
        public final int getMotorMaxValue()
        {
            return Motor.MOTOR_MAX_VALUE;
        }
    }
}
