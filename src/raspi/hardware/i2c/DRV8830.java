/**
 * 
 */
package raspi.hardware.i2c;

import java.io.IOException;

import com.pi4j.io.i2c.I2CDevice;


/**
 * Als Muster diente u.a. auch https://ladvien.com/porting-drv8830-nodejs/...
 * 
 * Zur Adressierung der Hardware:
 * 
 * I2C_ADDR1 = 0x60  # Default, both select jumpers bridged (not cut)
 * I2C_ADDR2 = 0x61  # Cut A0
 * I2C_ADDR3 = 0x63  # Cut A1
 * I2C_ADDR4 = 0x64  # Cut A0 and A1
 * (vgl. https://github.com/pimoroni/drv8830-python/blob/master/library/drv8830/__init__.py)
 * 
 * auch:
 * https://github.com/sparkfun/MiniMoto/blob/V_H1.0_L1.1.0/Libraries/Arduino/src/SparkFunMiniMoto.cpp
 * 
 * auch
 * https://github.com/evdrvn/drv8830-i2c
 * 
 * @author Detlef Tribius
 *
 */
public class DRV8830  extends I2C
{
    /**
     * REGISTER 0 – CONTROL
     * <p>
     * The CONTROL register is used to set the state of the outputs as well 
     * as the DAC setting for the output voltage.
     * </p>
     * <p>
     * The register is defined as follows:
     * <ul>
     * <li>
     * D0: IN1
     * </li>
     * <li>
     * D1: IN2
     * </li>
     * <li>
     * D7-D2: VSET[5..0]
     * </li>
     * </ul>
     * </p>
     */
    public static final int CONTROL_REGISTER = 0;
    /**
     * REGISTER 1 – FAULT
     * <p>
     * The FAULT register is used to read the source of a fault condition, 
     * and to clear the status bits that indicated the fault. 
     * </p>
     * <p>
     * <ul>
     * <li>FAULT_FREE(0x00)</li>
     * <li>FAULT(0x01)</li>
     * <li>OCP(0x02)</li>
     * <li>UVLO(0x04)</li>
     * <li>OTS(0x08)</li>
     * <li>ILIMIT(0x10)</li>
     * </ul>
     * </p>
     */
    public static final int FAULT_REGISTER = 1;
    
    /**
     * FAULT_CLEAR - 0x80 - Bitmuster zum Reset der Fehlerkennung
     * <p>
     * Bit 7 (beginnend bei 0): setzt im Register gespeicherte Fehler
     * auf 0 zurueck...
     * </p>
     */
    public static final byte FAULT_CLEAR = Integer.valueOf(0x80).byteValue();
    
    /**
     * Konstruktor, vgl. Basisklasse I2C
     * @param dev I2CDevice dev
     */
    public DRV8830 (final I2CDevice dev)
    {
        super(dev);
    }
  
    /**
     * Send the drive command over I2C to the DRV8830 chip. Bits 7:2 are the speed
     * setting; range is 0-63. Bits 1:0 are the mode setting:
     * - 00 = HI-Z => Freilauf
     * - 01 = Reverse => Richtung 1
     * - 10 = Forward => Richtung 2
     * - 11 = H-H (brake) => Bremsen
     * @throws IOException 
     */ 
    public void drive(int speed) throws IOException
    {
        // Die Ausgabe ueber den DRV8830 erfolgt nach Zusammenfuehren
        // der Drehrichtungsinformation (2 Bit) mit dem Betragswert
        // des Sollwertes (6 Bit, Betrag von 0 bis 63).
        // Ausgabe-Value immer positiv und im Bereich von 
        // 0 ... MIN_VALUE ... MAX_VALUE..., dann 2 Stellen nach links 
        // und mit der Richtungskennung verknuepfen...
        // 1.) direction je nach Drehrichtung (Vorzeichen) bestimmen...
        //     (direction kennt drei Zustaende)
        final int direction = Direction.getDirection(speed).getDirection();
        final int voltageSetting = Direction.getVoltageSetting(speed);
        // 2.) Betrag vom Sollwert nach links verschoben (2 Stellen)
        //     und direction ergaenzen...
        final byte desiredValue = (byte)((voltageSetting << 2) + direction);
        
        write(DRV8830.CONTROL_REGISTER, desiredValue);
    }
    
    /**
     * standBy() - Stand by => Direction.FREEWHEEL!
     * <p>
     * When both bits are zero, the output drivers are disabled and the device 
     * is placed into a low-power shutdown state. 
     * The current limit fault condition (if present) is also cleared.
     * </p>
     * @throws IOException
     */
    public void standBy() throws IOException
    {
        final int voltageSetting = 0;
        final byte desiredValue = (byte)((voltageSetting << 2) + Direction.FREEWHEEL.getDirection());
        
        write(DRV8830.CONTROL_REGISTER, desiredValue);
    }
    
    /**
     * 
     * @throws IOException
     */
    public void brake() throws IOException
    {
        final int voltageSetting = 0;
        final byte desiredValue = (byte)((voltageSetting << 2) + Direction.BRAKE.getDirection());
        
        write(DRV8830.CONTROL_REGISTER, desiredValue);
        
    }
    
    /**
     * getFault() - Return the fault status of the DRV8830 chip. 
     * Also clears any existing faults.
     * vgl. https://github.com/sparkfun/MiniMoto/blob/V_H1.0_L1.1.0/Libraries/Arduino/src/SparkFunMiniMoto.cpp
     * @return
     * @throws IOException 
     */
    public int getFault() throws IOException
    {
        // int fault - Fehlerkennung als int-Wert mit der Bedeutung:
        // Bit 0: ...wird gesetzt, wenn eines der andren Fehlerbits gesetzt wurde (?),
        // Bit 1: Der Maximalstrom von 1 A ist uebertreten worden,
        // Bit 2: Bit zeigt an, dass Unterspannung aufgetreten ist,
        // Bit 3: Ist die Temperatur des DRV8830 zu hoch, wird das Bit gesetzt,
        // Bit 4: Signalisiert, dass ueber den INSENSE-Eingang ein zu hoher Strom geflossen ist,
        // Bit 5+6: Keine Funktion,
        // Bit 7: Setzt im Register gespeicherte Fehler zurueck (bei write()...).
        final int fault = read(DRV8830.FAULT_REGISTER); 
        
        if (fault != 0)
        {
            // Wenn Fehler, dann auch Zuruecksetzen der Fehlerkennung...
            write(DRV8830.FAULT_REGISTER, DRV8830.FAULT_CLEAR);
        }
            
        return fault;
    }
    
    /**
     * resetFault() - setzt Fehlerkennung zurueck...
     * @throws IOException
     */
    public void resetFault() throws IOException
    {
        write(DRV8830.FAULT_REGISTER, DRV8830.FAULT_CLEAR);  
    }
    
    /**
     * Direction beschreibt die Direction bits.
     * Die Bits steuern die H-Bruecke...
     * @author Detlef Tribius
     */
    public static enum Direction
    {
        /**
         * Frei Drehen: Bit 0 => 0; Bit 1 => 0
         */
        FREEWHEEL(0b00000000),
        /**
         * Richtung 1: Bit 0 => 0; Bit 1 => 1
         */
        FORWARD(0b00000010),
        /**
         * Richtung 2: Bit 0 => 1; Bit 1 => 0
         */
        REVERSE(0b00000001),
        /**
         * Bremsen: Bit 0 = 1; Bit 1 => 1
         */
        BRAKE(0b00000011);

        /**
         * MAX_VALUE = 63, max. Spannungswert des DRV8830
         */
        public static final int MAX_VALUE = 63;
        
        /**
         * MIN_VALUE = 6, der erste von 0 verschiedene Spannungswert des DRV8830
         */
        public final static int MIN_VALUE = 6;
        
        /**
         * int direction - Ablage der Richtungsinformation
         * im int-Datum direction... 
         */
        private final int direction;
        
        /**
         * enum-Konstruktor
         * @param direction
         */
        private Direction(int direction)
        {
            this.direction = direction;
        }
        
        /**
         * getDirection()
         * @return
         */
        public final int getDirection()
        {
            return this.direction;
        }

        /**
         * getDirection(int speed) - liefert
         * enum Direction in Abh. vom Vorzeichen des int-Parameters
         * <p>
         * <ul>
         * <li>speed <= -Direction.MIN_VALUE: Direction.REVERSE </li>
         * <li>speed >= Direction.MIN_VALUE: Direction.FORWARD </li>
         * <li>Math.abs(speed) < Direction.MIN_VALUE: Direction.FREEWHEEL</li>
         * </ul>
         * </p>
         * @param speed - int-Parameter
         * @return Direction Kennung fuer die Drehrichtung
         */
        public final static Direction getDirection(int speed)
        {
            if (Math.abs(speed) < Direction.MIN_VALUE)
            {
                return Direction.FREEWHEEL;
            }
            return (speed < 0)? Direction.REVERSE : Direction.FORWARD; 
        }
        
        /**
         * getVoltageSetting() - liefert den Betrag des Sollwertes
         * unter der Massgabe, dass der Schaltkreis eine Totzone von
         * - MIN_VALUE ... + MIN_VALUE hat. In diesem Bereich der Totzone
         * wird als Sollwert 0 geliefert. Fuer diesen Bereich muss getDirection()
         * auch den Wert Direction.FREEWHEEL liefern.
         * <p> 
         * Dazu <br>
         * <code>if (Math.abs(speed) < Direction.MIN_VALUE){...}</code>
         * </p>
         * @param speed int-Sollwert. -Max ... +Max
         * @return int Sollwert fuer den Schaltkreis DRV8830: -MAX_VALUE ... +MAX_VALUE
         */
        public final static int getVoltageSetting(int speed)
        {
            if (Math.abs(speed) > Direction.MAX_VALUE)
            {
                return MAX_VALUE;
            }
            if (Math.abs(speed) < Direction.MIN_VALUE)
            {
                return 0;
            }
            return Math.abs(speed);
        }
    }
    
    /**
     * Enum Fault - beschreibt moegliche Fehlerzustaende...
     * 
     * 
     * @author Detlef Tribius
     *
     */
    public static enum Fault
    {
        /**
         * FAULT_FREE - Fehlerfrei...
         */
        FAULT_FREE(0x00)
        {
            /**
             * Fehlerfreiheit => isSuccess = true!
             */
            public boolean isSuccess()
            {
                return true;
            }

            @Override
            public String getReason()
            {
                return "Success, no fault!";
            }
        },
        /**
         * FAULT: Set if any fault condition exists,
         * ...wird gesetzt, wenn eines der anderen Fehlerbits gesetzt wurde...
         */
        FAULT(0x01) 
        {
            @Override
            public boolean isSuccess()
            {
                return false;
            }

            @Override
            public String getReason()
            {
                return "Any fault condition exists!";
            }
        },
        /**
         * OCP: If set, indicates the fault was caused by an overcurrent (OCP) event
         * ...wird gesetzt, wenn der Maximalstrom von 1 A ueberschritten wurde...
         */
        OCP(0x02) 
        {
            @Override
            public boolean isSuccess()
            {
                return false;
            }

            @Override
            public String getReason()
            {
                return "The fault was caused by an overcurrent (OCP) event!";
            }
        },
        /**
         * UVLO: If set, indicates the fault was caused by an undervoltage lockout
         * ...wird gesetzt, wenn Unterspannung auftritt...
         */
        UVLO(0x04) 
        {
            @Override
            public boolean isSuccess()
            {
                return false;
            }

            @Override
            public String getReason()
            {
                return "The fault was caused by an undervoltage lockout!";
            }
        },
        /**
         * OTS: If set, indicates that the fault was caused by an overtemperature (OTS) condition
         * ...wird gesetzt, wenn die Temperatur des DRV8833 zu hoch ist...
         */
        OTS(0x08) 
        {
            @Override
            public boolean isSuccess()
            {
                return false;
            }

            @Override
            public String getReason()
            {
                return "The fault was caused by an overtemperature (OTS) condition!";
            }
        },
        /**
         * ILIMIT: If set, indicates the fault was caused by an extended current limit event,
         * ...signalisiert, dass ueber den ISENSE-Eingang ein zu hoher Strom geflossen ist...
         */
        ILIMIT(0x10) 
        {
            @Override
            public boolean isSuccess()
            {
                return false;
            }

            @Override
            public String getReason()
            {
                return "The fault was caused by an extended current limit event!";
            }
        };
        /**
         * int fault - Datenhaltung...
         */
        private final int fault;
        /**
         * Fault(int fault) - notwendiger Konstruktor...
         * @param fault
         */
        private Fault(int fault)
        {
            this.fault = fault;
        }
        
        /**
         * byteValue() - liefert Datentype byte...
         * @return
         */
        public final byte byteValue()
        {
            return Integer.valueOf(this.fault).byteValue();
        }
        
        /**
         * isSuccess() - liefert boolsche Kennung Erfolg/Fehler 
         * (es gibt Kennung FAULT_FREE fuer fehlerfrei!)
         * @return boolsche Kennung Fehler ja/nein
         */
        public abstract boolean isSuccess();
        
        /**
         * getReason() - liefert textuelle Beschreibung des Fehlers...
         * @return
         */
        public abstract String getReason();
        
        /**
         * 
         * @param fault
         * @return
         */
        public final static Fault getFault(int fault)
        {
            final Fault reasons[] = { DRV8830.Fault.OCP,         // => 0x02
                                      DRV8830.Fault.UVLO,        // => 0x04
                                      DRV8830.Fault.OTS,         // => 0x08
                                      DRV8830.Fault.ILIMIT,      // => 0x10
                                      DRV8830.Fault.FAULT };     // => 0x01
            
            if (fault == 0)
            {
                return DRV8830.Fault.FAULT_FREE;
            }
            for (Fault reason: reasons)
            {
                if ((fault & reason.fault) != 0)
                {
                    return reason;
                }
            }
            final String message = new StringBuilder().append("getFault(): ")
                                                      .append(fault)
                                                      .append(" - Unbekannter Fehlercode!")
                                                      .toString();
            throw new RuntimeException(message);
        }
        
    }
    
    /**
     * Wandlung byte => String... (zu Protokollzwecken)
     * Anm.: Codemuster aus Internet...
     * @param byte
     * @return String
     */
    public static String byteToString(byte b) 
    {
        final byte[] masks = { -128, 64, 32, 16, 8, 4, 2, 1 };
        final StringBuilder builder = new StringBuilder();
        for (byte m: masks) 
        {
            builder.append(((b & m) == m)? '1' : '0');
        }
        return builder.toString();
    }    
}
