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
        long token = 0L;
        // index mit dem Index des HWT laden (HWT an Index 3)...
        int index = 3;
        while (index >= 0)      // ...bis einschliesslich Index 0. 
        {
            token <<= 8;        // untere 8 Bit frei-shiften...
            token += ((int)buffer[index--]) & 0xff;
        }
        // token ist bestimmt aus den unteren 4 Byte (von Index 0...3).
        // Da token als long eingefuehrt, ist token immer positiv!
        
        // Status ergibt sich aus dem byte[4]...
        final Status status = Status.getStatus(buffer[4]);
        
        // buffer[5] ... buffer[8] gebildet wird 
        // 1.) als int-Zahl interpretiert und damit mit Vorzeichen versehen
        //     (in C war die Zahl als long-Zahl vereinbart)
        // oder
        // 2.) als zwei nur positive Impulszahlen numberMA und numberMB angegeben.
        //     (in C waren die Zahlen jeweils uls unsigned int vereinbart).
        //
        // Die Wandlung wird zuerst in eine long Zahl data vorgenommen 
        // und dann jeweils in die Ergebnisvariablen umgewandelt...
        long data = 0;
        // index mit dem Index des HWT laden (HWT an Index 8)...
        index = 8;
        while (index >= 5)      // ...bis einschliesslich Index 5.
        {
            data <<= 8;        // untere 8 Bit frei-shiften...
            data += ((int)buffer[index--]) & 0xff;
        }
        // data ist bestimmt.
        
        final int value = (int) data & 0xffffffff;
        // data => value ist bestimmt aus den 4 Byte (von Index 5...8) mit VZ!
        
        final int numberMA =(int) ((data & 0xffff0000) >> 16);
        // data => numberMA ist bestimmt...
        
        final int numberMB = (int) (value & 0x0000ffff);
        // data => numberMB ist bestimmt... 
        
        return new ArduinoI2C.DataRequest(token, status, value, numberMA, numberMB);
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
         * numberMA - Impulsanzahl MA
         */
        final private int numberMA;
        
        /**
         * numberMB - Impulsanzahl MA
         */
        final private int numberMB;
        
        /**
         * DataRequest(long token, Status status, int value, int numberMA, int numberMB) - Konstruktor
         * aus den Zustandsgroessen...
         * @param token
         * @param status
         * @param value
         * @param numberMA
         * @param numberMB
         */
        public DataRequest(long token, 
                           Status status, 
                           int value,
                           int numberMA,
                           int numberMB)
        {
            this.token = token;
            this.status = status;
            this.value = value;
            this.numberMA = numberMA;
            this.numberMB = numberMB;
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
            return this.token;
        }

        /**
         * getStatus() - liefert Statusinformation zur Kommunikation
         * Raspi-Arduino
         * @see Status
         * @return the status
         */
        public final Status getStatus()
        {
            return this.status;
        }

        /**
         * getValue() - liefert den uebertragenen int-Value (z.B. die Position)
         * @return the value
         */
        public final int getValue()
        {
            return this.value;
        }

        /**
         * getNumberMA() - liefert die uebertragenen Impulsanzahl...
         * @return numberMA
         */
        public final int getNumberMA()
        {
            return this.numberMA;
        }
        
        /**
         * getNumberMB() - liefert die uebertragenen Impulsanzahl...
         * @return numberMB
         */
        public final int getNumberMB()
        {
            return this.numberMB;
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
                                      .append(" ")
                                      .append(this.numberMA)
                                      .append(" ")
                                      .append(this.numberMB)
                                      .append("]")
                                      .toString();
        }
    }
}
