
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;


public class PinTester implements Runnable {
        
    boolean Alive = true;
    String errStr;
    int mode = 0;

    public GpioController gpio;
    public GpioPinDigitalOutput led1;
       

    public PinTester(int mode) {    
        gpio = GpioFactory.getInstance();
        this.mode = mode;
    }


    public void shutDown(String message) {
        Alive = false;
        System.err.println(message);
    }
    
    
    public void run() {
        
        if(mode==0){
            led1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
            
            led1.high();
            while (Alive) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) { }
            }

            try {
                gpio.shutdown();
            } 
            catch (Exception ex) { }
        }
        
        if(mode==1){
            led1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
            
            try {
                led1.high();
                Thread.sleep(10000);
            } catch (Exception ex) { }
            led1.low();
            
            try {
                gpio.shutdown();
            } 
            catch (Exception ex) { }
        }
        
    } 
    



    public static void main(String args[]) {
        PinTester pt = new PinTester(1);
        pt.run();
    }
    
}