/**
 * 
 */
package raspi.hardware.i2c;

import  com.pi4j.io.i2c.*;

import  java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * I2C ist eine Klasse, die grundlegende Schreib-, Lese- und Bitoperationen fuer das 
 * I2C-Hardware-Interface unterstuetzt. Sie ist als Elternklasse für I2C-Geraete gedacht.
 * <p>
 * Uebrnahme aus der Literatur 'Raspberry Pi programmieren mit Java', Autor Wolfgang Hoefer,
 * Bearbeitung Detlef Tribius, insbesondere: 
 * <ul>
 * <li>Veraenderung des Exception-Handlings</li>
 * <li>Aufnahme des Logging <code>org.slf4j.Logger</code></li>
 * </ul>
 * </p>
 * @author Wolfgang Höfer
 */
public class I2C
{
    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(I2C.class);
    
    /**
     * I2CDevice
     */
     public final I2CDevice dev;
     
    /**
     * Constructor for objects of class I2C
     * <p>
     * Der Parameter <code>I2CDevice dev</code> muss beispielsweise zuvor ueber
     * </p>
     * <p><code>
     * I2CBus bus1 = I2CFactory.getInstance(I2CBus.BUS_1);<br>
     * dev = bus1.getDevice(0x18);<br>
     * </code></p>
     * <p>
     * erzeugt werden...
     * </p>
     * 
     * @param I2CDevice dev
     */
    public I2C(final I2CDevice dev)
    {
        this.dev = dev;
        logger.debug("I2C instanziiert...");
    }

    /**
     * setBit() setzt an die durch bitpos bestimmte Stelle in reg das Bit auf
     * 1 oder 0. Für bitpos muss zum Beispiel für Bit 5 der Wert 0b0001_0000
     * bzw. 16 übergeben werden. 
     * <p>Die Methode greift nicht auf ein I2C-Gerät zu. </p>
     *
     * @param reg Register, in dem das Bit geändert werden soll.
     * @param bitpos Bitposition
     * @param level Setzt Bit auf 0 oder 1
     * @return Gibt das geänderte Register zurück
     */
    public int setBit(int reg, int bitpos, int level)
    {
        if(level == 0)
        {
            reg =  reg & (~bitpos);
        }
        else if(level == 1)
        {
            reg =  reg | bitpos;
        }
        return reg;
    }

    /**
     * getBit() holt aus dem Register reg den Bitwert an der Stelle bitpos.
     * Für bitpos muss zum Beispiel für Bit 5 der Wert 0b0001_0000 bzw 16
     * übergeben werden.
     * 
     * <p>Die Methode greift nicht auf ein I2C-Gerät zu.</p>
     * 
     * @param reg Register
     * @param bitpos Bitposition
     * @return Bitwert 0/1
     */
    public int getBit(int reg, int bitpos)
    {
        // Wenn (reg | bitpos) == reg), 
        // dann muss reg an der bitpos gleich 1 sein!
        return ((reg | bitpos) == reg)? 1 : 0;
    }

    /**
     * isBit() liefert für ein gesetztes Bit aus dem Register reg den Wert true sonst false. 
     * Der Bitwert aus dem Register reg wird an der Stelle bitpos ausgelesen.Für bitpos 
     * muss zum Beispiel für Bit 5 der Wert 0b0001_0000 bzw 16 übergeben werden. 
     * 
     * <p>Die Methode greift nicht auf ein I2C-Gerät zu.</p>
     *
     * @param reg Register
     * @param bitpos Bitposition
     * @return true für ein gesetztes Bit sonst false
     */
    public boolean isBit(int reg, int bitpos)
    {
        final int val = getBit(reg, bitpos);
        return (val == 1);
    }

    
    /**
     * read() liest das Register reg eines I2C-Gerätes und gibt den Inhalt zurück.
     * Die Methode kann bis zu 3 Byte lange Register auslesen. 
     * 
     * @param reg Register
     * @return Inhalt des Registers reg
     * @throws IOException Fehler bei dev.read(reg)
     */
    public int read(int reg) throws IOException
    {
        try
        {
            return dev.read(reg);
        }
        catch(IOException exception)
        {
            final String msg = new StringBuilder().append("Lesefehler bei I2C aus Register ")
                                                  .append(reg)
                                                  .toString();
            logger.error(msg, exception);
            
            throw new IOException(msg);
        }
    }    

    /**
     * readArray() liest aus dem Register reg  eines I2C-Gerätes die Anzahl Bytes, 
     * die durch size angegeben wird und speichert diese in das Byte-Array array. 
     *
     * @param reg Register
     * @param array Byte-Array
     * @param size Anzahl Byte, die gelesen werden sollen
     * @return Byte-Array
     * @throws IOException 
     */
    public byte[] readArray(int reg, byte[] array, int size) throws IOException
    {
        boolean success = false;
        try
        {
            final int count = dev.read(reg, array, 0, size);
            success = (count == size);
        }
        catch(IOException exception)
        {
            final String msg = new StringBuilder().append("Lesefehler bei I2C aus Register ")
                                                  .append(reg)
                                                  .toString();
            logger.error(msg, exception);

            throw new IOException(msg);
        }
        if (!success)
        {
            final String msg = new StringBuilder().append("Lesefehler bei I2C aus Register ")
                                                  .append(reg)
                                                  .append(": Es wurden nicht so viele Bytes gelesen, wie angeforder wurden!")
                                                  .toString();
            logger.error(msg);
            
            throw new IOException(msg);
        }
        return array;
    }    

    
    /**
     * writeArray() schreibt den Inhalt aus dem Byte-Array array in das Register
     * reg eines I2C-Gerätes. Die Anzahl der zu schreibenden Bytes steht in size. 
     * Im Fehlerfall wird <code>IOException()</code> geworfen.
     *
     * @param reg Register
     * @param array Byte-Array, das in das Regsietr geschrieben wird.
     * @param size Anzahl zu schriebender Bytes.
     * @throws IOException 
     */
    public void writeArray(int reg, byte[] array, int size) throws IOException
    {
        try
        {
            dev.write(reg, array, 0, size);
        }
        catch(IOException exception)
        {
            final String msg = new StringBuilder().append("Schreibfehler bei I2C-Gerät nach Register: ")
                                                  .append(reg)
                                                  .toString();
            logger.error(msg, exception);

            throw new IOException(msg);
        }
    }    
    
    /**
     * readPin liest das Register reg des I2C-Gerätes und gibt das Bit bitpos zurück.
     * Für bitpos muss zum Beispiel für Bit 5 der Wert 0b0001_0000 bzw. 16
     * übergeben werden. Im Fehlerfall wird eine <code>IOException</code> geworfen.
     * 
     * @param reg Register
     * @param bitpos Bitposition
     * @return Bitwert an der vorgegebenen Bitposition
     * @throws IOException 
     */
    public int readPin(int reg, int bitpos) throws IOException
    {
        final int regVal = read(reg);
        return getBit(regVal, bitpos);
    }    

    
    /**
     * isHigh liest das Register reg des I2C-Gerätes und prüft das Bit an der Stelle
     * bitpos. Gibt true zurück, wenn das Bit gesetzt ist, sonst false.
     * Für bitpos muss zum Beispiel für Bit 5 der Wert 0b0001_0000 bzw 16
     * übergeben werden.
     *
     * @param reg Register
     * @param bitpos Bitposition
     * @return true für gesetzt, sonst false
     * @throws IOException 
     */
    public boolean isHigh(int reg, int bitpos) throws IOException
    {
        final int val = readPin(reg, bitpos);
        return (val == 1);
    }    

    /**
     * isLow liest das Register reg des I2C-Gerätes und prüft das Bit an der Stelle
     * bitpos. Gibt true zurück, wenn das Bit nicht gesetzt ist, sonst false.
     * Für bitpos muss zum Beispiel für Bit 5 der Wert 0b0001_0000 bzw 16
     * übergeben werden.
     *
     * @param reg Register
     * @param bitpos Bitposition
     * @return true für nicht gesetzt, sonst false
     * @throws IOException 
     */
    public boolean isLow(int reg, int bitpos) throws IOException
    {
        return (isHigh(reg, bitpos))? false : true;
    }    

    /**
     * write schreibt in das Register reg des I2C-Gerätes den Wert val.
     * Im Fehlerfall wird eine <code>IOException</code> geworfen.
     *
     * @param reg Register
     * @param val Wert
     * @throws IOException 
     */
    public void write(int reg, byte val) throws IOException
    {
        try
        {
            dev.write(reg, val);
        }
        catch(IOException exception)
        {
            final String msg = new StringBuilder().append("Schreibfehler bei I2C-Gerät nach Register: ")
                                                  .append(reg)
                                                  .append(" und Wert ")
                                                  .append(val)
                                                  .toString();
            logger.error(msg, exception);

            throw new IOException(msg);
        }
    }    

    /**
     * configPin liest von dem I2C-Gerät das Register reg und ändert
     * an der Stelle bitpos den Bitwert auf level und schreibt die
     * Daten wieder in das Register des I2C-Gerätes.
     * Für bitpos muss zum Beispiel für Bit 5 der Wert 0b0001_0000 bzw 16
     * übergeben werden.
     * Im Fehlerfall wird eine <code>IOException</code> geworfen.
     *
     * @param reg Register
     * @param level Bitwert
     * @param bitpos Bitposition
     * @throws IOException 
     */
    public void configPin(int reg, int level, int bitpos) throws IOException
    {
        int regVal = read(reg);
        regVal = setBit(regVal, bitpos, level);
        write(reg, (byte)regVal);        
    }    

    /**
     * configPin liest von dem I2C-Gerät das Register regRead und ändert
     * an der Stelle bitpos den Bitwert auf level und schreibt die
     * Daten in das Register regWrite des I2C-Gerätes.
     * Für bitpos muss zum Beispiel für Bit 5 der Wert 0b0001_0000 bzw 16
     * übergeben werden.
     * Im Fehlerfall wird eine <code>IOException</code> geworfen.
     *
     * @param regRead Register, das ausgeslen wird
     * @param regWrite Register, in das die Änderungen geschrieben werden
     * @param Bitwert
     * @param bitpos Bitposition
     * @throws IOException 
     */
    public void configPin(int regRead, int regWrite, int level, int bitpos) throws IOException
    {
        int regVal = read(regRead);
        regVal = setBit(regVal, bitpos, level);
        write(regWrite, (byte)regVal);
    }    

    /**
     * configPinToggle liest von einem I2C-Gerät aus dem Register regRead, 
     * invertiert den Bitwert an der Stelle bitpos und schreibt die Daten 
     * in das Regsiter regWrite des I2C-Gerätes.
     * Für bitpos muss zum Beispiel für Bit 5 der Wert 0b0001_0000 bzw 16
     * übergeben werden.
     * Im Fehlerfall wird eine <code>IOException</code> geworfen.
     *
     * @param regRead Register, das ausgeslen wird
     * @param regWrite Register, in das die Änderungen geschrieben werden
     * @param bitpos Bitposition
     * @throws IOException 
     */
    public void configPinToggle(int regRead, int regWrite, int bitpos) throws IOException
    {
        int regVal = read(regRead);
        final int bitValue = getBit(regVal, bitpos);
        // Wenn bitValue 0 ist, dann auf 1 setzen und umgekehrt...
        regVal = setBit(regVal, bitpos, ((bitValue == 1)? 0 : 1));
        write(regWrite, (byte)regVal);
    }    

}