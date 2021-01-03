/**
 * 
 */
package raspi.hardware.i2c;

import java.io.IOException;

import com.pi4j.io.i2c.I2CDevice;

import raspi.hardware.i2c.PCA9685.Property;

/**
 * 
 * <p>
 * Vorgehen:
 * (vgl. auch PCA9685_datasheet.pdf)
 * </p>
 * <ul>
 * <li>
 * => Init PCA9685 <br>
 *     - writeByte(MODE1, 0x00);
 * </li>
 * <li>
 * => setPwmFreq() PCA9685 <br>
 *     - The PRE_SCALE register can only be set when the SLEEP bit of MODE1 register is set to logic 1.
 * </li>    
 * </ul>
 * 
 * @author Detlef Tribius
 *
 */
public class MotorDriverHAT
{
    /**
     * pca9685
     */
    private final MotorDriverHAT.PCA9685 pca9685;
    
    /**
     * 
     * @param dev I2CDevice
     * @throws IOException 
     */
    public MotorDriverHAT(I2CDevice dev) throws IOException
    {
        this.pca9685 = new PCA9685(dev);
    }
    
    /**
     * MotorDriverHAT(I2CDevice dev, int frequency) - Intanziierung und Initialisierung
     * <p>
     * <code>
     * this(dev);<br>
     * getPca9685().initialize();<br>
     * getPca9685().setPwmFrequency(frequency);<br>
     * </code>
     * </p>
     * @param dev
     * @param frequency
     * @throws IOException
     */
    public MotorDriverHAT(I2CDevice dev, int frequency) throws IOException
    {
        this(dev);
        initialize(frequency);
    }
    
    /**
     * initialize(int frequency) - Initialisierung des PCA9685
     * 
     * @param frequency
     * @throws IOException
     */
    public void initialize(int frequency) throws IOException
    {
        // Initialisierung des PCA9685, vgl. auch Muster in
        // Motor_Driver_HAT_Code.7z (PCA9685_Init(char addr))...
        getPca9685().initialize();
        // Weitere Initialisierung mit dem Setzen der Fequenz.
        // Vgl. Muster PCA9685_SetPWMFreq(UWORD freq)...
        getPca9685().setPwmFrequency(frequency);
    }
    
    /**
     * setPwmMA(float speed) - Motorsteuerung MOTOR_A
     * @param speed Vorgabe Sollwert (-1.0f <= speed <= 1.0f)
     * @throws IOException
     */
    public void setPwmMA(float speed) throws IOException
    {
        if (((this.pca9685 == null) || (this.pca9685.pwm_A == null)))
        {
            throw new RuntimeException("Initialization failed!");
        }
        this.pca9685.pwm_A.setPwm(speed);        
    }
    
    /**
     * setPwmMB(float speed) - Motorsteuerung MOTOR_B
     * @param speed Vorgabe Sollwert (-1.0f <= speed <= 1.0f)
     * @throws IOException
     */
    public void setPwmMB(float speed) throws IOException
    {
        if (((this.pca9685 == null) || (this.pca9685.pwm_B == null)))
        {
            throw new RuntimeException("Initialization failed!");
        }
        this.pca9685.pwm_B.setPwm(speed);
    }

    /**
     * setPwm(Motor motor, float speed) - Vorgabe Sollwert
     * @param motor MOTOR_A oder MOTOR_B
     * @param speed Vorgabe Sollwert (-1.0f <= speed <= 1.0f)
     * @throws IOException 
     */
    public void setPwm(Motor motor, float speed) throws IOException
    {
        if (((getPca9685() == null) || (getPca9685().getPwm(motor) == null)))
        {
            throw new RuntimeException("Initialization failed!");
        }
        getPca9685().getPwm(motor).setPwm(speed);
    }
    
    /**
     * getPca9685()
     * @return Referenz auf this.pca9685
     */
    public MotorDriverHAT.PCA9685 getPca9685()
    {
        return this.pca9685;
    }
    
    /**
     * Klasse PCA9685
     * 
     * @author Detlef Tribius
     *
     */
    class PCA9685 extends I2C
    {
        
        // Register...
        /** MODE1_REGISTER = 0x00 Mode Register 1 */
        public final static int MODE1_REGISTER = 0x00;
        /** MODE2_REGISTER = 0x00 Mode Register 2 */
        public final static int MODE2_REGISTER = 0x01;
        // Adressen...
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

        // Die Steuerung erfolgt ueber die folgenden Kanaele:
        // PWM steuert jeweils die Ausgangsspannung,
        // IN1 und IN2 jeweils die Drehrichtung nach folgendem 
        // Muster:
        // - FORWARD:
        //       A:    PCA9685_SetLevel(AIN1, 0);
        //             PCA9685_SetLevel(AIN2, 1);
        //     oder
        //       B:    PCA9685_SetLevel(BIN1, 0);
        //             PCA9685_SetLevel(BIN2, 1);
        // - BACKWARD:
        //       A:    PCA9685_SetLevel(AIN1, 1);
        //             PCA9685_SetLevel(AIN2, 0);
        //     oder
        //       B:    PCA9685_SetLevel(BIN1, 1);
        //             PCA9685_SetLevel(BIN2, 0);
        //
        // mit Parameter = 1 => PCA9685_SetPWM(channel, 0, 4095);
        // mit Parameter = 0 => PCA9685_SetPWM(channel, 0, 0);
        // (vgl. MotorDriver.c/PCA9685.c aus Motor_Driver_HAT_Code.7z)
        //
        // PWMA        PCA_CHANNEL_0 (LED0)
        // AIN1        PCA_CHANNEL_1 (LED1)
        // AIN2        PCA_CHANNEL_2 (LED2)
        // PWMB        PCA_CHANNEL_5 (LED5)
        // BIN1        PCA_CHANNEL_3 (LED3)
        // BIN2        PCA_CHANNEL_4 (LED4)
        /**
         * PWMA_CHANNEL = 0
         */
        public final static int PWMA_CHANNEL = 0;
        /**
         * IN1A_CHANNEL = 1
         */
        public final static int IN1A_CHANNEL = 1;
        /**
         * IN2A_CHANNEL = 2
         */
        public final static int IN2A_CHANNEL = 2;
        /**
         * PMWB_CHANNEL = 5
         */
        public final static int PMWB_CHANNEL = 5;
        /**
         * IN1B_CHANNEL = 3
         */
        public final static int IN1B_CHANNEL = 3;
        /**
         * IN2B_CHANNEL = 4
         */
        public final static int IN2B_CHANNEL = 4;

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
         * <p>
         * Diese Interne Freuenz wird durch einen Vorteiler reduziert und 
         * ergibt dann die Pulsfrequenz des PCA9685...
         * </p>
         */
        public final static long INTERNAL_FREQUENCY = 25000000;
        
        /**
         * PWM_MAX = 4095 => (Aufloesung-1), bedeutet Stufigkeit der PWM-Channel...
         * <p>
         * PWM_MAX = 4095 bedeutet 12 Bit Aufloesung von 0 ... 4095, 
         * dann gibt es also PWM_MAX+1 Stufen!
         * </p>
         */
        public final static int PWM_MAX = 4095;
        
        /**
         * pwm_A - Channel zur Steuerung des Motor A
         */
        private final PwmChannel pwm_A;
        
        /**
         * pwm_B - Channel zur Steuerung des Motor B
         */
        private final PwmChannel pwm_B;
        
        /**
         * DELAY = 5 Pausenzeit (5 ms) fuer zeitl. Pausen 
         * waehrend der Konfiguration...
         */
        public final static int DELAY = 5;        
        
        /**
         * Konstruktor zum Baustein PCA9685...
         * @param dev - Referenz auf I2CDevice (zuvor angelegt)
         */
        private PCA9685(I2CDevice dev)
        {
            super(dev);
            // Zum Motor A geheoeren ein PWM-Channel und zwei In-Channel...
            // (Anm.: gesteuert wird ueber den PWM-Channel) 
            this.pwm_A = new PwmChannel(PWMA_CHANNEL, IN1A_CHANNEL, IN2A_CHANNEL);
            
            // Zum Motor B geheoeren ein PWM-Channel und zwei In-Channel...
            // (Anm.: gesteuert wird ueber den PWM-Channel) 
            this.pwm_B = new PwmChannel(PMWB_CHANNEL, IN1B_CHANNEL, IN1B_CHANNEL);
        }
        
        /**
         * getPwmMA()
         * @return PwmChannel zu MOTOR_A
         */
        private final PwmChannel getPwmMA()
        {
            return this.pwm_A;
        }
        
        /**
         * getPwmMB()
         * @return PwmChannel zu MOTOR_B
         */
        private final PwmChannel getPwmMB()
        {
            return this.pwm_A;
        }
        /**
         * 
         * @param motor
         * @return
         */
        private final PwmChannel getPwm(Motor motor)
        {
            if (motor == Motor.MOTOR_A)
            {
                return this.pwm_A;
            }
            if (motor == Motor.MOTOR_B)
            {
                return this.pwm_B;
            }
            return null;
        }
        
        /**
         * initialize() - 
         * @throws IOException
         */
        private void initialize() throws IOException
        {
            // 1.)
            write(MODE1_REGISTER, (byte)0x00);
            // 2.)
            write(ALL_LED_ON_L_REGISTER, (byte)0x00);
            write(ALL_LED_ON_H_REGISTER, (byte)0x00);
            write(ALL_LED_OFF_L_REGISTER, (byte)0x00);
            write(ALL_LED_OFF_H_REGISTER, (byte)0x00);
        }

        /**
         * setPwmFrequency(int frequency) - Puls-Frequenz setzen...
         * @param frequency Frequenz in Hz (z.B. 50, 100...)
         * @throws IOException 
         */
        private void setPwmFrequency(int frequency) throws IOException
        {
            // Vorteiler prescale bestimmen...
            final byte prescale = getPrescaleValue(frequency);
            
            // oldMode auslesen...
            final byte oldMode = (byte)(read(MODE1_REGISTER) & 0xff);
            // setMode: Zum Seten das Sleep-Bit setzen...
            final byte setMode = (byte)(oldMode | SLEEP); 
            // newMode: Aus oldMode mit Restart enabled...
            final byte newMode = (byte)(oldMode | RESTART);
            // Konfigurieren...
            write(MODE1_REGISTER, setMode);
            sleep(DELAY);
            write(PRE_SCALE_REGISTER, prescale);
            sleep(DELAY);
            write(MODE1_REGISTER, newMode);
            sleep(DELAY);
        }
        
        /**
         * getPrescaleValue(int frequency) - liefert den Wert
         * fuer den Vorteiler zum Einstellen einer bestimmten Frequenz.
         * <p>
         * Achtung: Es wird z.T. mit einem Koorekturwert gearbeitet 
         * (wird hier vernachlaessigt!).
         * <br>
         * <code>
         * freq *= 0.9;  // Correct for overshoot in the frequency setting
         * </code>
         * </p>
         * <p>
         * Anm.:
         * The maximum PWM frequency is 1526 Hz if the PRE_SCALE register is set "0x03h".
         * The minimum PWM frequency is 24 Hz if the PRE_SCALE register is set "0xFFh".
         * </p>
         * <p>
         * C-Muster:
         * <code>
         *     double prescaleval = 25000000.0;<br>
         *     prescaleval /= 4096.0;<br>
         *     prescaleval /= freq;<br>
         *     prescaleval -= 1;<br>
         *     UBYTE prescale = floor(prescaleval + 0.5);<br>
         * </code>
         * </p>
         * @param int frequency (...50 Hz...60 Hz...)
         * @return prescaleValue (byte)
         */
        private byte getPrescaleValue(int frequency)
        {
            // Am Ende -1+0.5 => -0.5 (Anm.: +0.5 zum Runden...) 
            final double value =  Math.floor(((double)INTERNAL_FREQUENCY)/(double)(4096*frequency)-0.5);
            
            final int prescaleValue = (int)value; 
            if (prescaleValue < 0x03)
            {
                // Begrenzung nach unten...
                return (byte)0x03;
            }
            if (prescaleValue > 0xff)
            {
                // Begrenzung nach oben...
                return (byte)0xff;
            }
            return (byte)prescaleValue;
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
         * Die abstrakte Klasse Channel beschreibt einen (der 16)
         * Channel des PWM-Bausteins PCA9685.
         * Von den Channel werden aber nur pro Motor 3 genutzt, also
         * insgesamt 6 Channel.
         * <p>
         * Dabei gehoert zu einem Motor ein PWM-Channel und zwei weitere zur 
         * Drehrichtungssteuerung.
         * </p> 
         * <p>
         * Diese unterschiedlichen Channel werden durch konkrete Klassen
         * beschrieben (PWMChanel, InChannel)
         * </p>
         * <ul>
         *  <li>LED0-PWMA</li>
         *  <li>LED1-AIN2</li>
         *  <li>LED2-AIN1</li>
         *  <li>LED3-BIN1</li>
         *  <li>LED4-BIN2</li>
         *  <li>LED5-PWMB</li>
         * </ul>
         * @author Detlef Tribius
         *
         */
        private abstract class Channel
        {
            /**
             * channel - Nummer des Channel, hier von 0 bis 5.
             */
            private final int channel;
            
            /**
             * Channel(int channel) - Konstruktor eines Channels
             * @param channel - Nummer des Channels
             */
            public Channel(int channel)
            {
                this.channel = channel;
            }

            /**
             * setPwm(int onValue, int offValue) 
             * @param onValue
             * @param offValue
             * @throws IOException
             */
            public void setPwm(int onValue, int offValue) throws IOException
            {
                write(getOnLowByteReg(), (byte)(onValue & 0xff));
                write(getOnHighByteReg(), (byte)((onValue >> 8) & 0xff));
                write(getOffLowByteReg(), (byte)(offValue & 0xff));
                write(getOffHighByteReg(), (byte)((offValue >> 8) & 0xff));
            }
            
            /**
             * @return int getOnLowByteReg()
             */
            private final int getOnLowByteReg()
            {
                return (this.channel<<2)+6;
            }
            /**
             * @return int getOnHighByteReg()
             */
            private final int getOnHighByteReg()
            {
                return (this.channel<<2)+7;
            }
            /**
             * @return int getOffLowByteReg()
             */
            private final int getOffLowByteReg()
            {
                return (this.channel<<2)+8;
            }
            /**
             * @return int getOffHighByteReg()
             */
            private final int getOffHighByteReg()
            {
                return (this.channel<<2)+9;
            }
        }
        
        /**
         * Die Klasse PwmChannel steuert einen Motor
         * und beinhaltet jeweils zwei InChannel zur Steuerung der 
         * Drehrichtung.
         * @author Detlef Tribius
         */
        private class PwmChannel extends Channel
        {
            
            /**
             * in1 - InChannel 1 (zusammen mit in2) zur
             * Steuerung der Drehrichtung des Motors
             */
            private final InChannel in1;
            
            /**
             * in2 - InChannel 2 (zusammen mit in1) zur
             * Steuerung der Drehrichtung des Motors
             */
            private final InChannel in2;
            
            /**
             * PwmChannel(int channel, int in1_channel, int in2_channel) - Konstruktor
             * @param channel - PWM-Channel
             * @param in1_channel - Steuerungschannel 1 zur Drehrichtung
             * @param in2_channel - Steuerungschannel 2 zur Drehrichtung
             */
            public PwmChannel(int channel, int in1_channel, int in2_channel)
            {
                super(channel);
                this.in1 = new InChannel(in1_channel);
                this.in2 = new InChannel(in1_channel);
            }
            
            /**
             * 
             * @param speed
             * @throws IOException
             */
            public void setPwm(float speed) throws IOException
            {
                // speed - Stellgroesse fuer die Ausgangsspannung
                // speed umfasst den Bereich von -1.0F bis +1.0F
                label:
                {
                    final int onValue = 0;
                    // PWM_MAX: Aufloesung der PWM, hier 4095 (12 Bit)
                    final float value = Math.abs(speed)*PWM_MAX;
                    final int offValue = (int)value; 
                    // onValue = 0; offValue = Math.abs(speed)*PWM_MAX (bis max. 4095)
                    //
                    // z.B. Motor A: 
                    // FORWARD: PCA9685_SetLevel(AIN1, 0);
                    //          PCA9685_SetLevel(AIN2, 1);
                    // BACKWARD:PCA9685_SetLevel(AIN1, 1);
                    //          PCA9685_SetLevel(AIN2, 0);
                    //
                    // FORWARD:  (speed > 0.0)
                    // BACKWARD: (speed < 0.0)
                    //
                    // 1.) offValue == 0 aussortieren...
                    if (offValue == 0)
                    {
                        // Sollwert ist offfensichtlich 0!
                        setPwm(0, 0);
                        getIn1Channel().setLevel(false);
                        getIn2Channel().setLevel(false);
                        break label;
                    }
                    // 2.) offValue != 0, jetzt FORWARD und BACKWARD gemeinsam behandeln.
                    // Dabei immmer Begrenzung von offValue auf max. PWM_MAX (4095)...
                    setPwm(onValue, (offValue > PWM_MAX)? PWM_MAX : offValue);
                    // FORWARD bei (speed > 0.0)
                    // BACKWARD bei (speed < 0.0)
                    // Also bei (speed > 0.0) => In1.setLevel(false); In2.setLevel(true);
                    //      bei (speed < 0.0) => In1.setLevel(true); In2.setLevel(false);
                    getIn1Channel().setLevel(speed < 0.0);
                    getIn2Channel().setLevel(speed > 0.0);
                }    
            }
            
            /**
             * getIn1Channel()
             * @return
             */
            private final InChannel getIn1Channel()
            {
                return this.in1;
            }
            /**
             * getIn2Channel()
             * @return
             */
            private final InChannel getIn2Channel()
            {
                return this.in2;
            }
        }
        
        /**
         * Klasse InChannel beschreibt einen "Steuer"-Channel.
         * Dieser spezielle Channel kennt nur die Ausgaben "0" 
         * oder "1". Diese Ausgaben werden realisiert, indem
         * setPwm() mit 0 oder PWM_MAX beauftragt wird.
         * <p>
         * 
         * </p>
         * @author Detlef Tribius
         *
         */
        private class InChannel extends Channel
        {
            /**
             * 
             * @param channel
             */
            public InChannel(int channel)
            {
                super(channel);
            }
            
            /**
             * setLevel(boolean isHighLevel)
             * @param isHighLevel boolean false => low level; true => high level
             * @throws IOException 
             */
            public void setLevel(boolean isHighLevel) throws IOException
            {
                setPwm(0, isHighLevel? PWM_MAX : 0);
            }
        }
    }
    
    /**
     * enum Motor
     * 
     * @author Detlef Tribius
     */
    static enum Motor
    {
        /**
         * MOTOR_A("Motor A")
         */
        MOTOR_A("Motor A"),
        /**
         * MOTOR_B("Motor B")
         */
        MOTOR_B("Motor B");
        /**
         * motor - textuelle Beschreibung
         */
        private final String motor;
        /**
         * Motor(String motor) - Konstruktor
         * @param motor
         */
        private Motor(String motor)
        {
            this.motor = motor;
        }
        /**
         * getName() 
         * @return
         */
        public String getName()
        {
            return this.motor;
        }
    }
}
