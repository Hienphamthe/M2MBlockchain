/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TestPackage;

/**
 *
 * @author Mrhie
 */
public class AlternateSuspendResume extends Object implements Runnable {
    private static volatile int firstVal;
    private static volatile int secondVal;
    private volatile boolean suspended;
    @Override
    public void run() {
        try {
            suspended = false;
            firstVal = 0;
            secondVal = 0;
            workMethod();
        } catch (InterruptedException x) {
            System.out.println("interrupted in workMethod()");
        }
    }
    
    public boolean areValuesEqual() {
        return (firstVal == secondVal);
    }

    private void workMethod() throws InterruptedException {
        int val = 1;
        while (true) {
            // blocks if suspended is true
            waitWhileSuspended();

            stepOne(val);
            stepTwo(val);
            val++;

            // blocks if suspended is true
            waitWhileSuspended();

            Thread.sleep(200); // pause before looping again
        }
    }

    private void stepOne(int newVal) throws InterruptedException {
        firstVal = newVal;
        // simulate some other lengthy process
        Thread.sleep(300);
    }
    private void stepTwo(int newVal) {
        secondVal = newVal;
    }
    public void suspendRequest() {
        suspended = true;
    }
    public void resumeRequest() {
        suspended = false;
    }
    private void waitWhileSuspended() throws InterruptedException {
        while (suspended) {
          Thread.sleep(200);
        }
    }

    public static void main(String[] args) {
        AlternateSuspendResume asr = new AlternateSuspendResume();

        Thread t = new Thread(asr);
        t.start();

        try {
          Thread.sleep(1000);
        } catch (InterruptedException x) {
        }

        for (int i = 0; i < 5; i++) {
            asr.suspendRequest();
            try {
                Thread.sleep(350);
            } catch (InterruptedException x) {
            }
            System.out.println("dsr.areValuesEqual()=" + asr.areValuesEqual());
            System.out.println(i+" "+firstVal+" "+secondVal);
//            asr.resumeRequest();
            try {
                Thread.sleep((long) (Math.random() * 2000.0));
            } catch (InterruptedException x) {
            }
        }
        System.exit(0);
    }
}
