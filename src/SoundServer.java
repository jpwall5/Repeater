
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;




public class SoundServer implements Runnable {
    
    Repeater repeater = null;
    AudioInputStream audioInputStream;
    TargetDataLine lineIN;
    SourceDataLine lineOUT;
    File file;
    ByteArrayOutputStream out;
    AudioFormat format;
    AudioFormat formatLow;
    AudioFormat formatHigh;
    Date date;
    
    
    boolean Alive = true;
    
    String errStr;
    double duration, seconds;
    byte[] data;
    final int bufSize = 16384;
    int frameSizeInBytes ;
    int bufferLengthInFrames;
    int bufferLengthInBytes;
    int numBytesRead;
    
    public int PBarCurrentLevel = -1;
    public int SliderRecON = -1;
    public int SliderRecOFF = -1;
    public int SliderRecOFFDelay = -1;
    public int SliderRecTime = -1;
    public int SliderRecPlayDelay = -1;
    
    public GpioController gpio;
    public GpioPinDigitalOutput gpio1;
    
   

    
    public SoundServer(Repeater rep, int RecON, int RecOFF, int RecOFFDelay, int RecTime, int RecPlayDelay) {    
        
        repeater = rep;
        SliderRecON = RecON;
        SliderRecOFF = RecOFF;
        SliderRecOFFDelay = RecOFFDelay;
        SliderRecTime = RecTime;
        SliderRecPlayDelay = RecPlayDelay;
        duration = 0;
        audioInputStream = null;
        
        gpio = GpioFactory.getInstance();
        gpio1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
        //led1.high();

        // define the required attributes for our line,
        // and make sure a compatible line is supported.
        //AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        //float rate = 44100.0f;
        //float rate = 48000.0f;
        //int sampleSize = 8;
        //int channels = 1;
        //int frameSize = 4;
        //boolean bigEndian = true;
        //boolean notbigEndian = false;
        
        try {
            //format = new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8) * channels, rate, bigEndian);
            //format = new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8) * channels, rate, notbigEndian);
            formatLow = new AudioFormat(8000.0f, 16, 1, true, false); //(sampleRate, sampleInbits, channels, signed, bigEndian);
            formatHigh = new AudioFormat(44100.0f, 16, 1, true, true);
        } 
        catch (Exception ex) { } 
        
        if (AudioSystem.isLineSupported(new DataLine.Info(TargetDataLine.class, formatHigh))) {
            format = formatHigh;
            System.out.println("Line formatHigh Supported");
        }
        else if (AudioSystem.isLineSupported(new DataLine.Info(TargetDataLine.class, formatLow))) {
            format = formatLow;
            System.out.println("Line formatLow Supported");
        }
        else{ 
            shutDown("Line matching not supported.");
            return;
        }
        
        
        //Line IN
        DataLine.Info infoIN = new DataLine.Info(TargetDataLine.class, format);
        /*if (!AudioSystem.isLineSupported(infoIN)) {
            shutDown("Line matching " + infoIN + " not supported.");
            return;
        }*/
        try {
          lineIN = (TargetDataLine) AudioSystem.getLine(infoIN);
          lineIN.open(format, lineIN.getBufferSize());
        } 
        catch (LineUnavailableException ex) { shutDown("Unable to open the line: " + ex); return; } 
        catch (SecurityException ex) { shutDown(ex.toString()); return; } 
        catch (Exception ex) { shutDown(ex.toString()); return; }
        
        
        //Line OUT
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        /*if (!AudioSystem.isLineSupported(info)) {
            shutDown("Line matching " + info + " not supported.");
            return;
        }*/
        try {
            lineOUT = (SourceDataLine) AudioSystem.getLine(info);
            //lineOUT.open(format, bufSize);
            lineOUT.open(format, lineOUT.getBufferSize());
        } 
        catch (LineUnavailableException ex) { shutDown("Unable to open the line: " + ex); return; }
        

        //out = new ByteArrayOutputStream();
        frameSizeInBytes = format.getFrameSize();
        bufferLengthInFrames = lineIN.getBufferSize() / 8;
        bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
        data = new byte[bufferLengthInBytes];
        
        //this.setLevel(50);
    }


    public void shutDown(String message) {
        Alive = false;
        System.err.println(message);
      }
    
    public int calculateRMSLevel(byte[] audioData) {
        // audioData might be buffered data read from a data line
        long lSum = 0;
        for (int i = 0; i < audioData.length; i++) {
            lSum = lSum + audioData[i];
        }

        double dAvg = lSum / audioData.length;

        double sumMeanSquare = 0d;
        for (int j = 0; j < audioData.length; j++) {
            sumMeanSquare = sumMeanSquare + Math.pow(audioData[j] - dAvg, 2d);
        }

        double averageMeanSquare = sumMeanSquare / audioData.length;
        return (int) (Math.pow(averageMeanSquare, 0.5d) + 0.5);

    }
    
    public int calculateRMSLevel1(byte[] audioData) {
        // audioData might be buffered data read from a data line
        long lSum = 0;
        for (int i = 0; i < audioData.length; i++) {
            lSum = lSum + audioData[i];
        }

        double dAvg = lSum / audioData.length;

        double sumMeanSquare = 0d;
        for (int j = 0; j < audioData.length; j++) {
            sumMeanSquare = sumMeanSquare + Math.pow(audioData[j] - dAvg, 2d);
        }
        
        double averageMeanSquare = sumMeanSquare / audioData.length;
        double rootMeanSquare = Math.sqrt(averageMeanSquare);
        return (int)rootMeanSquare;
        
    }
    
    public void setLevel(int lvl){
        if (repeater != null){
            repeater.setLevel(lvl);
        }
    }
    
    

    
    public void run() {

        lineIN.start();
        try {
            Thread.sleep(2000);
        } 
        catch (Exception ex) { ex.printStackTrace(); }
        

        while (Alive) {
            
            int RMSLevel = 0;
            out = new ByteArrayOutputStream();

            //Record
            while(Alive && RMSLevel < SliderRecON){
                if ((numBytesRead = lineIN.read(data, 0, bufferLengthInBytes)) == -1) {
                    break;
                }
                RMSLevel = calculateRMSLevel(data);
                this.setLevel(RMSLevel);
                //if(RMSLevel >= SliderRecON) {
                    System.out.println("SoundLevel - RMS > " + RMSLevel );
                //}
            }
            out.write(data, 0, numBytesRead);
            

            //Timer
            if(Alive) {
                long StartRec = new Date().getTime();
                System.out.println("Start Recording -> " + StartRec);
                int RMS_Low = 0;
                while(Alive && (new Date().getTime() - StartRec) < (SliderRecTime * 1000) && RMS_Low < SliderRecOFFDelay){
                //do {
                    //System.out.println(" Enter record -> ");
                    if ((numBytesRead = lineIN.read(data, 0, bufferLengthInBytes)) == -1) {
                        System.out.println("Recording break -> " );
                        break;
                    }
                    RMSLevel = calculateRMSLevel(data);
                    this.setLevel(RMSLevel);
                    System.out.println("Recording - RMS > " + RMSLevel);
                    if(RMSLevel >= SliderRecOFF){
                        RMS_Low = 0;
                        out.write(data, 0, numBytesRead);
                    }
                    else {
                        RMS_Low++;
                        out.write(data, 0, numBytesRead);
                    }
                }
                System.out.println("End Recording -> " + new Date().getTime());
            }
            
            if(Alive) {
                try {
                    out.flush();
                } 
                catch (IOException ex) { ex.printStackTrace(); }

                byte audioBytes[] = out.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
                audioInputStream = new AudioInputStream(bais, format, audioBytes.length / frameSizeInBytes);

                /*long milliseconds = (long) ((audioInputStream.getFrameLength() * 1000) / format.getFrameRate());
                duration = milliseconds / 1000.0;*/

                //Delay beteween Rec and Play
                try {
                    Thread.sleep(SliderRecPlayDelay * 1000);
                } 
                catch (Exception ex) { ex.printStackTrace(); }
            
            }

            if(Alive) {
                //Playback
                gpio1.high();
                if (audioInputStream == null) {
                    shutDown("No loaded audio to play back");
                    return;
                }
                // reset to the beginnning of the stream
                try {
                    audioInputStream.reset();
                } 
                catch (Exception e) { shutDown("Unable to reset the stream\n" + e); }

                AudioInputStream playbackInputStream = AudioSystem.getAudioInputStream(format, audioInputStream);

                if (playbackInputStream == null) {
                    shutDown("Unable to convert stream of format " + audioInputStream + " to format " + format);
                }

                // start the source data line
                lineOUT.start();

                while (Alive) {
                    
                    try {
                        if ((numBytesRead = playbackInputStream.read(data)) == -1) {
                          break;
                        }
                        System.out.println("Playback - RMS > " + calculateRMSLevel(data));
                        int numBytesRemaining = numBytesRead;
                        while (Alive && numBytesRemaining > 0) {
                            numBytesRemaining -= lineOUT.write(data, 0, numBytesRemaining);
                        }
                    } 
                    catch (Exception e) { shutDown("Error during playback: " + e); break; }
                }
                lineOUT.drain();
                lineOUT.stop();
                gpio1.low();
                try {
                    Thread.sleep(1000);
                } 
                catch (Exception ex) { ex.printStackTrace(); }
            }

        }
        
        try {
            lineIN.close();
            lineOUT.close();
            out.close();
            gpio.shutdown();
        } 
        catch (IOException ex) { }
    } 
    



    public static void main(String args[]) {
        //rep, RecON, RecOFF, RecOFFDelay, RecTime, RecPlayDelay)
        SoundServer ss = new SoundServer(null, 50, 20, 1, 10, 2);
        //SoundServer ss = new SoundServer(null, 5, 2, 1, 10, 2);
        ss.run();
    }
    
}