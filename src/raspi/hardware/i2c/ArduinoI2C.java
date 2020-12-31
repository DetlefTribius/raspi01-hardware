package raspi.hardware.i2c;

import java.io.IOException;

import com.pi4j.io.i2c.I2CDevice;

/**
 * Klasse ArduinoI2C 
 * <p>
 * 
 * </p>
 * @author Detlef Tribius
 */
public class ArduinoI2C extends I2C
{

    /**
     * Konstruktor, vgl. Basisklasse I2C
     * @param dev I2CDevice dev
     */
    public ArduinoI2C(I2CDevice dev)
    {
        super(dev);
    }
    
    /**
     * write(long token, Status status) - Uebrtragen der Parameter zum Arduino,
     * dabei von long token nur die 4 untersten Byte und der Status als 1 Byte.
     * @param token long Parameter
     * @param status Status, es wird die byte-Kennung uebertragen
     * @throws IOException 
     */
    public void write(long token, Status status) throws IOException
    {
        final byte buffer[] = {0, 0, 0, 0, 0};
        buffer[0] = (byte) (token & 0xff);
        token >>= 8;
        buffer[1] = (byte) (token & 0xff);
        token >>= 8;
        buffer[2] = (byte) (token & 0xff);
        token >>= 8;
        buffer[3] = (byte) (token & 0xff);
        buffer[4] = status.getStatus();
        dev.write(buffer);
    }
    
    /**
     * read() - Lesen der Antwort vom Arduino.
     * @return ArduinoI2C-Struktur mit der Antwort vom Arduino
     * @throws IOException
     */
    public ArduinoI2C.DataRequest read() throws IOException
    {
        // buffer mit 16 Byte initialisieren...
        final byte buffer[] = {0,0,0,0,0,0,0,0,
                               0,0,0,0,0,0,0,0};
        
        // number - number of bytes read
        final int numberRead = dev.read(buffer, 0, buffer.length);
        // Es sollten 16 Bytes vom Arduino gelesen werden.
        // Es wurden zumindestens 16 Bytes gesendet.
        // Notwendig sind mindestens 4 + 1 + 4 = 9 Bytes.
        // Testausgabe:
        // System.out.println("Raspi: " + numberRead + " Bytes vom Arduino gelesen." );
        if (numberRead < 9)
        {
            throw new RuntimeException("Fehler beim Lesen der Arduino-Daten!");
        }
        ///////////////////////////////////////////////////////////////////////////
        // buffer[0] ... buffer[3] gebildet aus einer 4 Byte FK Zahl ohne VZ,
        // => max. +4.294.967.295, Abbildung in Java als long-Zahl 
        // und daher immer positiv!
        // Ablage der FK Zahl mit dem oberen Teil in buffer[3] 
        // und den niederwertigsten Teil in buffer[0]. 
        // => Arduino und Raspi sind "little-endian"...
        final long token = ((((((((long)buffer[3]) & 0xff)<<8)
                              + (((long)buffer[2]) & 0xff))<<8)
                              + (((long)buffer[1]) & 0xff))<<8)
                              + (((long)buffer[0]) & 0xff);
        
        final Status status = Status.getStatus(buffer[4]);
        
        // buffer[5] ... buffer[8] gebildet aus einer 4 Byte ZK-Zahl
        // (in C als long vereinbart), Wandlung in Java als int-Zahl,
        // damit mit Betrachtung des VZ!
        final int value = ((((((((int)buffer[8]) & 0xff)<<8) 
                             + (((int)buffer[7]) & 0xff))<<8) 
                             + (((int)buffer[6]) & 0xff))<<8) 
                             + (((int)buffer[5]) & 0xff);
        
        return new ArduinoI2C.DataRequest(token, status, value);
    }
    
    /**
     * Status - Status der Raspberry-Arduino-Kommunikation
     * <p>
     * <ul>
     *  <li>INITIAL('I') - Initialisierung</li>
     *  <li>SUCCESS('S') - erfolgreiche Uebertragung</li>
     *  <li>ERROR('E') - Fehler</li>
     *  <li>NOP('N') - Keine Aktion</li>
     * </ul>
     * </p>
     * @author Detlef Tribius
     */
    public static enum Status
    {
        /**
         * INITIAL('I') - Initialisierung
         */
        INITIAL('I'),
        /**
         * SUCCESS('S') - erfolgreiche Uebertragung
         */
        SUCCESS('S'),
        /**
         * ERROR('E') - Fehler
         */
        ERROR('E'),
        /**
         * NOP('N') - Keine Aktion
         */
        NOP('N');
        
        /**
         * Status(char status) - Konversation
         * von char => byte, dabei Verlust des oberen Byte
         * und Ablage als 8-Bit byte!
         * @param status
         */
        private Status(char status)
        {
            this.status = (byte)status;
        }
        
        /**
         * Status-Kennung als 1 Byte
         */
        private final byte status;
        
        /**
         * getStatus() - liefert byte-Status
         * @return this.status
         */
        public byte getStatus()
        {
            return this.status;
        }
        
        /**
         * getStatus(byte status)
         * @param status
         * @return
         */
        public static Status getStatus(byte status)
        {
            for (Status value: Status.values())
            {
                if (value.status == status)
                {
                    return value;
                }
            }
            return null;
        }
    }

    /**
     * DataRequest - Datenklasse zur Zusammenfassung des
     * Request vom Arduino.
     * @author Detlef Tribius
     */
    public static class DataRequest
    {
        /**
         * token - long-Kennung
         */
        final private long token;
        /**
         * status - Statusinformation
         */
        final private Status status;
        /**
         * int-Value zum Token token
         */
        final private int value;
        /**
         * DataRequest(long token, Status status, int value) - Konstruktor
         * aus den Zustandsgroessen...
         * @param token
         * @param status
         * @param value
         */
        public DataRequest(long token, Status status, int value)
        {
            this.token = token;
            this.status = status;
            this.value = value;
        }
        
        /**
         * getToken() - liefert long Token
         * <p>
         * Im Token werden nur die unteren 4 Byte verwendet. 
         * </p>
         * @return the token (nur 4 Byte)
         */
        public final long getToken()
        {
            return token;
        }

        /**
         * getStatus() - liefert Statusinformation zur Kommunikation
         * Raspi-Arduino
         * @see Status
         * @return the status
         */
        public final Status getStatus()
        {
            return status;
        }

        /**
         * getValue() - liefert den uebertragenen int-Value (z.B. die Position)
         * @return the value
         */
        public final int getValue()
        {
            return value;
        }

        /**
         * toString() - zu Protokollzwecken...
         */
        @Override
        public String toString()
        {
            return new StringBuilder().append("[")
                                      .append(this.token)
                                      .append(" ")
                                      .append((this.status != null)? String.valueOf((char)this.status.getStatus()) 
                                                                   : "null")
                                      .append(" ")
                                      .append(this.value)
                                      .append("]")
                                      .toString();
        }
    }
}
