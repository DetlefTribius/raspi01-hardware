/**
 * 
 */
package raspi.hardware.i2c;
import java.io.IOException;

import com.pi4j.io.i2c.I2CDevice;

/**
 * I2C-Treiber für den Temperatursensor MCP9808.
 * 
 * <p>
 * Bearbeitung Detlef Tribius: 
 * <ul>
 * <li>Veraenderung des Exception-Handlings</li>
 * <li>Aufnahme des Logging</li>
 * </ul>
 * </p>
 * @author Wolfgang Höfer
 * 
 */
public class MCP9808  extends I2C
{
    /**
     * CONFIG - RW Konfigurationsregister
     */
    public static final int CONFIG = 0x01;
    /**
     * TUPPER - RW obere Grenze Alarmtemperatur
     */
    public static final int TUPPER = 0x02;
    /**
     * TLOWER - RW untere Grenze Alarmtemperatur
     */
    public static final int TLOWER = 0x03;
    /**
     * TCRIT - RW kritische Temperatur
     */
    public static final int TCRIT  = 0x04;
    /**
     * TEMPER - RW  Umgebungstemperatur
     */
    public static final int TEMPER = 0x05;
    /**
     * RESOL - RW Aufösung RES05 - RES00625
     */
    public static final int RESOL  = 0x08;

    // *** Aufloesung ***
    /**
     * Aufloesung 0,5°C
     */
    public static final int RES05    = 0x00;
    /**
     * Aufloesung 0,25°C
     */
    public static final int RES025   = 0x01;
    /**
     * Aufloesung 0,125°C
     */
    public static final int RES0125  = 0x10;
    /**
     * Aufloesung 0,0625°C
     */
    public static final int RES00625 = 0x11;

    // *** Hysterese ***
    /**
     * Hysterese = 0°C
     */
    public static final int HYST00 = 0x0;
    /**
     * Hysterese = 1,5°C
     */
    public static final int HYST15 = 0x1;
    /**
     * Hysterese = 3,0°C
     */
    public static final int HYST30 = 0x2;
    /**
     * Hysterese = 6,0°C
     */
    public static final int HYST60 = 0x3;

    public static final int  AlertOutputModeBit     = 0b0000_0001; //byte[1]
    public static final int  AlertOutputPolarityBit = 0b0000_0010; //byte[1]
    public static final int  AlertOutputSelectBit   = 0b0000_0100; //byte[1]
    public static final int  AlertOutputControlBit  = 0b0000_1000; //byte[1]
    public static final int  AlertOutputStatusBit   = 0b0001_0000; //byte[1]
    public static final int  InterruptClearBit      = 0b0010_0000; //byte[1]
    public static final int  WindowLockBit          = 0b0100_0000; //byte[1]
    public static final int  TCritLockBit           = 0b1000_0000; //byte[1]

    public static final int  ShutdownModeBit        = 0b0000_0001; //byte[0]


    public MCP9808(final I2CDevice dev)
    {
        super(dev);
    }

    /**
     * Konvertiert eine Temperaturangabe aus einem 2 Byte langen Array in einen double Wert. 
     * Das Format im Array muss MCP9808-Kompatibel sein.<br>
     * 16 15 14 13  12  11  10  09  08  07  06  05  04   03   02   01<br>
     * -  -  -  SGN 2^7 2^6 2^5 2^4 2^3 2^2 2^1 2^0 2^-1 2^-2 2^-3 2^-4<br>
     * byte[0]......                byte[1].......<br>
     * 
     * @param reg Temperaturangabe im MCO9808-Format
     * @return Temperatur als double-Wert
     */
    public double convertTemp(byte[] reg)
    {
        double nachkomma    = (reg[1] & (byte)0x0F)/16d;
        double vorkommaLow  = (double)((reg[1] & 0xF0) >> 4);
        double vorkommaHigh = (double)((reg[0] & 0x0F) << 4);
        double temp = vorkommaHigh + vorkommaLow + nachkomma;
        double sign = 1.0;
        if((reg[0] & 0b10000) == 0b10000)
        {
            sign = -1.0;
        }  
        return temp * sign;
    }

    /**
     * Konvertiert einen double-Wert, der eine Temperaturangabe repräsentiert, in ein 
     * 2 Byte langes Array. Das Format entspricht dem im MCP9808 benötigten Temperaturformat.<br>
     * 16 15 14 13  12  11  10  09  08  07  06  05  04   03   02   01<br>
     * -  -  -  SGN 2^7 2^6 2^5 2^4 2^3 2^2 2^1 2^0 2^-1 2^-2 2^-3 2^-4<br>
     * byte[0]......                byte[1].......<br>
     *
     * @param value Temperaturangabe als double-Wert
     * @return Temperaturangabe im MCO9808-Format
     */
    public static byte[] convertTempToReg(double value)
    {
        final byte[] reg = {(byte)0,(byte)0};
        int sign = 0b0001_0000;
        boolean pos = true;
        if(value < -40.0d)
        {
            value = -40.0d;
        }
        if(value > 125.0d)
        {
            value = 125.0d;
        } 
        if(value < 0.0d)
        {
            value = -value;
            pos = false;
        }    
        int h = (int)value;
        reg[0] = (byte)((h >> 4) & 0x0F);
        reg[1] = (byte)((h << 4) & 0xF0);
        int nachkomma = (int)((value % h) * 16d) ;
        reg[1] =  (byte)((reg[1] | nachkomma) & 0xFF);
        if(!pos)
        {
            reg[0] =  (byte)(reg[0] | sign);
        }  
        return reg;
    }

    /**
     * getAmbientTemp fragt die Umgebungstemperatur ab.
     *
     * @return Temperatur in °C
     * @throws IOException 
     */
    public double getAmbientTemp() throws IOException
    {
        final byte[] temp = {0,0};
        readArray(TEMPER, temp, 2);
        return convertTemp(temp);
    }

    /**
     * isAmbientGreaterEqualCrit erfragt, ob die Umgebungstemperatur 
     * größer/gleich der kritischen Temperatur ist.
     * Gibt true zurück wenn die Umgebungstemperatur 
     * größer/gleich der kritischen Temperatur ist.
     *
     * @return boolean
     * @throws IOException 
     * 
     */
    public boolean isAmbientGreaterEqualCrit() throws IOException
    {
        final byte[] temp = {0,0};
        readArray(TEMPER, temp, 2);
        return (temp[0] & 0b1000_0000) == 0b1000_0000;
    }

    /**
     * getCritTemp erfragt den Vorgabewert der kritischen Temperatur ab.
     *
     * @return Temperatur in °C
     * @throws IOException 
     */
    public double getCritTemp() throws IOException
    {
        final byte[] temp = {0,0};
        readArray(TCRIT, temp, 2);
        return convertTemp(temp);
    }

    /**
     * setCritTemp setzt den Vorgabewert für die kritische Temperatur.
     *
     * @param dTemp Temperatur in °C
     * @throws IOException 
     */
    public void setCritTemp(double dTemp) throws IOException
    {
        final byte[] temp = convertTempToReg(dTemp);
        writeArray(TCRIT, temp,2);
    }

    /**
     * getUpperTemp erfragt den Vorgabewert der oberen Temperaturgrenze ab.
     *
     * @return Temperatur in °C
     * @throws IOException 
     */
    public double getUpperTemp() throws IOException
    {
        final byte[] temp = {0,0};
        readArray(TUPPER, temp, 2);
        return convertTemp(temp);
    }

    /**
     * setUpperTemp setzt den Vorgabewert für die obere Temperaturgrenze.
     *
     * @param dTemp Temperatur in °C
     * @throws IOException 
     */
    public void setUpperTemp(double dTemp) throws IOException
    {
        byte[] temp = {0,0};
        temp = convertTempToReg(dTemp);
        writeArray(TUPPER, temp, 2);
    }

    /**
     * isAmbientGreaterUpperBoundary erfragt, ob die Umgebungstemperatur
     * größer als die obere Temperaturgrenze ist.
     *
     * @return boolean
     * @throws IOException 
     */
    public boolean isAmbientGreaterUpperBoundary() throws IOException
    {
        byte[] temp = {0,0};
        readArray(TEMPER, temp, 2);
        return (temp[0] & 0b0100_0000) == 0b0100_0000;
    }

    /**
     * getLowerTemp erfragt den Vorgabewert die untere Temperaturgrenze ab.
     *
     * @return Temperatur in °C
     * @throws IOException 
     */
    public double getLowerTemp() throws IOException
    {
        byte[] temp = {0,0};
        readArray(TLOWER, temp, 2);
        return convertTemp(temp);
    }

    /**
     * setLowerTemp setzt den Vorgabewert für die untere Temperaturgrenze.
     *
     * @param dTemp Temperatur in °C
     * @throws IOException 
     */
    public void setLowerTemp(double dTemp) throws IOException
    {
        final byte[] temp = convertTempToReg(dTemp);
        writeArray(TLOWER, temp, 2);
    }

    /**
     * isAmbientLessLowerBoundary() erfragt, ob die Umgebungstemperatur
     * kleiner als die untere Temperaturgrenze ist.
     *
     * @return boolean
     * @throws IOException 
     */
    public boolean isAmbientLessLowerBoundary() throws IOException
    {
        final byte[] temp = {0,0};
        readArray(TEMPER, temp, 2);
        return (temp[0] & 0b0010_0000) == 0b0010_0000;
    }


    /**
     * configComparatorMode() aktiviert den Alert-Ausgang im Komparator-Modus.
     * Mit activeHigh = true/false wird der Alert-Ausgang High-/Low-Aktiv gesetzt.
     * Mit alertOnlyCrit = true reagiert der Alert-Ausgang nur auf das Überschreiten
     * der kritischen Temperatur.
     *
     * @param activeHigh Alert-Ausgang Activ-High/Activ-Low 
     * @param alertOnlyCrit Alert nur bei Überschreitung der kritischen Temperatur
     * @throws IOException 
     */
    public void configComparatorMode(boolean activeHigh, boolean alertOnlyCrit) throws IOException
    {
        byte[] config = {0,0};
        readArray(CONFIG, config, 2);
        
        int conf = config[1];                           //Bit 1...8
        conf = setBit(conf, AlertOutputControlBit, 1);  //Alert Output enabled
        conf = setBit(conf, AlertOutputModeBit, 0);     //Mode = Comparator
        
        // activeHigh gesetzt => Alert Output = activ-high
        //     sonst          => Alert Output = activ-low
        conf = setBit(conf, AlertOutputPolarityBit, (activeHigh? 1 : 0));
        
        // alertOnlyCrit gesetzt => Alarmmeldung nur wenn Umgebungstemperatur > kritische Temparatur /
        //      sonst            => Alarmmeldung bei TUpper, TLower and TCrit
        conf = setBit(conf, AlertOutputSelectBit, (alertOnlyCrit? 1 : 0));
        
        config[1] = (byte)conf;
        
        writeArray(CONFIG, config, 2);
    }

    /**
     * alertOutputDisable() deaktiviert den Alert-Ausgang
     *
     * @throws IOException 
     */
    public void alertOutputDisable() throws IOException
    {
        final byte[] config = {0,0};
        readArray(CONFIG, config, 2);
        // Bit 1...8
        int conf = config[1];
        // Alert Output disabled
        conf = setBit(conf, AlertOutputControlBit, 0); 
        config[1] = (byte)conf;
        writeArray(CONFIG, config, 2);
    }

    /**
     * isAlertOutputStatus() erfragt ob der Alertstatus auf true gesetzt wurde.
     *
     * @return Status
     * 
     * @throws IOException 
     */
    public boolean isAlertOutputStatus() throws IOException
    {
        final byte[] config = {0,0};
        readArray(CONFIG, config, 2);
        int conf = config[1];
        return isHigh(conf, AlertOutputStatusBit);
    }

    /**
     * setHysteresis() setzt die Hysterese für alle Alarmtemperaturen.
     * <p> 
     * Die folgenden Konstanten können verwendet werden:
     * </p>
     * <p>
     * <ul>
     * <li>MCP9808.HYST00 = 0°C</li>
     * <li>MCP9808.HYST15 = 1,5°C</li>
     * <li>MCP9808.HYST30 = 3,0°C</li>
     * <li>MCP9808.HYST60 = 6,0°C</li>
     * </ul>
     * </p>
     * 
     * @param hyst Wert für Hysterese
     * @throws IOException
     */
    public void setHysteresis(int hyst) throws IOException
    {
        final byte[] config = {0,0};
        readArray(CONFIG, config, 2);
        int conf = config[0];
        switch(hyst)
        {
            case HYST00:
            {  //Hysterese = 0°C
                conf = setBit(conf, 0b0000_0010, 0); 
                conf = setBit(conf, 0b0000_0100, 0); 
                break;
            }
            case HYST15:
            {  //Hysterese = 1,5°C
                conf = setBit(conf, 0b0000_0010, 1); 
                conf = setBit(conf, 0b0000_0100, 0); 
                break;
            }
            case HYST30:
            {  //Hysterese = 3,0°C
                conf = setBit(conf, 0b0000_0010, 0); 
                conf = setBit(conf, 0b0000_0100, 1); 
                break;
            }
            case HYST60:
            {  //Hysterese = 6,0°C
                conf = setBit(conf, 0b0000_0010, 1); 
                conf = setBit(conf, 0b0000_0100, 1); 
                break;
            }
            default:
            {
                // Runtimeexception?
                return;
            }

        }
        config[0] = (byte)conf;
        writeArray(CONFIG, config, 2);
    }

    /**
     * setResultion() legt die Auflösung der gemessenen Umgebungstemperatur fest.
     * <p>
     * Mit den folgenden Konstanten werden die Verschiedenen Auflösungen eingestellt.
     * </p>
     * <p>
     * <ul>
     * <li>MCP9808.RES05     -> 0,5°C</li>
     * <li>MCP9808.RES025    -> 0,25°C</li>
     * <li>MCP9808.RES01255  -> 0,125°C</li>
     * <li>MCP9808.RES006255 -> 0,0625°C</li>
     * </ul>
     * </p>
     *
     * @param res Wert zum Einstellen der Hysterese
     * @throws IOException 
     */
    public void setResultion(int res) throws IOException
    {
        int conf = read(RESOL);
        switch(res)
        {
            case RES05:
            {  //Auflösung = 0,5°C
                conf = setBit(conf, 0b0000_0001, 0); 
                conf = setBit(conf, 0b0000_0010, 0); 
                break;
            }
            case RES025:
            {  //Auflösung = 0,25°C
                conf = setBit(conf, 0b0000_0001, 1); 
                conf = setBit(conf, 0b0000_0010, 0); 
                break;
            }
            case RES0125:
            {  //Auflösung = 0,125°C
                conf = setBit(conf, 0b0000_0001, 0); 
                conf = setBit(conf, 0b0000_0010, 1); 
                break;
            }
            case RES00625:
            {  //Auflösung = 0,0625°C
                conf = setBit(conf, 0b0000_0001, 1); 
                conf = setBit(conf, 0b0000_0010, 1); 
                break;
            }
            default:
            {
                // RuntimeException??
                return;
            }

        }
        write(RESOL, (byte)conf);
    }

    /**
     * setShutdownMode() setzt den MCP9808 in den Shutdown Modus.
     * @throws IOException 
     */
    public void setShutdownMode() throws IOException
    {
        final byte[] config = {0,0};
        readArray(CONFIG, config, 2);
        int conf = config[0];
        config[0] = (byte)setBit(conf, ShutdownModeBit, 1); 
        writeArray(CONFIG, config, 2);
    }

    /**
     * setActiveMode() setzt den MCP9808 wird in den aktiven Modus.
     * @throws IOException 
     */
    public void setActiveMode() throws IOException
    {
        final byte[] config = {0,0};
        readArray(CONFIG, config, 2);
        int conf = config[0];
        config[0] = (byte)setBit(conf, ShutdownModeBit, 0); 
        writeArray(CONFIG, config, 2);
    }


    /**
     * reset() setzt das Konfigurationsregister auf Defaulteinstellung.
     * @throws IOException 
     */
    public void reset() throws IOException
    {
        final byte[] config = {0,0};
        writeArray(CONFIG, config, 2);
    }

}